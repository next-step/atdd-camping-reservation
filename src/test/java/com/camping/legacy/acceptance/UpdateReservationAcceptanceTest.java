package com.camping.legacy.acceptance;

import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UpdateReservationAcceptanceTest extends AcceptanceTestBase {

    private static final String RESERVATIONS_API = "/api/reservations";

    @Test
    void 올바른_확인코드면_예약자_이름을_수정할_수_있다() {
        // Given: 예약 생성
        LocalDate start = LocalDate.now().plusDays(10);
        LocalDate end = LocalDate.now().plusDays(12);
        ExtractableResponse<Response> created = createReservation("김철수", "A-1", start, end);
        assertThat(created.statusCode()).isEqualTo(201);

        Long id = created.jsonPath().getLong("id");
        String confirmationCode = created.jsonPath().getString("confirmationCode");

        // When: 이름 변경
        ExtractableResponse<Response> updated = updateReservation(
                id,
                confirmationCode,
                new ReservationUpdateBody("김철수(수정)", null, null, null, null, null, null, null)
        );

        // Then
        assertThat(updated.statusCode()).isEqualTo(200);
        assertThat(updated.jsonPath().getString("customerName")).isEqualTo("김철수(수정)");
    }

    @Test
    void 확인코드가_일치하지_않으면_예약_수정이_거부된다() {
        // Given
        LocalDate start = LocalDate.now().plusDays(10);
        LocalDate end = LocalDate.now().plusDays(12);
        ExtractableResponse<Response> created = createReservation("김철수", "A-1", start, end);
        Long id = created.jsonPath().getLong("id");

        // When
        ExtractableResponse<Response> response = updateReservation(
                id,
                "ZZZ999",
                new ReservationUpdateBody("김철수(수정)", null, null, null, null, null, null, null)
        );

        // Then
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("message")).isEqualTo("확인 코드가 일치하지 않습니다.");
    }

    @Test
    void 변경된_날짜가_과거라면_예약_수정이_거부된다() {
        // Given
        LocalDate start = LocalDate.now().plusDays(10);
        LocalDate end = LocalDate.now().plusDays(12);
        ExtractableResponse<Response> created = createReservation("김철수", "A-1", start, end);
        Long id = created.jsonPath().getLong("id");
        String confirmationCode = created.jsonPath().getString("confirmationCode");

        // When: 과거로 변경 요청
        LocalDate pastStart = LocalDate.now().minusDays(3);
        LocalDate pastEnd = LocalDate.now().minusDays(1);

        ExtractableResponse<Response> response = updateReservation(
                id,
                confirmationCode,
                new ReservationUpdateBody(null, pastStart.toString(), pastEnd.toString(), null, null, null, null, null)
        );

        // Then
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("message")).isEqualTo("과거 날짜로 예약할 수 없습니다.");
    }

    // FIXME 현재 updateReservation에는 '중복 예약 방지' 로직이 없음
    @Disabled
    @Test
    void 변경된_예약이_다른_예약과_기간이_겹치면_수정이_거부된다() {
        // Given: 충돌 예약(홍길동)
        LocalDate conflictStart = LocalDate.now().plusDays(20);
        LocalDate conflictEnd = LocalDate.now().plusDays(22);
        ExtractableResponse<Response> conflictCreated = createReservation("홍길동", "A-1", conflictStart, conflictEnd);
        assertThat(conflictCreated.statusCode()).isEqualTo(201);

        // Given: 수정 대상 예약(김철수) - 겹치지 않게 생성
        LocalDate targetStart = LocalDate.now().plusDays(24);
        LocalDate targetEnd = LocalDate.now().plusDays(25);
        ExtractableResponse<Response> targetCreated = createReservation("김철수", "A-1", targetStart, targetEnd);
        Long targetId = targetCreated.jsonPath().getLong("id");
        String targetCode = targetCreated.jsonPath().getString("confirmationCode");

        // When: 홍길동 예약과 겹치는 기간으로 변경 시도
        ExtractableResponse<Response> response = updateReservation(
                targetId,
                targetCode,
                new ReservationUpdateBody(null, conflictStart.toString(), conflictEnd.toString(), null, null, null, null, null)
        );

        // Then
        // 현재 updateReservation 구현에는 '중복 예약 방지' 로직이 없어서(단순 save) 이 테스트는 실패할 수 있습니다.
        // 스펙을 만족시키려면 update 시에도 겹침 체크가 필요합니다.
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("message")).isEqualTo("해당 기간에 이미 예약이 존재합니다.");
    }

    // FIXME 현재 updateReservation에는 '30일 제한' 로직이 없음
    @Disabled
    @Test
    void 예외_변경된_예약_기간이_30일을_초과하면_수정이_거부된다() {
        // Given
        LocalDate start = LocalDate.now().plusDays(10);
        LocalDate end = LocalDate.now().plusDays(12);
        ExtractableResponse<Response> created = createReservation("김철수", "A-1", start, end);
        Long id = created.jsonPath().getLong("id");
        String confirmationCode = created.jsonPath().getString("confirmationCode");

        // When: 30일 초과로 변경
        LocalDate newStart = LocalDate.now().plusDays(10);
        LocalDate newEnd = newStart.plusDays(31);

        ExtractableResponse<Response> response = updateReservation(
                id,
                confirmationCode,
                new ReservationUpdateBody(null, newStart.toString(), newEnd.toString(), null, null, null, null, null)
        );

        // Then
        // 현재 updateReservation은 30일 제한 검증을 하지 않습니다. (createReservation만 검증)
        // 스펙을 만족시키려면 update에서도 동일 제한 검증을 추가해야 합니다.
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("message")).isEqualTo("예약 기간은 최대 30일입니다.");
    }

    private ExtractableResponse<Response> createReservation(
            String customerName,
            String siteNumber,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return given()
                .contentType(ContentType.JSON)
                .body(new ReservationCreateBody(
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

    private ExtractableResponse<Response> updateReservation(
            Long reservationId,
            String confirmationCode,
            ReservationUpdateBody body
    ) {
        return given()
                .contentType(ContentType.JSON)
                .queryParam("confirmationCode", confirmationCode)
                .body(body)
                .when()
                .put(RESERVATIONS_API + "/" + reservationId)
                .then()
                .extract();
    }

    /** createReservation 요청 바디 */
    private record ReservationCreateBody(
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

    /** updateReservation 요청 바디: ReservationRequest와 호환되는 필드명을 사용 */
    private record ReservationUpdateBody(
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

