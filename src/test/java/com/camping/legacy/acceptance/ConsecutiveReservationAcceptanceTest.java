package com.camping.legacy.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.camping.legacy.acceptance.fixture.ReservationRequestFixture;
import com.camping.legacy.dto.ReservationRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(statements = {
        "TRUNCATE TABLE reservations",
        "ALTER TABLE reservations ALTER COLUMN id RESTART WITH 1"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ConsecutiveReservationAcceptanceTest {

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    @DisplayName("한 사용자가 연속된 날짜로 예약을 시도하면 모두 성공하는지")
    @Test
    void reservationWithConsecutiveDates() {
        // when: 사용자가 3박 4일 연박 예약을 시도한다
        final LocalDate start = LocalDate.now();
        final LocalDate end = start.plusDays(4);

        ReservationRequest request = ReservationRequestFixture.builder()
                .startDate(start)
                .endDate(end)
                .build();

        var response = RestAssured
                .given().log().all()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/reservations")
                .then()
                .extract();

        // then: 예약이 성공한다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    }

    @DisplayName("한 사용자가 연속된 날짜로 예약을 시도할 때 일부 날짜가 이미 예약되어 있으면 예약이 실패하는지")
    @Test
    void reservationWithPartiallyBookedDates() {
        // given: 3박 4일 중간 날짜가 이미 예약되어 있다
        final LocalDate existingStart = LocalDate.now();
        final LocalDate existingEnd = existingStart.plusDays(3);

        ReservationRequest existingRequest = ReservationRequestFixture.builder()
                .startDate(existingStart)
                .endDate(existingEnd)
                .customerName("TEST-EXISTING")
                .build();

        RestAssured
                .given().log().all()
                .contentType(ContentType.JSON)
                .body(existingRequest)
                .when()
                .post("/api/reservations")
                .then()
                .statusCode(HttpStatus.CREATED.value());

        // when: 사용자가 3박 4일 연박 예약을 시도한다 (일부 날짜가 겹침)
        final LocalDate newStart = existingStart.plusDays(2);
        final LocalDate newEnd = newStart.plusDays(4);

        ReservationRequest newRequest = ReservationRequestFixture.builder()
                .startDate(newStart)
                .endDate(newEnd)
                .customerName("TEST-NEW")
                .build();

        var response = RestAssured
                .given().log().all()
                .contentType(ContentType.JSON)
                .body(newRequest)
                .when()
                .post("/api/reservations")
                .then()
                .extract();

        // then: 예약이 실패한다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    }
}
