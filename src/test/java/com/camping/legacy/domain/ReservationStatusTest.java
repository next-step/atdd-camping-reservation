package com.camping.legacy.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReservationStatus 도메인 단위 테스트")
class ReservationStatusTest {

    @Nested
    @DisplayName("값 조회 테스트")
    class ValueTest {

        @Test
        @DisplayName("CONFIRMED 상태의 값은 'CONFIRMED'이다")
        void getValue_ForConfirmed_ReturnsConfirmed() {
            // Given & When
            String value = ReservationStatus.CONFIRMED.getValue();

            // Then
            assertThat(value).isEqualTo("CONFIRMED");
        }

        @Test
        @DisplayName("CANCELLED 상태의 값은 'CANCELLED'이다")
        void getValue_ForCancelled_ReturnsCancelled() {
            // Given & When
            String value = ReservationStatus.CANCELLED.getValue();

            // Then
            assertThat(value).isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("CANCELLED_SAME_DAY 상태의 값은 'CANCELLED_SAME_DAY'이다")
        void getValue_ForCancelledSameDay_ReturnsCancelledSameDay() {
            // Given & When
            String value = ReservationStatus.CANCELLED_SAME_DAY.getValue();

            // Then
            assertThat(value).isEqualTo("CANCELLED_SAME_DAY");
        }
    }

    @Nested
    @DisplayName("값으로부터 상태 생성 테스트")
    class FromValueTest {

        @Test
        @DisplayName("'CONFIRMED' 값으로부터 CONFIRMED 상태를 생성한다")
        void fromValue_WithConfirmed_ReturnsConfirmed() {
            // Given & When
            ReservationStatus status = ReservationStatus.fromValue("CONFIRMED");

            // Then
            assertThat(status).isEqualTo(ReservationStatus.CONFIRMED);
        }

        @Test
        @DisplayName("'CANCELLED' 값으로부터 CANCELLED 상태를 생성한다")
        void fromValue_WithCancelled_ReturnsCancelled() {
            // Given & When
            ReservationStatus status = ReservationStatus.fromValue("CANCELLED");

            // Then
            assertThat(status).isEqualTo(ReservationStatus.CANCELLED);
        }

        @Test
        @DisplayName("'CANCELLED_SAME_DAY' 값으로부터 CANCELLED_SAME_DAY 상태를 생성한다")
        void fromValue_WithCancelledSameDay_ReturnsCancelledSameDay() {
            // Given & When
            ReservationStatus status = ReservationStatus.fromValue("CANCELLED_SAME_DAY");

            // Then
            assertThat(status).isEqualTo(ReservationStatus.CANCELLED_SAME_DAY);
        }

        @Test
        @DisplayName("null 값으로부터는 기본값 CONFIRMED를 반환한다")
        void fromValue_WithNull_ReturnsConfirmed() {
            // Given & When
            ReservationStatus status = ReservationStatus.fromValue(null);

            // Then
            assertThat(status).isEqualTo(ReservationStatus.CONFIRMED);
        }

        @Test
        @DisplayName("알 수 없는 값으로부터는 기본값 CONFIRMED를 반환한다")
        void fromValue_WithUnknownValue_ReturnsConfirmed() {
            // Given & When
            ReservationStatus status = ReservationStatus.fromValue("UNKNOWN_STATUS");

            // Then
            assertThat(status).isEqualTo(ReservationStatus.CONFIRMED);
        }

        @Test
        @DisplayName("빈 문자열로부터는 기본값 CONFIRMED를 반환한다")
        void fromValue_WithEmptyString_ReturnsConfirmed() {
            // Given & When
            ReservationStatus status = ReservationStatus.fromValue("");

            // Then
            assertThat(status).isEqualTo(ReservationStatus.CONFIRMED);
        }
    }

    @Nested
    @DisplayName("취소 상태 확인 테스트")
    class CancelledStatusTest {

        @Test
        @DisplayName("CANCELLED 상태는 취소된 상태다")
        void isCancelled_WithCancelled_ReturnsTrue() {
            // Given & When
            boolean cancelled = ReservationStatus.CANCELLED.isCancelled();

            // Then
            assertThat(cancelled).isTrue();
        }

        @Test
        @DisplayName("CANCELLED_SAME_DAY 상태는 취소된 상태다")
        void isCancelled_WithCancelledSameDay_ReturnsTrue() {
            // Given & When
            boolean cancelled = ReservationStatus.CANCELLED_SAME_DAY.isCancelled();

            // Then
            assertThat(cancelled).isTrue();
        }

        @Test
        @DisplayName("CONFIRMED 상태는 취소된 상태가 아니다")
        void isCancelled_WithConfirmed_ReturnsFalse() {
            // Given & When
            boolean cancelled = ReservationStatus.CONFIRMED.isCancelled();

            // Then
            assertThat(cancelled).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {"CANCELLED", "CANCELLED_SAME_DAY"})
        @DisplayName("취소 관련 문자열 상태값은 취소된 상태로 인식된다")
        void isCancelledStatus_WithCancelledStrings_ReturnsTrue(String status) {
            // Given & When
            boolean cancelled = ReservationStatus.isCancelledStatus(status);

            // Then
            assertThat(cancelled).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"CONFIRMED", "UNKNOWN", "", " "})
        @DisplayName("취소가 아닌 문자열 상태값은 취소된 상태로 인식되지 않는다")
        void isCancelledStatus_WithNonCancelledStrings_ReturnsFalse(String status) {
            // Given & When
            boolean cancelled = ReservationStatus.isCancelledStatus(status);

            // Then
            assertThat(cancelled).isFalse();
        }

