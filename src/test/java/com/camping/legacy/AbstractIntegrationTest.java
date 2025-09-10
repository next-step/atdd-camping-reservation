package com.camping.legacy;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.domain.Reservation;
import com.camping.legacy.repository.CampsiteRepository;
import com.camping.legacy.repository.ReservationRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public abstract class AbstractIntegrationTest {

    @Autowired
    protected CampsiteRepository campsiteRepository;

    @Autowired
    protected ReservationRepository reservationRepository;

    protected static final String CUSTOMER_NAME = "홍길동";
    protected static final String PHONE_NUMBER = "010-1234-5678";
    protected static final String SITE_NUMBER = "A-1";

    protected Campsite site;

    @BeforeEach
    void setUpBase() {
        campsiteRepository.deleteAll();
        reservationRepository.deleteAll();

        site = new Campsite();
        site.setSiteNumber(SITE_NUMBER);
        site.setDescription("대형 사이트 - 전기 있음, 화장실 인근");
        site.setMaxPeople(6);
        campsiteRepository.save(site);
    }

    protected Map<String, String> createReservationMap(String customerName, LocalDate start, LocalDate end,
                                                       String siteNumber, String phone) {
        return Map.of(
                "customerName", customerName,
                "startDate", start.toString(),
                "endDate", end.toString(),
                "siteNumber", siteNumber,
                "phoneNumber", phone
        );
    }

    protected ExtractableResponse<Response> postReservation(Map<String, String> reservation) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(reservation)
                .when()
                .post("/api/reservations")
                .then()
                .extract();
    }

    protected ExtractableResponse<Response> cancelReservation(Long reservationId) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .when()
                .delete("/api/reservations/" + reservationId)
                .then()
                .extract();
    }

    protected void saveReservation(String customerName, LocalDate start, LocalDate end, Campsite campsite) {
        Reservation reservation = new Reservation();
        reservation.setCustomerName(customerName);
        reservation.setStartDate(start);
        reservation.setEndDate(end);
        reservation.setReservationDate(LocalDate.now().plusDays(7));
        reservation.setCampsite(campsite);
        reservation.setPhoneNumber(PHONE_NUMBER);
        reservation.setStatus("CONFIRMED");
        reservation.setConfirmationCode("ABC123");
        reservation.setCreatedAt(LocalDateTime.now());
        reservationRepository.save(reservation);
    }

    protected ExtractableResponse<Response> getAvailability(String siteNumber, LocalDate date) {
        return RestAssured
                .when()
                .get("/api/sites/" + siteNumber + "/availability?date=" + date)
                .then().log().all()
                .extract();
    }

    protected void assertStatusAndMessage(ExtractableResponse<Response> response, int status, String message) {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.jsonPath().getString("message")).isEqualTo(message);
    }
}
