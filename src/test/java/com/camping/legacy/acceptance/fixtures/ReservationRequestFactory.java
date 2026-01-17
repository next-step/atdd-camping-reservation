package com.camping.legacy.acceptance.fixtures;

import java.time.LocalDate;

/**
 * 테스트에서 예약 요청(ReservationRequest)을 '의도 중심'으로 만들기 위한 팩토리.
 *
 * - builder 사용은 유지하되, 테스트 코드에서 불필요한 파싱/중복을 줄입니다.
 * - "기본값 + 필요한 것만 변경" 패턴을 한 줄로 표현하는 용도입니다.
 */
@SuppressWarnings("NonAsciiCharacters")
public final class ReservationRequestFactory {

    private ReservationRequestFactory() {
    }

    public static ReservationRequest 같은_기간_다른_고객(ReservationRequest base, String customerName) {
        return ReservationRequest.builder()
                .customerName(customerName)
                .startDate(parse(base.getStartDate()))
                .endDate(parse(base.getEndDate()))
                .siteNumber(base.getSiteNumber())
                .build();
    }

    public static ReservationRequest 예약_정보_변경(String customerName) {
        return ReservationRequest.builder()
                .customerName(customerName)
                .siteNumber("A-1")
                .build();
    }

    public static ReservationRequest 예약_기간_변경(LocalDate startDate, LocalDate endDate) {
        return ReservationRequest.builder()
                .customerName("수정한 이름")
                .startDate(startDate)
                .endDate(endDate)
                .siteNumber("A-1")
                .build();
    }

    public static ReservationRequest 예약_생성_요청(String customerName, LocalDate startDate, LocalDate endDate, String siteNumber) {
        return ReservationRequest.builder()
                .customerName(customerName)
                .startDate(startDate)
                .endDate(endDate)
                .siteNumber(siteNumber)
                .build();
    }

    private static LocalDate parse(String date) {
        return date == null ? null : LocalDate.parse(date);
    }
}
