package com.camping.legacy.acceptance;

import com.camping.legacy.dto.ReservationRequest;
import com.camping.legacy.repository.ReservationRepository;
import com.camping.legacy.stub.ReservationRequestStub;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ReservationCreateAcceptanceTest {

    @LocalServerPort
    int port;

    @Autowired
    ReservationRepository reservationRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        reservationRepository.deleteAll();
    }

    /**
     * TODO:
     *  - db가 어디에 붙고 있는지 확인하기 (h2? 아니면 로컬 application-atdd.yml 설정해야할 것 같은데?)
     *  - objectMapper가 필요한건지 확인
     *  - testBackdoor 추가?
     */

    @Test
    @DisplayName("예약 생성을 성공한다.")
    void create_success_returns_id() {
        // Given
        String givenCustomerName = "홍길동";
        String givenSiteNumber = "A-3";
        LocalDate givenStartDate = LocalDate.now().plusDays(1);
        LocalDate givenEndDate = givenStartDate.plusDays(1);
        ReservationRequest req = ReservationRequestStub.get(
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
    void create_fail_missing_name() {
        // Given
        String givenInvalidCustomerName = "";
        String givenSiteNumber = "A-3";
        LocalDate givenStartDate = LocalDate.now().plusDays(1);
        LocalDate givenEndDate = givenStartDate.plusDays(1);
        ReservationRequest req = ReservationRequestStub.get(
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

    // TODO: 과거 날짜 선택 못하도록 검증하는 코드가 없어 보인다. (버그)

    @Test
    @DisplayName("예약 생성에 실패한다 (종료일이 시작일보다 이전)")
    void create_fail_end_before_start() {
        // Given
        String givenCustomerName = "홍길동";
        String givenSiteNumber = "A-3";
        LocalDate givenStartDate = LocalDate.now().minusDays(1);
        LocalDate givenEndDate = givenStartDate.minusDays(10);
        ReservationRequest req = ReservationRequestStub.get(
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

    /**
     * TODO: LocalDate.of( 이거 다 바꾸기
     *  - extends Exception 제거
     */
    // TODO: 30일 초과 코드가 없어 보인다. (버그)
    @Test
    @DisplayName("예약 생성에 실패한다 (30일 초과 기간 선택)")
    void create_fail_over_30_days() {
        String givenCustomerName = "홍길동";
        String givenSiteNumber = "A-3";
        LocalDate givenStartDate = LocalDate.now().plusDays(40);
        LocalDate givenEndDate = givenStartDate.plusDays(1);
        ReservationRequest req = ReservationRequestStub.get(
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
                .body("message", notNullValue());
    }

    @Test
    @DisplayName("예약 생성에 실패한다 (이미 예약된 사이트 동일 기간)")
    void create_fail_duplicate_same_period() {
        // Given
        String givenCustomerName = "홍길동";
        String givenSiteNumber = "A-3";
        LocalDate givenStartDate = LocalDate.now().plusDays(1);
        LocalDate givenEndDate = givenStartDate.plusDays(1);
        ReservationRequest req = ReservationRequestStub.get(
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
        ReservationRequest req2 = ReservationRequestStub.get(
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
