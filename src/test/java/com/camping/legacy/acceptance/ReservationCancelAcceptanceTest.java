package com.camping.legacy.acceptance;

import com.camping.legacy.dto.ReservationRequest;
import com.camping.legacy.repository.ReservationRepository;
import com.camping.legacy.stub.ReservationRequestStub;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ReservationUpdateAcceptanceTest {

    @LocalServerPort
    int port;

    @Autowired
    ReservationRepository reservationRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        reservationRepository.deleteAll();
    }

    public static class Context {
        public static String confirmationCode = null;
    }

    private Long createReservationAndGetId(String customerName, String siteNumber, LocalDate startDate, LocalDate endDate) {
        ReservationRequest req = ReservationRequestStub.get(
                customerName, siteNumber, startDate, endDate
        );
        Response res = given().log().all()
                .contentType(ContentType.JSON)
                .body(req)
                .post("/api/reservations");
        res.then().log().all()
                .statusCode(201)
                .body("id", notNullValue());
        Context.confirmationCode = res.jsonPath().getString("confirmationCode");
        return res.jsonPath().getLong("id");
    }

    /**
     * TODO:
     *  - db가 어디에 붙고 있는지 확인하기 (h2? 아니면 로컬 application-atdd.yml 설정해야할 것 같은데?)
     *  - objectMapper가 필요한건지 확인
     *  - testBackdoor 추가?
     */

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
        ReservationRequest req = ReservationRequestStub.get(
                givenCustomerName, givenSiteNumber, givenUpdateStartDate, givenUpdateEndDate
        );
        // When
        Response res = given().log().all()
                .contentType(ContentType.JSON)
                .body(req)
                .pathParam("id", reservationId)
                .param("confirmationCode", Context.confirmationCode)
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
        ReservationRequest req = ReservationRequestStub.get(
                givenUpdateCustomerName, givenSiteNumber, givenStartDate, givenEndDate
        );
        // When
        Response res = given().log().all()
                .contentType(ContentType.JSON)
                .body(req)
                .pathParam("id", reservationId)
                .param("confirmationCode", Context.confirmationCode)
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
                .param("confirmationCode", Context.confirmationCode)
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
        ReservationRequest req = ReservationRequestStub.get(
                givenCustomerName, givenUpdateSiteNumber, givenStartDate, givenEndDate
        );
        // When
        Response res = given().log().all()
                .contentType(ContentType.JSON)
                .body(req)
                .pathParam("id", reservationId)
                .param("confirmationCode", Context.confirmationCode)
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
        ReservationRequest req = ReservationRequestStub.get(
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
