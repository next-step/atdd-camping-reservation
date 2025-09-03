package com.camping.legacy.acceptance;

import com.camping.legacy.support.TestBase;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("예약 시스템 인수 테스트")
class ReservationAcceptanceTest extends TestBase {

    // Given - 고객 정보 설정
    private CustomerInfo 김철수() {
        return new CustomerInfo("김철수", "010-1234-5678");
    }

    private CustomerInfo 이영희() {
        return new CustomerInfo("이영희", "010-2222-2222");
    }

    private CustomerInfo 기존예약자() {
        return new CustomerInfo("기존예약자", "010-0000-0000");
    }

    // Given - 날짜 설정
    private LocalDate 미래날짜(int days) {
        return LocalDate.now().plusDays(days);
    }

    private LocalDate 과거날짜(int days) {
        return LocalDate.now().minusDays(days);
    }

    // Given - 기존 예약 생성
    private ExtractableResponse<Response> 기존_예약_생성(String customerName, String phoneNumber, String siteId, LocalDate startDate, LocalDate endDate) {
        return createReservation(customerName, phoneNumber, siteId, startDate, endDate);
    }

    // When - 예약 요청
    private ExtractableResponse<Response> 예약_요청(CustomerInfo customer, String siteId, LocalDate startDate, LocalDate endDate) {
        return createReservation(customer.name, customer.phoneNumber, siteId, startDate, endDate);
    }

    // When - 동시 예약 요청
    private List<ExtractableResponse<Response>> 동시_예약_요청(CustomerInfo customer1, CustomerInfo customer2, String siteId, LocalDate startDate, LocalDate endDate) throws Exception {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        List<ExtractableResponse<Response>> responses = new ArrayList<>();

        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
            try {
                startLatch.await();
                ExtractableResponse<Response> response = 예약_요청(customer1, siteId, startDate, endDate);
                synchronized (responses) {
                    responses.add(response);
                }
                doneLatch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
            try {
                startLatch.await();
                ExtractableResponse<Response> response = 예약_요청(customer2, siteId, startDate, endDate);
                synchronized (responses) {
                    responses.add(response);
                }
                doneLatch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        startLatch.countDown();
        doneLatch.await();
        
        return responses;
    }

    // Then - 성공적인 예약 생성 검증
    private void 예약_생성_성공_검증(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        
        String confirmationCode = response.jsonPath().getString("confirmationCode");
        assertThat(confirmationCode).isNotNull().hasSize(6);
        
        assertThat(response.jsonPath().getString("status")).isEqualTo("CONFIRMED");
    }

    // Then - 예약 실패 검증
    private void 예약_실패_검증(ExtractableResponse<Response> response, HttpStatus expectedStatus, String expectedMessage) {
        assertThat(response.statusCode()).isEqualTo(expectedStatus.value());
        assertThat(response.jsonPath().getString("message")).contains(expectedMessage);
    }

    // Then - 동시성 결과 검증
    private void 동시성_결과_검증(List<ExtractableResponse<Response>> responses, String expectedConflictMessage) {
        long successCount = responses.stream()
                .mapToInt(ExtractableResponse::statusCode)
                .filter(status -> status == HttpStatus.CREATED.value())
                .count();
        assertThat(successCount).isEqualTo(1L);

        long conflictCount = responses.stream()
                .mapToInt(ExtractableResponse::statusCode)
                .filter(status -> status == HttpStatus.CONFLICT.value())
                .count();
        assertThat(conflictCount).isEqualTo(1L);

        ExtractableResponse<Response> conflictResponse = responses.stream()
                .filter(r -> r.statusCode() == HttpStatus.CONFLICT.value())
                .findFirst()
                .orElseThrow();
        assertThat(conflictResponse.jsonPath().getString("message")).contains(expectedConflictMessage);
    }

    // Then - 예약 취소 성공 검증
    private void 예약_취소_성공_검증(ExtractableResponse<Response> response, Long reservationId) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getString("message")).contains("취소");

        ExtractableResponse<Response> getResponse = getReservation(reservationId);
        assertThat(getResponse.jsonPath().getString("status")).isEqualTo("CANCELLED");
    }

    // Helper class for customer info
    private static class CustomerInfo {
        final String name;
        final String phoneNumber;

        CustomerInfo(String name, String phoneNumber) {
            this.name = name;
            this.phoneNumber = phoneNumber;
        }
    }

