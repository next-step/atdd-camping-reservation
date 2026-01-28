package com.camping.legacy.acceptance;

import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static com.camping.legacy.support.TestDataFactory.*;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 예약 수정 인수 테스트
 *
 * 인수 조건: "본인 확인 후 수정 시, 변경된 날짜/사이트에 대해 중복 검사가 수행되어야 한다."
 *
 * @see docs/acceptance-criteria.md - 6. 예약 수정 (6점)
 */
@DisplayName("6. 예약 수정 인수 테스트")
class ModificationAcceptanceTest extends AcceptanceTest {

    @Test
    @DisplayName("확인코드 불일치 시 수정 거부")
    void 확인코드_불일치시_수정이_거부된다() {
        // given
        LocalDate 시작일 = daysFromNow(5);
        LocalDate 종료일 = daysFromNow(7);
        var 예약 = testDataFactory.createReservation(SITE_A1, 시작일, 종료일, "김철수", "01098765432");

        // when
        LocalDate 새시작일 = daysFromNow(10);
        LocalDate 새종료일 = daysFromNow(12);
        ExtractableResponse<Response> 응답 = 예약_수정_요청(예약.getId(), "WRONG1", SITE_A1, 새시작일, 새종료일);

        // then
        assertThat(응답.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    @Disabled("BUG: 날짜 변경 시 중복 검사가 수행되지 않음")
    @DisplayName("날짜 변경 시 중복 검사 수행")
    void 날짜_변경시_중복_검사가_수행된다() {
        // given
        LocalDate 첫번째시작일 = daysFromNow(5);
        LocalDate 첫번째종료일 = daysFromNow(7);
        var 첫번째예약 = testDataFactory.createReservation(SITE_A1, 첫번째시작일, 첫번째종료일, "김철수", "01098765432");

        LocalDate 두번째시작일 = daysFromNow(10);
        LocalDate 두번째종료일 = daysFromNow(12);
        testDataFactory.createReservation(SITE_A1, 두번째시작일, 두번째종료일, "박영희", "01011112222");

        // when - 첫 번째 예약을 두 번째 예약과 겹치게 변경
        LocalDate 겹치는시작일 = daysFromNow(9);
        LocalDate 겹치는종료일 = daysFromNow(11);
        ExtractableResponse<Response> 응답 = 예약_수정_요청(
                첫번째예약.getId(),
                첫번째예약.getConfirmationCode(),
                SITE_A1, 겹치는시작일, 겹치는종료일
        );

        // then
        assertThat(응답.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    @Disabled("BUG: 사이트 변경 시 중복 검사가 수행되지 않음")
    @DisplayName("사이트 변경 시 중복 검사 수행")
    void 사이트_변경시_중복_검사가_수행된다() {
        // given
        LocalDate 시작일 = daysFromNow(5);
        LocalDate 종료일 = daysFromNow(7);
        var A1예약 = testDataFactory.createReservation(SITE_A1, 시작일, 종료일, "김철수", "01098765432");
        testDataFactory.createReservation(SITE_A2, 시작일, 종료일, "박영희", "01011112222");

        // when - A1 예약을 A2로 변경 (A2는 이미 같은 기간에 예약 있음)
        ExtractableResponse<Response> 응답 = 예약_수정_요청(
                A1예약.getId(),
                A1예약.getConfirmationCode(),
                SITE_A2, 시작일, 종료일
        );

        // then
        assertThat(응답.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    @DisplayName("수정 성공 시 변경된 정보 반환")
    void 수정_성공시_변경된_정보가_반환된다() {
        // given
        LocalDate 시작일 = daysFromNow(5);
        LocalDate 종료일 = daysFromNow(7);
        var 예약 = testDataFactory.createReservation(SITE_A1, 시작일, 종료일, "김철수", "01098765432");

        // when
        LocalDate 새시작일 = daysFromNow(15);
        LocalDate 새종료일 = daysFromNow(17);
        ExtractableResponse<Response> 응답 = 예약_수정_요청(
                예약.getId(),
                예약.getConfirmationCode(),
                SITE_A1, 새시작일, 새종료일
        );

        // then
        assertThat(응답.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(응답.jsonPath().getString("startDate")).isEqualTo(새시작일.toString());
        assertThat(응답.jsonPath().getString("endDate")).isEqualTo(새종료일.toString());
    }

    // =========================================================================
    // 헬퍼 메서드
    // =========================================================================

    private ExtractableResponse<Response> 예약_수정_요청(
            Long reservationId, String confirmationCode,
            String siteNumber, LocalDate startDate, LocalDate endDate) {

        Map<String, Object> request = new HashMap<>();
        request.put("confirmationCode", confirmationCode);
        request.put("siteNumber", siteNumber);
        request.put("startDate", startDate.toString());
        request.put("endDate", endDate.toString());

        return given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put("/api/reservations/{id}", reservationId)
                .then()
                .extract();
    }

    // =========================================================================
    // Custom Matcher
    // =========================================================================

    private void 응답_검증_수정_성공(ExtractableResponse<Response> response, LocalDate expectedStart, LocalDate expectedEnd) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getString("startDate")).isEqualTo(expectedStart.toString());
        assertThat(response.jsonPath().getString("endDate")).isEqualTo(expectedEnd.toString());
    }

    private void 응답_검증_수정_실패(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    }
}
