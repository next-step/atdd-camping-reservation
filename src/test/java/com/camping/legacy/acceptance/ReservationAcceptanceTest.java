package com.camping.legacy.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.camping.legacy.dto.ReservationRequest;
import com.camping.legacy.dto.ReservationResponse;
import com.camping.legacy.test_config.TestConfig;
import com.camping.legacy.test_utils.CleanUp;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;


@Sql("/data.sql")
@ActiveProfiles("test")
@Import(TestConfig.class)
@DisplayNameGeneration(ReplaceUnderscores.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReservationAcceptanceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CleanUp cleanUp;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @AfterEach
    void tearDown() {
        cleanUp.all();
    }

    /**
     * When 사용자가 이름, 전화번호로 사이트 'A-1'에 '2025-09-10' 날짜로 예약을 요청하면
     * Then 예약은 성공적으로 처리된다.
     * And 응답에는 6자리의 영숫자 확인 코드가 포함되어야 한다.
     */
    @Test
    void 사용자가_유효한_정보로_예약을_성공적으로_생성한다() throws Exception {
        // when
        ReservationRequest request = ReservationRequest.builder()
                .customerName("김철수")
                .phoneNumber("010-1234-5678")
                .startDate(LocalDate.of(2025, 9, 10))
                .endDate(LocalDate.of(2025, 9, 11))
                .siteNumber("A-1")
                .numberOfPeople(2)
                .build();

        String responseBody = RestAssured
                .given()
                .log().all()
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .when()
                .post("/api/reservations")
                .then()
                .log().all()
                .statusCode(HttpStatus.CREATED.value())
                .extract().asString();

        // then
        ReservationResponse response = objectMapper.readValue(responseBody, ReservationResponse.class);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.getConfirmationCode()).hasSize(6);
            softly.assertThat(response.getConfirmationCode()).isNotNull();
        });
    }

    /**
     * Given 2025-09-10일에 대형 사이트 'A-1'에 이미 예약이 존재할 때
     * When 다른 사용자가 사이트 'A-1'에 대해 2025-09-10일을 포함하는 기간으로 예약을 요청하면
     * Then "이미 예약된 날짜입니다."와 같은 메시지와 함께 예약이 실패한다.
     */
    @Test
    void 이미_예약된_날짜에_중복으로_예약을_시도하면_실패한다() throws Exception {
        // Given: 2025-09-10일에 대형 사이트 'A-1'에 이미 예약이 존재할 때
        ReservationRequest initialRequest = ReservationRequest.builder()
                .customerName("홍길동")
                .phoneNumber("010-1111-2222")
                .startDate(LocalDate.of(2025, 9, 10))
                .endDate(LocalDate.of(2025, 9, 11))
                .siteNumber("A-1")
                .numberOfPeople(2)
                .build();

        RestAssured
                .given()
                    .contentType(ContentType.JSON)
                    .body(objectMapper.writeValueAsString(initialRequest))
                .when()
                    .post("/api/reservations")
                .then()
                    .statusCode(HttpStatus.CREATED.value());

        // When: 다른 사용자가 사이트 'A-1'에 대해 2025-09-10일을 포함하는 기간으로 예약을 요청하면
        ReservationRequest conflictingRequest = ReservationRequest.builder()
                .customerName("김중복")
                .phoneNumber("010-3333-4444")
                .startDate(LocalDate.of(2025, 9, 10)) // 중복되는 날짜
                .endDate(LocalDate.of(2025, 9, 11))
                .siteNumber("A-1")
                .numberOfPeople(3)
                .build();

        String errorMessage = RestAssured
                .given()
                    .log().all()
                    .contentType(ContentType.JSON)
                    .body(objectMapper.writeValueAsString(conflictingRequest))
                .when()
                    .post("/api/reservations")
                .then()
                    .log().all()
                    .statusCode(HttpStatus.CONFLICT.value())
                    .extract().path("message");

        // Then: "해당 기간에 이미 예약이 존재합니다."와 같은 메시지와 함께 예약이 실패한다.
        assertThat(errorMessage).contains("해당 기간에 이미 예약이 존재합니다.");
    }

    /**
     * Scenario: 과거 날짜로 예약을 시도하면 실패한다.
     * When 사용자가 예약할 때, 시작 날짜를 오늘보다 과거로 지정하면
     * Then "유효하지 않은 날짜입니다."와 같은 메시지와 함께 예약은 실패한다.
     */
    @Test
    void 과거_날짜로_예약을_시도하면_실패한다() throws Exception {
        // When: 사용자가 예약할 때, 시작 날짜를 오늘보다 과거로 지정하면
        ReservationRequest request = ReservationRequest.builder()
                .customerName("김시간")
                .phoneNumber("010-9876-5432")
                .startDate(LocalDate.now().minusDays(1)) // 과거 날짜
                .endDate(LocalDate.now().plusDays(1))
                .siteNumber("A-1")
                .numberOfPeople(2)
                .build();

        String errorMessage = RestAssured
                .given()
                    .log().all()
                    .contentType(ContentType.JSON)
                    .body(objectMapper.writeValueAsString(request))
                .when()
                    .post("/api/reservations")
                .then()
                    .log().all()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                .extract().path("message");

        // Then: "유효하지 않은 날짜입니다."와 같은 메시지와 함께 예약은 실패한다.
        assertThat(errorMessage).contains("유효하지 않은 날짜입니다.");
    }
}
