package com.camping.legacy.reservation;

import com.camping.legacy.utils.AcceptanceTest;

public class ConcurrentAcceptanceTest extends AcceptanceTest {

    // todo: 동시성 문제 해결 후 테스트 케이스 활성화 필요
//    @DisplayName("동시 예약 요청을 처리한다.")
//    @Test
//    void 동시_예약_요청_처리() {
//        // given
//        예약_가능한_캠핑_사이트_A001이_존재한다();
//        오늘_날짜가_설정된다("2024-01-01");
//
//        // when
//        var responses = 동시에_예약을_요청한다(
//                "김철수", "010-1111-1111",
//                "이영희", "010-2222-2222",
//                "2024-01-15", "2024-01-16", "A-1");
//
//        // then
//        하나의_예약만_성공한다(responses);
//        나머지_예약은_실패한다(responses);
//        실패한_예약에_오류_메시지가_반환된다(responses, "해당 기간에 이미 예약이 존재합니다.");
//    }

    // todo: 취소된 예약이 중복 체크에서 제외되는지 검증하는 로직 추가 필요
//    @DisplayName("취소된 예약 사이트를 재예약한다.")
//    @Test
//    void 취소된_예약_사이트_재예약() {
//        // given
//        Long cancelledReservationId = 사이트에_취소된_예약이_존재한다("A-1", "2024-01-15", "2024-01-16");
//
//        // when
//        var response = 고객이_예약을_요청한다(
//                "박민수", "010-3333-3333", "2024-01-15", "2024-01-16", "A-1");
//
//        // then
//        예약이_성공적으로_생성된다(response);
//        취소된_예약은_중복_체크에서_제외된다(cancelledReservationId);
//    }
}
