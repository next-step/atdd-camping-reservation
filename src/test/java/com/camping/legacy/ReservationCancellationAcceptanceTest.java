package com.camping.legacy;

import static com.camping.legacy.client.ReservationClient.*;
import static com.camping.legacy.client.SiteClient.*;
import static com.camping.legacy.fixture.ReservationFixture.*;
import static com.camping.legacy.fixture.ReservationRequestBuilder.*;
import static com.camping.legacy.step.ReservationStep.*;
import static com.camping.legacy.step.SiteStep.*;
import static org.springframework.http.HttpStatus.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ReservationCancellationAcceptanceTest extends AcceptanceTest {

  @DisplayName("올바른 예약 확인 코드로 취소 시 재고가 즉시 복구된다")
  @Test
  void 올바른_예약_확인코드로_취소시_재고가_즉시_복구된다() {
    // given: 사전 예약 생성
    var 시작일_offset = 10;
    var 종료일_offset = 12;

    var 예약_요청 = aReservationRequest().withStartDate(시작일_offset).withEndDate(종료일_offset);
    var 예약_응답 = 예약을_요청한다(예약_요청.build());
    var 예약_ID = 예약_응답.jsonPath().getLong("id");

    var 예약_상세_정보 = 예약을_조회한다(예약_ID);
    String 확인_코드 = 예약_상세_정보.jsonPath().get("[0].confirmationCode");

    // when
    var 취소_응답 = 예약을_취소한다(예약_ID, 확인_코드);
    예약_취소가_성공했다(취소_응답);

    var 조회_응답 = 예약을_조회한다(예약_ID);
    예약_상태_확인(조회_응답, CANCELLED_STATUS);

    // then
    var 검색_결과 = 사이트를_검색한다(시작일_offset, 종료일_offset, LARGE_SITE_TYPE);
    검색_결과에_해당_사이트가_포함된다(검색_결과, SITE_A1);
  }

  @DisplayName("예약 확인 코드가 일치하지 않으면 취소할 수 없다")
  @Test
  void 예약_확인_코드가_일치하지_않으면_취소할_수_없다() {
    // given
    var 예약_요청 = aReservationRequest().withEndDate(2);
    var 예약_응답 = 예약을_요청한다(예약_요청.build());
    var 예약_ID = 예약_응답.jsonPath().getLong("id");

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
    var 예약_요청 = aReservationRequest().withEndDate(2);
    var 예약_응답 = 예약을_요청한다(예약_요청.build());
    var 예약_ID = 예약_응답.jsonPath().getLong("id");

    var 예약확인_조회_응답 = 예약을_조회한다(예약_ID);
    String 확인_코드 = 예약확인_조회_응답.jsonPath().get("[0].confirmationCode");

    // when
    var 취소_응답 = 예약을_취소한다(예약_ID, 확인_코드);
    예약_취소가_성공했다(취소_응답);

    // then
    var 조회_응답 = 예약을_조회한다(예약_ID);
    예약_상태_확인(조회_응답, CANCELLED_SAME_DAY_STATUS);
  }

  @DisplayName("예외 상황 테스트: 존재하지 않는 예약 ID로 취소를 시도하면 실패한다")
  @Test
  void 존재하지_않는_예약_ID로_취소하면_실패한다() {
    // when
    var 응답 = 예약을_취소한다(99999L, "아무코드");

    // then
    예약_취소가_거부되었다(응답);
  }

  @DisplayName("예외 상황 테스트: 이미 취소된 예약을 다시 취소하려고 하면 실패한다")
  @Test
  void 이미_취소된_예약을_다시_취소하면_실패한다() {
    // given
    var 예약_응답 = 예약을_요청한다(aReservationRequest().withEndDate(2).build());
    var 예약_ID = 예약_응답.jsonPath().getLong("id");
    var 예약_상세_정보 = 예약을_조회한다(예약_ID);
    String 확인_코드 = 예약_상세_정보.jsonPath().get("[0].confirmationCode");

    예약을_취소한다(예약_ID, 확인_코드);

    // when
    var 두번째_취소_응답 = 예약을_취소한다(예약_ID, 확인_코드);

    // then
    예약_취소가_거부되었다(두번째_취소_응답);
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
    var 예약_응답 = 예약을_요청한다(
        aReservationRequest()
            .withStartDate(startDayOffset)
            .withEndDate(startDayOffset + 2)
            .build()
    );
    var 예약_ID = 예약_응답.jsonPath().getLong("id");
    var 예약_상세_정보 = 예약을_조회한다(예약_ID);
    String 확인_코드 = 예약_상세_정보.jsonPath().get("[0].confirmationCode");

    // when
    예약을_취소한다(예약_ID, 확인_코드);

    // then
    var 조회_응답 = 예약을_조회한다(예약_ID);
    예약_상태_확인(조회_응답, expectedStatus);
  }
}
