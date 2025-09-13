package com.camping.legacy.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ReservationCreateRequestTest {

    @DisplayName("유효성 검사 성공")
    @Test
    void success(){
        assertDoesNotThrow(() -> ReservationCreateRequestFixture.builder().build());
    }

    @ValueSource(strings = {" "})
    @ParameterizedTest
    @NullAndEmptySource
    void 이름이_누락된_경우_예약_실패(String customerName) {
        // when, then
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                ReservationCreateRequestFixture.builder().customerName(customerName).build()
        );
        assertThat(exception.getMessage()).isEqualTo("예약자 이름을 입력해주세요.");
    }

    @ValueSource(strings = {"asdf", "010-1234-567", "010-12345-6789", " "})
    @NullAndEmptySource
    @ParameterizedTest
    void 전화번호_형식이_잘못된_경우_예약_실패(String phoneNumber) {
        // when, then
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                ReservationCreateRequestFixture.builder().phoneNumber(phoneNumber).build()
        );
        assertThat(exception.getMessage()).isEqualTo("전화번호를 입력해주세요.");
    }

    @ValueSource(strings = {" "})
    @NullAndEmptySource
    @ParameterizedTest
    void 캠핑장_번호를_입력하지_않은_경우_예약_실패(String siteNumber) {
        // when, then
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                ReservationCreateRequestFixture.builder().siteNumber(siteNumber).build()
        );
        assertThat(exception.getMessage()).isEqualTo("캠핑장 번호를 입력해주세요.");
    }

    @Test
    void 예약_기간을_입력하지_않은_경우_예약_실패() {
        // when, then
        RuntimeException exception1 = assertThrows(RuntimeException.class, () ->
                ReservationCreateRequestFixture.builder().startDate(null).build()
        );
        assertThat(exception1.getMessage()).isEqualTo("예약 기간을 선택해주세요.");

        RuntimeException exception2 = assertThrows(RuntimeException.class, () ->
                ReservationCreateRequestFixture.builder().endDate(null).build()
        );
        assertThat(exception2.getMessage()).isEqualTo("예약 기간을 선택해주세요.");
    }

    @Test
    void 예약_종료일이_시작일보다_빠른_경우_예약_실패() {
        // when, then
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                ReservationCreateRequestFixture.builder()
                        .startDate(java.time.LocalDate.of(2025, 9, 12))
                        .endDate(java.time.LocalDate.of(2025, 9, 10))
                        .build()
        );
        assertThat(exception.getMessage()).isEqualTo("종료일이 시작일보다 이전일 수 없습니다.");
    }

}