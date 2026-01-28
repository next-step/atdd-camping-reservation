package com.camping.legacy.acceptance;

import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.camping.legacy.support.TestDataFactory.*;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 동시성 제어 인수 테스트
 *
 * 인수 조건: "동일 사이트/기간에 동시 예약 요청이 들어와도 단 하나의 예약만 성공해야 한다."
 *
 * @see docs/acceptance-criteria.md - 5. 동시성 제어 (6점)
 */
@DisplayName("5. 동시성 제어 인수 테스트")
class ConcurrencyAcceptanceTest extends AcceptanceTest {

    @Test
    @Disabled("BUG: 동시 요청 시 중복 예약이 가능함 - 락/트랜잭션 처리 필요")
    @DisplayName("동시 10건 요청 시 1건만 성공")
    void 동시_10건_요청시_1건만_성공한다() throws Exception {
        // given
        LocalDate 시작일 = daysFromNow(5);
        LocalDate 종료일 = daysFromNow(7);

        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Callable<ExtractableResponse<Response>>> tasks = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            final int index = i;
            tasks.add(() -> 예약_생성_요청_고객정보(
                    SITE_A1, 시작일, 종료일,
                    "고객" + index, "0101234567" + index
            ));
        }

        // when
        List<Future<ExtractableResponse<Response>>> futures = executor.invokeAll(tasks);
        executor.shutdown();

        // then
        int 성공_수 = 0;
        for (Future<ExtractableResponse<Response>> future : futures) {
            ExtractableResponse<Response> 응답 = future.get();
            if (응답.statusCode() == HttpStatus.CREATED.value()) {
                성공_수++;
            }
        }

        assertThat(성공_수).isEqualTo(1);
    }

    @Test
    @Disabled("BUG: 동시 요청 시 중복 예약이 가능함")
    @DisplayName("나머지는 충돌 에러 반환")
    void 실패한_요청은_충돌_에러를_반환한다() throws Exception {
        // given
        LocalDate 시작일 = daysFromNow(5);
        LocalDate 종료일 = daysFromNow(7);

        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<Callable<ExtractableResponse<Response>>> tasks = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            final int index = i;
            tasks.add(() -> 예약_생성_요청_고객정보(
                    SITE_A1, 시작일, 종료일,
                    "고객" + index, "0101234567" + index
            ));
        }

        // when
        List<Future<ExtractableResponse<Response>>> futures = executor.invokeAll(tasks);
        executor.shutdown();

        // then
        int 실패_수 = 0;
        for (Future<ExtractableResponse<Response>> future : futures) {
            ExtractableResponse<Response> 응답 = future.get();
            if (응답.statusCode() == HttpStatus.CONFLICT.value()) {
                실패_수++;
            }
        }

        assertThat(실패_수).isEqualTo(4); // 5건 중 1건 성공, 4건 실패
    }

    @Test
    @Disabled("BUG: 동시 요청 시 중복 예약이 가능함")
    @DisplayName("성공한 예약 데이터 정합성 유지")
    void 성공한_예약의_데이터_정합성이_유지된다() throws Exception {
        // given
        LocalDate 시작일 = daysFromNow(5);
        LocalDate 종료일 = daysFromNow(7);

        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Callable<ExtractableResponse<Response>>> tasks = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            final int index = i;
            tasks.add(() -> 예약_생성_요청_고객정보(
                    SITE_A1, 시작일, 종료일,
                    "고객" + index, "0101234567" + index
            ));
        }

        // when
        List<Future<ExtractableResponse<Response>>> futures = executor.invokeAll(tasks);
        executor.shutdown();

        // then
        ExtractableResponse<Response> 성공응답 = null;
        for (Future<ExtractableResponse<Response>> future : futures) {
            ExtractableResponse<Response> 응답 = future.get();
            if (응답.statusCode() == HttpStatus.CREATED.value()) {
                성공응답 = 응답;
                break;
            }
        }

        assertThat(성공응답).isNotNull();
        assertThat(성공응답.jsonPath().getString("siteNumber")).isEqualTo(SITE_A1);
        assertThat(성공응답.jsonPath().getString("startDate")).isEqualTo(시작일.toString());
        assertThat(성공응답.jsonPath().getString("endDate")).isEqualTo(종료일.toString());
        assertThat(성공응답.jsonPath().getString("confirmationCode")).hasSize(6);
    }

    // =========================================================================
    // 헬퍼 메서드
    // =========================================================================

    private ExtractableResponse<Response> 예약_생성_요청_고객정보(
            String siteNumber, LocalDate startDate, LocalDate endDate,
            String customerName, String phoneNumber) {
        return given()
                .contentType(ContentType.JSON)
                .body(reservationRequest(siteNumber, startDate, endDate, customerName, phoneNumber))
                .when()
                .post("/api/reservations")
                .then()
                .extract();
    }

    // =========================================================================
    // Custom Matcher
    // =========================================================================

    private void 응답_검증_동시성_결과(List<ExtractableResponse<Response>> responses, int expectedSuccess, int expectedFailure) {
        int 성공_수 = 0;
        int 실패_수 = 0;

        for (ExtractableResponse<Response> 응답 : responses) {
            if (응답.statusCode() == HttpStatus.CREATED.value()) {
                성공_수++;
            } else if (응답.statusCode() == HttpStatus.CONFLICT.value()) {
                실패_수++;
            }
        }

        assertThat(성공_수).isEqualTo(expectedSuccess);
        assertThat(실패_수).isEqualTo(expectedFailure);
    }

    private void 응답_검증_예약_데이터_정합성(ExtractableResponse<Response> response, String expectedSite, LocalDate expectedStart, LocalDate expectedEnd) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(response.jsonPath().getString("siteNumber")).isEqualTo(expectedSite);
        assertThat(response.jsonPath().getString("startDate")).isEqualTo(expectedStart.toString());
        assertThat(response.jsonPath().getString("endDate")).isEqualTo(expectedEnd.toString());
        assertThat(response.jsonPath().getString("confirmationCode")).hasSize(6);
    }
}
