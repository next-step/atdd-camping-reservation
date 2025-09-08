package com.camping.legacy.reservation;

import com.camping.legacy.utils.AcceptanceTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.camping.legacy.reservation.ReservationSteps.고객명_사이트번호_예약기간이_포함된다;
import static com.camping.legacy.reservation.ReservationSteps.고객명으로_예약을_조회한다;
import static com.camping.legacy.reservation.ReservationSteps.고객의_예약이_존재한다;
import static com.camping.legacy.reservation.ReservationSteps.고객이_예약을_요청한다;
import static com.camping.legacy.reservation.ReservationSteps.사이트에_예약이_존재한다;
import static com.camping.legacy.reservation.ReservationSteps.시작일인_예약이_존재한다;
import static com.camping.legacy.reservation.ReservationSteps.예약_ID로_조회한다;
import static com.camping.legacy.reservation.ReservationSteps.예약_가능한_캠핑_사이트_A001이_존재한다;
import static com.camping.legacy.reservation.ReservationSteps.예약_상태가_CANCELLED_SAME_DAY로_설정된다;
import static com.camping.legacy.reservation.ReservationSteps.예약_상태가_CANCELLED로_변경된다;
import static com.camping.legacy.reservation.ReservationSteps.예약_상태가_CONFIRMED로_설정된다;
import static com.camping.legacy.reservation.ReservationSteps.예약_정보가_반환된다;
import static com.camping.legacy.reservation.ReservationSteps.예약을_취소한다;
import static com.camping.legacy.reservation.ReservationSteps.예약이_성공적으로_생성된다;
import static com.camping.legacy.reservation.ReservationSteps.예약이_실패한다;
import static com.camping.legacy.reservation.ReservationSteps.예약이_존재한다;
import static com.camping.legacy.reservation.ReservationSteps.예약이_취소된다;
import static com.camping.legacy.reservation.ReservationSteps.오늘_날짜가_설정된다;
import static com.camping.legacy.reservation.ReservationSteps.오류_메시지가_반환된다;
import static com.camping.legacy.reservation.ReservationSteps.해당_고객의_모든_예약이_반환된다;
import static com.camping.legacy.reservation.ReservationSteps.확인코드_6자리가_생성된다;
import static com.camping.legacy.reservation.ReservationSteps.확인코드로_예약을_취소한다;
import static com.camping.legacy.reservation.ReservationSteps.확인코드인_예약이_존재한다;
import static io.restassured.RestAssured.given;

public class ReservationAcceptanceTest extends AcceptanceTest {

    @DisplayName("정상적인 예약을 생성한다.")
    @Test
    void 정상적인_예약_생성() {
        // given
        예약_가능한_캠핑_사이트_A001이_존재한다();
        오늘_날짜가_설정된다("2024-01-01");

        // when
        var response = 고객이_예약을_요청한다(
                "김철수", "010-1234-5678", "2024-01-15", "2024-01-16", "A-1");

        // then
        예약이_성공적으로_생성된다(response);
        확인코드_6자리가_생성된다(response);
        예약_상태가_CONFIRMED로_설정된다(response);
    }

    // todo: 버그 30일 제한 검증 로직을 추가해야 함
//    @DisplayName("30일 초과 예약 시도")
//    @Test
//    void 예약_30일_초과_시도() {
//        // given
//        오늘_날짜가_설정된다("2024-01-01");
//
//        // when
//        var response = 고객이_예약을_요청한다(
//                "김철수", "010-1234-5678", "2024-02-01", "2024-02-02", "A-1");
//
//        // then
//        예약이_실패한다(response);
//        오류_메시지가_반환된다(response, "30일 이내에만 예약 가능합니다");
//    }

    @DisplayName("중복 예약 시도")
    @Test
    void 중복_예약_시도() {
        // given
        사이트에_예약이_존재한다("A-1", "2024-01-15", "2024-01-16");

        // when
        var response = 고객이_예약을_요청한다(
                "이영희", "010-9876-5432", "2024-01-15", "2024-01-16", "A-1");

        // then
        예약이_실패한다(response);
        오류_메시지가_반환된다(response, "해당 기간에 이미 예약이 존재합니다.");
    }

    @DisplayName("예약 ID로 조회한다.")
    @Test
    void 예약_ID로_조회() {
        // given
        Long reservationId = 예약이_존재한다("1");

        // when
        var response = 예약_ID로_조회한다(reservationId);

        // then
        예약_정보가_반환된다(response);
        고객명_사이트번호_예약기간이_포함된다(response);
    }

    @DisplayName("고객명으로 예약을 조회한다.")
    @Test
    void 고객명으로_예약_조회() {
        // given
        고객의_예약이_존재한다("김철수");

        // when
        var response = 고객명으로_예약을_조회한다("김철수");

        // then
        해당_고객의_모든_예약이_반환된다(response, "김철수");
    }

    @DisplayName("확인코드로 예약을 취소한다.")
    @Test
    void 확인코드로_예약_취소() {
        // given
        Long reservationId = 확인코드인_예약이_존재한다("ABC123");

        // 생성된 예약의 실제 확인코드 조회
        var getResponse = given().log().all()
                .when().get("/api/reservations/" + reservationId)
                .then().log().all().extract();
        String actualConfirmationCode = getResponse.jsonPath().getString("confirmationCode");

        // when
        var response = 확인코드로_예약을_취소한다(reservationId, actualConfirmationCode);

        // then
        예약이_취소된다(response);
        예약_상태가_CANCELLED로_변경된다(reservationId);
    }

    // todo: 시간을 주입받아 테스트할 수 있도록 변경해야 함
//    @DisplayName("당일 예약을 취소한다.")
//    @Test
//    void 당일_예약_취소() {
//        // given
//        오늘_날짜가_설정된다("2024-01-15");
//        Long reservationId = 시작일인_예약이_존재한다("2024-01-15");
//
//        var getResponse = given().log().all()
//                .when().get("/api/reservations/" + reservationId)
//                .then().log().all().extract();
//        String confirmationCode = getResponse.jsonPath().getString("confirmationCode");
//
//        // when
//        var response = 예약을_취소한다(reservationId, confirmationCode);
//
//        // then
//        예약이_취소된다(response);
//        예약_상태가_CANCELLED_SAME_DAY로_설정된다(reservationId);
//    }


}
