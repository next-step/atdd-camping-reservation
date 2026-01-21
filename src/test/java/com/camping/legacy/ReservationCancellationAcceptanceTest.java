package com.camping.legacy;

import static com.camping.legacy.assertion.ReservationAssertion.*;
import static com.camping.legacy.assertion.SiteAssertion.*;
import static com.camping.legacy.client.ReservationClient.*;
import static com.camping.legacy.client.SiteClient.*;
import static com.camping.legacy.fixture.ReservationFixture.*;
import static com.camping.legacy.fixture.ReservationRequestBuilder.*;
import static com.camping.legacy.step.ReservationStep.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ReservationCancellationAcceptanceTest extends AcceptanceTest {

  @DisplayName("올바른 예약 확인 코드로 취소 시 재고가 즉시 복구된다")
  @Test
  void 올바른_예약_확인코드로_취소시_재고가_즉시_복구된다() {
    // given
    var 시작일_offset = 10;
    var 종료일_offset = 12;

    // when
    사전_예약을_취소한다(aReservationRequest()
            .withSiteNumber(SITE_A1)
            .withStartDate(시작일_offset)
            .withEndDate(종료일_offset));

    // then
    특정_기간에_사이트가_노출됨을_확인한다(시작일_offset, 종료일_offset, LARGE_SITE_TYPE, SITE_A1);
  }

  @DisplayName("예약 확인 코드가 일치하지 않으면 취소할 수 없다")
  @Test
  void 예약_확인_코드가_일치하지_않으면_취소할_수_없다() {
    // given
    var 예약 = 예약을_완료한다(aReservationRequest().withEndDate(2));

    // when
    var 취소_응답 = 예약을_취소한다(예약.id(), WRONG_CONFIRMATION_CODE);

    // then
    예약_취소가_거부되었다(취소_응답);
  }

  @DisplayName("당일 예약 취소 시 별도의 상태 코드로 관리된다")
  @Test
  void 당일_예약_취소_시_별도의_상태_코드로_관리된다() {
    // given
    var 예약 = 예약을_완료한다(aReservationRequest().withEndDate(2));

    // when
    예약을_취소한다(예약.id(), 예약.confirmationCode());

    // then
    예약_상태가_변경됨을_확인한다(예약.id(), CANCELLED_SAME_DAY_STATUS);
  }

  @DisplayName("예외 상황 테스트: 존재하지 않는 예약 ID로 취소를 시도하면 실패한다")
  @Test
  void 존재하지_않는_예약_ID로_취소하면_실패한다() {
    // when
    var 응답 = 예약을_취소한다(99999L, "아무코드");

    // then
    예약_취소가_거부되었다(응답);
    메시지가_확인된다(응답, NONE_EXIST_RESERVATION_MESSAGE);
  }

  @DisplayName("예외 상황 테스트: 이미 취소된 예약을 다시 취소하려고 하면 실패한다")
  @Test
  void 이미_취소된_예약을_다시_취소하면_실패한다() {
    // given
    var 예약 = 사전_예약을_취소한다(aReservationRequest().withEndDate(2));

    // when
    var 응답 = 예약을_취소한다(예약.id(), 예약.confirmationCode());

    // then
    예약_취소가_거부되었다(응답);
    메시지가_확인된다(응답, ALREADY_CANCELED_RESERVATION_MESSAGE);
  }

  @DisplayName("경계값: 예약 시작일에 따른 취소 상태 변경 테스트")
  @ParameterizedTest(name = "예약 시작일이 {0}일 후일 때, 취소하면 {1} 상태가 된다")
  @CsvSource({
      "0, CANCELLED_SAME_DAY",
      "1, CANCELLED",
      "2, CANCELLED"
  })
  void 예약_시작일에_따라_취소_상태가_결정된다(int startDayOffset, String expectedStatus) {
    // given
    var 예약 = 예약을_완료한다(aReservationRequest()
            .withStartDate(startDayOffset)
            .withEndDate(startDayOffset + 2));

    // when
    예약을_취소한다(예약.id(), 예약.confirmationCode());

    // then
    예약_상태가_변경됨을_확인한다(예약.id(), expectedStatus);
  }
}
