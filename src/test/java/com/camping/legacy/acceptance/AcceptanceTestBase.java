package com.camping.legacy.acceptance;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 인수 테스트 공통 베이스 클래스
 *
 * 목적
 * - 모든 인수 테스트에서 공통으로 쓰는 환경설정/헬퍼를 제공한다.
 * - 테스트는 실제 HTTP 통신(RANDOM_PORT)로 수행하여 블랙박스 관점(ATDD)을 유지한다.
 *
 * 특징
 * - SpringBootTest(RANDOM_PORT): 내장 서버를 띄워 실제 HTTP 호출
 * - TestPropertySource(test.properties): 테스트용 H2 및 JPA 설정 적용
 * - DirtiesContext(BEFORE_EACH_TEST_METHOD): 각 테스트 케이스 간 애플리케이션 컨텍스트 격리
 * - @Sql: 테스트 데이터 시드(/data/test-data.sql) 주입
 * - RestAssured: 포트 설정 및 공통 헬퍼 제공
 *
 * 주의
 * - 동시성 시나리오를 재현하기 위해 ReservationService#createReservation 내의
 *   Thread.sleep(100)은 수업 의도대로 유지 (step 1때 리뷰를 통해 학습/재현 장치라고 해 주셨다는 것을 다시 확인했습니다...)
 * - 우리 API의 계약은 ReservationRequest에 "siteNumber"를 사용함.
 *   (참고용 코드에서 쓰던 "campsiteId"와 다름에 유의)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:test-application.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Sql(scripts = "/data/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public abstract class AcceptanceTestBase {

    @LocalServerPort
    protected int port;

    @BeforeEach
    void setUpRestAssured() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
    }

    /* =========================
       공통 헬퍼 메서드 (HTTP 레벨)
       ========================= */

    /** 예약 생성 */
    protected ExtractableResponse<Response> createReservation(
            String customerName,
            String phoneNumber,
            String siteNumber,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return RestAssured
                .given().log().all()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "customerName", customerName,
                        "phoneNumber", phoneNumber,
                        "siteNumber", siteNumber,
                        "startDate", startDate.toString(),
                        "endDate", endDate.toString()
                ))
                .when().post("/api/reservations")
                .then().log().all()
                .extract();
    }

    /** 예약 취소 */
    protected ExtractableResponse<Response> cancelReservation(Long reservationId, String confirmationCode) {
        return RestAssured
                .given().log().all()
                .queryParam("confirmationCode", confirmationCode)
                .when().delete("/api/reservations/{id}", reservationId)
                .then().log().all()
                .extract();
    }

    /** 예약 단건 조회 */
    protected ExtractableResponse<Response> getReservation(Long reservationId) {
        return RestAssured
                .given().log().all()
                .when().get("/api/reservations/{id}", reservationId)
                .then().log().all()
                .extract();
    }

    /** 내 예약 조회 */
    protected ExtractableResponse<Response> getMyReservations(String name, String phone) {
        return RestAssured
                .given().log().all()
                .queryParam("name", name)
                .queryParam("phone", phone)
                .when().get("/api/reservations/my")
                .then().log().all()
                .extract();
    }

    /** 사이트 전체 조회 */
    protected ExtractableResponse<Response> getAllSites() {
        return RestAssured
                .given().log().all()
                .when().get("/api/sites")
                .then().log().all()
                .extract();
    }

    /** 사이트 상세 조회 */
    protected ExtractableResponse<Response> getSiteDetail(Long siteId) {
        return RestAssured
                .given().log().all()
                .when().get("/api/sites/{siteId}", siteId)
                .then().log().all()
                .extract();
    }

    /** 단일 날짜 가용성 확인 */
    protected ExtractableResponse<Response> checkSiteAvailability(String siteNumber, LocalDate date) {
        return RestAssured
                .given().log().all()
                .queryParam("date", date.toString())
                .when().get("/api/sites/{siteNumber}/availability", siteNumber)
                .then().log().all()
                .extract();
    }

    /** 특정 날짜의 가용 사이트 조회 */
    protected ExtractableResponse<Response> getAvailableSites(LocalDate date) {
        return RestAssured
                .given().log().all()
                .queryParam("date", date.toString())
                .when().get("/api/sites/available")
                .then().log().all()
                .extract();
    }

    /** 기간 가용성 검색(크기 필터 없음) */
    protected ExtractableResponse<Response> searchAvailableSites(LocalDate startDate, LocalDate endDate) {
        return RestAssured
                .given().log().all()
                .queryParam("startDate", startDate.toString())
                .queryParam("endDate", endDate.toString())
                .when().get("/api/sites/search")
                .then().log().all()
                .extract();
    }

    /** 기간 가용성 검색(크기 필터 포함) */
    protected ExtractableResponse<Response> searchAvailableSitesBySize(LocalDate startDate, LocalDate endDate, String size) {
        return RestAssured
                .given().log().all()
                .queryParam("startDate", startDate.toString())
                .queryParam("endDate", endDate.toString())
                .queryParam("size", size)
                .when().get("/api/sites/search")
                .then().log().all()
                .extract();
    }

    /** 월별 예약 현황 조회 */
    protected ExtractableResponse<Response> getReservationCalendar(Integer year, Integer month, Long siteId) {
        return RestAssured
                .given().log().all()
                .queryParam("year", year)
                .queryParam("month", month)
                .queryParam("siteId", siteId)
                .when().get("/api/reservations/calendar")
                .then().log().all()
                .extract();
    }

    /* =========================
       보조 유틸
       ========================= */

    /**
     * 시드 데이터(/data/test-data.sql)에서 아무 사이트 하나의 siteNumber를 가져온다.
     * - 테스트가 하드코딩된 번호(A-1 등)에 의존하지 않도록 하기 위함
     */
    @SuppressWarnings("unchecked")
    protected String anySiteNumber() {
        var res = getAllSites();
        List<Map<String, Object>> list = res.body().as(List.class);
        assertThat(list).isNotEmpty();

        Object siteNumber = list.get(0).get("siteNumber");
        assertThat(siteNumber).isNotNull();
        return String.valueOf(siteNumber);
    }
}
