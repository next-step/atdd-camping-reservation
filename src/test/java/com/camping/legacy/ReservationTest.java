package com.camping.legacy;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class ReservationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("고객이 빈 사이트를 예약할 수 있다")
    void 예약_생성_성공() {
        Map<String, String> reservation = createReservationMap(CUSTOMER_NAME, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), SITE_NUMBER, PHONE_NUMBER);
        ExtractableResponse<Response> response = postReservation(reservation);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(response.jsonPath().getLong("id")).isPositive();
    }

    @Test
    @DisplayName("고객이 이미 예약된 사이트를 예약할 수 없다")
    void 이미_예약된_사이트_예약_생성_실패() {
        saveReservation(CUSTOMER_NAME, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), site);
        Map<String, String> reservation = createReservationMap(CUSTOMER_NAME, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), SITE_NUMBER, PHONE_NUMBER);

        ExtractableResponse<Response> response = postReservation(reservation);

        assertStatusAndMessage(response, HttpStatus.CONFLICT.value(), "해당 기간에 이미 예약이 존재합니다.");
    }

    @Test
    @DisplayName("동시에 같은 사이트를 예약하면 하나만 성공해야 한다")
    void 동시_예약_처리() throws InterruptedException {
        int threadCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<Integer> statusCodes = new CopyOnWriteArrayList<>();

        Map<String, String> reservation = createReservationMap(CUSTOMER_NAME, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), SITE_NUMBER, PHONE_NUMBER);

        Runnable task = () -> {
            try {
                startLatch.await();
                int status = postReservation(reservation).statusCode();
                statusCodes.add(status);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        };

        for (int i = 0; i < threadCount; i++) new Thread(task).start();

        startLatch.countDown();
        doneLatch.await();

        long success = statusCodes.stream().filter(c -> c == 201).count();
        long fail = statusCodes.stream().filter(c -> c == 409).count();

        assertThat(success).isEqualTo(1);
        assertThat(fail).isEqualTo(threadCount - 1);
    }

    @Test
    @DisplayName("예약은 오늘로부터 30일 이내에만 가능해야 한다")
    void 예약_30일_이내() {
        Map<String, String> reservation = createReservationMap(CUSTOMER_NAME,
                LocalDate.now().plusDays(31),
                LocalDate.now().plusDays(34),
                SITE_NUMBER, PHONE_NUMBER);

        ExtractableResponse<Response> response = postReservation(reservation);

        assertStatusAndMessage(response, HttpStatus.CONFLICT.value(), "예약은 오늘부터 30일 이내에만 가능합니다.");
    }

    @Test
    @DisplayName("예약은 과거 날짜가 아니어야 한다")
    void 예약_과거_불가() {
        Map<String, String> reservation = createReservationMap(CUSTOMER_NAME,
                LocalDate.now().minusDays(10),
                LocalDate.now().minusDays(7),
                SITE_NUMBER, PHONE_NUMBER);

        ExtractableResponse<Response> response = postReservation(reservation);

        assertStatusAndMessage(response, HttpStatus.CONFLICT.value(), "오늘 날짜 이후로 예약이 가능합니다.");
    }

    @Test
    @DisplayName("종료일이 시작일보다 이전일 수 없다")
    void 종료일_이전_불가() {
        Map<String, String> reservation = createReservationMap(CUSTOMER_NAME,
                LocalDate.now().plusDays(3),
                LocalDate.now().plusDays(1),
                SITE_NUMBER, PHONE_NUMBER);

        ExtractableResponse<Response> response = postReservation(reservation);

        assertStatusAndMessage(response, HttpStatus.CONFLICT.value(), "종료일이 시작일보다 이전일 수 없습니다.");
    }

    @Test
    @DisplayName("시작일과 종료일은 빈 값일 수 없다")
    void 시작_종료_빈값_불가() {
        Map<String, String> reservation = Map.of(
                "customerName", CUSTOMER_NAME,
                "startDate", "",
                "endDate", "",
                "siteNumber", SITE_NUMBER,
                "phoneNumber", PHONE_NUMBER
        );

        ExtractableResponse<Response> response = postReservation(reservation);

        assertStatusAndMessage(response, HttpStatus.CONFLICT.value(), "예약 기간을 선택해주세요.");
    }

    @Test
    @DisplayName("당일 예약 취소할 경우 환불 불가 메시지를 안내한다")
    void 당일_취소_환불_불가() {
        LocalDate today = LocalDate.now();

        Map<String, String> reservation = createReservationMap(
                CUSTOMER_NAME,
                today,
                today.plusDays(1),
                SITE_NUMBER,
                PHONE_NUMBER
        );

        ExtractableResponse<Response> reservationResponse = postReservation(reservation);
        Long reservationId = reservationResponse.jsonPath().getLong("id");
        String confirmationCode = reservationResponse.jsonPath().getString("confirmationCode");

        // When: 취소 요청
        ExtractableResponse<Response> canceledResponse = cancelReservation(reservationId, confirmationCode);

        // Then: 당일 취소는 환불 불가
        assertThat(canceledResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertStatusAndMessage(canceledResponse, HttpStatus.OK.value(), "예약이 취소되었습니다. 예약 당일 취소는 환불이 불가능 합니다.");
    }

    @Test
    @DisplayName("취소된 예약 사이트는 즉시 재예약 가능하다")
    void 취소된_예약_사이트_예약_가능() {
        LocalDate today = LocalDate.now();

        Map<String, String> reservation = createReservationMap(
                CUSTOMER_NAME,
                today,
                today.plusDays(1),
                SITE_NUMBER,
                PHONE_NUMBER
        );

        ExtractableResponse<Response> reservationResponse = postReservation(reservation);
        Long reservationId = reservationResponse.jsonPath().getLong("id");
        String confirmationCode = reservationResponse.jsonPath().getString("confirmationCode");

        // When: 예약 취소
        cancelReservation(reservationId, confirmationCode);

        // Then: 다른 고객이 같은 날짜/사이트로 예약 가능
        Map<String, String> newReservation = createReservationMap(
                "김철수",
                today,
                today.plusDays(1),
                SITE_NUMBER,
                "010-0000-0000"
        );

        ExtractableResponse<Response> response = postReservation(newReservation);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(response.jsonPath().getLong("id")).isPositive();
    }

    @Test
    @DisplayName("연박 예약 시 전체 기간 가용성 확인 후 예약 가능 여부 결정")
    void 연박_예약_전체_기간_가용성_확인() {
        // Given: 일부 날짜가 이미 예약된 사이트
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = startDate.plusDays(2); // 3일 예약

        // 중간 날짜 하나 예약
        saveReservation("홍길동", startDate.plusDays(1), startDate.plusDays(1), site);

        Map<String, String> newReservation = createReservationMap(
                "김철수",
                startDate,
                endDate,
                site.getSiteNumber(),
                "010-0000-0000"
        );

        // When: 예약 요청
        ExtractableResponse<Response> response = postReservation(newReservation);

        // Then: 일부 날짜가 이미 예약되어 있으므로 예약 실패
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.jsonPath().getString("message")).isEqualTo("해당 기간에 이미 예약이 존재합니다.");
    }

    @Test
    @DisplayName("예약자는 본인만 예약을 취소할 수 있다")
    void 예약_취소_본인_확인() {
        // Given: 예약 생성
        Map<String, String> reservation = createReservationMap(
                "홍길동",
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(3),
                site.getSiteNumber(),
                "010-0000-0000"
        );
        Long reservationId = postReservation(reservation).jsonPath().getLong("id");

        // When: 다른 사용자가 취소 시도
        ExtractableResponse<Response> response2 = RestAssured.given()
                .log().all()
                .contentType(ContentType.JSON)
                .when()
                .delete("/api/reservations/" + reservationId)
                .then().log().all()
                .extract();

        // Then: 본인이 아니므로 취소 불가
        assertThat(response2.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response2.jsonPath().getString("message")).isEqualTo("예약자 본인만 수정/취소가 가능합니다.");
    }
}