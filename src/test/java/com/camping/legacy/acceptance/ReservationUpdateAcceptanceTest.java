package com.camping.legacy.acceptance;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;

import static com.camping.legacy.acceptance.ReservationSteps.createReservation;
import static com.camping.legacy.acceptance.ReservationSteps.updateReservation;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

public class ReservationUpdateAcceptanceTest extends AcceptanceTest {

    @Test
    @DisplayName("올바른 확인코드로 예약 날짜 수정 성공")
    void 예약_수정_성공() {
        // Given - 예약 생성
        var createResponse = createReservation("홍길동", "A-1", 5, 7);
        var reservationId = createResponse.jsonPath().getLong("id");
        var confirmationCode = createResponse.jsonPath().getString("confirmationCode");

        // When - 날짜 변경 요청
        var updateRequest = new HashMap<String, Object>();
        updateRequest.put("startDate", LocalDate.now().plusDays(10).toString());
        updateRequest.put("endDate", LocalDate.now().plusDays(12).toString());

        var updateResponse = updateReservation(reservationId, confirmationCode, updateRequest);

        // Then - 수정 성공 검증
        assertThat(updateResponse.statusCode()).isEqualTo(200);
        assertThat(updateResponse.jsonPath().getString("startDate"))
                .isEqualTo(LocalDate.now().plusDays(10).toString());
    }

    @Test
    @DisplayName("잘못된 확인코드로 수정 시 실패")
    void 잘못된_확인코드_수정_실패() {
        // Given - 예약 생성
        var createResponse = createReservation("홍길동", "A-1", 5, 7);
        var reservationId = createResponse.jsonPath().getLong("id");

        // When - 잘못된 확인코드로 수정 요청
        var updateRequest = new HashMap<String, Object>();
        updateRequest.put("startDate", LocalDate.now().plusDays(10).toString());

        var updateResponse = updateReservation(reservationId, "WRONG1", updateRequest);

        // Then - 실패 응답 검증
        assertThat(updateResponse.statusCode()).isEqualTo(400);
    }

    /**
     * 레거시 버그: updateReservation에 중복 날짜 체크 로직 누락
     * - 현재 상태: 200 반환 (버그)
     * - 기대 동작: 400 반환 (중복 체크 후 실패)
     */
    @Test
    @Disabled("BUG-001: 버그 수정 대상 - updateReservation 중복 날짜 체크 누락")
    @DisplayName("중복 발생하는 날짜로 수정 시 실패")
    void 중복_날짜_수정_실패() {
        // Given - 첫 번째 예약 (A-1, 10~15일)
        createReservation("김철수", "A-1", 10, 15);

        // Given - 두 번째 예약 (A-1, 20~25일)
        var secondResponse = createReservation("홍길동", "A-1", 20, 25);
        var reservationId = secondResponse.jsonPath().getLong("id");
        var confirmationCode = secondResponse.jsonPath().getString("confirmationCode");

        // When - 첫 번째와 겹치는 날짜로 수정 시도
        var updateRequest = new HashMap<String, Object>();
        updateRequest.put("customerName", "홍길동");
        updateRequest.put("phoneNumber", "01012345678");
        updateRequest.put("siteNumber", "A-1");
        updateRequest.put("startDate", LocalDate.now().plusDays(22).toString());
        updateRequest.put("endDate", LocalDate.now().plusDays(24).toString());

        var updateResponse = updateReservation(reservationId, confirmationCode, updateRequest);

        // Then - 중복으로 인한 실패 검증
        assertThat(updateResponse.statusCode()).isEqualTo(400);
    }
}
