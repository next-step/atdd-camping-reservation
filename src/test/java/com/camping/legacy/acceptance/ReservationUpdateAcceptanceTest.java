package com.camping.legacy.acceptance;

import com.camping.legacy.ShareContext;
import com.camping.legacy.TestBase;
import com.camping.legacy.dto.ReservationRequest;
import com.camping.legacy.stub.ReservationRequestStub;
import com.camping.legacy.stub.ReservationRequestTestDataBuilder;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class ReservationUpdateAcceptanceTest extends TestBase {

    @BeforeEach
    void setUp() {
        RestAssured.port = this.port;
        this.clearDB();
    }

    @Test
    @DisplayName("예약자가 예약 기간을 성공적으로 변경한다.")
    void a() {
        // Given
        String givenCustomerName = "홍길동";
        String givenSiteNumber = "A-3";
        LocalDate givenStartDate = LocalDate.now().plusDays(1);
        LocalDate givenEndDate = givenStartDate.plusDays(1);
        Long reservationId = this.createReservationAndGetId(givenCustomerName, givenSiteNumber, givenStartDate, givenEndDate);
        // 변경할 예약 기간
        LocalDate givenUpdateStartDate = givenStartDate.plusDays(1);
        LocalDate givenUpdateEndDate = givenEndDate.plusDays(1);
        ReservationRequest req = ReservationRequestTestDataBuilder.get(
                givenCustomerName, givenSiteNumber, givenUpdateStartDate, givenUpdateEndDate
        );
        // When
        Response res = given().log().all()
                .contentType(ContentType.JSON)
                .body(req)
                .pathParam("id", reservationId)
                .param("confirmationCode", ShareContext.CONFIRMMATION_CODE)
                .put("/api/reservations/{id}");
        // Then
        res.then().log().all()
                .statusCode(200)
                .body("id", notNullValue())
                .body("startDate", equalTo(givenUpdateStartDate.toString()))
                .body("endDate", equalTo(givenUpdateEndDate.toString()));
    }

    @Test
    @DisplayName("예약자가 예약자 이름을 성공적으로 변경한다.")
    void b() {
        // Given
        String givenCustomerName = "홍길동";
        String givenSiteNumber = "A-3";
        LocalDate givenStartDate = LocalDate.now().plusDays(1);
        LocalDate givenEndDate = givenStartDate.plusDays(1);
        Long reservationId = this.createReservationAndGetId(givenCustomerName, givenSiteNumber, givenStartDate, givenEndDate);
        // 변경할 예약자 이름
        String givenUpdateCustomerName = "홍길순";
        ReservationRequest req = ReservationRequestTestDataBuilder.get(
                givenUpdateCustomerName, givenSiteNumber, givenStartDate, givenEndDate
        );
        // When
        Response res = given().log().all()
                .contentType(ContentType.JSON)
                .body(req)
                .pathParam("id", reservationId)
                .param("confirmationCode", ShareContext.CONFIRMMATION_CODE)
                .put("/api/reservations/{id}");
        // Then
        res.then().log().all()
                .statusCode(200)
                .body("id", notNullValue())
                .body("customerName", equalTo(givenUpdateCustomerName));
    }

    @Test
    @DisplayName("예약자가 연락처를 성공적으로 변경한다.")
    void c() {
        // Given
        String givenCustomerName = "홍길동";
        String givenSiteNumber = "A-3";
        LocalDate givenStartDate = LocalDate.now().plusDays(1);
        LocalDate givenEndDate = givenStartDate.plusDays(1);
        Long reservationId = this.createReservationAndGetId(givenCustomerName, givenSiteNumber, givenStartDate, givenEndDate);
        // 변경할 연락처
        String givenUpdatePhoneNumber = "010-9999-8888";
        ReservationRequest req = ReservationRequestStub.get(
                givenCustomerName, givenSiteNumber, givenStartDate, givenEndDate, givenUpdatePhoneNumber, null, null, null
        );
        // When
        Response res = given().log().all()
                .contentType(ContentType.JSON)
                .body(req)
                .pathParam("id", reservationId)
                .param("confirmationCode", ShareContext.CONFIRMMATION_CODE)
                .put("/api/reservations/{id}");
        // Then
        res.then().log().all()
                .statusCode(200)
                .body("id", notNullValue())
                .body("phoneNumber", equalTo(givenUpdatePhoneNumber));
    }

    @Test
    @DisplayName("예약자가 성공적으로 새로운 사이트를 변경한다.")
    void d() {
        // Given
        String givenCustomerName = "홍길동";
        String givenSiteNumber = "A-3";
        LocalDate givenStartDate = LocalDate.now().plusDays(1);
        LocalDate givenEndDate = givenStartDate.plusDays(1);
        Long reservationId = this.createReservationAndGetId(givenCustomerName, givenSiteNumber, givenStartDate, givenEndDate);
        // 변경할 사이트
        String givenUpdateSiteNumber = "A-4";
        ReservationRequest req = ReservationRequestTestDataBuilder.get(
                givenCustomerName, givenUpdateSiteNumber, givenStartDate, givenEndDate
        );
        // When
        Response res = given().log().all()
                .contentType(ContentType.JSON)
                .body(req)
                .pathParam("id", reservationId)
                .param("confirmationCode", ShareContext.CONFIRMMATION_CODE)
                .put("/api/reservations/{id}");
        // Then
        res.then().log().all()
                .statusCode(200)
                .body("id", notNullValue())
                .body("siteNumber", equalTo(givenUpdateSiteNumber));
    }

    @Test
    @DisplayName("예약자가 잘못된 확인 코드로 인해 예약 변경에 실패한다.")
    void e() {
        // Given
        String givenCustomerName = "홍길동";
        String givenSiteNumber = "A-3";
        LocalDate givenStartDate = LocalDate.now().plusDays(1);
        LocalDate givenEndDate = givenStartDate.plusDays(1);
        Long reservationId = this.createReservationAndGetId(givenCustomerName, givenSiteNumber, givenStartDate, givenEndDate);
        // 변경할 예약자 이름
        String givenUpdateCustomerName = "홍길순";
        ReservationRequest req = ReservationRequestTestDataBuilder.get(
                givenCustomerName, givenUpdateCustomerName, givenStartDate, givenEndDate
        );
        // 잘못된 확인 코드
        String invalidConfirmationCode = "WRONGCODE";
        // When
        Response res = given().log().all()
                .contentType(ContentType.JSON)
                .body(req)
                .pathParam("id", reservationId)
                .param("confirmationCode", invalidConfirmationCode)
                .put("/api/reservations/{id}");
        // Then
        res.then().log().all()
                .statusCode(400);
    }
}
