package com.camping.acceptance;

import com.camping.legacy.CampingApplication;
import com.camping.legacy.domain.Campsite;
import com.camping.legacy.dto.ReservationResponse;
import com.camping.legacy.repository.CampsiteRepository;
import com.camping.legacy.repository.ReservationRepository;
import com.camping.support.DatabaseCleaner;
import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;


@SuppressWarnings("NonAsciiCharacters")
@ActiveProfiles("test")
@SpringBootTest(classes = CampingApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(DatabaseCleaner.class)
@DisplayName("캠핑장 예약 인수 테스트")
public class ReservationAcceptanceTest {

    public static final String API_RESERVATIONS_CALENDAR = "/api/reservations/calendar";
    public static final String 예약시작일 = "2027-09-10";
    public static final String WRONG_CODE = "WRONG456";
    public static final String 김캘린더 = "김캘린더";
    public static final String API_RESERVATIONS = "/api/reservations/";


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
    ReservationRepository reservationRepository;

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

        var 예약_시작_날짜 = 날짜_생성(2027, 8, 15);
        var 예약_마감_날짜 = 날짜_생성(2027, 8, 17);


        var 동시_요청_결과 = 동시에_예약을_요청한다(10, SITE_A1_NUMBER, 예약_시작_날짜, 예약_마감_날짜);

        // then
        assertThat(성공한_예약_개수(동시_요청_결과)).isEqualTo(1);
        assertThat(실패한_예약_개수(동시_요청_결과)).isEqualTo(9);
        
//        long 저장된_예약_개수 = 예약된_개수를_반환한다(예약_시작_날짜, 예약_마감_날짜);
//        assertThat(저장된_예약_개수).isEqualTo(1);
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
        var 예약_시작_날짜 = 날짜_생성(2027, 9, 10);
        var 예약_마감_날짜 = 날짜_생성(2027, 9, 11);

        var request = 예약_요청_생성(SITE_B2_NUMBER, 예약_시작_날짜, 예약_마감_날짜, 김캘린더, "010-1234-5678");

        예약을_생성한다(request);

        // then
        var 예약_조회_결과 = 캘린터에서_특정사이트_예약을_조회한다(2027, 9, siteB2);
        var 특정_날짜_예약_조회_결과 = 특정_날짜_예약_응답_생성한다(예약_조회_결과, 예약시작일);

        assertThat(특정_날짜_예약_조회_결과.get("available")).isEqualTo(false);
        assertThat(특정_날짜_예약_조회_결과.get("customerName")).isEqualTo(김캘린더);
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
        var 예약_시작_날짜 = 날짜_생성(2027, 10, 20);
        var 예약_마감_날짜 = 예약_시작_날짜.plusDays(1);
        var 예약요청 = 예약_요청_생성(SITE_C3_NUMBER, 예약_시작_날짜, 예약_마감_날짜, "김영희", "010-1234-5678");

        var 예약응답 = 예약을_생성한다(예약요청);
        var 예약 = 예약응답.body().as(ReservationResponse.class);

        var 예약_확인코드 = 예약.getConfirmationCode();
        var 예약_ID = 예약.getId();

        // when
        정확한_확인_코드로_예약을_취소한다(예약_확인코드, 예약_ID);

        // then
        var 예약_조회_결과 = 날짜로_예약을_조회한다(예약_시작_날짜, 예약_마감_날짜);
        assertThat(예약_조회_결과.statusCode()).isEqualTo(HttpStatus.OK.value());
        var 예약_가능_사이트 = 예약_조회_결과.jsonPath().getList("siteNumber");
        assertThat(예약_가능_사이트).contains(SITE_C3_NUMBER);
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
        var 예약_시작_날짜 = 날짜_생성(2027, 11, 3);
        var 예약_마감_날짜 = 예약_시작_날짜.plusDays(1);
                
        var 예약_정보 = 예약_요청_생성(SITE_D4_NUMBER, 예약_시작_날짜, 예약_마감_날짜, "박중간", "010-1234-5678");
        예약을_생성한다(예약_정보);

        // when
        var 예약_조회_결과 = 날짜로_예약을_조회한다(예약_시작_날짜, 예약_마감_날짜);

        // then
        assertThat(예약_조회_결과.statusCode()).isEqualTo(HttpStatus.OK.value());
        var 예약_가능_사이트 = 예약_조회_결과.jsonPath().getList("siteNumber");
        assertThat(예약_가능_사이트).doesNotContain(SITE_D4_NUMBER);
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

        var 예약_시작_날짜 = 날짜_생성(2026, 2, 14);
        var 예약_마감_날짜 = 예약_시작_날짜.plusDays(1);

        var 예약_정보 = 예약_요청_생성(SITE_E5_NUMBER, 예약_시작_날짜, 예약_마감_날짜, "박서준", "010-1234-5678");
        var 예약_응답 = 예약을_생성한다(예약_정보);

        var 예약 = 예약_응답.body().as(ReservationResponse.class);
        var 예약_확인_코드 = 예약.getConfirmationCode();

        // case1: 잘못된 확인 코드인 경우
        var 잘못된_확인코드_응답 = 잘못된_코드로_예약을_취소한다(예약);
        assertThat(잘못된_확인코드_응답.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(잘못된_확인코드_응답.jsonPath().getString("message")).isEqualTo("확인 코드가 일치하지 않습니다.");

        var 예약_조회_응답 = 예약ID로_예약을_조회한다(예약);
        var 예약정보 = 예약_조회_응답.body().as(ReservationResponse.class);
        assertThat(예약정보.getStatus()).isNotEqualTo("CANCELLED");

        // case2: 올바른 확인 코드인 경우
        var 올바른_확인_코드_응답 = 정확한_확인_코드로_예약을_취소한다(예약_확인_코드, 예약.getId());

        assertThat(올바른_확인_코드_응답.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(올바른_확인_코드_응답.jsonPath().getString("message")).isEqualTo("예약이 취소되었습니다.");

        var 조회응답 = 예약ID로_예약을_조회한다(예약);
        var 올바른_확인_코드인_경우_응답 = 조회응답.body().as(ReservationResponse.class);
        assertThat(올바른_확인_코드인_경우_응답.getStatus()).isEqualTo("CANCELLED");
    }



    private Campsite create(String siteNumber) {
        return Campsite.builder()
                .siteNumber(siteNumber)
                .description("test")
                .maxPeople(10)
                .build();
    }

    private long 예약된_개수를_반환한다(LocalDate 예약_시작_날짜, LocalDate 예약_마감_날짜) {
        return reservationRepository.findAll().stream()
                .filter(r -> r.getCampsite().getSiteNumber().equals(SITE_A1_NUMBER) &&
                        r.getStartDate().equals(예약_시작_날짜) &&
                        r.getEndDate().equals(예약_마감_날짜))
                .count();
    }


    private LocalDate 날짜_생성(int 년도, int 월, int 날짜) {
        return LocalDate.of(년도, 월, 날짜);
    }

    private static Map<String, Object> 특정_날짜_예약_응답_생성한다(ExtractableResponse<Response> response, String 확인날짜) {
        List<Map<String, Object>> days = response.jsonPath().getList("days");
        Map<String, Object> day10 = days.stream()
                .filter(d -> d.get("date").equals(확인날짜))
                .findFirst()
                .orElseThrow();
        return day10;
    }


    private static long 실패한_예약_개수(ConcurrentLinkedQueue<Response> responses) {
        return responses.stream().filter(r -> r.statusCode() == HttpStatus.CONFLICT.value()).count();
    }

    private static long 성공한_예약_개수(ConcurrentLinkedQueue<Response> responses) {
        long successCount = responses.stream().filter(r -> r.statusCode() == HttpStatus.CREATED.value()).count();
        return successCount;
    }

    private static ConcurrentLinkedQueue<Response> 동시에_예약을_요청한다(int numberOfUsers, String site, LocalDate 예약_시작_날짜, LocalDate 예약_마감_날짜) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfUsers);
        CountDownLatch latch = new CountDownLatch(numberOfUsers);
        ConcurrentLinkedQueue<Response> responses = new ConcurrentLinkedQueue<>();

        // when
        for (int i = 0; i < numberOfUsers; i++) {
            int userIndex = i;
            executorService.submit(() -> {
                try {
                    Map<String, Object> request = 예약_요청_생성(SITE_A1_NUMBER, 예약_시작_날짜, 예약_마감_날짜, "사용자-" + userIndex, "010-1234-567" + userIndex);

                    ExtractableResponse<Response> 예약하기 = 예약을_생성한다(request);
                    Response response = 예약하기.response();
                    responses.add(response);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();
        return responses;
    }


    //=====예약 관련 API 호출 ======
    private static ExtractableResponse<Response> 정확한_확인_코드로_예약을_취소한다(String confirmationCode, Long id) {

        return RestAssured
                .given().log().all()
                .queryParam("confirmationCode", confirmationCode)
                .when()
                .delete(API_RESERVATIONS + id)
                .then().log().all()
                .extract();
    }

    private static ExtractableResponse<Response> 예약ID로_예약을_조회한다(ReservationResponse reservation) {
        return RestAssured
                .when()
                .get(API_RESERVATIONS + reservation.getId())
                .then().log().all()
                .extract();
    }

    private static ExtractableResponse<Response> 잘못된_코드로_예약을_취소한다(ReservationResponse reservation) {
        return RestAssured
                .given().log().all()
                .queryParam("confirmationCode", WRONG_CODE)
                .when()
                .delete(API_RESERVATIONS + reservation.getId())
                .then().log().all()
                .extract();
    }


    private static ExtractableResponse<Response> 예약을_생성한다(Map<String, Object> request) {
        return RestAssured.given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(request)
                .when()
                .post("/api/reservations")
                .then().log().all()
                .extract();
    }



    private ExtractableResponse<Response> 캘린터에서_특정사이트_예약을_조회한다(int 년도, int 월, Campsite 사이트) {
        return RestAssured
                .given().log().all()
                .param("year", 년도)
                .param("month", 월)
                .param("siteId", 사이트.getId())
                .when()
                .get(API_RESERVATIONS_CALENDAR)
                .then().log().all()
                .extract();
    }


    private static ExtractableResponse<Response> 날짜로_예약을_조회한다(LocalDate reservationDate, LocalDate reservationEndDate) {
        return RestAssured
                .given().log().all()
                .param("startDate", reservationDate.toString())
                .param("endDate", reservationEndDate.toString())
                .when()
                .get("/api/sites/search")
                .then().log().all()
                .extract();
    }


    private static Map<String, Object> 예약_요청_생성(String 사이트_번호, LocalDate 시작날짜, LocalDate 마감날짜, String 이름, String 핸드폰번호) {
        Map<String, Object> request = new HashMap<>();
        request.put("siteNumber", 사이트_번호);
        request.put("startDate", 시작날짜);
        request.put("endDate", 마감날짜);
        request.put("customerName", 이름);
        request.put("phoneNumber", 핸드폰번호);
        return request;
    }
}
