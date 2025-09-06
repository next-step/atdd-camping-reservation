package com.camping.legacy.acceptance;

import com.camping.legacy.ShareContext;
import com.camping.legacy.TestBase;
import com.camping.legacy.domain.Reservation;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

public class ReservationCancelAcceptanceTest extends TestBase {

    @BeforeEach
    void setUp() {
        RestAssured.port = this.port;
        this.clearDB();
    }

    public static class Context {
        public static LocalDate NOW_PLUS_1_DAY = LocalDate.now().plusDays(1);
        public static LocalDate NOW_PLUS_2_DAY = LocalDate.now().plusDays(2);
    }

    @Test
    @DisplayName("예약자가 예약을 성공적으로 취소한다.")
    void a() {
        // Given
        String givenCustomerName = "홍길동";
        String givenSiteNumber = "A-3";
        LocalDate givenStartDate = LocalDate.now().plusDays(1);
        LocalDate givenEndDate = givenStartDate.plusDays(1);
        Long reservationId = this.createReservationAndGetId(givenCustomerName, givenSiteNumber, givenStartDate, givenEndDate);
        // When
        Response res = given().log().all()
                .contentType(ContentType.JSON)
                .pathParam("id", reservationId)
                .param("confirmationCode", ShareContext.CONFIRMMATION_CODE)
                .delete("/api/reservations/{id}");
        // Then
        res.then().log().all()
                .statusCode(200)
                .body("message", equalTo("예약이 취소되었습니다."));
    }

    @Test
    @DisplayName("(예약 취소 실패) 예약자가 잘못된 예약 ID로 인해 예약 취소에 실패한다.")
    void b() {
        // Given
        String givenCustomerName = "홍길동";
        String givenSiteNumber = "A-3";
        LocalDate givenStartDate = LocalDate.now().plusDays(1);
        LocalDate givenEndDate = givenStartDate.plusDays(1);
        Long reservationId = this.createReservationAndGetId(givenCustomerName, givenSiteNumber, givenStartDate, givenEndDate);
        // 잘못된 예약 ID
        Long invalidReservationId = reservationId + 9999;
        // When
        Response res = given().log().all()
                .contentType(ContentType.JSON)
                .pathParam("id", invalidReservationId)
                .param("confirmationCode", ShareContext.CONFIRMMATION_CODE)
                .delete("/api/reservations/{id}");
        // Then
        res.then().log().all()
                .statusCode(400)
                .body("message", containsString("예약을 찾을 수 없습니다."));

    }

    @Test
    @DisplayName("(예약 취소 실패) 예약자가 이미 취소된 예약을 다시 취소하려고 시도하면 실패한다.")
    void c() {
        // Given
        String givenCustomerName = "홍길동";
        String givenSiteNumber = "A-3";
        LocalDate givenStartDate = LocalDate.now().plusDays(1);
        LocalDate givenEndDate = givenStartDate.plusDays(1);
        Long reservationId = this.createReservationAndGetId(givenCustomerName, givenSiteNumber, givenStartDate, givenEndDate);
        // When
        Response res = given().log().all()
                .contentType(ContentType.JSON)
                .pathParam("id", reservationId)
                .param("confirmationCode", ShareContext.CONFIRMMATION_CODE)
                .delete("/api/reservations/{id}");
        // Then
        res.then().log().all()
                .statusCode(200)
                .body("message", equalTo("예약이 취소되었습니다."));

        // When - 이미 취소된 예약을 다시 취소 시도
        Response res2 = given().log().all()
                .contentType(ContentType.JSON)
                .pathParam("id", reservationId)
                .param("confirmationCode", ShareContext.CONFIRMMATION_CODE)
                .delete("/api/reservations/{id}");
        // Then
        res2.then().log().all()
                .statusCode(400)
                .body("message", containsString("이미 취소된 예약입니다."));
    }

    @Test
    @DisplayName("예약자가 당일 취소시 환불금액의 비율은 0%이다.")
    void d() {
        // Given
        String givenCustomerName = "홍길동";
        String givenSiteNumber = "A-3";
        LocalDate givenStartDate = LocalDate.now();
        LocalDate givenEndDate = givenStartDate.plusDays(1);
        Long reservationId = this.createReservationAndGetId(givenCustomerName, givenSiteNumber, givenStartDate, givenEndDate);
        // When
        Response res = given().log().all()
                .contentType(ContentType.JSON)
                .pathParam("id", reservationId)
                .param("confirmationCode", ShareContext.CONFIRMMATION_CODE)
                .delete("/api/reservations/{id}");
        // Then
        res.then().log().all()
                .statusCode(200)
                .body("message", equalTo("예약이 취소되었습니다."));
        Reservation reservation = this.findReservationById(reservationId);
        assertThat(reservation.getRefundPercent()).isEqualTo(0);
    }

    @Test
    @DisplayName("예약자가 사전 취소시 환불금액의 비율은 100%이다.")
    void e() {
        // Given
        String givenCustomerName = "홍길동";
        String givenSiteNumber = "A-3";
        LocalDate givenStartDate = Context.NOW_PLUS_1_DAY;
        LocalDate givenEndDate = Context.NOW_PLUS_2_DAY;
        Long reservationId = this.createReservationAndGetId(givenCustomerName, givenSiteNumber, givenStartDate, givenEndDate);
        // When
        Response res = given().log().all()
                .contentType(ContentType.JSON)
                .pathParam("id", reservationId)
                .param("confirmationCode", ShareContext.CONFIRMMATION_CODE)
                .delete("/api/reservations/{id}");
        // Then
        res.then().log().all()
                .statusCode(200)
                .body("message", equalTo("예약이 취소되었습니다."));
        Reservation reservation = this.findReservationById(reservationId);
        assertThat(reservation.getRefundPercent()).isEqualTo(100);
    }
}
