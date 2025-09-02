package com.camping.legacy.support;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.Map;

/**
 * 모든 테스트 클래스에서 공통으로 사용할 기본 설정을 제공하는 추상 클래스
 * <p>
 * - SpringBootTest 설정 (RANDOM_PORT)
 * - H2 인메모리 데이터베이스 설정
 * - RestAssured 포트 설정
 * - 테스트 격리를 위한 DirtiesContext 설정
 * - 초기 데이터 로딩을 위한 Sql 어노테이션 지원
 * - 공통 헬퍼 메소드 제공
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=true",
        "spring.jpa.properties.hibernate.format_sql=true"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Sql(scripts = "/data/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public abstract class TestBase {

    @LocalServerPort
    protected int port;

    @BeforeEach
    void setUpTestBase() {
        RestAssured.port = port;
    }

    // 공통 헬퍼 메소드들

    /**
     * 예약 생성 요청을 보내는 헬퍼 메소드
     */
    protected ExtractableResponse<Response> createReservation(String customerName, String phoneNumber, String campsiteId, LocalDate startDate, LocalDate endDate) {
        return RestAssured
                .given().log().all()
                .contentType("application/json")
                .body(Map.of(
                        "customerName", customerName,
                        "phoneNumber", phoneNumber,
                        "campsiteId", campsiteId,
                        "startDate", startDate.toString(),
                        "endDate", endDate.toString()
                ))
                .when().post("/api/reservations")
                .then().log().all()
                .extract();
    }

    /**
     * 예약 취소 요청을 보내는 헬퍼 메소드
     */
    protected ExtractableResponse<Response> cancelReservation(Long reservationId, String confirmationCode) {
        return RestAssured
                .given().log().all()
                .queryParam("confirmationCode", confirmationCode)
                .when().delete("/api/reservations/" + reservationId)
                .then().log().all()
                .extract();
    }

    /**
     * 예약 조회 요청을 보내는 헬퍼 메소드
     */
    protected ExtractableResponse<Response> getReservation(Long reservationId) {
        return RestAssured
                .given().log().all()
                .when().get("/api/reservations/" + reservationId)
                .then().log().all()
                .extract();
    }

    /**
     * 내 예약 조회 요청을 보내는 헬퍼 메소드
     */
    protected ExtractableResponse<Response> getMyReservations(String name, String phone) {
        return RestAssured
                .given().log().all()
                .queryParam("name", name)
                .queryParam("phone", phone)
                .when().get("/api/reservations/my")
                .then().log().all()
                .extract();
    }

    /**
     * 사이트 목록 조회 요청을 보내는 헬퍼 메소드
     */
    protected ExtractableResponse<Response> getAllSites() {
        return RestAssured
                .given().log().all()
                .when().get("/api/sites")
                .then().log().all()
                .extract();
    }

    /**
     * 사이트 상세 조회 요청을 보내는 헬퍼 메소드
     */
    protected ExtractableResponse<Response> getSiteDetail(Long siteId) {
        return RestAssured
                .given().log().all()
                .when().get("/api/sites/" + siteId)
                .then().log().all()
                .extract();
    }

    /**
     * 사이트 가용성 확인 요청을 보내는 헬퍼 메소드
     */
    protected ExtractableResponse<Response> checkSiteAvailability(String siteNumber, LocalDate date) {
        return RestAssured
                .given().log().all()
                .queryParam("date", date.toString())
                .when().get("/api/sites/" + siteNumber + "/availability")
                .then().log().all()
                .extract();
    }

    /**
     * 날짜별 가용 사이트 조회 요청을 보내는 헬퍼 메소드
     */
    protected ExtractableResponse<Response> getAvailableSites(LocalDate date) {
        return RestAssured
                .given().log().all()
                .queryParam("date", date.toString())
                .when().get("/api/sites/available")
                .then().log().all()
                .extract();
    }

    /**
     * 기간별 사이트 검색 요청을 보내는 헬퍼 메소드
     */
    protected ExtractableResponse<Response> searchAvailableSites(LocalDate startDate, LocalDate endDate) {
        return RestAssured
                .given().log().all()
                .queryParam("startDate", startDate.toString())
                .queryParam("endDate", endDate.toString())
                .when().get("/api/sites/search")
                .then().log().all()
                .extract();
    }

    /**
     * 크기별 사이트 검색 요청을 보내는 헬퍼 메소드
     */
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

    /**
     * 월별 예약 현황 캘린더 조회 요청을 보내는 헬퍼 메소드
     */
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
}