    @DisplayName("올바른 고객 정보와 날짜로 예약을 요청하면, 예약이 성공적으로 생성되고 6자리 확인코드가 발급된다")
    @Test
    void scenario_SuccessfulReservationCreation() {
        // Given: 고객 "김철수"가 5일 후부터 3일간 예약을 원한다
        CustomerInfo customer = 김철수();
        LocalDate startDate = 미래날짜(5);
        LocalDate endDate = startDate.plusDays(2);

        // When: 올바른 고객 정보와 날짜로 예약을 요청하면
        ExtractableResponse<Response> response = 예약_요청(customer, "A001", startDate, endDate);

        // Then: 예약이 성공적으로 생성되고 6자리 확인코드가 발급된다
        예약_생성_성공_검증(response);
    }

    @DisplayName("30일을 초과한 날짜로 예약을 시도하면, '30일 이내 예약만 가능합니다'는 오류 메시지를 받는다")
    @Test
    void scenario_ReservationExceedsDateLimit() {
        // Given: 고객 "김철수"가 35일 후 예약을 원한다
        CustomerInfo customer = 김철수();
        LocalDate startDate = 미래날짜(35);
        LocalDate endDate = startDate.plusDays(2);

        // When: 30일을 초과한 날짜로 예약을 시도하면
        ExtractableResponse<Response> response = 예약_요청(customer, "A001", startDate, endDate);

        // Then: 예약이 실패하고 "30일 이내 예약만 가능합니다" 오류 메시지를 받는다
        예약_실패_검증(response, HttpStatus.BAD_REQUEST, "30일");
    }

    @DisplayName("과거 날짜로 예약을 시도하면, '과거 날짜로 예약할 수 없습니다'는 오류 메시지를 받는다")
    @Test
    void scenario_PastDateReservationAttempt() {
        // Given: 고객 "김철수"가 과거 날짜로 예약을 원한다
        CustomerInfo customer = 김철수();
        LocalDate startDate = 과거날짜(1);
        LocalDate endDate = startDate.plusDays(2);

        // When: 과거 날짜로 예약을 시도하면
        ExtractableResponse<Response> response = 예약_요청(customer, "A001", startDate, endDate);

        // Then: 예약이 실패하고 "과거 날짜로 예약할 수 없습니다" 오류 메시지를 받는다
        예약_실패_검증(response, HttpStatus.BAD_REQUEST, "과거");
    }

    @DisplayName("두 고객이 동시에 같은 사이트와 기간을 예약 요청하면, 하나의 예약만 성공하고 나머지는 충돌 오류로 실패한다")
    @Test
    void scenario_ConcurrentReservationOnlyOneSucceeds() throws Exception {
        // Given: 김철수와 이영희가 같은 사이트와 기간에 예약을 원한다
        CustomerInfo customer1 = 김철수();
        CustomerInfo customer2 = 이영희();
        String siteId = "A001";
        LocalDate startDate = 미래날짜(10);
        LocalDate endDate = startDate.plusDays(2);

        // When: 두 고객이 동시에 같은 사이트와 기간을 예약 요청하면
        List<ExtractableResponse<Response>> responses = 동시_예약_요청(customer1, customer2, siteId, startDate, endDate);

        // Then: 하나의 예약만 성공하고 나머지는 충돌 오류로 실패한다
        동시성_결과_검증(responses, "이미 예약");
    }

    @DisplayName("이미 예약된 날짜가 중간에 포함된 기간으로 연박 예약을 시도하면, '해당 기간에 예약이 불가능합니다'는 오류 메시지를 받는다")
    @Test
    void scenario_MultiDayReservationFullPeriodAvailability() {
        // Given: 사이트 "A001"의 중간 날짜가 이미 예약되어 있다
        String siteId = "A001";
        LocalDate conflictDate = 미래날짜(16);
        CustomerInfo 기존고객 = 기존예약자();
        기존_예약_생성(기존고객.name, 기존고객.phoneNumber, siteId, conflictDate, conflictDate.plusDays(1));

        // When: 이미 예약된 날짜가 중간에 포함된 기간으로 연박 예약을 시도하면
        CustomerInfo customer = 김철수();
        LocalDate startDate = conflictDate.minusDays(1);
        LocalDate endDate = conflictDate.plusDays(2);
        ExtractableResponse<Response> response = 예약_요청(customer, siteId, startDate, endDate);

        // Then: 예약이 실패하고 "해당 기간에 예약이 불가능합니다" 오류 메시지를 받는다
        예약_실패_검증(response, HttpStatus.CONFLICT, "해당 기간");
    }

