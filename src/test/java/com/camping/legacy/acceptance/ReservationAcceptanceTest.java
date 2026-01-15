package com.camping.legacy.acceptance;

import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * top3 인수 시나리오용 스켈레톤 테스트.
 *
 * 실제 시나리오(Gherkin)에 맞춰 given/when/then을 채워 넣으면 됩니다.
 */
class ReservationAcceptanceTest extends AcceptanceTestBase {

    private static final String RESERVATIONS_API = "/api/reservations";

    @Test
    void 예약_생성() {
        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = LocalDate.now().plusDays(12);

        // When: 고객 "김철수"가 ... 예약을 요청한다
        ExtractableResponse<Response> response = createReservation(
                "김철수",
                "A-1",
                startDate,
                endDate
        );

        // Then: 예약이 생성된다
        assertThat(response.statusCode()).isEqualTo(201);

        // And: 예약 상태는 "CONFIRMED"이다
        assertThat(response.jsonPath().getString("status")).isEqualTo("CONFIRMED");

        // And: 6자리 영숫자 확인코드가 발급된다
        String confirmationCode = response.jsonPath().getString("confirmationCode");
        assertThat(confirmationCode).isNotBlank();
        assertThat(confirmationCode).matches("^[A-Z0-9]{6}$");
    }

    @Test
    void 예외_종료일이_시작일보다_이전이면_예약이_거부된다() {
        // When
        ExtractableResponse<Response> response = createReservation("김철수", "A-1",
                LocalDate.now().plusDays(12),
                LocalDate.now().plusDays(10)
        );

        // Then
        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.jsonPath().getString("message"))
                .isEqualTo("종료일이 시작일보다 이전일 수 없습니다.");
    }

    @Test
    void 예외_과거_날짜로_예약을_시도하면_거부된다() {
        // Given: A-1 사이트가 존재한다

        LocalDate startDate = LocalDate.now().minusDays(3);
        LocalDate endDate = LocalDate.now().minusDays(1);

        // When
        ExtractableResponse<Response> response = createReservation("김철수", "A-1", startDate, endDate);

        // Then
        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.jsonPath().getString("message"))
                .isEqualTo("과거 날짜로 예약할 수 없습니다.");
    }

    @Test
    void 예외_허용된_기간_30일을_초과하면_예약이_거부된다() {
        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = startDate.plusDays(31);

        // When
        ExtractableResponse<Response> response = createReservation("김철수", "A-1", startDate, endDate);

        // Then
        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.jsonPath().getString("message"))
                .isEqualTo("예약 기간은 최대 30일입니다.");
    }

    @Test
    void 예외_동일_사이트의_기간이_겹치면_중복_예약이_거부된다() {
        // Given:  특정 미래 구간에 이미 예약이 존재한다
        LocalDate existingStart = LocalDate.now().plusDays(20);
        LocalDate existingEnd = LocalDate.now().plusDays(22);
        ExtractableResponse<Response> existedReservation = createReservation("홍길동", "A-1", existingStart, existingEnd);
        assertThat(existedReservation.statusCode()).isEqualTo(201);

        // When: 겹치는 기간으로 또 예약한다
        ExtractableResponse<Response> response = createReservation("김철수", "A-1",
                existingStart,
                existingEnd
        );

        // Then
        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.jsonPath().getString("message"))
                .isEqualTo("해당 기간에 이미 예약이 존재합니다.");
    }

    // FIXME: 동시성 이슈 해결 필요
    @Test
    @Disabled
    void 예외_동시에_동일_사이트_기간으로_예약_요청이_여러_건_들어와도_하나만_성공해야_한다() throws ExecutionException, InterruptedException {
        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = LocalDate.now().plusDays(12);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> task1 = () -> createReservation("사용자1", "A-1", startDate, endDate).statusCode();
            Callable<Integer> task2 = () -> createReservation("사용자2", "A-1", startDate, endDate).statusCode();

            List<Future<Integer>> futures = pool.invokeAll(List.of(task1, task2));

            int s1 = futures.get(0).get();
            int s2 = futures.get(1).get();

            // 성공: 201, 중복 거부: 409 (컨트롤러 정책)
            assertThat(List.of(s1, s2)).contains(201);
            assertThat(List.of(s1, s2)).contains(409);
        } finally {
            pool.shutdownNow();
        }
    }

    private ExtractableResponse<Response> createReservation(
            String customerName,
            String siteNumber,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return given()
                .contentType(ContentType.JSON)
                .body(new ReservationRequestBody(
                        customerName,
                        startDate.toString(),
                        endDate.toString(),
                        siteNumber,
                        "010-0000-0000",
                        2,
                        "12가3456",
                        "요청사항 없음"
                ))
                .when()
                .post(RESERVATIONS_API)
                .then()
                .extract();
    }

    /**
     * 테스트 요청 바디 전용 DTO.
     *
     * LocalDate 직렬화(ObjectMapper 설정)에 의존하지 않고,
     * ISO-8601 문자열(yyyy-MM-dd)로 명확히 요청하려고 String으로 둡니다.
     */
    private record ReservationRequestBody(
            String customerName,
            String startDate,
            String endDate,
            String siteNumber,
            String phoneNumber,
            Integer numberOfPeople,
            String carNumber,
            String requests
    ) {
    }
}
