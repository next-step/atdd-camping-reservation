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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class ReservationTest {

    private static final String CUSTOMER_NAME = "홍길동";
    private static final String PHONE_NUMBER = "010-1234-5678";
    private static final String SITE_NUMBER = "A-1";

    private LocalDate startDate;
    private LocalDate endDate;
    private Campsite site;

    @Autowired
    private CampsiteRepository campsiteRepository;
    @Autowired
    private ReservationRepository reservationRepository;

    @BeforeEach
    void setUp() {
        startDate = LocalDate.now().plusDays(1);
        endDate = startDate.plusDays(3);

        campsiteRepository.deleteAll();
        reservationRepository.deleteAll();

        site = new Campsite();
        site.setSiteNumber(SITE_NUMBER);
        site.setDescription("대형 사이트 - 전기 있음, 화장실 인근");
        site.setMaxPeople(6);
        campsiteRepository.save(site);
    }

    private Map<String, String> createReservationMap(String customerName, LocalDate start, LocalDate end,
                                                     String siteNumber, String phone) {
        return Map.of(
                "customerName", customerName,
                "startDate", start.toString(),
                "endDate", end.toString(),
                "siteNumber", siteNumber,
                "phoneNumber", phone
        );
    }

    private ExtractableResponse<Response> postReservation(Map<String, String> reservation) {
        return RestAssured.given()
                .log().all()
                .contentType(ContentType.JSON)
                .body(reservation)
                .when()
                .post("/api/reservations")
                .then().log().all()
                .extract();
    }

    private void saveReservation(String customerName, LocalDate start, LocalDate end, Campsite campsite) {
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

    private void assertStatusAndMessage(ExtractableResponse<Response> response, int status, String message) {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.jsonPath().getString("message")).isEqualTo(message);
    }

    @Test
    @DisplayName("고객이 빈 사이트를 예약할 수 있다")
    void 예약_생성_성공() {
        Map<String, String> reservation = createReservationMap(CUSTOMER_NAME, startDate, endDate, SITE_NUMBER, PHONE_NUMBER);
        ExtractableResponse<Response> response = postReservation(reservation);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(response.jsonPath().getLong("id")).isPositive();
    }

    @Test
    @DisplayName("고객이 이미 예약된 사이트를 예약할 수 없다")
    void 이미_예약된_사이트_예약_생성_실패() {
        saveReservation(CUSTOMER_NAME, startDate, endDate, site);
        Map<String, String> reservation = createReservationMap(CUSTOMER_NAME, startDate, endDate, SITE_NUMBER, PHONE_NUMBER);

        ExtractableResponse<Response> response = postReservation(reservation);

        assertStatusAndMessage(response, HttpStatus.CONFLICT.value(), "해당 기간에 이미 예약이 존재합니다.");
    }

    @Test
    @DisplayName("동시에 같은 사이트를 예약하면 하나만 성공해야 한다")
    void 동시_예약_처리() throws InterruptedException {
        int threadCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<Integer> statusCodes = new CopyOnWriteArrayList<>();

        Map<String, String> reservation = createReservationMap(CUSTOMER_NAME, startDate, endDate, SITE_NUMBER, PHONE_NUMBER);

        Runnable task = () -> {
            try {
                startLatch.await();
                int status = postReservation(reservation).statusCode();
                statusCodes.add(status);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        };

        for (int i = 0; i < threadCount; i++) new Thread(task).start();

        startLatch.countDown();
        doneLatch.await();

        long success = statusCodes.stream().filter(c -> c == 201).count();
        long fail = statusCodes.stream().filter(c -> c == 409).count();

        assertThat(success).isEqualTo(1);
        assertThat(fail).isEqualTo(threadCount - 1);
    }

    @Test
    @DisplayName("예약은 오늘로부터 30일 이내에만 가능해야 한다")
    void 예약_30일_이내() {
        Map<String, String> reservation = createReservationMap(CUSTOMER_NAME,
                LocalDate.now().plusDays(31),
                LocalDate.now().plusDays(34),
                SITE_NUMBER, PHONE_NUMBER);

        ExtractableResponse<Response> response = postReservation(reservation);

        assertStatusAndMessage(response, HttpStatus.CONFLICT.value(), "예약은 오늘부터 30일 이내에만 가능합니다.");
    }
}