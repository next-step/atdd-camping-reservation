package com.camping.legacy.fixture;


public class ReservationFixture {

    // 예약 정보 관련 상수
    public static final String CUSTOMER_NAME = "홍길동";
    public static final String PHONE_NUMBER = "01012345678";
    public static final String INVALID_CUSTOMER_NAME = "김";
    public static final String INVALID_PHONE_NUMBER = "010-123-456";
    public static final String WRONG_CONFIRMATION_CODE = "WRONG_CODE";

    // 메시지 관련 상수
    public static final String MAX_PERIOD_EXCEEDED_MESSAGE = "예약 기간은 최대 30일입니다.";
    public static final String INVALID_CUSTOMER_NAME_MESSAGE = "예약자 이름은 최소 2자 이상이어야 합니다.";
    public static final String INVALID_PHONE_NUMBER_MESSAGE = "전화번호 형식이 올바르지 않습니다.";

    // 상태 코드 관련 상수
    public static final String CONFIRMED_STATUS = "CONFIRMED";
    public static final String CANCELLED_STATUS = "CANCELLED";
    public static final String CANCELLED_SAME_DAY_STATUS = "CANCELLED_SAME_DAY";

    // 사이트 관련 상수
    public static final String SITE_A1 = "A-1";
    public static final String SITE_A2 = "A-2";
    public static final String SITE_B1 = "B-1";
    public static final String LARGE_SITE_TYPE = "대형";

    // 성수기 (7 ~ 8월)
    public static final int PEAK_SEASON_START_MONTH = 7;
    public static final int PEAK_SEASON_END_MONTH = 8;
}
