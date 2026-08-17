package com.camping.legacy;

import com.camping.legacy.repository.ReservationRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-2: 전화번호는 필수다. 없으면 예약할 수 없다.
 *
 * 인수 조건: docs/acceptance-criteria.md
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReservationPhoneNumberAcceptanceTest {

    @LocalServerPort
    int port;

    @Autowired
    ReservationRepository reservationRepository;

    @BeforeEach
    void setUp(TestInfo testInfo) {
        RestAssured.port = port;
        reservationRepository.deleteAll();
        log.info("\n\n======== setUp completed — [{}] ========\n", testInfo.getDisplayName());
    }

    @Nested
    @DisplayName("예약 생성")
    class 예약_생성 {

        @Test
        @DisplayName("전화번호 없이 예약하면 거부된다")
        void 전화번호_없이_예약하면_거부된다() {
            String date = LocalDate.now().plusDays(1).toString();

            given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                      "siteNumber": "A-19",
                      "startDate": "%s",
                      "endDate": "%s",
                      "customerName": "테스터"
                    }
                    """.formatted(date, date))
            .when()
                .post("/api/reservations")
            .then()
                .statusCode(409);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("전화번호가 빈 값이면 예약이 거부된다")
        void 전화번호가_빈_값이면_예약이_거부된다(String blankPhoneNumber) {
            String date = LocalDate.now().plusDays(1).toString();

            given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                      "siteNumber": "A-20",
                      "startDate": "%s",
                      "endDate": "%s",
                      "customerName": "테스터",
                      "phoneNumber": "%s"
                    }
                    """.formatted(date, date, blankPhoneNumber))
            .when()
                .post("/api/reservations")
            .then()
                .statusCode(409);
        }

        @Test
        @DisplayName("전화번호가 있으면 예약이 완료된다")
        void 전화번호가_있으면_예약이_완료된다() {
            String date = LocalDate.now().plusDays(1).toString();

            given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                      "siteNumber": "B-1",
                      "startDate": "%s",
                      "endDate": "%s",
                      "customerName": "테스터",
                      "phoneNumber": "010-1111-2222"
                    }
                    """.formatted(date, date))
            .when()
                .post("/api/reservations")
            .then()
                .statusCode(201);
        }
    }

    @Nested
    @DisplayName("예약 수정")
    class 예약_수정 {

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("전화번호를 빈 값으로 변경하면 거부된다")
        void 전화번호를_빈_값으로_변경하면_거부된다(String blankPhoneNumber) {
            String date = LocalDate.now().plusDays(1).toString();

            var created = given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                      "siteNumber": "B-2",
                      "startDate": "%s",
                      "endDate": "%s",
                      "customerName": "테스터",
                      "phoneNumber": "010-1111-2222"
                    }
                    """.formatted(date, date))
            .when()
                .post("/api/reservations")
            .then()
                .statusCode(201)
                .extract().response();

            long id = ((Number) created.path("id")).longValue();
            String confirmationCode = created.path("confirmationCode");

            given()
                .contentType(ContentType.JSON)
                .queryParam("confirmationCode", confirmationCode)
                .body("""
                    {
                      "phoneNumber": "%s"
                    }
                    """.formatted(blankPhoneNumber))
            .when()
                .put("/api/reservations/" + id)
            .then()
                .statusCode(400);
        }

        @Test
        @DisplayName("전화번호 필드 없이 다른 필드만 수정하면 기존 전화번호가 유지된다")
        void 전화번호_필드_없이_다른_필드만_수정하면_기존_전화번호가_유지된다() {
            String date = LocalDate.now().plusDays(1).toString();

            var created = given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                      "siteNumber": "B-3",
                      "startDate": "%s",
                      "endDate": "%s",
                      "customerName": "테스터",
                      "phoneNumber": "010-2222-3333"
                    }
                    """.formatted(date, date))
            .when()
                .post("/api/reservations")
            .then()
                .statusCode(201)
                .extract().response();

            long id = ((Number) created.path("id")).longValue();
            String confirmationCode = created.path("confirmationCode");

            String phoneNumber = given()
                .contentType(ContentType.JSON)
                .queryParam("confirmationCode", confirmationCode)
                .body("""
                    {
                      "customerName": "테스터2"
                    }
                    """)
            .when()
                .put("/api/reservations/" + id)
            .then()
                .statusCode(200)
                .extract().path("phoneNumber");

            assertThat(phoneNumber).isEqualTo("010-2222-3333");
        }

        @Test
        @DisplayName("전화번호를 정상 형식의 새 값으로 변경하면 예약이 수정된다")
        void 전화번호를_정상_형식의_새_값으로_변경하면_예약이_수정된다() {
            String date = LocalDate.now().plusDays(1).toString();

            var created = given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                      "siteNumber": "B-4",
                      "startDate": "%s",
                      "endDate": "%s",
                      "customerName": "테스터",
                      "phoneNumber": "010-2222-3333"
                    }
                    """.formatted(date, date))
            .when()
                .post("/api/reservations")
            .then()
                .statusCode(201)
                .extract().response();

            long id = ((Number) created.path("id")).longValue();
            String confirmationCode = created.path("confirmationCode");

            given()
                .contentType(ContentType.JSON)
                .queryParam("confirmationCode", confirmationCode)
                .body("""
                    {
                      "phoneNumber": "010-9999-8888"
                    }
                    """)
            .when()
                .put("/api/reservations/" + id)
            .then()
                .statusCode(200)
                .body("phoneNumber", org.hamcrest.Matchers.equalTo("010-9999-8888"));
        }
    }
}
