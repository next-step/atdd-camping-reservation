package com.camping.legacy;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class SiteTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("고객이 원하는 날짜에 사이트가 예약 가능한지 확인한다.")
    void 예약_가능_확인() {
        // Given
        String siteNumber = SITE_NUMBER;
        LocalDate reservationDate = LocalDate.now().plusDays(1);

        // When
        ExtractableResponse<Response> response = getAvailability(siteNumber, reservationDate);

        // Then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getString("siteNumber")).isEqualTo(siteNumber);
        assertThat(response.jsonPath().getString("date")).isEqualTo(reservationDate.toString());
        assertThat(response.jsonPath().getBoolean("available")).isTrue();
    }

    @Test
    @DisplayName("예약된 날짜와 사이트는 예약 불가능을 확인한다.")
    void 예약_불가_확인() {
        // Given: API로 사전 예약 생성
        Map<String, String> reservation = createReservationMap(
                CUSTOMER_NAME,
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(3),
                SITE_NUMBER,
                PHONE_NUMBER
        );
        postReservation(reservation);

        LocalDate reservedDate = LocalDate.now().plusDays(1);

        // When
        ExtractableResponse<Response> response = getAvailability(SITE_NUMBER, reservedDate);

        // Then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getString("siteNumber")).isEqualTo(SITE_NUMBER);
        assertThat(response.jsonPath().getString("date")).isEqualTo(reservedDate.toString());
        assertThat(response.jsonPath().getBoolean("available")).isFalse();
    }
}