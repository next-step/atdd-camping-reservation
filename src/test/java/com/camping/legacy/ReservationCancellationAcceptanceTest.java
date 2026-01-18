package com.camping.legacy;

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
    신규_사이트를_등록한다("A-2", "대형", 5);

    var 사이트 = campsiteRepository.findBySiteNumber("A-2").get();
    var 시작일 = LocalDate.now().plusMonths(1);
    var 확인코드 = "SAFE12";

    var 저장된_예약 = reservationRepository.save(예약_데이터_준비(사이트, 시작일, 확인코드));
    long reservationId = 저장된_예약.getId();
    String confirmationCode = 저장된_예약.getConfirmationCode();

    // when
    var 취소_응답 = 예약을_취소한다(reservationId, confirmationCode);
    예약_취소가_성공했다(취소_응답);

    var 조회_응답 = 예약을_조회한다(reservationId);
    예약_상태_확인(조회_응답, "CANCELLED");

    // then
    var 검색_결과 = 사이트를_검색한다(저장된_예약.getStartDate(), 저장된_예약.getEndDate(), "대형");
    검색_결과에_해당_사이트가_포함된다(검색_결과, "A-2");
  }

  @DisplayName("예약 확인 코드가 일치하지 않으면 취소할 수 없다")
  @Test
  void 예약_확인_코드가_일치하지_않으면_취소할_수_없다() {
    // given
    var 시작일 = LocalDate.now();
    var 종료일 = 시작일.plusDays(2);
    var 예약요청 = 예약요청_생성(시작일, 종료일, "홍길동", "01012345678");

    var 예약_응답 = 예약을_요청한다(예약요청);

    long reservationId = 예약_응답.jsonPath().getLong("id");
    String confirmationCode = "WRONG_CODE";

    // when
    var 취소_응답 = 예약을_취소한다(reservationId, confirmationCode);

    // then
    예약_취소가_거부되었다(취소_응답);

    var 조회_응답 = 예약을_조회한다(reservationId);
    예약_상태_확인(조회_응답, "CONFIRMED");
  }

  @DisplayName("당일 예약 취소 시 별도의 상태 코드로 관리된다")
  @Test
  void 당일_예약_취소_시_별도의_상태_코드로_관리된다() {
    // given
    var 시작일 = LocalDate.now();
    var 종료일 = 시작일.plusDays(2);
    var 예약요청 = 예약요청_생성(시작일, 종료일, "홍길동", "01012345678");

    var 예약_응답 = 예약을_요청한다(예약요청);

    long reservationId = 예약_응답.jsonPath().getLong("id");
    String confirmationCode = 예약_응답.jsonPath().get("confirmationCode");

    // when
    var 취소_응답 = 예약을_취소한다(reservationId, confirmationCode);
    예약_취소가_성공했다(취소_응답);

    // then
    var 조회_응답 = 예약을_조회한다(reservationId);
    예약_상태_확인(조회_응답, "CANCELLED_SAME_DAY");
  }

  private static Reservation 예약_데이터_준비(Campsite site, LocalDate start, String code) {
    Reservation reservation = new Reservation();
    reservation.setCampsite(site);
    reservation.setStartDate(start);
    reservation.setEndDate(start.plusDays(2));
    reservation.setConfirmationCode(code);
    reservation.setStatus("CONFIRMED");
    reservation.setCustomerName("익명의 고객");
    return reservation;
  }
}
