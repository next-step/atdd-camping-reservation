package com.camping.legacy;

import static com.camping.legacy.fixture.ReservationFixture.*;
import static com.camping.legacy.step.ReservationStep.성수기_주말에_예약을_요청한다;
import static com.camping.legacy.step.ReservationStep.예약을_요청한다;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReservationCreationAcceptanceTest extends AcceptanceTest {

  @DisplayName("유효한 정보로 예약 요청 시 예약이 확정된다")
  @Test
  void 유효한_정보로_예약요청시_예약이_확정된다() {
    // given
    // when
    var 응답 = 예약을_요청한다(0, 2, CUSTOMER_NAME, SITE_A1, PHONE_NUMBER);

    // then
    예약_성공_확인(응답);
    예약이_확정된_상태이다(응답, CONFIRMED_STATUS);
    예약_확인코드가_발급되었다(응답);
  }

  @DisplayName("최대 예약 가능 기간(30박)을 꽉 채워 예약 요청한다")
  @Test
  void 최대_예약_가능기간을_꽉_채워_예약_요청한다() {
    // given
    // when
    var 응답 = 예약을_요청한다(0, 30, CUSTOMER_NAME, SITE_A1, PHONE_NUMBER);

    // then
    예약_성공_확인(응답);
  }

  @DisplayName("예약 제한 기간(30박)을 초과하여 요청하면 예약이 거부된다")
  @Test
  void 예약_제한_기간을_초과하여_요청하면_예약이_거부된다() {
    // given
    // when
    var 응답 = 예약을_요청한다(0, 31, CUSTOMER_NAME, SITE_A1, PHONE_NUMBER);

    // then
    예약이_거부되었다(응답);
    에러메시지가_확인된다(응답, MAX_PERIOD_EXCEEDED_MESSAGE);
  }

  @DisplayName("유효하지 않은 고객 이름으로 예약할 경우 예약 거부된다")
  @Test
  void 유효하지_않은_고객_이름으로_예약할_경우_예약_거부된다() {
    // given
    // when
    var 응답 = 예약을_요청한다(0, 2, INVALID_CUSTOMER_NAME, SITE_A1, PHONE_NUMBER);

    // then
    예약이_거부되었다(응답);
    에러메시지가_확인된다(응답, INVALID_CUSTOMER_NAME_MESSAGE);
  }

  @DisplayName("유효하지 않은 전화번호로 예약 요청하면 예약 거부된다")
  @Test
  void 유효하지_않은_전화번호로_예약_요청하면_예약_거부된다() {
    // given
    // when
    var 응답 = 예약을_요청한다(0, 2, CUSTOMER_NAME, SITE_A1, INVALID_PHONE_NUMBER);

    // then
    예약이_거부되었다(응답);
    에러메시지가_확인된다(응답, INVALID_PHONE_NUMBER_MESSAGE);
  }

  // NOTE. 계산 가격을 저장하지 않고 콘솔 로그 출력만 한다.
  @DisplayName("성수기 주말 할증 요금이 자동 계산된다")
  @Test
  void 성수기_주말_할증_요금이_자동_계산된다() {
    // when
    var 응답 = 성수기_주말에_예약을_요청한다(PEAK_SEASON_START_MONTH, "성수기 주말 고객", SITE_A1, PHONE_NUMBER);

    // then
    예약_성공_확인(응답);
  }
}
