package com.camping.legacy.acceptance;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// API 레벨의 인수테스트를 만들기 위해 스프링 부트 테스트 활용
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/sql/data.sql",    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class ReservationTest {
    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.config = RestAssured.config()
                .logConfig(LogConfig.logConfig()
                        .enablePrettyPrinting(true)
                        .blacklistHeader("Authorization", "Cookie", "Set-Cookie"));
        RestAssured.requestSpecification = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept("application/json; charset=UTF-8")
                .build();
    }

    @DisplayName("예약 정보를 모두 정상적으로 등록해 예약에 성공한다.")
    @Test
    void reservationSuccessCase() {
        /**
         * 누가 : 예약하려는 사용자
         * 언제 : 오늘
         * 무엇을 : 30일내로 비어있는 날짜에 대한 예약을
         * 왜 : 동시성 제어를 고려하지 않은 상태에서 예약에 성공하기 위해
         */
        // given - 오늘 날짜를 기준으로 30일 이내의 날짜를 계산하고
        Map<String, String> map = Map.of(
                "customerName", "홍길동"
                , "startDate", "2025-09-09"
                , "endDate", "2025-09-10"
                , "siteNumber", "B-1"
                , "phoneNumber", "010-1234-1234"
        );

        // when - 30일 이내 날짜의 예약을 신청했을 때
        ExtractableResponse<Response> response = RestAssured
                .given()
                    .body(map)
                .when()
                    .post("/api/reservations")
                .then()
                .extract();

        // then - 예약에 성공한다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    }

    @DisplayName("예약은 오늘로부터 30일 이내에만 가능해야 한다")
    @Test // 테스트가 뜰 때, Spring 부트 애플리케이션에 해당되는 컴포넌트들이 테스트 컨테이너에 Bean으로 같이 환경으로 딸려 올라감.
    // 이때, 딸려올라가는 웹 설정도 같이 해줄 수 있다.
    void reservationDateLimit() {
        /**
         * 누가 : 예약하려는 사용자
         * 언제 : 오늘
         * 무엇을 : 30일 이후의 날짜 예약을
         * 왜 : 30일 이후의 날짜는 예약할 수 없다는 안내를 받기 위해
         */
        // given - 오늘 날짜를 기준으로 30일 이후의 날짜를 계산하고
        Map<String, String> map = Map.of(
              "customerName", "홍길동"
            , "startDate", "2025-10-09"
            , "endDate", "2025-10-10"
            , "siteNumber", "B-1"
            , "phoneNumber", "010-1234-1234"
        );

        // when - 30일 이후 날짜의 예약을 신청했을 때
        ExtractableResponse<Response> response = RestAssured
                .given()
                    .body(map)
                .when()
                    .post("/api/reservations")
                .then()
                .extract();

        // then - 예약에 실패하고, "30일 이후의 날짜는 예약할 수 없다는 안내"를 받는다.
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.jsonPath().getString("message")).isEqualTo("오늘로부터 30일 이내로만 예약할 수 있습니다.");
    }

    @DisplayName("존재하는 캠핑장으로 예약 수정에 성공하다")
    @Test
    void reservationExistCampsite() {
        /**
         * 누가 : 홍길동이
         * 무엇을 : B-1 캠핑장을
         * 어떻게 : 예약 1을 확인 코드 "ABC123"으로 캠핑장 수정
         * 왜 : 존재하는 캠핑장에 대해 예약이 정상 수정되는지 확인하기 위해
         */
        // given - 예약 번호, 존재하는 캠핑장 번호가 담긴 예약 정보와 생성된 확인코드로
        Long id = 1L;
        String baseURI = "/api/reservations/";
        ExtractableResponse<Response> reservationInfo = RestAssured
                .given()
                .when()
                .get(baseURI + id)
                .then()
                .extract();

        Long reservationId = Long.valueOf(reservationInfo.jsonPath().getString("id"));
        String customerName = reservationInfo.jsonPath().getString("customerName");
        String startDate = reservationInfo.jsonPath().getString("startDate");
        String endDate = reservationInfo.jsonPath().getString("endDate");
        String phoneNumber = reservationInfo.jsonPath().getString("phoneNumber");
        String confirmationCode = reservationInfo.jsonPath().getString("confirmationCode");

        Map<String, String> map = Map.of(
                "customerName", customerName
                , "startDate", startDate
                , "endDate", endDate
                , "siteNumber", "B-1"
                , "phoneNumber", phoneNumber
        );


        // when - 존재하는 캠핑장으로 예약을 수정했을 때
        ExtractableResponse<Response> response = RestAssured
                .given()
                    .body(map)
                .when()
                    .put("/api/reservations/" + reservationId + "?confirmationCode=" + confirmationCode)
                .then()
                .extract();

        // then - 예약에 성공한다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    }

    @DisplayName("존재하는 캠핑장만 예약 수정할 수 있다")
    @Test
    void reservationNotExistCampsite() {
        /**
         * 누가 : 홍길동이
         * 무엇을 : x-999 캠핑장을
         * 어떻게 : 예약 1을 확인 코드 "ABC123"으로 캠핑장 수정
         * 왜 : 존재하는 캠핑장만 예약할 수 있다는 안내를 받기 위해 
         */
        // given - 예약 번호, 존재하지 않는 캠핑장 번호가 담긴 예약 정보와 생성된 확인코드로
        Long id = 1L;
        String baseURI = "/api/reservations/";
        ExtractableResponse<Response> reservationInfo = RestAssured
                .given()
                .when()
                    .get(baseURI + id)
                .then()
                .extract();

        Long reservationId = Long.valueOf(reservationInfo.jsonPath().getString("id"));
        String customerName = reservationInfo.jsonPath().getString("customerName");
        String startDate = reservationInfo.jsonPath().getString("startDate");
        String endDate = reservationInfo.jsonPath().getString("endDate");
        String phoneNumber = reservationInfo.jsonPath().getString("phoneNumber");
        String confirmationCode = reservationInfo.jsonPath().getString("confirmationCode");

        Map<String, String> map = Map.of(
                "customerName", customerName
                , "startDate", startDate
                , "endDate", endDate
                , "siteNumber", "x-999"
                , "phoneNumber", phoneNumber
        );

        // when - 존재하지 않는 캠핑장으로 예약을 수정했을 때
        ExtractableResponse<Response> response = RestAssured
                .given()
                    .body(map)
                .when()
                    .put("/api/reservations/" + reservationId + "?confirmationCode=" + confirmationCode)
                .then()
                .extract();

        // then - 예약에 실패하고, "존재하지 않는 캠핑장입니다"라는 안내를 받는다.
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.jsonPath().getString("message")).isEqualTo("존재하지 않는 캠핑장입니다.");
    }
}