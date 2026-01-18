package com.camping.legacy;

import static com.camping.legacy.fixture.ReservationFixture.*;
import static com.camping.legacy.step.ReservationStep.*;
import static com.camping.legacy.step.SiteStep.*;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReservationCancellationAcceptanceTest extends AcceptanceTest {

  @DisplayName("올바른 예약 확인 코드로 취소 시 재고가 즉시 복구된다")
  @Test
  void 올바른_예약_확인코드로_취소시_재고가_즉시_복구된다() {
    // given: 사전 예약 생성
    var 예약_응답 = 예약을_요청한다(10, 12, CUSTOMER_NAME, SITE_A1, PHONE_NUMBER);
    long 예약_ID = 예약_응답.jsonPath().getLong("id");

    var 예약_상세_정보 = 예약을_조회한다(예약_ID);
    예약_상태_확인(예약_상세_정보, CONFIRMED_STATUS);

    String 확인_코드 = 예약_상세_정보.jsonPath().get("[0].confirmationCode");
    LocalDate 시작일 = LocalDate.parse(예약_상세_정보.jsonPath().get("[0].startDate"));
    LocalDate 종료일 = LocalDate.parse(예약_상세_정보.jsonPath().get("[0].endDate"));

    // when
    var 취소_응답 = 예약을_취소한다(예약_ID, 확인_코드);
    예약_취소가_성공했다(취소_응답);

    var 조회_응답 = 예약을_조회한다(예약_ID);
    예약_상태_확인(조회_응답, CANCELLED_STATUS);

    // then
    var 검색_결과 = 사이트를_검색한다(시작일, 종료일, LARGE_SITE_TYPE);
    검색_결과에_해당_사이트가_포함된다(검색_결과, SITE_A1);
  }

  @DisplayName("예약 확인 코드가 일치하지 않으면 취소할 수 없다")
  @Test
  void 예약_확인_코드가_일치하지_않으면_취소할_수_없다() {
    // given
    var 예약_응답 = 예약을_요청한다(0, 2, CUSTOMER_NAME, PHONE_NUMBER);
    long 예약_ID = 예약_응답.jsonPath().getLong("id");

    var 예약확인_조회_응답 = 예약을_조회한다(예약_ID);
    예약_상태_확인(예약확인_조회_응답, CONFIRMED_STATUS);

    // when
    var 취소_응답 = 예약을_취소한다(예약_ID, WRONG_CONFIRMATION_CODE);

    // then
    예약_취소가_거부되었다(취소_응답);

    var 조회_응답 = 예약을_조회한다(예약_ID);
    예약_상태_확인(조회_응답, CONFIRMED_STATUS);
  }

  @DisplayName("당일 예약 취소 시 별도의 상태 코드로 관리된다")
  @Test
  void 당일_예약_취소_시_별도의_상태_코드로_관리된다() {
    // given
    var 예약_응답 = 예약을_요청한다(0, 2, CUSTOMER_NAME, PHONE_NUMBER);
    long 예약_ID = 예약_응답.jsonPath().getLong("id");

    var 예약확인_조회_응답 = 예약을_조회한다(예약_ID);
    long reservationId = 예약확인_조회_응답.jsonPath().getLong("[0].id");
    String confirmationCode = 예약확인_조회_응답.jsonPath().get("[0].confirmationCode");

    // when
    var 취소_응답 = 예약을_취소한다(reservationId, confirmationCode);
    예약_취소가_성공했다(취소_응답);

    // then
    var 조회_응답 = 예약을_조회한다(reservationId);
    예약_상태_확인(조회_응답, CANCELLED_SAME_DAY_STATUS);
  }
}
