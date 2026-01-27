package com.camping.legacy.acceptance;

import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

    // 테스트용 기간 상수
    private static final int 기본_시작일_오프셋 = 5;
    private static final int 기본_종료일_오프셋 = 7;

    // 동시 요청 수
    private static final int 동시_요청_수_10건 = 10;
    private static final int 동시_요청_수_5건 = 5;

    // 확인코드 길이
    private static final int 확인코드_길이 = 6;

    // =========================================================================
    // 검증 포인트
    // =========================================================================

    @Nested
    @DisplayName("검증 포인트")
    class 검증_포인트 {

        @Test
        @Disabled("BUG: 동시 요청 시 중복 예약이 가능함 - 락/트랜잭션 처리 필요")
        @DisplayName("VP-01: 동시 10건 요청 시 1건만 성공")
        void 동시_10건_요청시_1건만_성공한다() throws Exception {
            // given
            LocalDate 시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 종료일 = daysFromNow(기본_종료일_오프셋);

            ExecutorService executor = Executors.newFixedThreadPool(동시_요청_수_10건);
            List<Callable<ExtractableResponse<Response>>> tasks = new ArrayList<>();

            for (int i = 0; i < 동시_요청_수_10건; i++) {
                final int index = i;
                tasks.add(() -> 예약_생성_요청_고객정보(
                        SITE_A1, 시작일, 종료일,
                        "고객" + index, "0101234567" + index
                ));
            }

            // when - 동시에 10건 요청
            List<Future<ExtractableResponse<Response>>> futures = executor.invokeAll(tasks);
            executor.shutdown();

            // then - 결과 분석
            int 성공_수 = 0;
            int 실패_수 = 0;

            for (Future<ExtractableResponse<Response>> future : futures) {
                ExtractableResponse<Response> 응답 = future.get();
                if (응답.statusCode() == HttpStatus.CREATED.value()) {
                    성공_수++;
                } else if (응답.statusCode() == HttpStatus.CONFLICT.value()) {
                    실패_수++;
                }
            }

            // 정확히 1건만 성공해야 함
            assertThat(성공_수).isEqualTo(1);
            assertThat(실패_수).isEqualTo(동시_요청_수_10건 - 1);
        }

        @Test
        @Disabled("BUG: 동시 요청 시 중복 예약이 가능함")
        @DisplayName("VP-02: 나머지는 충돌 에러 반환")
        void 실패한_요청은_충돌_에러를_반환한다() throws Exception {
            // given
            LocalDate 시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 종료일 = daysFromNow(기본_종료일_오프셋);

            ExecutorService executor = Executors.newFixedThreadPool(동시_요청_수_5건);
            List<Callable<ExtractableResponse<Response>>> tasks = new ArrayList<>();

            for (int i = 0; i < 동시_요청_수_5건; i++) {
                final int index = i;
                tasks.add(() -> 예약_생성_요청_고객정보(
                        SITE_A1, 시작일, 종료일,
                        "고객" + index, "0101234567" + index
                ));
            }

            // when
            List<Future<ExtractableResponse<Response>>> futures = executor.invokeAll(tasks);
            executor.shutdown();

            // then - 실패한 요청들은 CONFLICT 에러를 반환해야 함
            List<String> 에러메시지목록 = new ArrayList<>();
            for (Future<ExtractableResponse<Response>> future : futures) {
                ExtractableResponse<Response> 응답 = future.get();
                if (응답.statusCode() == HttpStatus.CONFLICT.value()) {
                    String message = 응답.jsonPath().getString("message");
                    에러메시지목록.add(message);
                }
            }

            // 모든 실패 응답은 적절한 에러 메시지를 포함해야 함
            for (String 메시지 : 에러메시지목록) {
                assertThat(메시지).isEqualTo("해당 기간에 이미 예약이 존재합니다.");
            }
        }

        @Test
        @Disabled("BUG: 동시 요청 시 중복 예약이 가능함")
        @DisplayName("VP-03: 성공한 예약 데이터 정합성 유지")
        void 성공한_예약의_데이터_정합성이_유지된다() throws Exception {
            // given
            LocalDate 시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 종료일 = daysFromNow(기본_종료일_오프셋);

            ExecutorService executor = Executors.newFixedThreadPool(동시_요청_수_10건);
            List<Callable<ExtractableResponse<Response>>> tasks = new ArrayList<>();

            for (int i = 0; i < 동시_요청_수_10건; i++) {
                final int index = i;
                tasks.add(() -> 예약_생성_요청_고객정보(
                        SITE_A1, 시작일, 종료일,
                        "고객" + index, "0101234567" + index
                ));
            }

            // when
            List<Future<ExtractableResponse<Response>>> futures = executor.invokeAll(tasks);
            executor.shutdown();

            // then - 성공한 예약 찾기
            ExtractableResponse<Response> 성공응답 = null;
            for (Future<ExtractableResponse<Response>> future : futures) {
                ExtractableResponse<Response> 응답 = future.get();
                if (응답.statusCode() == HttpStatus.CREATED.value()) {
                    성공응답 = 응답;
                    break;
                }
            }

            // 성공한 예약의 데이터 정합성 검증
            assertThat(성공응답).isNotNull();
            assertThat(성공응답.jsonPath().getString("siteNumber")).isEqualTo(SITE_A1);
            assertThat(성공응답.jsonPath().getString("startDate")).isEqualTo(시작일.toString());
            assertThat(성공응답.jsonPath().getString("endDate")).isEqualTo(종료일.toString());
            assertThat(성공응답.jsonPath().getString("status")).isEqualTo("CONFIRMED");
            assertThat(성공응답.jsonPath().getString("confirmationCode")).hasSize(확인코드_길이);
        }
    }

    // =========================================================================
    // 추가 시나리오
    // =========================================================================

    @Nested
    @DisplayName("추가 시나리오")
    class 추가_시나리오 {

        @Test
        @DisplayName("서로 다른 사이트에 동시 예약은 모두 성공해야 한다")
        void 서로_다른_사이트에_동시_예약은_모두_성공한다() throws Exception {
            // given
            LocalDate 시작일 = daysFromNow(기본_시작일_오프셋);
            LocalDate 종료일 = daysFromNow(기본_종료일_오프셋);
            String[] 사이트목록 = {SITE_A1, SITE_A2, SITE_B1, SITE_B2};

            ExecutorService executor = Executors.newFixedThreadPool(사이트목록.length);
            List<Callable<ExtractableResponse<Response>>> tasks = new ArrayList<>();

            for (int i = 0; i < 사이트목록.length; i++) {
                final String 사이트 = 사이트목록[i];
                final int index = i;
                tasks.add(() -> 예약_생성_요청_고객정보(
                        사이트, 시작일, 종료일,
                        "고객" + index, "0101234567" + index
                ));
            }

            // when
            List<Future<ExtractableResponse<Response>>> futures = executor.invokeAll(tasks);
            executor.shutdown();

            // then - 모든 요청이 성공해야 함 (서로 다른 사이트이므로)
            int 성공_수 = 0;
            for (Future<ExtractableResponse<Response>> future : futures) {
                ExtractableResponse<Response> 응답 = future.get();
                if (응답.statusCode() == HttpStatus.CREATED.value()) {
                    성공_수++;
                }
            }

            assertThat(성공_수).isEqualTo(사이트목록.length);
        }

        @Test
        @DisplayName("서로 다른 기간에 동시 예약은 모두 성공해야 한다")
        void 서로_다른_기간에_동시_예약은_모두_성공한다() throws Exception {
            // given - 겹치지 않는 4개의 기간
            int[][] 기간목록 = {
                    {5, 7},   // 5~7일
                    {8, 10},  // 8~10일
                    {11, 13}, // 11~13일
                    {14, 16}  // 14~16일
            };

            ExecutorService executor = Executors.newFixedThreadPool(기간목록.length);
            List<Callable<ExtractableResponse<Response>>> tasks = new ArrayList<>();

            for (int i = 0; i < 기간목록.length; i++) {
                final int[] 기간 = 기간목록[i];
                final int index = i;
                tasks.add(() -> 예약_생성_요청_고객정보(
                        SITE_A1,
                        daysFromNow(기간[0]),
                        daysFromNow(기간[1]),
                        "고객" + index, "0101234567" + index
                ));
            }

            // when
            List<Future<ExtractableResponse<Response>>> futures = executor.invokeAll(tasks);
            executor.shutdown();

            // then - 모든 요청이 성공해야 함 (겹치지 않는 기간이므로)
            int 성공_수 = 0;
            for (Future<ExtractableResponse<Response>> future : futures) {
                ExtractableResponse<Response> 응답 = future.get();
                if (응답.statusCode() == HttpStatus.CREATED.value()) {
                    성공_수++;
                }
            }

            assertThat(성공_수).isEqualTo(기간목록.length);
        }
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
}
