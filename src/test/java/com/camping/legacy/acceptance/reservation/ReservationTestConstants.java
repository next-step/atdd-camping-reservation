package com.camping.legacy.acceptance.reservation;

public class ReservationTestConstants {

    // ====== 사이트 정보 ======
    public static final String 사이트번호_A_1 = "A-1";
    public static final String 사이트_크기_대형 = "대형";

    // ====== 예약자 정보 ======
    public static final String 홍길동 = "홍길동";
    public static final String 김철수 = "김철수";
    public static final String 연락처 = "010-1234-1234";
    public static final String 차량번호 = "가1234";
    public static final String 요청사항 = "1시간 일찍 입실 예정";

    // ====== 인원수 ======
    public static final int 인원수_0명 = 0;
    public static final int 인원수_5명 = 5;
    public static final int 인원수_6명 = 6;
    public static final int 인원수_7명 = 7;

    // ====== 예약 날짜 ======
    public static final String 기존예약_시작일 = "2026-02-01";
    public static final String 기존예약_종료일 = "2026-02-03";
    public static final String 중복예약_시작일 = "2026-02-02";
    public static final String 중복예약_종료일 = "2026-02-04";
    public static final String 변경예약_시작일 = "2026-02-05";
    public static final String 변경예약_종료일 = "2026-02-07";
    public static final String 장기예약_시작일 = "2026-02-01";
    public static final String 장기예약_종료일_30일 = "2026-03-02";
    public static final String 장기예약_종료일_32일 = "2026-03-05";
    public static final String 역전예약_시작일 = "2026-02-05";
    public static final String 역전예약_종료일 = "2026-02-03";
    public static final String 경계_예약_시작일 = "2026-02-03";
    public static final String 경계_예약_종료일 = "2026-02-05";
    public static final String 연속_예약_시작일 = "2026-02-04";
    public static final String 연속_예약_종료일 = "2026-02-06";
    public static final String 시작일_포함_예약_종료일 = "2026-02-02";

    // ====== 예약 상태 ======
    public static final String 예약상태_예약완료 = "CONFIRMED";
    public static final String 예약상태_사전취소 = "CANCELLED";
    public static final String 예약상태_당일취소 = "CANCELLED_SAME_DAY";

    // ====== 확인 코드 ======
    public static final int 확인코드_길이_6자리 = 6;
    public static final String 잘못된_확인코드 = "WRONG1";

    // ====== 비수기 날짜 ======
    public static final String 비수기_평일_시작일 = "2026-02-02";
    public static final String 비수기_평일_종료일 = "2026-02-03";
    public static final String 비수기_수요일_시작일 = "2026-02-04";
    public static final String 비수기_목요일_종료일 = "2026-02-05";
    public static final String 비수기_월요일_시작일 = "2026-02-02";
    public static final String 비수기_2박_수요일_종료일 = "2026-02-04";
    public static final String 비수기_토요일_시작일 = "2026-02-07";
    public static final String 비수기_토요일_종료일 = "2026-02-08";
    public static final String 비수기_금요일_시작일 = "2026-02-13";
    public static final String 비수기_일요일_종료일 = "2026-02-15";

    // ====== 성수기 날짜 ======
    public static final String 성수기_7월_평일_시작일 = "2026-07-06";
    public static final String 성수기_7월_평일_종료일 = "2026-07-07";
    public static final String 성수기_8월_토요일_시작일 = "2026-08-01";
    public static final String 성수기_8월_토요일_종료일 = "2026-08-02";
    public static final String 비수기_6월30일_시작일 = "2026-06-30";
    public static final String 성수기_7월2일_종료일 = "2026-07-02";

    // ====== 성수기 경계값 날짜 ======
    public static final String 성수기_시작일_7월1일 = "2026-07-01";
    public static final String 성수기_시작일_7월2일 = "2026-07-02";
    public static final String 성수기_종료일_8월31일 = "2026-08-31";
    public static final String 성수기_종료일_9월1일 = "2026-09-01";
    public static final String 성수기_전날_6월30일 = "2026-06-30";
    public static final String 성수기_전날_7월1일 = "2026-07-01";
    public static final String 성수기_다음날_9월1일 = "2026-09-01";
    public static final String 성수기_다음날_9월2일 = "2026-09-02";

    // ====== 요금 ======
    public static final int A_사이트_기본요금 = 80000;
    public static final int 주말_할증_요금 = 104000;
    public static final int 성수기_평일_할증_요금 = 120000;
    public static final int 성수기_주말_할증_요금 = 136000;
    public static final int 평일_2박_요금 = 160000;
    public static final int 평일_주말_혼합_요금 = 184000;
    public static final int 비수기_성수기_혼합_요금 = 200000;

    // ====== 포인트 ======
    public static final int 평일_포인트 = 4000;
    public static final int 주말_포인트 = 10400;
    public static final int 성수기_포인트 = 3600;

    private ReservationTestConstants() {
    }
}