        @Test
        @DisplayName("null 문자열 상태값은 취소된 상태로 인식되지 않는다")
        void isCancelledStatus_WithNull_ReturnsFalse() {
            // Given & When
            boolean cancelled = ReservationStatus.isCancelledStatus(null);

            // Then
            assertThat(cancelled).isFalse();
        }
    }

    @Nested
    @DisplayName("활성 상태 확인 테스트")
    class ActiveStatusTest {

        @Test
        @DisplayName("CONFIRMED 상태는 활성 상태다")
        void isActive_WithConfirmed_ReturnsTrue() {
            // Given & When
            boolean active = ReservationStatus.CONFIRMED.isActive();

            // Then
            assertThat(active).isTrue();
        }

        @Test
        @DisplayName("CANCELLED 상태는 활성 상태가 아니다")
        void isActive_WithCancelled_ReturnsFalse() {
            // Given & When
            boolean active = ReservationStatus.CANCELLED.isActive();

            // Then
            assertThat(active).isFalse();
        }

        @Test
        @DisplayName("CANCELLED_SAME_DAY 상태는 활성 상태가 아니다")
        void isActive_WithCancelledSameDay_ReturnsFalse() {
            // Given & When
            boolean active = ReservationStatus.CANCELLED_SAME_DAY.isActive();

            // Then
            assertThat(active).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {"CONFIRMED", "UNKNOWN", "", " "})
        @DisplayName("취소가 아닌 문자열 상태값은 활성 상태로 인식된다")
        void isActiveStatus_WithNonCancelledStrings_ReturnsTrue(String status) {
            // Given & When
            boolean active = ReservationStatus.isActiveStatus(status);

            // Then
            assertThat(active).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"CANCELLED", "CANCELLED_SAME_DAY"})
        @DisplayName("취소 관련 문자열 상태값은 활성 상태로 인식되지 않는다")
        void isActiveStatus_WithCancelledStrings_ReturnsFalse(String status) {
            // Given & When
            boolean active = ReservationStatus.isActiveStatus(status);

            // Then
            assertThat(active).isFalse();
        }

        @Test
        @DisplayName("null 문자열 상태값은 활성 상태로 인식된다")
        void isActiveStatus_WithNull_ReturnsTrue() {
            // Given & When
            boolean active = ReservationStatus.isActiveStatus(null);

            // Then
            assertThat(active).isTrue();
        }
    }

    @Nested
    @DisplayName("열거형 전체 테스트")
    class EnumComprehensiveTest {

        @ParameterizedTest
        @EnumSource(ReservationStatus.class)
        @DisplayName("모든 상태는 null이 아닌 값을 가진다")
        void getAllStatuses_HaveNonNullValues(ReservationStatus status) {
            // Given & When
            String value = status.getValue();

            // Then
            assertThat(value).isNotNull();
            assertThat(value).isNotBlank();
        }

        @Test
        @DisplayName("총 3개의 상태가 정의되어 있다")
        void getAllStatuses_ContainsExactlyThreeStatuses() {
            // Given & When
            ReservationStatus[] statuses = ReservationStatus.values();

            // Then
            assertThat(statuses).hasSize(3);
            assertThat(statuses).containsExactlyInAnyOrder(
                    ReservationStatus.CONFIRMED,
                    ReservationStatus.CANCELLED,
                    ReservationStatus.CANCELLED_SAME_DAY
            );
        }

        @Test
        @DisplayName("각 상태의 toString은 값과 동일하다")
        void toString_ReturnsCorrectFormat() {
            // Given & When & Then
            assertThat(ReservationStatus.CONFIRMED.toString()).isEqualTo("CONFIRMED");
            assertThat(ReservationStatus.CANCELLED.toString()).isEqualTo("CANCELLED");
            assertThat(ReservationStatus.CANCELLED_SAME_DAY.toString()).isEqualTo("CANCELLED_SAME_DAY");
        }

        @ParameterizedTest
        @EnumSource(ReservationStatus.class)
        @DisplayName("모든 상태는 자기 자신의 값으로부터 복원 가능하다")
        void fromValue_WithOwnValue_ReturnsSelf(ReservationStatus status) {
            // Given
            String value = status.getValue();

            // When
            ReservationStatus restored = ReservationStatus.fromValue(value);

            // Then
            assertThat(restored).isEqualTo(status);
        }

        @Test
        @DisplayName("활성 상태와 취소 상태는 서로 반대다")
        void activeAndCancelledStates_AreOpposite() {
            // Given & When & Then
            for (ReservationStatus status : ReservationStatus.values()) {
                assertThat(status.isActive()).isNotEqualTo(status.isCancelled());
            }
        }
    }
}
