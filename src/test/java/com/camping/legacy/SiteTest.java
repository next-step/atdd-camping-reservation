package com.camping.legacy;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.domain.Reservation;
import com.camping.legacy.repository.CampsiteRepository;
import com.camping.legacy.repository.ReservationRepository;
import io.restassured.RestAssured;
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
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class SiteTest {
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
    @DisplayName("고객이 원하는 날짜에 사이트가 예약 가능한지 확인한다.")
    void 예약_가능_확인() {
        // Given: 고객이 특정 날짜와 사이트 A-1을 선택했을 때
        String siteNumber = "A-1";
        LocalDate reservationDate = startDate;

        // When: 가용성 조회를 하면
        ExtractableResponse<Response> response = RestAssured
                .when()
                .get("/api/sites/"+siteNumber+"/availability?date="+reservationDate)
                .then().log().all()
                .extract();

        // Then: 시스템은 예약 가능 상태를 보여준다.
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getString("siteNumber")).isEqualTo(siteNumber);
        assertThat(response.jsonPath().getString("date")).isEqualTo(reservationDate.toString());
        assertThat(response.jsonPath().getBoolean("available")).isTrue();
    }

    @Test
    @DisplayName("예약된 날짜와 사이트는 예약 불가능을 확인한다.")
    void 예약_불가_확인() {
        // Given: 고객이 이미 예약된 날짜와 사이트를 선택했을 때
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
        String siteNumber = reservation.getCampsite().getSiteNumber();
        LocalDate reservationDate = reservation.getStartDate();

        // When: 가용성 조회를 하면
        ExtractableResponse<Response> response = RestAssured
                .when()
                .get("/api/sites/"+siteNumber+"/availability?date="+reservationDate)
                .then().log().all()
                .extract();

        // Then: 시스템은 예약 불가 상태를 보여준다.
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getString("siteNumber")).isEqualTo(siteNumber);
        assertThat(response.jsonPath().getString("date")).isEqualTo(reservationDate.toString());
        assertThat(response.jsonPath().getBoolean("available")).isFalse();
    }
}
