package com.camping.legacy;

import static com.camping.legacy.fixture.ReservationFixture.*;
import static com.camping.legacy.step.ReservationStep.예약을_요청한다;
import static org.assertj.core.api.Assertions.assertThat;

import com.camping.legacy.utils.ConcurrencyTestHelper;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

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

  @Disabled
  @DisplayName("동일한 사이트와 기간에 대해 중복 예약 시도를 하면 한명만 예약된다 (동시성 제어)")
  @Test
  void 동일한_사이트와_기간에_대해_중복_예약_시도를_하면_한명만_예약된다() throws InterruptedException {
    // given
    AtomicInteger 예약_성공_카운트 = new AtomicInteger(0);

    // when
    ConcurrencyTestHelper.execute(
        () -> recordResult(예약_성공_카운트, 예약을_요청한다(10, 11, "홍길동", SITE_A1, PHONE_NUMBER)),
        () -> recordResult(예약_성공_카운트, 예약을_요청한다(10, 11, "이순신", SITE_A1,"01056781234")));

    // then
    assertThat(예약_성공_카운트.get()).isEqualTo(1);
  }

  private void recordResult(AtomicInteger count, ExtractableResponse<Response> response) {
    if (response.statusCode() == HttpStatus.CREATED.value()) {
      count.incrementAndGet();
    }
  }

  @DisplayName("성수기 주말 할증 요금이 자동 계산된다")
  @Test
  void 성수기_주말_할증_요금이_자동_계산된다() {
    // given
    LocalDate 성수기_시작 = LocalDate.of(TODAY.getYear(), 7, 1);
    if (TODAY.isAfter(성수기_시작)) {
      성수기_시작 = 성수기_시작.plusYears(1); // 올해 성수기가 지났으면 내년으로
    }

    LocalDate 성수기_토요일 = 성수기_시작.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
    LocalDate 성수기_일요일 = 성수기_토요일.plusDays(1);

    // when
    var 예약_응답 = 예약을_요청한다(
            (int) TODAY.until(성수기_토요일, java.time.temporal.ChronoUnit.DAYS),
            (int) TODAY.until(성수기_일요일, java.time.temporal.ChronoUnit.DAYS),
            "성수기 주말 고객",
            SITE_A1,
            PHONE_NUMBER
    );

    // then
    예약_성공_확인(예약_응답);
  }
}
