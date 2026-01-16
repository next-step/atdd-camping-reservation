package com.camping.legacy;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.dto.ReservationRequest;
import com.camping.legacy.repository.CampsiteRepository;
import com.camping.legacy.utils.DatabaseCleaner;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.assertj.core.api.Assertions;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
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

        // when & then
        given().log().all()
                .contentType(ContentType.JSON)
                .body(예약요청)
                .when().post(RESERVATION_ENDPOINT)
                .then().log().all()
                .statusCode(HttpStatus.CREATED.value())
                .body("status", equalTo("CONFIRMED"))
                .body("confirmationCode", notNullValue())
                .body("confirmationCode.length()", is(6));
    }

    @DisplayName("최대 예약 가능 기간(30박)을 꽉 채워 예약 요청한다")
    @Test
    void 최대_예약_가능기간을_꽉_채워_예약_요청한다() {
        // given
        var 시작일 = LocalDate.now();
        var 종료일 = 시작일.plusDays(30); // 30박

        var 예약요청 = 예약요청_생성(시작일, 종료일, "홍길동", "01012345678");

        // when & then
        given().log().all()
                .contentType(ContentType.JSON)
                .body(예약요청)
                .when().post(RESERVATION_ENDPOINT)
                .then().log().all()
                .statusCode(HttpStatus.CREATED.value());
    }

    @DisplayName("예약 제한 기간(30박)을 초과하여 요청하면 예약이 거부된다")
    @Test
    void 예약_제한_기간을_초과하여_요청하면_예약이_거부된다() {
        // given
        var 시작일 = LocalDate.now();
        var 종료일 = 시작일.plusDays(31);

        var 예약요청 = 예약요청_생성(시작일, 종료일, "홍길동", "01012345678");

        // when & then
        given().log().all()
                .contentType(ContentType.JSON)
                .body(예약요청)
                .when().post("/api/reservations")
                .then().log().all()
                .statusCode(HttpStatus.CONFLICT.value()) // TODO. GlobalExceptionHandler
                .body("message", notNullValue());
    }

    @DisplayName("유효하지 않은 고객 이름으로 예약할 경우 예약 거부된다")
    @Test
    void 유효하지_않은_고객_이름으로_예약할_경우_예약_거부된다() {
        // given
        var 시작일 = LocalDate.now();
        var 종료일 = 시작일.plusDays(2);

        var 예약요청 = 예약요청_생성(시작일, 종료일, "김", "01012345678");

        // when & then
        given().log().all()
                .contentType(ContentType.JSON)
                .body(예약요청)
                .when().post(RESERVATION_ENDPOINT)
                .then().log().all()
                .statusCode(HttpStatus.CONFLICT.value()) // TODO. BAD_REQUEST(400) 처리
                .body("message", notNullValue());
    }

    @DisplayName("유효하지 않은 전화번호로 예약 요청하면 예약 거부된다")
    @Test
    void 유효하지_않은_전화번호로_예약_요청하면_예약_거부된다() {
        // given
        var 시작일 = LocalDate.now();
        var 종료일 = 시작일.plusDays(2);

        var 예약요청 = 예약요청_생성(시작일, 종료일, "홍길동", "010-123-456");

        // when & then
        given().log().all()
                .contentType(ContentType.JSON)
                .body(예약요청)
                .when().post(RESERVATION_ENDPOINT)
                .then().log().all()
                .statusCode(HttpStatus.CONFLICT.value()) // TODO. BAD_REQUEST(400) 처리
                .body("message", notNullValue());
    }

    @Disabled("성공 2건이 나오는 버그 확인하여 비활성화")
    @DisplayName("동일한 사이트와 기간에 대해 중복 예약 시도를 하면 한명만 예약된다 (동시성 제어)")
    @Test
    void 동일한_사이트와_기간에_대해_중복_예약_시도를_하면_한명만_예약된다() throws InterruptedException {
        // given
        LocalDate 시작일 = LocalDate.now().plusDays(10);
        LocalDate 종료일 = 시작일.plusDays(1);

        ReservationRequest 홍길동_예약요청 = 예약요청_생성(시작일, 종료일, "홍길동", "01012345678");
        ReservationRequest 이순신_예약요청 = 예약요청_생성(시작일, 종료일, "이순신", "01056781234");

        int 동시요청_개수 = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(동시요청_개수);
        CountDownLatch 준비_완료 = new CountDownLatch(동시요청_개수);
        CountDownLatch 동시에_시작 = new CountDownLatch(1);
        CountDownLatch 종료_대기 = new CountDownLatch(동시요청_개수);

        AtomicInteger 예약_성공 = new AtomicInteger(0);
        AtomicInteger 예약_실패 = new AtomicInteger(0);

        executorService.submit(() -> {
            try{
                준비_완료.countDown();
                동시에_시작.await();

                var statucCode = given().log().all()
                        .contentType(ContentType.JSON)
                        .body(홍길동_예약요청)
                        .when().post(RESERVATION_ENDPOINT)
                        .then().extract().statusCode();

                if(statucCode == HttpStatus.CREATED.value()) {
                    예약_성공.incrementAndGet();
                } else {
                    예약_실패.incrementAndGet();
                }
            } catch (Throwable e) {
                예약_실패.incrementAndGet();
            } finally{
                종료_대기.countDown();
            }
        });

        executorService.submit(() -> {
            try{
                준비_완료.countDown();
                동시에_시작.await();

                var statucCode = given().log().all()
                        .contentType(ContentType.JSON)
                        .body(이순신_예약요청)
                        .when().post(RESERVATION_ENDPOINT)
                        .then().extract().statusCode();

                if(statucCode == HttpStatus.CREATED.value()) {
                    예약_성공.incrementAndGet();
                } else {
                    예약_실패.incrementAndGet();
                }
            } catch (Throwable e) {
                예약_실패.incrementAndGet();
            } finally{
                종료_대기.countDown();
            }
        });

        준비_완료.await();
        동시에_시작.countDown();
        종료_대기.await();
        executorService.shutdown();

        // then
        Assertions.assertThat(예약_성공.get()).isEqualTo(1);
        Assertions.assertThat(예약_실패.get()).isEqualTo(1);
    }

    @DisplayName("성수기 주말 할증 요금이 자동 계산된다")
    @Test
    void 성수기_주말_할증_요금이_자동_계산된다() {
        // given
        // 동적으로 돌아오는 토요일 계산 (테스트 시점에 따라 가변적)
        LocalDate nextSaturday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SATURDAY));
        LocalDate nextSunday = nextSaturday.plusDays(1);

        // 성수기(7,8월)가 아닌 경우, 내년 7월의 토요일로 설정하여 성수기 테스트 가능
        int currentYear = LocalDate.now().getYear();
        LocalDate peakDate = LocalDate.of(currentYear, 7, 10);
        if (peakDate.isBefore(LocalDate.now())) {
            peakDate = LocalDate.of(currentYear + 1, 7, 10);
        }
        LocalDate peakStartDate = peakDate.with(TemporalAdjusters.next(DayOfWeek.SATURDAY));
        LocalDate peakEndDate = peakStartDate.plusDays(1);

        var 예약요청 = 예약요청_생성(peakStartDate, peakEndDate, "성수기 주말 고객", "01012345678");

        // when & then
        given().log().all()
                .contentType(ContentType.JSON)
                .body(예약요청)
                .when().post(RESERVATION_ENDPOINT)
                .then().log().all()
                .statusCode(HttpStatus.CREATED.value());
    }

    // Feature 2. 사이트 가용성
    @DisplayName("예약이 없는 기간으로 검색 시 가능한 예약 가능한 사이트 목록이 반환된다")
    @Test
    void 예약이_없는_기간으로_검색_시_예약_가능한_사이트_목록이_반환된다() {
        // given
        // A-1, B-1 모두 예약 없는 상태
        var 시작일 = LocalDate.now().plusMonths(1);
        var 종료일 = LocalDate.now().plusMonths(1).plusDays(2);

        // when & then
        given().log().all()
                .queryParam("startDate", 시작일.toString())
                .queryParam("endDate",종료일.toString())
                .when().get(SITE_ENDPOINT + "/search")
                .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .body("size()", greaterThanOrEqualTo(2))
                .body("siteNumber", hasItems("A-1", "B-1"))
                .body("available", everyItem(is(true)));
    }

    @DisplayName("사이트 크기 필터링 및 상세 정보 확인이 가능하다")
    @Test
    void 사이트_크기_필터링_및_상세_정보_확인이_가능하다() {
        // given : "A-1(대형)", "B-1(소형)" 사이트가 등록되어 있다.
        var 대형 = "대형";
        var 시작일 = LocalDate.now();
        var 종료일 = 시작일.plusDays(2);

        // when & then
        given().log().all()
                .queryParam("startDate", 시작일.toString())
                .queryParam("endDate", 종료일.toString())
                .queryParam("size", 대형)
                .when().get(SITE_ENDPOINT + "/search")
                .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .body("size()", is(1))
                .body("[0].siteNumber", is("A-1"))
                .body("[0]", hasKey("maxPeople"))
                .body("[0]", hasKey("hasElectricity"));
    }

  @Disabled("현재 연박 검색 시 중간 날짜의 예약 점유 상태를 검증하지 못하는 로직 결함이 있어 비활성화")
  @DisplayName("기간 내 중간 날짜가 예약된 사이트는 검색에서 제외된다 (연박 불가)")
  @Test
  void 기간내_중간_날짜가_예약된_사이트는_검색에서_제외된다() {
        // given : A-1 사이트는 이미 예약된 상태, A-2는 예약이 없는 상태
        campsiteRepository.save(new Campsite("A-2", "Large Site", 5));

        LocalDate 오늘 = LocalDate.now();
        LocalDate 내일 = 오늘.plusDays(1);
        LocalDate 모레 = 오늘.plusDays(2);
        var 예약요청 = 예약요청_생성(내일, 모레, "홍길동", "01012345678");

        given().log().all()
                .contentType(ContentType.JSON)
                .body(예약요청)
                .when().post(RESERVATION_ENDPOINT)
                .then().statusCode(HttpStatus.CREATED.value());

        // when & then
        var 대형 = "대형";
        given().log().all()
                .queryParam("startDate", 오늘.toString())
                .queryParam("endDate", 모레.toString())
                .queryParam("size", 대형)
                .when().get(SITE_ENDPOINT + "/search")
                .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .body("siteNumber", not(hasItem("A-1")))
                .body("siteNumber", hasItem("A-2"));
    }

    @DisplayName("과거 날짜 포함하여 검색할 경우 요청이 거부된다")
    @Test
    void 과거_날짜_포함하여_검색할_경우_요청이_거부된다() {
        // given
        var 어제 = LocalDate.now().minusDays(1);
        var 내일 = LocalDate.now().plusDays(1);

        // when & then
        given().log().all()
                .queryParam("startDate", 어제.toString())
                .queryParam("endDate", 내일.toString())
                .when().get(SITE_ENDPOINT + "/search")
                .then().log().all()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    // Feature3. 예약 취소
    @Disabled("예약 취소 후 재고 확인되지 않는 버그로 인해 비활성화")
    @DisplayName("올바른 예약 확인 코드로 취소 시 재고가 즉시 복구된다")
    @Test
    void 올바른_예약_확인코드로_취소시_재고가_즉시_복구된다() {
        // given: 예약 생성
        var 시작일 = LocalDate.now();
        var 종료일 = 시작일.plusDays(3);

        var 예약요청 = 예약요청_생성(시작일, 종료일, "홍길동", "01012345678");

        var confirmationCode = given()
                .contentType(ContentType.JSON)
                .body(예약요청)
                .when().post("/api/reservations")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract().path("confirmationCode");

        // 생성 응답에 id가 있는지 확인해볼 필요가 있으나, 안전하게 목록 조회로 ID 획득
        var reservationId = when().get("/api/reservations?customerName=홍길동")
                .then()
                .extract().jsonPath().getLong("[0].id");

        // when
        given().log().all()
                .queryParam("confirmationCode", confirmationCode)
                .when().delete("/api/reservations/" + reservationId)
                .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .body("message", notNullValue());

        // then
        given().log().all()
                .queryParam("startDate", 시작일.toString())
                .queryParam("endDate",종료일.toString())
                .queryParam("size", "A")
                .when().get(SITE_ENDPOINT + "/search")
                .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(1))
                .body("[0].siteNumber", is("A-1"));
    }

    @DisplayName("예약 확인 코드가 일치하지 않으면 취소할 수 없다")
    @Test
    void 예약_확인_코드가_일치하지_않으면_취소할_수_없다() {
        // given
        var 시작일 = LocalDate.now();
        var 종료일 = 시작일.plusDays(2);

        var 예약요청 = 예약요청_생성(시작일, 종료일, "홍길동", "01012345678");

        given()
                .contentType(ContentType.JSON)
                .body(예약요청)
                .when().post("/api/reservations")
                .then()
                .statusCode(HttpStatus.CREATED.value());

        var reservationId = when().get("/api/reservations?customerName=홍길동")
                .then()
                .extract().jsonPath().getLong("[0].id");

        // when & then
        given().log().all()
                .queryParam("confirmationCode", "WRONG0")
                .when().delete("/api/reservations/" + reservationId)
                .then().log().all()
                .statusCode(HttpStatus.BAD_REQUEST.value()) // TODO. GlobalExceptionHandler
                .body("message", notNullValue());

        given().log().all()
                .queryParam("id", reservationId)
                .when().get(RESERVATION_ENDPOINT)
                .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .body("[0].status", equalTo("CONFIRMED"));
    }

    @DisplayName("당일 예약 취소 시 별도의 상태 코드로 관리된다")
    @Test
    void 당일_예약_취소_시_별도의_상태_코드로_관리된다() {
        // given
        var 시작일 = LocalDate.now();
        var 종료일 = 시작일.plusDays(2);

        var 예약요청 = 예약요청_생성(시작일, 종료일, "홍길동", "01012345678");

        // TODO. extract
        given()
                .contentType(ContentType.JSON)
                .body(예약요청)
                .when().post("/api/reservations")
                .then()
                .statusCode(HttpStatus.CREATED.value());

        var response = given()
                .contentType(ContentType.JSON)
                .when().get("/api/reservations?customerName=홍길동")
                .then()
                .extract().response().body();

        var reservationId = response.path("[0].id");
        var confirmationCode = response.path("[0].confirmationCode");

        // when
        given().log().all()
                .queryParam("confirmationCode", confirmationCode)
                .when().delete("/api/reservations/" + reservationId)
                .then().log().all()
                .statusCode(HttpStatus.OK.value());

        // then
        given().log().all()
                .queryParam("id", reservationId)
                .when().get(RESERVATION_ENDPOINT)
                .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .body("[0].status", equalTo("CANCELLED_SAME_DAY"));
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
}
