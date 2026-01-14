package com.camping.acceptance;

import com.camping.legacy.CampingApplication;
import com.camping.legacy.domain.Campsite;
import com.camping.legacy.dto.ReservationResponse;
import com.camping.legacy.repository.CampsiteRepository;
import com.camping.support.DatabaseCleaner;
import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(classes = CampingApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(DatabaseCleaner.class)
@DisplayName("캠핑장 예약 인수 테스트")
public class ReservationAcceptanceTest {

    @LocalServerPort
    private int port;

    private static final String SITE_A1_NUMBER = "A-01";
    private static final String SITE_B2_NUMBER = "B-02";
    private static final String SITE_C3_NUMBER = "C-03";
    private static final String SITE_D4_NUMBER = "D-04";
    private static final String SITE_E5_NUMBER = "E-05";


    @Autowired
    CampsiteRepository campsiteRepository;

    @Autowired
    DatabaseCleaner databaseCleaner;

    private Campsite siteA1;
    private Campsite siteB2;
    private Campsite siteC3;
    private Campsite siteD4;
    private Campsite siteE5;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        databaseCleaner.execute();

        siteA1 = campsiteRepository.save(create(SITE_A1_NUMBER));
        siteB2 = campsiteRepository.save(create(SITE_B2_NUMBER));
        siteC3 = campsiteRepository.save(create(SITE_C3_NUMBER));
        siteD4 = campsiteRepository.save(create(SITE_D4_NUMBER));
        siteE5 = campsiteRepository.save(create(SITE_E5_NUMBER));
    }

    /**
     * 시나리오: 여러 사용자가 동시에 예약을 시도할 경우 오직 하나만 성공한다 (동시성 제어)
     * Given "A-01" 사이트의 "2027-08-15"부터 "2027-08-17"까지 예약이 비어있다
     * When 10명의 사용자가 동시에 "A-01" 사이트의 "2027-08-15"부터 "2027-08-17"까지 예약을 요청한다
     * Then 단 1개의 예약 요청만 "성공" 응답을 받는다
     * And 나머지 9개의 예약 요청은 "실패" 응답과 함께 "이미 예약이 완료되었거나 진행 중인 요청이 있습니다." 메시지를 받는다
     * And 최종적으로 데이터베이스에는 "A-01" 사이트의 "2027-08-15"부터 "2027-08-17"까지 단 1개의 예약만 저장된다
     */
    @Test
    @DisplayName("여러_사용자가_동시에_예약을_시도할_경우_오직_하나만_성공한다")
    void concurrencyControl_OnlyOneReservationSucceeds() throws InterruptedException {
        // given
        int numberOfUsers = 10;
        LocalDate startDate = LocalDate.of(2027, 8, 15);
        LocalDate endDate = LocalDate.of(2027, 8, 17);

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfUsers);
        CountDownLatch latch = new CountDownLatch(numberOfUsers);
        ConcurrentLinkedQueue<Response> responses = new ConcurrentLinkedQueue<>();

        // when
        for (int i = 0; i < numberOfUsers; i++) {
            int userIndex = i;
            executorService.submit(() -> {
                try {
                    Map<String, Object> request = new HashMap<>();
                    request.put("siteNumber", SITE_A1_NUMBER);
                    request.put("startDate", startDate.toString());
                    request.put("endDate", endDate.toString());
                    request.put("customerName", "사용자-" + userIndex);
                    request.put("phoneNumber", "010-1234-567" + userIndex);

                    Response response = RestAssured.given()
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .body(request)
                            .when()
                            .post("/api/reservations")
                            .then()
                            .extract().response();
                    responses.add(response);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();

        // then
        long successCount = responses.stream().filter(r -> r.statusCode() == HttpStatus.CREATED.value()).count();
        long failureCount = responses.stream().filter(r -> r.statusCode() == HttpStatus.CONFLICT.value()).count();

        assertThat(successCount).isEqualTo(1);
        assertThat(failureCount).isEqualTo(9);

        List<ReservationResponse> reservations = RestAssured.given()
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .get("/api/reservations")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", ReservationResponse.class);

        long finalReservationCount = reservations.stream()
                .filter(r -> r.getSiteNumber().equals(SITE_A1_NUMBER) &&
                        r.getStartDate().equals(startDate) &&
                        r.getEndDate().equals(endDate))
                .count();
        assertThat(finalReservationCount).isEqualTo(1);
    }


    /**
     * 시나리오: 예약 직후 월별 캘린더에 즉시 반영된다
     * Given "B-02" 사이트의 "2027-09-10" 날짜가 비어있다
     * When 사용자가 "B-02" 사이트의 "2027-09-10" 날짜를 예약한다
     * Then 사용자가 "2027년 9월" 월별 캘린더를 조회하면 "10일"은 "예약 불가능" 상태로 표시된다
     */
    @Test
    @DisplayName("예약_직후_월별_캘린더에_즉시_반영된다")
    void calendarReflectsReservationImmediately() {
        // given
        LocalDate reservationDate = LocalDate.of(2027, 9, 10);

        // when
        createReservationFor(SITE_B2_NUMBER, "김캘린더", reservationDate, reservationDate.plusDays(1));

        // then
        ExtractableResponse<Response> response = RestAssured
                .given().log().all()
                .param("year", 2027)
                .param("month", 9)
                .param("siteId", siteB2.getId())
                .when()
                .get("/api/reservations/calendar")
                .then().log().all()
                .extract();

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());

        List<Map<String, Object>> days = response.jsonPath().getList("days");
        Map<String, Object> day10 = days.stream()
                .filter(d -> d.get("date").equals("2027-09-10"))
                .findFirst()
                .orElseThrow();

        assertThat(day10.get("available")).isEqualTo(false);
        assertThat(day10.get("customerName")).isEqualTo("김캘린더");
    }

    /**
     * 시나리오: 예약 취소 직후 예약 가능 목록에 즉시 반영된다
     * Given "C-03" 사이트의 "2027-10-20" 날짜가 "김영희"에게 예약되어 있다
     * When "김영희"가 해당 예약을 취소한다
     * Then 사용자가 "2027-10-20" 날짜로 예약 가능한 사이트를 검색하면 결과에 "C-03" 사이트가 포함된다
     */
    @Test
    @DisplayName("예약_취소_직후_예약_가능_목록에_즉시_반영된다")
    void siteBecomesAvailableAfterCancellation() {
        // given
        LocalDate reservationDate = LocalDate.of(2027, 10, 20);
        ReservationResponse reservation = createReservationFor(SITE_C3_NUMBER, "김영희", reservationDate, reservationDate.plusDays(1));

        // when
        RestAssured
                .given().log().all()
                .queryParam("confirmationCode", reservation.getConfirmationCode())
                .when()
                .delete("/api/reservations/" + reservation.getId())
                .then().log().all()
                .statusCode(HttpStatus.OK.value());

        // then
        ExtractableResponse<Response> searchResponse = RestAssured
                .given().log().all()
                .param("startDate", reservationDate.toString())
                .param("endDate", reservationDate.plusDays(1).toString())
                .when()
                .get("/api/sites/search")
                .then().log().all()
                .extract();

        assertThat(searchResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
        List<String> availableSites = searchResponse.jsonPath().getList("siteNumber");
        assertThat(availableSites).contains(SITE_C3_NUMBER);
    }

    /**
     * 시나리오: 연박 예약 시 중간 날짜가 이미 예약된 경우 조회되지 않는다
     * Given "D-04" 사이트의 "2027-11-03" 날짜가 이미 예약되어 있다
     * When 사용자가 "D-04" 사이트에 대해 "2027-11-01"부터 "2027-11-05"까지 예약 가능 여부를 조회한다
     * Then "D-04" 사이트는 예약 불가능한 것으로 나타나야 한다
     */
    @Test
    @DisplayName("연박_예약_시_중간_날짜가_이미_예약된_경우_조회되지_않는다")
    void searchingForOverlappingDatesExcludesSite() {
        // given
        LocalDate reservedDate = LocalDate.of(2027, 11, 3);
        createReservationFor(SITE_D4_NUMBER, "박중간", reservedDate, reservedDate.plusDays(1));

        // when
        ExtractableResponse<Response> searchResponse = RestAssured
                .given().log().all()
                .param("startDate", "2027-11-01")
                .param("endDate", "2027-11-05")
                .when()
                .get("/api/sites/search")
                .then().log().all()
                .extract();

        // then
        assertThat(searchResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
        List<String> availableSites = searchResponse.jsonPath().getList("siteNumber");
        assertThat(availableSites).doesNotContain(SITE_D4_NUMBER);
    }

    /**
     * 시나리오: 정확한 확인 코드를 입력해야만 예약을 취소할 수 있다
     * Given "E-05" 사이트가 "2026-02-14" 날짜에 "박서준"의 이름으로 예약되어 있고 확인 코드는 "ABC123"이다
     * When "박서준"이 예약 취소를 위해 "ABC123", "WRONG456", "" 코드를 입력한다
     * Then 각각 "취소됨", "유지됨", "유지됨" 상태가 되고 해당하는 메시지를 받는다
     */
    @Test
    @DisplayName("정확한_확인_코드를_입력해야만_예약을_취소할_수_있다")
    void cancellationRequiresCorrectConfirmationCode() {
        // given
        LocalDate reservationDate = LocalDate.of(2026, 2, 14);
        ReservationResponse reservation = createReservationFor(SITE_E5_NUMBER, "박서준", reservationDate, reservationDate.plusDays(1));
        String correctCode = reservation.getConfirmationCode();
        String wrongCode = "WRONG456";

        // when & then: Case 1 - Wrong Code
        ExtractableResponse<Response> wrongCodeResponse = RestAssured
                .given().log().all()
                .queryParam("confirmationCode", wrongCode)
                .when()
                .delete("/api/reservations/" + reservation.getId())
                .then().log().all()
                .extract();

        assertThat(wrongCodeResponse.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(wrongCodeResponse.jsonPath().getString("message")).isEqualTo("확인 코드가 일치하지 않습니다.");

        ReservationResponse reservationAfterWrongCode = getReservationById(reservation.getId());
        assertThat(reservationAfterWrongCode.getStatus()).isNotEqualTo("CANCELLED");

        // when & then: Case 2 - Correct Code
        ExtractableResponse<Response> correctCodeResponse = RestAssured
                .given().log().all()
                .queryParam("confirmationCode", correctCode)
                .when()
                .delete("/api/reservations/" + reservation.getId())
                .then().log().all()
                .extract();

        assertThat(correctCodeResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(correctCodeResponse.jsonPath().getString("message")).isEqualTo("예약이 취소되었습니다.");

        ReservationResponse reservationAfterCorrectCode = getReservationById(reservation.getId());
        assertThat(reservationAfterCorrectCode.getStatus()).isEqualTo("CANCELLED");
    }

    private Campsite create(String siteNumber) {
        return Campsite.builder()
                .siteNumber(siteNumber)
                .description("test")
                .maxPeople(10)
                .build();
    }

    private ReservationResponse createReservationFor(String siteNumber, String customerName, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> request = new HashMap<>();
        request.put("siteNumber", siteNumber);
        request.put("startDate", startDate.toString());
        request.put("endDate", endDate.toString());
        request.put("customerName", customerName);
        request.put("phoneNumber", "010-1234-5678");

        return RestAssured
                .given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(request)
                .when()
                .post("/api/reservations")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract().body().as(ReservationResponse.class);
    }

    private ReservationResponse getReservationById(Long reservationId) {
        return RestAssured.when().get("/api/reservations/" + reservationId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract().body().as(ReservationResponse.class);
    }
}