    @DisplayName("올바른 확인코드로 예약 취소를 요청하면, 예약 취소가 성공하고 상태가 CANCELLED로 변경된다")
    @Test
    void scenario_SuccessfulCancellationWithCorrectConfirmationCode() {
        // Given: 고객 "김철수"가 확인코드로 예약을 완료했다
        CustomerInfo customer = 김철수();
        LocalDate startDate = 미래날짜(20);
        ExtractableResponse<Response> createResponse = 예약_요청(customer, "A001", startDate, startDate.plusDays(2));
        Long reservationId = createResponse.jsonPath().getLong("id");
        String confirmationCode = createResponse.jsonPath().getString("confirmationCode");

        // When: 올바른 확인코드로 예약 취소를 요청하면
        ExtractableResponse<Response> response = cancelReservation(reservationId, confirmationCode);

        // Then: 예약 취소가 성공하고 상태가 CANCELLED로 변경된다
        예약_취소_성공_검증(response, reservationId);
    }

    @DisplayName("잘못된 확인코드로 예약 취소를 시도하면, '확인코드가 일치하지 않습니다'는 오류 메시지를 받는다")
    @Test
    void scenario_CancellationWithWrongConfirmationCode() {
        // Given: 고객 "김철수"가 확인코드로 예약을 완료했다
        CustomerInfo customer = 김철수();
        LocalDate startDate = 미래날짜(22);
        ExtractableResponse<Response> createResponse = 예약_요청(customer, "A001", startDate, startDate.plusDays(2));
        Long reservationId = createResponse.jsonPath().getLong("id");

        // When: 잘못된 확인코드로 예약 취소를 시도하면
        ExtractableResponse<Response> response = cancelReservation(reservationId, "WRONG1");

        // Then: 예약 취소가 실패하고 "확인 코드가 일치하지 않습니다" 오류 메시지를 받는다
        예약_실패_검증(response, HttpStatus.BAD_REQUEST, "확인 코드");
    }

    @DisplayName("예약이 취소된 사이트에 새로운 예약을 요청하면, 예약이 성공적으로 생성된다")
    @Test
    void scenario_CancelledSiteCanBeReservedAgain() {
        // Given: 사이트의 예약이 취소된 상태이다
        String siteId = "A001";
        LocalDate reservationDate = 미래날짜(25);
        CustomerInfo customer1 = 김철수();
        CustomerInfo customer2 = 이영희();

        ExtractableResponse<Response> createResponse = 예약_요청(customer1, siteId, reservationDate, reservationDate.plusDays(1));
        Long reservationId = createResponse.jsonPath().getLong("id");
        String confirmationCode = createResponse.jsonPath().getString("confirmationCode");
        cancelReservation(reservationId, confirmationCode);

        // When: 예약이 취소된 사이트에 새로운 예약을 요청하면
        ExtractableResponse<Response> response = 예약_요청(customer2, siteId, reservationDate, reservationDate.plusDays(1));

        // Then: 예약이 성공적으로 생성된다
        예약_생성_성공_검증(response);
    }

    @DisplayName("이름과 전화번호로 예약을 조회하면, 해당 고객의 모든 예약 목록이 반환된다")
    @Test
    void scenario_QueryMyReservationsByNameAndPhone() {
        // Given: 고객 "김철수"가 여러 예약을 완료했다
        CustomerInfo customer = 김철수();
        예약_요청(customer, "A001", 미래날짜(5), 미래날짜(7));
        예약_요청(customer, "A002", 미래날짜(10), 미래날짜(12));

        // When: 이름과 전화번호로 예약을 조회하면
        ExtractableResponse<Response> response = getMyReservations(customer.name, customer.phoneNumber);

        // Then: 해당 고객의 모든 예약 목록이 반환된다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        List<Map<String, Object>> reservations = response.jsonPath().getList("$");
        assertThat(reservations).hasSizeGreaterThanOrEqualTo(2);

        reservations.forEach(reservation -> {
            assertThat(reservation.get("customerName")).isEqualTo(customer.name);
            assertThat(reservation.get("phoneNumber")).isEqualTo(customer.phoneNumber);
        });
    }

    @DisplayName("년도, 월, 사이트ID로 캘린더를 조회하면, 예약된 날짜와 가능한 날짜가 구분되어 표시된다")
    @Test
    void scenario_MonthlyReservationCalendar() {
        // Given: 사이트에 해당 월 예약이 있다
        Long siteId = 1L;
        LocalDate reservationDate = 미래날짜(10);
        CustomerInfo customer = 김철수();
        예약_요청(customer, siteId.toString(), reservationDate, reservationDate.plusDays(1));

        // When: 년도, 월, 사이트ID로 캘린더를 조회하면
        ExtractableResponse<Response> response = getReservationCalendar(reservationDate.getYear(), reservationDate.getMonthValue(), siteId);

        // Then: 예약된 날짜와 가능한 날짜가 구분되어 표시된다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getInt("year")).isEqualTo(reservationDate.getYear());
        assertThat(response.jsonPath().getInt("month")).isEqualTo(reservationDate.getMonthValue());
        assertThat(response.jsonPath().getInt("siteId")).isEqualTo(siteId.intValue());
    }
}
