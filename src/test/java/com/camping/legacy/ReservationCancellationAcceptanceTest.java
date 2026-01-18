package com.camping.legacy;

import static com.camping.legacy.fixture.ReservationFixture.*;
import static com.camping.legacy.step.ReservationStep.*;
import static com.camping.legacy.step.SiteStep.*;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.domain.Reservation;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReservationCancellationAcceptanceTest extends AcceptanceTest {

  @DisplayName("올바른 예약 확인 코드로 취소 시 재고가 즉시 복구된다")
  @Test
  void 올바른_예약_확인코드로_취소시_재고가_즉시_복구된다() {
    // given: 예약 생성
    신규_사이트를_등록한다(SITE_A2, LARGE_SITE_TYPE, 5);

    var 사이트 = campsiteRepository.findBySiteNumber(SITE_A2).get();
    var 시작일 = ONE_MONTH_LATER;
    var 확인코드 = CONFIRMATION_CODE;

    var 저장된_예약 = reservationRepository.save(예약_데이터_준비(사이트, 시작일, 확인코드));
    long reservationId = 저장된_예약.getId();
    String confirmationCode = 저장된_예약.getConfirmationCode();

    // when
    var 취소_응답 = 예약을_취소한다(reservationId, confirmationCode);
    예약_취소가_성공했다(취소_응답);

    var 조회_응답 = 예약을_조회한다(reservationId);
    예약_상태_확인(조회_응답, CANCELLED_STATUS);

    // then
    var 검색_결과 = 사이트를_검색한다(저장된_예약.getStartDate(), 저장된_예약.getEndDate(), LARGE_SITE_TYPE);
    검색_결과에_해당_사이트가_포함된다(검색_결과, SITE_A2);
  }

  @DisplayName("예약 확인 코드가 일치하지 않으면 취소할 수 없다")
  @Test
  void 예약_확인_코드가_일치하지_않으면_취소할_수_없다() {
    // given
    var 예약_응답 = 예약을_요청한다(0, 2, CUSTOMER_NAME, PHONE_NUMBER);

    long reservationId = 예약_응답.jsonPath().getLong("id");
    String confirmationCode = WRONG_CONFIRMATION_CODE;

    // when
    var 취소_응답 = 예약을_취소한다(reservationId, confirmationCode);

    // then
    예약_취소가_거부되었다(취소_응답);

    var 조회_응답 = 예약을_조회한다(reservationId);
    예약_상태_확인(조회_응답, CONFIRMED_STATUS);
  }

  @DisplayName("당일 예약 취소 시 별도의 상태 코드로 관리된다")
  @Test
  void 당일_예약_취소_시_별도의_상태_코드로_관리된다() {
    // given
    var 예약_응답 = 예약을_요청한다(0, 2, CUSTOMER_NAME, PHONE_NUMBER);

    long reservationId = 예약_응답.jsonPath().getLong("id");
    String confirmationCode = 예약_응답.jsonPath().get("confirmationCode");

    // when
    var 취소_응답 = 예약을_취소한다(reservationId, confirmationCode);
    예약_취소가_성공했다(취소_응답);

    // then
    var 조회_응답 = 예약을_조회한다(reservationId);
    예약_상태_확인(조회_응답, CANCELLED_SAME_DAY_STATUS);
  }

  private static Reservation 예약_데이터_준비(Campsite site, LocalDate start, String code) {
    Reservation reservation = new Reservation();
    reservation.setCampsite(site);
    reservation.setStartDate(start);
    reservation.setEndDate(start.plusDays(2));
    reservation.setConfirmationCode(code);
    reservation.setStatus(CONFIRMED_STATUS);
    reservation.setCustomerName("익명의 고객");
    return reservation;
  }
}
