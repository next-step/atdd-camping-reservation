package com.camping.legacy;
import static com.camping.legacy.assertion.ReservationAssertion.*;
import static com.camping.legacy.client.ReservationClient.*;
import static com.camping.legacy.fixture.ReservationFixture.*;
import static com.camping.legacy.fixture.ReservationRequestBuilder.*;
import static com.camping.legacy.step.ReservationStep.*;
import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;
import static java.time.temporal.TemporalAdjusters.nextOrSame;

import java.time.DayOfWeek;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;

class ReservationCreationAcceptanceTest extends AcceptanceTest {

  @DisplayName("유효한 정보로 예약 요청 시 예약이 확정된다")
  @Test
  void 유효한_정보로_예약요청시_예약이_확정된다() {
    // given
    // when
    var 응답 = 예약을_요청한다(aReservationRequest()
            .withStartDate(0)
            .withEndDate(2)
            .build());

    // then
    예약이_성공적으로_생성되었다(응답);
    예약이_확정된_상태이다(응답);
    예약_확인코드가_발급되었다(응답);
  }

  @DisplayName("최대 예약 가능 기간(30박)을 꽉 채워 예약 요청한다")
  @Test
  void 최대_예약_가능기간을_꽉_채워_예약_요청한다() {
    // given
    // when
    var 응답 = 예약을_요청한다(aReservationRequest()
            .withStartDate(0)
            .withEndDate(30)
            .build());

    // then
    예약이_성공적으로_생성되었다(응답);
  }

  @DisplayName("예약 제한 기간(30박)을 초과하여 요청하면 예약이 거부된다")
  @Test
  void 예약_제한_기간을_초과하여_요청하면_예약이_거부된다() {
    // given
    // when
    var 응답 = 예약을_요청한다(aReservationRequest()
            .withStartDate(0)
            .withEndDate(31)
            .build());

    // then
    예약이_거부되었다(응답);
    메시지가_확인된다(응답, MAX_PERIOD_EXCEEDED_MESSAGE);
  }

  @DisplayName("유효하지 않은 고객 이름으로 예약할 경우 예약 거부된다")
  @Test
  void 유효하지_않은_고객_이름으로_예약할_경우_예약_거부된다() {
    // given
    // when
    var 응답 = 예약을_요청한다(aReservationRequest()
            .withCustomerName(INVALID_CUSTOMER_NAME)
            .build());

    // then
    예약이_거부되었다(응답);
    메시지가_확인된다(응답, INVALID_CUSTOMER_NAME_MESSAGE);
  }

  @DisplayName("유효하지 않은 전화번호로 예약 요청하면 예약 거부된다")
  @Test
  void 유효하지_않은_전화번호로_예약_요청하면_예약_거부된다() {
    // given
    // when
    var 응답 = 예약을_요청한다(aReservationRequest()
            .withPhoneNumber(INVALID_PHONE_NUMBER)
            .build());

    // then
    예약이_거부되었다(응답);
    메시지가_확인된다(응답, INVALID_PHONE_NUMBER_MESSAGE);
  }

  @DisplayName("성수기 주말 할증 요금이 자동 계산된다")
  @Test
  void 성수기_주말_할증_요금이_자동_계산된다() {
    // given
    var 토요일 = 성수기_첫_토요일_계산(PEAK_SEASON_START_MONTH);
    var 일요일 = 토요일.plusDays(1);

    // when
    var 응답 = 예약을_요청한다(aReservationRequest()
            .withStartDate(토요일)
            .withEndDate(일요일)
            .build());

    // then
    예약_비용을_확인한다(응답, 272000);
  }

  private static LocalDate 성수기_첫_토요일_계산(int peakMonth) {
    var today = LocalDate.now();
    var targetDate = LocalDate.of(today.getYear(), peakMonth, 1);

    if (today.isAfter(targetDate.with(lastDayOfMonth()))) {
      targetDate = targetDate.plusYears(1);
    }

    return targetDate.with(nextOrSame(DayOfWeek.SATURDAY));
  }

  @DisplayName("다양한 시작/종료일 오프셋(경계값)에 따른 예약 결과 테스트")
  @ParameterizedTest(name = "시작일 {0}일 후, 종료일 {1}일 후 예약 시 {2} 응답")
  @CsvSource({
      "0, 0, CREATED",  // 0박 1일 (당일 퇴실) - 성공 (정책 확인 필요)
      "0, 1, CREATED",   // 1박 2일 (최소 기간) - 성공
      "-1, 1, CONFLICT"  // 과거 날짜 포함 - 거부
  })
  void 다양한_날짜_경계값_예약_테스트(int startDayOffset, int endDayOffset, HttpStatus expectedStatus) {
    // given
    // when
    var 응답 = 예약을_요청한다(aReservationRequest()
            .withStartDate(startDayOffset)
            .withEndDate(endDayOffset).build());

    // then
    예약_결과를_확인한다(응답, expectedStatus);
  }

  @DisplayName("예외 상황: 존재하지 않는 사이트 ID로 예약하면 거부된다")
  @Test
  void 존재하지_않는_사이트_예약은_거부된다() {
    // given
    // when
    var 응답 = 예약을_요청한다(aReservationRequest()
            .withSiteNumber("없는-사이트-99")
            .build());

    // then
    예약이_거부되었다(응답);
  }

  @DisplayName("예외 상황: 필수 필드(예약자 이름)가 누락된 경우 예약이 거부된다")
  @Test
  void 필수_필드_누락시_예약이_거부된다() {
    // given
    // when
    var 응답 = 예약을_요청한다(aReservationRequest()
            .withCustomerName(null)
            .build());

    // then
    예약이_거부되었다(응답);
  }

  @DisplayName("엣지 케이스: 기존 예약과 정확히 일치하는 기간에 신규 예약을 시도하면 거부된다")
  @Test
  void 기존_예약과_기간이_일치하면_거부된다() {
    // given
    예약을_완료한다(aReservationRequest()
            .withStartDate(5)
            .withEndDate(7));

    // when
    var 응답 = 예약을_요청한다(aReservationRequest()
            .withStartDate(5)
            .withEndDate(7)
            .build());

    // then
    예약이_거부되었다(응답);
  }
}
