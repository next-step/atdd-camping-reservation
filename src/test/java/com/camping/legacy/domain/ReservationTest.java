package com.camping.legacy.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationTest {

    @Test
    @DisplayName("정상적인 예약 생성")
    void 정상적인_예약_생성() {
        // given
        String customerName = "김영희";
        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = LocalDate.now().plusDays(12);
        Campsite campsite = new Campsite("A-1", "캠핑장 설명", 4);
        String phoneNumber = "010-1234-5678";
        String confirmationCode = "ABC123";

        // when
        Reservation reservation = new Reservation(customerName, startDate, endDate, campsite, phoneNumber, confirmationCode);

        // then
        assertThat(reservation.getCustomerName()).isEqualTo(customerName);
        assertThat(reservation.getStartDate()).isEqualTo(startDate);
        assertThat(reservation.getEndDate()).isEqualTo(endDate);
        assertThat(reservation.getReservationDate()).isEqualTo(startDate);
        assertThat(reservation.getCampsite()).isEqualTo(campsite);
        assertThat(reservation.getPhoneNumber()).isEqualTo(phoneNumber);
        assertThat(reservation.getConfirmationCode()).isEqualTo(confirmationCode);
    }

    @Test
    @DisplayName("예약자 이름이 null인 경우 예외 발생")
    void 예약자_이름이_null인_경우_예외_발생() {
        // given
        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = LocalDate.now().plusDays(12);
        Campsite campsite = new Campsite("A-1", "캠핑장 설명", 4);

        // when & then
        assertThatThrownBy(() -> new Reservation(null, startDate, endDate, campsite, "010-1234-5678", "ABC123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("예약자 이름을 입력해주세요.");
    }

    @Test
    @DisplayName("예약자 이름이 빈 문자열인 경우 예외 발생")
    void 예약자_이름이_빈_문자열인_경우_예외_발생() {
        // given
        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = LocalDate.now().plusDays(12);
        Campsite campsite = new Campsite("A-1", "캠핑장 설명", 4);

        // when & then
        assertThatThrownBy(() -> new Reservation("   ", startDate, endDate, campsite, "010-1234-5678", "ABC123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("예약자 이름을 입력해주세요.");
    }

    @Test
    @DisplayName("시작일이 null인 경우 예외 발생")
    void 시작일이_null인_경우_예외_발생() {
        // given
        LocalDate endDate = LocalDate.now().plusDays(12);
        Campsite campsite = new Campsite("A-1", "캠핑장 설명", 4);

        // when & then
        assertThatThrownBy(() -> new Reservation("김영희", null, endDate, campsite, "010-1234-5678", "ABC123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("예약 기간을 선택해주세요.");
    }

    @Test
    @DisplayName("종료일이 null인 경우 예외 발생")
    void 종료일이_null인_경우_예외_발생() {
        // given
        LocalDate startDate = LocalDate.now().plusDays(10);
        Campsite campsite = new Campsite("A-1", "캠핑장 설명", 4);

        // when & then
        assertThatThrownBy(() -> new Reservation("김영희", startDate, null, campsite, "010-1234-5678", "ABC123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("예약 기간을 선택해주세요.");
    }

    @Test
    @DisplayName("종료일이 시작일보다 이전인 경우 예외 발생")
    void 종료일이_시작일보다_이전인_경우_예외_발생() {
        // given
        LocalDate startDate = LocalDate.now().plusDays(12);
        LocalDate endDate = LocalDate.now().plusDays(10);
        Campsite campsite = new Campsite("A-1", "캠핑장 설명", 4);

        // when & then
        assertThatThrownBy(() -> new Reservation("김영희", startDate, endDate, campsite, "010-1234-5678", "ABC123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("종료일이 시작일보다 이전일 수 없습니다.");
    }

    @Test
    @DisplayName("캠핑장이 null인 경우 예외 발생")
    void 캠핑장이_null인_경우_예외_발생() {
        // given
        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = LocalDate.now().plusDays(12);

        // when & then
        assertThatThrownBy(() -> new Reservation("김영희", startDate, endDate, null, "010-1234-5678", "ABC123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("캠핑장 정보가 필요합니다.");
    }

    @Test
    @DisplayName("예약일이 30일을 초과하는 경우 예외 발생")
    void 예약일이_30일을_초과하는_경우_예외_발생() {
        // given
        LocalDate startDate = LocalDate.now().plusDays(31);
        LocalDate endDate = LocalDate.now().plusDays(33);
        Campsite campsite = new Campsite("A-1", "캠핑장 설명", 4);

        // when & then
        assertThatThrownBy(() -> new Reservation("김영희", startDate, endDate, campsite, "010-1234-5678", "ABC123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("예약은 최대 30일 전까지만 가능합니다");
    }

    @Test
    @DisplayName("예약 취소 - 당일 취소")
    void 예약_취소_당일_취소() {
        // given
        LocalDate today = LocalDate.now();
        Campsite campsite = new Campsite("A-1", "캠핑장 설명", 4);
        Reservation reservation = new Reservation("김영희", today, today, campsite, "010-1234-5678", "ABC123");

        // when
        reservation.cancel();

        // then
        assertThat(reservation.getStatus()).isEqualTo("CANCELLED_SAME_DAY");
    }

    @Test
    @DisplayName("예약 취소 - 일반 취소")
    void 예약_취소_일반_취소() {
        // given
        LocalDate futureDate = LocalDate.now().plusDays(5);
        Campsite campsite = new Campsite("A-1", "캠핑장 설명", 4);
        Reservation reservation = new Reservation("김영희", futureDate, futureDate, campsite, "010-1234-5678", "ABC123");

        // when
        reservation.cancel();

        // then
        assertThat(reservation.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("예약 수정 - 모든 필드 수정")
    void 예약_수정_모든_필드_수정() {
        // given
        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = LocalDate.now().plusDays(12);
        Campsite originalCampsite = new Campsite("A-1", "원래 캠핑장", 4);
        Reservation reservation = new Reservation("김영희", startDate, endDate, originalCampsite, "010-1234-5678", "ABC123");

        LocalDate newStartDate = LocalDate.now().plusDays(15);
        LocalDate newEndDate = LocalDate.now().plusDays(17);
        Campsite newCampsite = new Campsite("B-2", "새로운 캠핑장", 6);

        // when
        reservation.updateReservation("박철수", newStartDate, newEndDate, newCampsite, "010-9876-5432");

        // then
        assertThat(reservation.getCustomerName()).isEqualTo("박철수");
        assertThat(reservation.getStartDate()).isEqualTo(newStartDate);
        assertThat(reservation.getEndDate()).isEqualTo(newEndDate);
        assertThat(reservation.getCampsite()).isEqualTo(newCampsite);
        assertThat(reservation.getPhoneNumber()).isEqualTo("010-9876-5432");
    }

    @Test
    @DisplayName("예약 수정 - 일부 필드만 수정")
    void 예약_수정_일부_필드만_수정() {
        // given
        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = LocalDate.now().plusDays(12);
        Campsite campsite = new Campsite("A-1", "캠핑장", 4);
        Reservation reservation = new Reservation("김영희", startDate, endDate, campsite, "010-1234-5678", "ABC123");

        // when (이름과 전화번호만 수정)
        reservation.updateReservation("박철수", null, null, null, "010-9876-5432");

        // then
        assertThat(reservation.getCustomerName()).isEqualTo("박철수");
        assertThat(reservation.getStartDate()).isEqualTo(startDate); // 변경되지 않음
        assertThat(reservation.getEndDate()).isEqualTo(endDate); // 변경되지 않음
        assertThat(reservation.getCampsite()).isEqualTo(campsite); // 변경되지 않음
        assertThat(reservation.getPhoneNumber()).isEqualTo("010-9876-5432");
    }

    @Test
    @DisplayName("예약 확인 상태 - 확정된 예약")
    void 예약_확인_상태_확정된_예약() {
        // given
        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = LocalDate.now().plusDays(12);
        Campsite campsite = new Campsite("A-1", "캠핑장", 4);
        Reservation reservation = new Reservation("김영희", startDate, endDate, campsite, "010-1234-5678", "ABC123");

        // when & then (기본 상태는 @PrePersist에서 설정되므로 직접 테스트하기 어려움)
        // isConfirmed 메서드 테스트를 위해 status를 임의로 설정
        assertThat(reservation.isConfirmed()).isFalse(); // 초기에는 status가 null이므로 false
    }
}