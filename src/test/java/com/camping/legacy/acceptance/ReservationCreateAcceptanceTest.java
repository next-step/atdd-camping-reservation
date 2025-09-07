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
        ReservationRequest req = new ReservationRequestTestDataBuilder()
                .withName(givenCustomerName)
                .withSiteNumber(givenSiteNumber)
                .withStartDate(givenStartDate)
                .withEndDate(givenEndDate)
                .build();
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
    @DisplayName("(예약 생성 실패) 존재하지 않는 사이트 번호면 예약에 실패한다.")
    void b() {
        // Given
        String givenNotExistSiteNumber = "AAAAA-3";
        ReservationRequest req = new ReservationRequestTestDataBuilder()
                .withSiteNumber(givenNotExistSiteNumber)
                .build();
        // When
        Response res = given().log().all()
                .contentType(ContentType.JSON)
                .body(req)
                .post("/api/reservations");
        // Then
        res.then().log().all()
                .statusCode(409)
                .body("message", containsString("존재하지 않는 캠핑장입니다."));
    }

    @Test
    @DisplayName("(예약 생성 실패) 예약자 이름이 없으면 예약에 실패한다.")
    void b2() {
        // Given
        String givenInvalidCustomerName = "";
        ReservationRequest req = new ReservationRequestTestDataBuilder()
                .withName(givenInvalidCustomerName)
                .build();
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

    @Test
    @DisplayName("(예약 생성 실패) 시작일이 오늘보다 이전이면 예약에 실패한다.")
    void c() {
        // Given
        LocalDate givenStartDate = LocalDate.now().minusDays(7);
        LocalDate givenEndDate = Context.NOW_PLUS_1_DAY;
        ReservationRequest req = new ReservationRequestTestDataBuilder()
                .withStartDate(givenStartDate)
                .withEndDate(givenEndDate)
                .build();
        // When
        Response res = given().log().all()
                .contentType(ContentType.JSON)
                .body(req)
                .post("/api/reservations");
        // Then
        res.then().log().all()
                .statusCode(409)
                .body("message", containsString("시작일과 종료일은 오늘보다 이전일 수 없습니다."));
    }

    @Test
    @DisplayName("(예약 생성 실패) 종료일이 오늘보다 이전이면 예약에 실패한다.")
    void d() {
        // Given
        LocalDate givenStartDate = Context.NOW_PLUS_1_DAY;
        LocalDate givenEndDate = LocalDate.now().minusDays(7);
        ReservationRequest req = new ReservationRequestTestDataBuilder()
                .withStartDate(givenStartDate)
                .withEndDate(givenEndDate)
                .build();
        // When
        Response res = given().log().all()
                .contentType(ContentType.JSON)
                .body(req)
                .post("/api/reservations");
        // Then
        res.then().log().all()
                .statusCode(409)
                .body("message", containsString("시작일과 종료일은 오늘보다 이전일 수 없습니다."));
    }


    @Test
    @DisplayName("(예약 생성 실패) 종료일이 시작일보다 이전이면 예약에 실패한다.")
    void e() {
        // Given
        LocalDate givenStartDate = Context.NOW_PLUS_2_DAY;
        LocalDate givenEndDate = Context.NOW_PLUS_1_DAY;
        ReservationRequest req = new ReservationRequestTestDataBuilder()
                .withStartDate(givenStartDate)
                .withEndDate(givenEndDate)
                .build();
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

    @Test
    @DisplayName("(예약 생성 실패) 30일 초과 기간을 선택하면 예약에 실패한다.")
    void e2() {
        LocalDate givenStartDate = LocalDate.now().plusDays(40);
        LocalDate givenEndDate = givenStartDate.plusDays(1);
        ReservationRequest req = new ReservationRequestTestDataBuilder()
                .withStartDate(givenStartDate)
                .withEndDate(givenEndDate)
                .build();
        // When
        Response res = given().log().all()
                .contentType(ContentType.JSON)
                .body(req)
                .post("/api/reservations");
        // Then
        res.then().log().all()
                .statusCode(409)
                .body("message", notNullValue());
    }

    @Test
    @DisplayName("(예약 생성 실패) 이미 예약된 사이트의 동일 기간을 선택하면 예약에 실패한다.")
    void e3() {
        // Given
        String givenSiteNumber = "A-3";
        LocalDate givenStartDate = Context.NOW_PLUS_1_DAY;
        LocalDate givenEndDate = Context.NOW_PLUS_2_DAY;
        ReservationRequest req = new ReservationRequestTestDataBuilder()
                .withSiteNumber(givenSiteNumber)
                .withStartDate(givenStartDate)
                .withEndDate(givenEndDate)
                .build();
        // When
        Response res = given().log().all()
                .contentType(ContentType.JSON)
                .body(req)
                .post("/api/reservations");
        // Then
        res.then().log().all()
                .statusCode(201)
                .body("id", notNullValue())
                .body("siteNumber", equalTo(givenSiteNumber));
        ReservationRequest req2 = new ReservationRequestTestDataBuilder()
                .withSiteNumber(givenSiteNumber)
                .withStartDate(givenStartDate)
                .withEndDate(givenEndDate)
                .build();
        // When
        Response res2 = given().log().all()
                .contentType(ContentType.JSON)
                .body(req2)
                .post("/api/reservations");
        // Then
        res2.then().log().all()
                .statusCode(409)
                .body("message", containsString("해당 기간에 이미 예약이 존재합니다."));
    }
}
