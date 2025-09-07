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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class ReservationTest {

    private LocalDate startDate;
    private LocalDate endDate;
    private Campsite site;
    @Autowired
    private CampsiteRepository campsiteRepository;
    @Autowired
    private ReservationRepository reservationRepository;

    @BeforeEach
    void setUp() {
        startDate = LocalDate.now().plusDays(1);  // 내일
        endDate = startDate.plusDays(3);

        campsiteRepository.deleteAll();

        site = new Campsite();
        site.setSiteNumber("A-1");
        site.setDescription("대형 사이트 - 전기 있음, 화장실 인근");
        site.setMaxPeople(6);
        campsiteRepository.save(site);

        reservationRepository.deleteAll();
    }

    @Test
    @DisplayName("고객이 빈 사이트를 예약할 수 있다")
    void 예약_생성_성공() {
        // Given: 비어있는 사이트 정보
        Map<String, String> map = Map.of(
                "customerName", "홍길동",
                "startDate", startDate.toString(),
                "endDate", endDate.toString(),
                "siteNumber", "A-1",
                "phoneNumber", "010-1234-5678"
        );

        // When: 예약을 요청
        ExtractableResponse<Response> response = RestAssured
                .given().log().all()
                .contentType(ContentType.JSON)
                .body(map)
                .when()
                .post("/api/reservations")
                .then().log().all()
                .extract();

        // Then: 예약이 성공적으로 생성되었는지 검증 (상태 코드 201, 응답 데이터 등)
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(response.jsonPath().getLong("id")).isPositive();
    }

    @Test
    @DisplayName("고객이 이미 예약된 사이트를 예약할 수 없다")
    void 이미_예약된_사이트_예약_생성_실패() {
        // Given: 예약 생성
        Reservation reservation = new Reservation();
        reservation.setCustomerName("홍길동");
        reservation.setStartDate(startDate);
        reservation.setEndDate(endDate);
        reservation.setReservationDate(LocalDate.now().plusDays(7));
        reservation.setCampsite(site);  // Campsite 엔티티 연결
        reservation.setPhoneNumber("010-1234-5678");
        reservation.setStatus("CONFIRMED");
        reservation.setConfirmationCode("ABC123");
        reservation.setCreatedAt(LocalDateTime.now());

        reservationRepository.save(reservation);

        Map<String, String> map = Map.of(
                "customerName", "홍길동",
                "startDate", startDate.toString(),
                "endDate", endDate.toString(),
                "siteNumber", "A-1",
                "phoneNumber", "010-1234-5678"
        );

        // When: 예약을 요청
        ExtractableResponse<Response> response = RestAssured
                .given().log().all()
                .contentType(ContentType.JSON)
                .body(map)
                .when()
                .post("/api/reservations")
                .then().log().all()
                .extract();

        // Then: 예약이 성공적으로 생성되었는지 검증 (상태 코드 201, 응답 데이터 등)
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.jsonPath().getString("message")).isEqualTo("해당 기간에 이미 예약이 존재합니다.");
    }
}
