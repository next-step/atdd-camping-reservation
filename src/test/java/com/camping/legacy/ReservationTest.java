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

    @Test
    @DisplayName("동시에 같은 사이트를 예약하면 하나만 성공해야 한다")
    void 동시_예약_처리() throws InterruptedException {
        // Given
        int threadCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1); // 출발 신호
        CountDownLatch doneLatch = new CountDownLatch(threadCount); // 완료 신호
        List<Integer> statusCodes = new CopyOnWriteArrayList<>();

        Map<String, String> reservation = Map.of(
                "customerName", "홍길동",
                "startDate", startDate.toString(),
                "endDate", endDate.toString(),
                "siteNumber", "A-1",
                "phoneNumber", "010-1234-5678"
        );

        // When
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await(); // 모든 스레드가 동시에 출발하도록 대기
                    int statusCode = RestAssured.given()
                            .contentType(ContentType.JSON)
                            .body(reservation)
                            .when()
                            .post("/api/reservations")
                            .getStatusCode();
                    statusCodes.add(statusCode);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown(); // 끝났음을 알림
                }
            }).start();
        }

        startLatch.countDown(); // 모든 스레드 동시에 출발
        doneLatch.await();      // 모든 요청이 끝날 때까지 대기

        // Then
        long successCount = statusCodes.stream().filter(code -> code == 201).count();
        long failCount = statusCodes.stream().filter(code -> code == 409).count();

        assertThat(successCount).isEqualTo(1);
        assertThat(failCount).isEqualTo(threadCount - 1);
    }
}
