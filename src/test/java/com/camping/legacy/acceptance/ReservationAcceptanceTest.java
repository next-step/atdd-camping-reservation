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

    @DisplayName("올바른 고객 정보와 날짜로 예약을 요청하면, 예약이 성공적으로 생성되고 6자리 확인코드가 발급된다")
    @Test
    void scenario_SuccessfulReservationCreation() {
        /*
         * 누가(Who), 무엇을(What), 언제(When), 어디서(Where), 왜(Why), 어떻게(How)
         * 누가(Who): 예약을 하려는 고객 "김철수"
         * 무엇을(What): 캠핑 사이트 예약을
         * 언제(When): 오늘로부터 5일 후부터 3일간
         * 어디서(Where): 캠핑장 예약 시스템에서
         * 왜(Why): 캠핑을 위한 사이트 예약을 하기 위해
         * 어떻게(How): 올바른 고객 정보와 날짜로 예약 요청을 보내서
         */

        // 캠핑 사이트 "A001"(대형)과 "B001"(소형)이 등록되어 있다
        // 오늘 날짜는 현재 날짜이다
        // Given: 고객 "김철수"의 전화번호는 "010-1234-5678"이다
        String customerName = "김철수";
        String phoneNumber = "010-1234-5678";
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.plusDays(5);
        LocalDate endDate = startDate.plusDays(2);

        // When: startDate부터 endDate까지 사이트 "1"을 예약 요청한다
        ExtractableResponse<Response> response = createReservation(customerName, phoneNumber, "1", startDate, endDate);

        // Then: 예약이 성공적으로 생성된다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());

        // And: 6자리 확인코드가 발급된다
        String confirmationCode = response.jsonPath().getString("confirmationCode");
        assertThat(confirmationCode).isNotNull().hasSize(6);

        // And: 예약 상태는 "CONFIRMED"이다
        assertThat(response.jsonPath().getString("status")).isEqualTo("CONFIRMED");
    }

    @DisplayName("30일을 초과한 날짜로 예약을 시도하면, '30일 이내 예약만 가능합니다'는 오류 메시지를 받는다")
    @Test
    void scenario_ReservationExceedsDateLimit() {
        /*
         * 누가(Who), 무엇을(What), 언제(When), 어디서(Where), 왜(Why), 어떻게(How)
         * 누가(Who): 예약을 하려는 고객 "김철수"
         * 무엇을(What): 30일 초과 날짜 예약을
         * 언제(When): 오늘로부터 35일 후
         * 어디서(Where): 캠핑장 예약 시스템에서
         * 왜(Why): 30일 이내 예약만 가능하다는 규칙을 검증하기 위해
         * 어떻게(How): 30일을 초과한 날짜로 예약 요청을 보내서
         */

        // Given: 고객 "김철수"의 전화번호는 "010-1234-5678"이다
        String customerName = "김철수";
        String phoneNumber = "010-1234-5678";
        LocalDate farFutureDate = LocalDate.now().plusDays(35);

        // When: 30일 초과 날짜로 예약을 요청한다
        ExtractableResponse<Response> response = createReservation(customerName, phoneNumber, "1", farFutureDate, farFutureDate.plusDays(2));

        // Then: 예약이 실패한다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());

        // And: "30일 이내 예약만 가능합니다" 오류 메시지를 받는다
        assertThat(response.jsonPath().getString("message")).contains("30일");
    }

    @DisplayName("과거 날짜로 예약을 시도하면, '과거 날짜로 예약할 수 없습니다'는 오류 메시지를 받는다")
    @Test
    void scenario_PastDateReservationAttempt() {
        /*
         * 누가(Who), 무엇을(What), 언제(When), 어디서(Where), 왜(Why), 어떻게(How)
         * 누가(Who): 예약을 하려는 고객 "김철수"
         * 무엇을(What): 과거 날짜 예약을
         * 언제(When): 어제 날짜로
         * 어디서(Where): 캠핑장 예약 시스템에서
         * 왜(Why): 과거 날짜 예약이 불가능하다는 규칙을 검증하기 위해
         * 어떻게(How): 과거 날짜로 예약 요청을 보내서
         */

        // Given: 고객 "김철수"의 전화번호는 "010-1234-5678"이다
        String customerName = "김철수";
        String phoneNumber = "010-1234-5678";
        LocalDate pastDate = LocalDate.now().minusDays(1);

        // When: 과거 날짜로 예약을 요청한다
        ExtractableResponse<Response> response = createReservation(customerName, phoneNumber, "1", pastDate, pastDate.plusDays(2));

        // Then: 예약이 실패한다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());

        // And: "과거 날짜로 예약할 수 없습니다" 오류 메시지를 받는다
        assertThat(response.jsonPath().getString("message")).contains("과거");
    }

    @DisplayName("두 고객이 동시에 같은 사이트와 기간을 예약 요청하면, 하나의 예약만 성공하고 나머지는 충돌 오류로 실패한다")
    @Test
    void scenario_ConcurrentReservationOnlyOneSucceeds() throws Exception {
        /*
         * 누가(Who), 무엇을(What), 언제(When), 어디서(Where), 왜(Why), 어떻게(How)
         * 누가(Who): 두 명의 고객 "김철수"와 "이영희"
         * 무엇을(What): 동일한 사이트의 동일한 기간 예약을
         * 언제(When): 정확히 같은 시점에
         * 어디서(Where): 캠핑장 예약 시스템에서
         * 왜(Why): 동시성 제어를 통해 중복 예약을 방지하기 위해
         * 어떻게(How): 동시에 같은 예약 요청을 보내서
         */

        // Given: 고객 "김철수"와 "이영희"가 동시에 같은 예약을 요청한다
        String siteId = "1";
        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = startDate.plusDays(2);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        List<ExtractableResponse<Response>> responses = new ArrayList<>();

        // When: 두 고객이 동시에 같은 사이트/기간을 예약 요청한다
        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
            try {
                startLatch.await();
                ExtractableResponse<Response> response = createReservation("김철수", "010-1111-1111", siteId, startDate, endDate);
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
                ExtractableResponse<Response> response = createReservation("이영희", "010-2222-2222", siteId, startDate, endDate);
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

        // Then: 하나의 예약만 성공한다
        long successCount = responses.stream()
                .mapToInt(ExtractableResponse::statusCode)
                .filter(status -> status == HttpStatus.CREATED.value())
                .count();
        assertThat(successCount).isEqualTo(1L);

        // And: 나머지 예약은 "이미 예약된 기간입니다" 오류로 실패한다
        long conflictCount = responses.stream()
                .mapToInt(ExtractableResponse::statusCode)
                .filter(status -> status == HttpStatus.CONFLICT.value())
                .count();
        assertThat(conflictCount).isEqualTo(1L);

        ExtractableResponse<Response> conflictResponse = responses.stream()
                .filter(r -> r.statusCode() == HttpStatus.CONFLICT.value())
                .findFirst()
                .orElseThrow();
        assertThat(conflictResponse.jsonPath().getString("message")).contains("이미 예약");
    }

    @DisplayName("이미 예약된 날짜가 중간에 포함된 기간으로 연박 예약을 시도하면, '해당 기간에 예약이 불가능합니다'는 오류 메시지를 받는다")
    @Test
    void scenario_MultiDayReservationFullPeriodAvailability() {
        /*
         * 누가(Who), 무엇을(What), 언제(When), 어디서(Where), 왜(Why), 어떻게(How)
         * 누가(Who): 연박 예약을 하려는 고객 "김철수"
         * 무엇을(What): 연박 예약을
         * 언제(When): 이미 예약된 날짜가 중간에 포함된 기간에
         * 어디서(Where): 캠핑장 예약 시스템에서
         * 왜(Why): 연박 예약 시 전체 기간의 가용성 확인이 필요하다는 규칙을 검증하기 위해
         * 어떻게(How): 이미 예약된 날짜가 중간에 포함된 연박 예약 요청을 보내서
         */

        // Given: 사이트 "1"의 중간 날짜가 이미 예약되어 있다
        String siteId = "1";
        LocalDate conflictDate = LocalDate.now().plusDays(16);

        // 중간 날짜에 기존 예약 생성
        createReservation("기존예약자", "010-0000-0000", siteId, conflictDate, conflictDate.plusDays(1));

        // When: 기존 예약과 겹치는 기간으로 연박 예약을 요청한다
        LocalDate startDate = conflictDate.minusDays(1);
        LocalDate endDate = conflictDate.plusDays(2);
        ExtractableResponse<Response> response = createReservation("김철수", "010-1234-5678", siteId, startDate, endDate);

        // Then: 예약이 실패한다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());

        // And: "해당 기간에 예약이 불가능합니다" 오류 메시지를 받는다
        assertThat(response.jsonPath().getString("message")).contains("해당 기간");
    }

    @DisplayName("올바른 확인코드로 예약 취소를 요청하면, 예약 취소가 성공하고 상태가 CANCELLED로 변경된다")
    @Test
    void scenario_SuccessfulCancellationWithCorrectConfirmationCode() {
        /*
         * 누가(Who), 무엇을(What), 언제(When), 어디서(Where), 왜(Why), 어떻게(How)
         * 누가(Who): 예약을 취소하려는 고객 "김철수"
         * 무엇을(What): 자신의 예약을
         * 언제(When): 예약 완료 후 취소가 필요할 때
         * 어디서(Where): 캠핑장 예약 시스템에서
         * 왜(Why): 예약을 취소하고 싶어서
         * 어떻게(How): 올바른 확인코드로 취소 요청을 보내서
         */

        // Given: 고객 "김철수"가 확인코드로 예약을 완료했다
        String customerName = "김철수";
        String phoneNumber = "010-1234-5678";
        LocalDate startDate = LocalDate.now().plusDays(20);

        ExtractableResponse<Response> createResponse = createReservation(customerName, phoneNumber, "1", startDate, startDate.plusDays(2));
        Long reservationId = createResponse.jsonPath().getLong("id");
        String confirmationCode = createResponse.jsonPath().getString("confirmationCode");

        // When: 올바른 확인코드로 예약 취소를 요청한다
        ExtractableResponse<Response> response = cancelReservation(reservationId, confirmationCode);

        // Then: 예약 취소가 성공한다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getString("message")).contains("취소");

        // And: 예약 상태가 "CANCELLED"로 변경된다
        ExtractableResponse<Response> getResponse = getReservation(reservationId);
        assertThat(getResponse.jsonPath().getString("status")).isEqualTo("CANCELLED");
    }

    @DisplayName("잘못된 확인코드로 예약 취소를 시도하면, '확인코드가 일치하지 않습니다'는 오류 메시지를 받는다")
    @Test
    void scenario_CancellationWithWrongConfirmationCode() {
        /*
         * 누가(Who), 무엇을(What), 언제(When), 어디서(Where), 왜(Why), 어떻게(How)
         * 누가(Who): 예약을 취소하려는 고객 "김철수"
         * 무엇을(What): 자신의 예약을
         * 언제(When): 예약 완료 후 취소가 필요할 때
         * 어디서(Where): 캠핑장 예약 시스템에서
         * 왜(Why): 잘못된 확인코드로는 취소가 불가능하다는 보안 규칙을 검증하기 위해
         * 어떻게(How): 잘못된 확인코드로 취소 요청을 보내서
         */

        // Given: 고객 "김철수"가 확인코드로 예약을 완료했다
        ExtractableResponse<Response> createResponse = createReservation("김철수", "010-1234-5678", "1",
                                                                         LocalDate.now().plusDays(22), LocalDate.now().plusDays(24)
        );
        Long reservationId = createResponse.jsonPath().getLong("id");

        // When: 잘못된 확인코드로 예약 취소를 요청한다
        ExtractableResponse<Response> response = cancelReservation(reservationId, "WRONG1");

        // Then: 예약 취소가 실패한다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());

        // And: "확인코드가 일치하지 않습니다" 오류 메시지를 받는다
        assertThat(response.jsonPath().getString("message")).contains("확인코드");
    }

    @DisplayName("예약이 취소된 사이트에 새로운 예약을 요청하면, 예약이 성공적으로 생성된다")
    @Test
    void scenario_CancelledSiteCanBeReservedAgain() {
        /*
         * 누가(Who), 무엇을(What), 언제(When), 어디서(Where), 왜(Why), 어떻게(How)
         * 누가(Who): 취소된 사이트를 예약하려는 새로운 고객 "이영희"
         * 무엇을(What): 취소된 예약 사이트를
         * 언제(When): 예약이 취소된 후
         * 어디서(Where): 캠핑장 예약 시스템에서
         * 왜(Why): 취소된 예약은 중복 체크에서 제외되어 다시 예약 가능해야 하기 때문에
         * 어떻게(How): 취소된 사이트에 새로운 예약 요청을 보내서
         */

        // Given: 사이트의 예약이 취소된 상태이다
        String siteId = "1";
        LocalDate reservationDate = LocalDate.now().plusDays(25);

        ExtractableResponse<Response> createResponse = createReservation("김철수", "010-1234-5678", siteId, reservationDate, reservationDate.plusDays(1));
        Long reservationId = createResponse.jsonPath().getLong("id");
        String confirmationCode = createResponse.jsonPath().getString("confirmationCode");

        // 예약 취소
        cancelReservation(reservationId, confirmationCode);

        // When: 취소된 사이트에 새로 예약을 요청한다
        ExtractableResponse<Response> response = createReservation("이영희", "010-5555-5555", siteId, reservationDate, reservationDate.plusDays(1));

        // Then: 예약이 성공적으로 생성된다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    }

    @DisplayName("이름과 전화번호로 예약을 조회하면, 해당 고객의 모든 예약 목록이 반환된다")
    @Test
    void scenario_QueryMyReservationsByNameAndPhone() {
        /*
         * 누가(Who), 무엇을(What), 언제(When), 어디서(Where), 왜(Why), 어떻게(How)
         * 누가(Who): 자신의 예약을 조회하려는 고객 "김철수"
         * 무엇을(What): 자신의 모든 예약 목록을
         * 언제(When): 여러 예약을 완료한 후
         * 어디서(Where): 캠핑장 예약 시스템에서
         * 왜(Why): 자신의 예약 현황을 확인하기 위해
         * 어떻게(How): 이름과 전화번호로 예약 조회 요청을 보내서
         */

        // Given: 고객 "김철수"가 전화번호 "010-1234-5678"로 여러 예약을 완료했다
        String customerName = "김철수";
        String phoneNumber = "010-1234-5678";

        // 첫 번째 예약
        createReservation(customerName, phoneNumber, "1", LocalDate.now().plusDays(5), LocalDate.now().plusDays(7));

        // 두 번째 예약
        createReservation(customerName, phoneNumber, "2", LocalDate.now().plusDays(10), LocalDate.now().plusDays(12));

        // When: 이름과 전화번호로 예약을 조회한다
        ExtractableResponse<Response> response = getMyReservations(customerName, phoneNumber);

        // Then: 해당 고객의 모든 예약 목록이 반환된다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        List<Map<String, Object>> reservations = response.jsonPath().getList("$");
        assertThat(reservations).hasSizeGreaterThanOrEqualTo(2);

        reservations.forEach(reservation -> {
            assertThat(reservation.get("customerName")).isEqualTo(customerName);
            assertThat(reservation.get("phoneNumber")).isEqualTo(phoneNumber);
        });
    }

    @DisplayName("년도, 월, 사이트ID로 캘린더를 조회하면, 예약된 날짜와 가능한 날짜가 구분되어 표시된다")
    @Test
    void scenario_MonthlyReservationCalendar() {
        /*
         * 누가(Who), 무엇을(What), 언제(When), 어디서(Where), 왜(Why), 어떻게(How)
         * 누가(Who): 월별 예약 현황을 확인하려는 사용자
         * 무엇을(What): 특정 사이트의 월별 예약 현황을
         * 언제(When): 특정 월에 대해
         * 어디서(Where): 캠핑장 예약 시스템에서
         * 왜(Why): 예약된 날짜와 가능한 날짜를 구분해서 보기 위해
         * 어떻게(How): 년도, 월, 사이트ID로 캘린더 조회 요청을 보내서
         */

        // Given: 사이트에 해당 월 예약이 있다
        Long siteId = 1L;
        LocalDate reservationDate = LocalDate.now().plusDays(10);

        createReservation("김철수", "010-1234-5678", siteId.toString(), reservationDate, reservationDate.plusDays(1));

        // When: 해당 월 사이트의 캘린더를 조회한다
        ExtractableResponse<Response> response = getReservationCalendar(reservationDate.getYear(), reservationDate.getMonthValue(), siteId);

        // Then: 예약된 날짜와 가능한 날짜가 구분되어 표시된다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getInt("year")).isEqualTo(reservationDate.getYear());
        assertThat(response.jsonPath().getInt("month")).isEqualTo(reservationDate.getMonthValue());
        assertThat(response.jsonPath().getInt("siteId")).isEqualTo(siteId.intValue());
    }
}
