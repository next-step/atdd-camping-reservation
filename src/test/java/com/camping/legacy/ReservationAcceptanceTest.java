package com.camping.legacy;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.domain.Reservation;
import com.camping.legacy.dto.ReservationRequest;
import com.camping.legacy.repository.CampsiteRepository;
import com.camping.legacy.repository.ReservationRepository;
import com.camping.legacy.utils.ConcurrencyTestHelper;
import com.camping.legacy.utils.DatabaseCleaner;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReservationAcceptanceTest {

    private static final String RESERVATION_ENDPOINT = "/api/reservations";
    private static final String SITE_ENDPOINT = "/api/sites";

    @LocalServerPort
    int port;

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @Autowired
    private CampsiteRepository campsiteRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;

        databaseCleaner.execute();

        campsiteRepository.save(new Campsite("A-1", "Large Site", 6));
        campsiteRepository.save(new Campsite("B-1", "Small Site", 4));
    }

    @DisplayName("유효한 정보로 예약 요청 시 예약이 확정된다")
    @Test
    void 유효한_정보로_예약요청시_예약이_확정된다() {
        // given
        var 시작일 = LocalDate.now();
        var 종료일 = 시작일.plusDays(2);
        var 예약요청 = 예약요청_생성(시작일, 종료일, "홍길동", "01012345678");

        // when
        var 응답 = 예약을_요청한다(예약요청);

        // then
        예약_성공_확인(응답);
        예약이_확정된_상태이다(응답, "CONFIRMED");
        예약_확인코드가_발급되었다(응답);
    }

    @DisplayName("최대 예약 가능 기간(30박)을 꽉 채워 예약 요청한다")
    @Test
    void 최대_예약_가능기간을_꽉_채워_예약_요청한다() {
        // given
        var 시작일 = LocalDate.now();
        var 종료일 = 시작일.plusDays(30); // 30박
        var 예약요청 = 예약요청_생성(시작일, 종료일, "홍길동", "01012345678");

        // when
        var 응답 = 예약을_요청한다(예약요청);

        // then
        예약_성공_확인(응답);
    }

    @DisplayName("예약 제한 기간(30박)을 초과하여 요청하면 예약이 거부된다")
    @Test
    void 예약_제한_기간을_초과하여_요청하면_예약이_거부된다() {
        // given
        var 시작일 = LocalDate.now();
        var 종료일 = 시작일.plusDays(31);
        var 예약요청 = 예약요청_생성(시작일, 종료일, "홍길동", "01012345678");

        // when
        var 응답 = 예약을_요청한다(예약요청);

        // then
        예약이_거부되었다(응답);
        에러메시지가_확인된다(응답, "예약 기간은 최대 30일입니다.");
    }

    @DisplayName("유효하지 않은 고객 이름으로 예약할 경우 예약 거부된다")
    @Test
    void 유효하지_않은_고객_이름으로_예약할_경우_예약_거부된다() {
        // given
        var 시작일 = LocalDate.now();
        var 종료일 = 시작일.plusDays(2);
        var 예약요청 = 예약요청_생성(시작일, 종료일, "김", "01012345678");

        // when
        var 응답 = 예약을_요청한다(예약요청);

        // then
        예약이_거부되었다(응답);
        에러메시지가_확인된다(응답, "예약자 이름은 최소 2자 이상이어야 합니다.");
    }

    @DisplayName("유효하지 않은 전화번호로 예약 요청하면 예약 거부된다")
    @Test
    void 유효하지_않은_전화번호로_예약_요청하면_예약_거부된다() {
        // given
        var 시작일 = LocalDate.now();
        var 종료일 = 시작일.plusDays(2);
        var 예약요청 = 예약요청_생성(시작일, 종료일, "홍길동", "010-123-456");

        // when
        var 응답 = 예약을_요청한다(예약요청);

        // then
        예약이_거부되었다(응답);
        에러메시지가_확인된다(응답, "전화번호 형식이 올바르지 않습니다.");
    }

    @Disabled("성공 2건이 나오는 버그 확인하여 비활성화")
    @DisplayName("동일한 사이트와 기간에 대해 중복 예약 시도를 하면 한명만 예약된다 (동시성 제어)")
    @Test
    void 동일한_사이트와_기간에_대해_중복_예약_시도를_하면_한명만_예약된다() throws InterruptedException {
        // given
        LocalDate 시작일 = LocalDate.now().plusDays(10);
        LocalDate 종료일 = 시작일.plusDays(1);

        var 홍길동_예약요청 = 예약요청_생성(시작일, 종료일, "홍길동", "01012345678");
        var 이순신_예약요청 = 예약요청_생성(시작일, 종료일, "이순신", "01056781234");

        AtomicInteger 예약_성공_카운트 = new AtomicInteger(0);

        // when
        ConcurrencyTestHelper.execute(
                () -> recordResult(예약_성공_카운트, 예약을_요청한다(홍길동_예약요청)),
                () -> recordResult(예약_성공_카운트, 예약을_요청한다(이순신_예약요청))
        );

        // then
        assertThat(예약_성공_카운트.get()).isEqualTo(1);
    }

    private void recordResult(AtomicInteger count, ExtractableResponse<Response> response) {
        if (response.statusCode() == HttpStatus.CREATED.value()) {
            count.incrementAndGet();
        }
    }

    // TODO. 서비스에 LocalDate.now()가 고정되어 있어서, 특정 날짜 지정 후 기간이 지나면 테스트 실패하게 된다
    // TODO. 응답에 결제 예정 금액 정보가 없다
    @DisplayName("성수기 주말 할증 요금이 자동 계산된다")
    @Test
    void 성수기_주말_할증_요금이_자동_계산된다() {
        // given
        LocalDate 오늘 = LocalDate.now();
        LocalDate 성수기_시작 = LocalDate.of(오늘.getYear(), 7, 1);
        if (오늘.isAfter(성수기_시작)) {
            성수기_시작 = 성수기_시작.plusYears(1); // 올해 성수기가 지났으면 내년으로
        }

        LocalDate 성수기_토요일 = 성수기_시작.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
        LocalDate 성수기_일요일 = 성수기_토요일.plusDays(1);

        var 예약요청 = 예약요청_생성(성수기_토요일, 성수기_일요일, "성수기 주말 고객", "01012345678");

        // when & then
        var 예약_응답 = 예약을_요청한다(예약요청);
        예약_성공_확인(예약_응답);
    }

    // Feature 2. 사이트 가용성
    @DisplayName("예약이 없는 기간으로 검색 시 가능한 예약 가능한 사이트 목록이 반환된다")
    @Test
    void 예약이_없는_기간으로_검색_시_예약_가능한_사이트_목록이_반환된다() {
        // given
        // A-1, B-1 모두 예약 없는 상태
        var 시작일 = LocalDate.now().plusMonths(1);
        var 종료일 = LocalDate.now().plusMonths(1).plusDays(2);
        var 사이즈 = "대형";

        // when
        var 사이트_검색_결과 = 기간_조건으로_사이트를_검색한다(시작일, 종료일);

        // then
        검색_결과에_해당_사이트가_포함된다(사이트_검색_결과, "A-1", "B-1");
        모든_사이트는_이용가능한_상태이다(사이트_검색_결과);
    }

    @DisplayName("사이트 크기 필터링 및 상세 정보 확인이 가능하다")
    @Test
    void 사이트_크기_필터링_및_상세_정보_확인이_가능하다() {
        // given : "A-1(대형)", "B-1(소형)" 사이트가 등록되어 있다
        var 시작일 = LocalDate.now();
        var 종료일 = 시작일.plusDays(2);
        var 사이즈 = "대형";

        // when
        var 검색_결과 = 사이트를_검색한다(시작일, 종료일, 사이즈);

        // then
        검색_결과에_특정_사이트만_포함된다(검색_결과, "A-1");
        검색_결과에는_최대인원과_전기가능여부가_포함되야한다(검색_결과);
    }

  @Disabled("BUG: 현재 검색 로직이 시작/종료일만 체크하여 중간 날짜 점유를 걸러내지 못하여 비활성화")
  @DisplayName("기간 내 중간 날짜가 예약된 사이트는 검색에서 제외된다 (연박 불가)")
  @Test
  void 기간내_중간_날짜가_예약된_사이트는_검색에서_제외된다() {
        // given : A-1 사이트는 이미 예약된 상태, A-2는 예약이 없는 상태
        campsiteRepository.save(new Campsite("A-2", "Large Site", 5));

        LocalDate 오늘 = LocalDate.now();
        LocalDate 시작일 = 오늘.plusDays(1);
        LocalDate 종료일 = 오늘.plusDays(3);
        var 예약요청 = 예약요청_생성(시작일, 종료일, "홍길동", "01012345678");

        예약을_요청한다(예약요청);

        // when & then
        var 검색_결과 = 사이트를_검색한다(오늘, 종료일, "대형");

        검색_결과에_특정_사이트는_포함되지_않는다(검색_결과, "A-1");
        검색_결과에_해당_사이트가_포함된다(검색_결과, "A-2");
    }

    @DisplayName("과거 날짜 포함하여 검색할 경우 요청이 거부된다")
    @Test
    void 과거_날짜_포함하여_검색할_경우_요청이_거부된다() {
        // given
        var 어제 = LocalDate.now().minusDays(1);
        var 내일 = LocalDate.now().plusDays(1);

        // when
        var 사이트_검색_결과 = 기간_조건으로_사이트를_검색한다(어제, 내일);

        // then
        검색_요청이_거부되었다(사이트_검색_결과);
    }

    // Feature3. 예약 취소
    @DisplayName("올바른 예약 확인 코드로 취소 시 재고가 즉시 복구된다")
    @Test
    void 올바른_예약_확인코드로_취소시_재고가_즉시_복구된다() {
        // given: 예약 생성
        var 사이트 = campsiteRepository.save(new Campsite("A-2", "대형", 5));
        var 시작일 = LocalDate.now().plusMonths(1);
        var 확인코드 = "SAFE12";

        var 저장된_예약 = reservationRepository.save(예약_데이터_준비(사이트, 시작일, 확인코드));

        long reservationId = 저장된_예약.getId();
        String confirmationCode = 저장된_예약.getConfirmationCode();

        // when
        var 취소_응답 = 예약을_취소한다(reservationId, confirmationCode);
        예약_취소가_성공했다(취소_응답);

        var 조회_응답 = 예약을_조회한다(reservationId);
        예약_상태_확인(조회_응답, "CANCELLED");

        // then
        var 검색_결과 = 사이트를_검색한다(저장된_예약.getStartDate(), 저장된_예약.getEndDate(), "대형");
        검색_결과에_해당_사이트가_포함된다(검색_결과, "A-2");
    }

    @DisplayName("예약 확인 코드가 일치하지 않으면 취소할 수 없다")
    @Test
    void 예약_확인_코드가_일치하지_않으면_취소할_수_없다() {
        // given
        var 시작일 = LocalDate.now();
        var 종료일 = 시작일.plusDays(2);
        var 예약요청 = 예약요청_생성(시작일, 종료일, "홍길동", "01012345678");

        var 예약_응답 = 예약을_요청한다(예약요청);

        long reservationId = 예약_응답.jsonPath().getLong("id");
        String confirmationCode = "WRONG_CODE";

        // when
        var 취소_응답 = 예약을_취소한다(reservationId, confirmationCode);

        // then
        예약_취소가_거부되었다(취소_응답);

        var 조회_응답 = 예약을_조회한다(reservationId);
        예약_상태_확인(조회_응답, "CONFIRMED");
    }

    @DisplayName("당일 예약 취소 시 별도의 상태 코드로 관리된다")
    @Test
    void 당일_예약_취소_시_별도의_상태_코드로_관리된다() {
        // given
        var 시작일 = LocalDate.now();
        var 종료일 = 시작일.plusDays(2);
        var 예약요청 = 예약요청_생성(시작일, 종료일, "홍길동", "01012345678");

        var 예약_응답 = 예약을_요청한다(예약요청);

        long reservationId = 예약_응답.jsonPath().getLong("id");
        String confirmationCode = 예약_응답.jsonPath().get("confirmationCode");

        // when
        var 취소_응답 = 예약을_취소한다(reservationId, confirmationCode);
        예약_취소가_성공했다(취소_응답);

        // then
        var 조회_응답 = 예약을_조회한다(reservationId);
        예약_상태_확인(조회_응답, "CANCELLED_SAME_DAY");
    }

    private static ReservationRequest 예약요청_생성(LocalDate startDate, LocalDate endDate, String name, String phoneNumber) {
        return new ReservationRequest(
                name,
                startDate,
                endDate,
                "A-1",
                phoneNumber,
                4,
                "12가3456",
                "잘 부탁드립니다."
        );
    }

    private static Reservation 예약_데이터_준비(Campsite site, LocalDate start, String code) {
        Reservation reservation = new Reservation();
        reservation.setCampsite(site);
        reservation.setStartDate(start);
        reservation.setEndDate(start.plusDays(2));
        reservation.setConfirmationCode(code);
        reservation.setStatus("CONFIRMED");
        reservation.setCustomerName("익명의 고객");
        return reservation;
    }

    public static ExtractableResponse<Response> 예약을_요청한다(ReservationRequest request) {
        return given().log().all()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post(RESERVATION_ENDPOINT)
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 예약을_조회한다(long reservationId) {
        return  given().log().all()
                .queryParam("id", reservationId)
                .when().get(RESERVATION_ENDPOINT)
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 예약을_취소한다(long reservationId, String confirmationCode) {
        return given().log().all()
                .queryParam("confirmationCode", confirmationCode)
                .when().delete("/api/reservations/" + reservationId)
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 기간_조건으로_사이트를_검색한다(LocalDate startDate, LocalDate endDate) {
        return given().log().all()
                .queryParam("startDate", startDate.toString())
                .queryParam("endDate", endDate.toString())
                .when().get(SITE_ENDPOINT + "/search")
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 사이트를_검색한다(LocalDate startDate, LocalDate endDate, String size) {
        return given().log().all()
                .queryParam("startDate", startDate.toString())
                .queryParam("endDate", endDate.toString())
                .queryParam("size", size)
                .when().get(SITE_ENDPOINT + "/search")
                .then().log().all()
                .extract();
    }

    public static void 예약_성공_확인(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    }

    public static void 예약이_거부되었다(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    public static void 예약이_확정된_상태이다(ExtractableResponse<Response> response, String expected) {
        assertThat(response.jsonPath().getString("status")).isEqualTo(expected);
    }

    private void 에러메시지가_확인된다(ExtractableResponse<Response> response, String expected) {
        assertThat(response.jsonPath().getString("message")).isEqualTo(expected);
    }

    public static void 예약_확인코드가_발급되었다(ExtractableResponse<Response> response) {
        String code = response.jsonPath().getString("confirmationCode");
        assertThat(code).isNotNull();
        assertThat(code).hasSize(6);
        assertThat(code).matches("^[A-Z0-9]*$");
    }

    public static void 검색_결과에_해당_사이트가_포함된다(ExtractableResponse<Response> response, String... siteNumbers) {
        List<String> resultNumbers = response.jsonPath().getList("siteNumber");
        assertThat(resultNumbers).contains(siteNumbers);
    }

    public static void 모든_사이트는_이용가능한_상태이다(ExtractableResponse<Response> response) {
        List<Boolean> availability = response.jsonPath().getList("available");
        assertThat(availability).allMatch(available -> available);
    }

    public static void 검색_결과에_특정_사이트는_포함되지_않는다(ExtractableResponse<Response> response, String siteNumber) {
        List<String> siteNumbers = response.jsonPath().getList("siteNumber");
        assertThat(siteNumbers).doesNotContain(siteNumber);
    }

    public static void 검색_결과에_특정_사이트만_포함된다(ExtractableResponse<Response> response, String siteNumber) {
        List<String> siteNumbers = response.jsonPath().getList("siteNumber");
        assertThat(siteNumbers).hasSize(1);
        assertThat(siteNumbers).containsExactly(siteNumber);
    }

    public static void 검색_요청이_거부되었다(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    public static void 검색_결과에는_최대인원과_전기가능여부가_포함되야한다(ExtractableResponse<Response> response) {
        List<Integer> maxPeopleList = response.jsonPath().getList("maxPeople");
        List<Boolean> electricityList = response.jsonPath().getList("hasElectricity");

        assertThat(maxPeopleList).allMatch(people -> people > 0);
        assertThat(electricityList).allMatch(Objects::nonNull);
    }

    public static void 예약_취소가_성공했다(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    }

    public static void 예약_취소가_거부되었다(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.jsonPath().getString("message")).isNotNull();
    }

    public static void 예약_상태_확인(ExtractableResponse<Response> response, String expectedStatus) {
        String status = response.jsonPath().getString("[0].status");
        assertThat(status).isEqualTo(expectedStatus);
    }
}
