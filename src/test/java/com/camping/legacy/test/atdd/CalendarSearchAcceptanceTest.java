package com.camping.legacy.test.atdd;

import com.camping.legacy.dto.CalendarResponse;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;

import static com.camping.legacy.test.atdd.testfixture.CalendarSearchTestFixture.createReservationRequest;
import static io.restassured.RestAssured.*;
import static io.restassured.http.ContentType.JSON;
import static java.time.LocalDate.now;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.HttpStatus.*;

@Sql(scripts = "/sql/modify-reservation_create-campsites.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/clear-reservations.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@DisplayName("예약 캘린더 조회 테스트")
public class CalendarSearchAcceptanceTest extends AcceptanceTestBase {

    /**
     * Scenario: 정상적인 캘린더 조회
     * <p>
     * Given 사이트 A-1이 존재한다.
     * And 현재로부터 3일후~5일후를 예약기간으로 홍길동이 A-1을 예약했다.
     * <p>
     * When 사용자가 사이트 ID 1의 캘린더를 조회한다.
     * <p>
     * Then 캘린더 조회가 성공한다
     * and HTTP 상태 코드는 200이다
     * and 홀기동의 예약 정보가 조회된다.
     */
    @Test
    @DisplayName("정상적인 캘린더 조회 - 유효한 연도, 월, 사이트 ID로 캘린더 조회가 성공한다.")
    void 정상적인_캘린더_조회_테스트() {

        // Given: 사이트 A-1이 존재한다.
        // And 현재로부터 3일후~5일후를 예약기간으로 홍길동이 A-1을 예약했다.
        var startDate = now().plusDays(3);
        var endDate = now().plusDays(5);
        var hongReservation = createReservationRequest(
                "홍길동", "010-1234-5678", "A-1", startDate, endDate);

        given().contentType(JSON)
                .body(hongReservation)
                .when()
                .post("/api/reservations");

        // When: 사용자가 사이트 ID 1의 캘린더를 조회한다.
        var year = startDate.getYear();
        var month = startDate.getMonth().getValue();
        var day = startDate.getDayOfMonth();
        var response = given()
                .when()
                .get(String.format("/api/reservations/calendar?year=%d&month=%d&siteId=1", year, month))
                .then()
                .log().all()
                .extract();

        // Then: 캘린더 조회가 성공한다
        SoftAssertions.assertSoftly(softly -> {
            // 기본 날짜 정보
            softly.assertThat(response.statusCode()).isEqualTo(OK.value());
            softly.assertThat(response.jsonPath().getInt("year")).isEqualTo(year);
            softly.assertThat(response.jsonPath().getInt("month")).isEqualTo(month);
            softly.assertThat(response.jsonPath().getLong("siteId")).isEqualTo(1L);
            softly.assertThat(response.jsonPath().getString("siteNumber")).isEqualTo("A-1");
            softly.assertThat(response.jsonPath().getList("days")).isNotEmpty();
            softly.assertThat(response.jsonPath().getMap("summary")).isNotEmpty();
            // 예약 정보
            var reservedDay = response.jsonPath().getList("days", CalendarResponse.DayStatus.class).stream()
                    .filter(x -> x.getDate().equals(startDate))
                    .findFirst()
                    .get();
            softly.assertThat(reservedDay.getCustomerName()).isEqualTo("홍길동");
            softly.assertThat(reservedDay.getReservationId()).isEqualTo(1);
            softly.assertThat(reservedDay.getAvailable()).isFalse();
        });
    }

    /**
     * Scenario: 존재하지 않는 사이트 ID로 조회 시 에러
     * <p>
     * Given 사이트 ID 999가 존재하지 않는다
     * <p>
     * When 사용자가 사이트 ID 999의 2024년 12월 캘린더를 조회한다
     * <p>
     * Then 캘린더 조회가 실패한다
     * and 에러 메시지 "사이트를 찾을 수 없습니다"가 반환된다
     */
    @Test
    @DisplayName("존재하지 않는 사이트 ID로 조회시 - 캘린더 조회가 실패한다.")
    void 존재하지_않는_사이트_ID로_조회_시_에러() {

        // Given: 사이트 ID 999가 존재하지 않는다
        var nonExistentSiteId = 999L;

        // When: 사용자가 사이트 ID 999의 2024년 12월 캘린더를 조회한다
        var response = given()
                .when()
                .get("/api/reservations/calendar?year=2024&month=12&siteId=" + nonExistentSiteId)
                .then()
                .log().all()
                .extract();

        // Then: 캘린더 조회가 실패한다
        assertThat(response.statusCode()).isEqualTo(BAD_REQUEST.value());
    }
}
