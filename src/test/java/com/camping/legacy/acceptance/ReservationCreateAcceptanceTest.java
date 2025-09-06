package com.camping.legacy.acceptance;

import com.camping.legacy.TestBase;
import com.camping.legacy.dto.ReservationRequest;
import com.camping.legacy.stub.ReservationRequestTestDataBuilder;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class ReservationCreateAcceptanceTest extends TestBase {

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
    @DisplayName("예약 생성을 성공한다.")
    void a() {
        // Given
        String givenCustomerName = "홍길동";
        String givenSiteNumber = "A-3";
        LocalDate givenStartDate = Context.NOW_PLUS_1_DAY;
        LocalDate givenEndDate = Context.NOW_PLUS_2_DAY;
        ReservationRequest req = ReservationRequestTestDataBuilder.get(
                givenCustomerName, givenSiteNumber, givenStartDate, givenEndDate
        );
        // When
        Response res = given().log().all()
                .contentType(ContentType.JSON)
                .body(req)
                .post("/api/reservations");
        // Then
        res.then().log().all()
                .statusCode(201)
                .body("id", notNullValue())
                .body("customerName", equalTo(givenCustomerName))
                .body("siteNumber", equalTo(givenSiteNumber));
    }

    @Test
    @DisplayName("예약 생성시 예약자 이름이 없으면 예약에 실패한다.")
    void b() {
        // Given
        String givenInvalidCustomerName = "";
        String givenSiteNumber = "A-3";
        LocalDate givenStartDate = Context.NOW_PLUS_1_DAY;
        LocalDate givenEndDate = Context.NOW_PLUS_2_DAY;
        ReservationRequest req = ReservationRequestTestDataBuilder.get(
                givenInvalidCustomerName, givenSiteNumber, givenStartDate, givenEndDate
        );
        // When
        Response res = given().log().all()
                .contentType(ContentType.JSON)
                .body(req)
                .post("/api/reservations");
        // Then
        res.then().log().all()
                .statusCode(409)
                .body("message", containsString("예약자 이름을 입력해주세요."));
    }

    // TODO: [Note] 과거 날짜 선택 못하도록 검증하는 코드가 없는 것 같음. (버그)

    @Test
    @DisplayName("예약 생성에 실패한다 (종료일이 시작일보다 이전)")
    void c() {
        // Given
        String givenCustomerName = "홍길동";
        String givenSiteNumber = "A-3";
        LocalDate givenStartDate = Context.NOW_PLUS_2_DAY;
        LocalDate givenEndDate = Context.NOW_PLUS_1_DAY;
        ReservationRequest req = ReservationRequestTestDataBuilder.get(
                givenCustomerName, givenSiteNumber, givenStartDate, givenEndDate
        );
        // When
        Response res = given().log().all()
                .contentType(ContentType.JSON)
                .body(req)
                .post("/api/reservations");
        // Then
        res.then().log().all()
                .statusCode(409)
                .body("message", containsString("종료일이 시작일보다 이전"));
    }

    // TODO: [Note] 30일 초과 코드가 없어 보인다. (버그)
//    @Test
//    @DisplayName("예약 생성에 실패한다 (30일 초과 기간 선택)")
//    void d() {
//        String givenCustomerName = "홍길동";
//        String givenSiteNumber = "A-3";
//        LocalDate givenStartDate = LocalDate.now().plusDays(40);
//        LocalDate givenEndDate = givenStartDate.plusDays(1);
//        ReservationRequest req = ReservationRequestStub.get(
//                givenCustomerName, givenSiteNumber, givenStartDate, givenEndDate
//        );
//        // When
//        Response res = given().log().all()
//                .contentType(ContentType.JSON)
//                .body(req)
//                .post("/api/reservations");
//        // Then
//        res.then().log().all()
//                .statusCode(409)
//                .body("message", notNullValue());
//    }

    @Test
    @DisplayName("예약 생성에 실패한다 (이미 예약된 사이트 동일 기간)")
    void e() {
        // Given
        String givenCustomerName = "홍길동";
        String givenSiteNumber = "A-3";
        LocalDate givenStartDate = Context.NOW_PLUS_1_DAY;
        LocalDate givenEndDate = Context.NOW_PLUS_2_DAY;
        ReservationRequest req = ReservationRequestTestDataBuilder.get(
                givenCustomerName, givenSiteNumber, givenStartDate, givenEndDate
        );
        // When
        Response res = given().log().all()
                .contentType(ContentType.JSON)
                .body(req)
                .post("/api/reservations");
        // Then
        res.then().log().all()
                .statusCode(201)
                .body("id", notNullValue())
                .body("customerName", equalTo(givenCustomerName))
                .body("siteNumber", equalTo(givenSiteNumber));
        ReservationRequest req2 = ReservationRequestTestDataBuilder.get(
                givenCustomerName, givenSiteNumber, givenStartDate, givenEndDate
        );
        // When
        Response res2 = given().log().all()
                .contentType(ContentType.JSON)
                .body(req2)
                .post("/api/reservations");
        // Then
        res2.then().log().all()
                .statusCode(409)
                .body("message", containsString("이미 예약"));
    }
}
