package com.camping.legacy.acceptance;

import com.camping.legacy.dto.ReservationRequest;
import com.camping.legacy.dto.ReservationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.transaction.Transactional;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

@DisplayNameGeneration(ReplaceUnderscores.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class ReservationAcceptanceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
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
}
