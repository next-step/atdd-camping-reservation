package com.camping.legacy.stub;

import com.camping.legacy.dto.ReservationRequest;

import java.time.LocalDate;

public class ReservationRequestStub {
    /**
     * customerName, siteNumber, startDate, endDate를 매개변수로 받아서 ReservationRequest 객체를 생성
     * - 나머지 필드는 기본값으로 설정
     */
    public static ReservationRequest get(
            String customerName,
            String siteNumber,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return get(customerName, siteNumber, startDate, endDate, null, null, null, null);
    }
    public static ReservationRequest get(
            String customerName,
            String siteNumber,
            LocalDate startDate,
            LocalDate endDate,
            String phoneNumber,
            Integer numberOfPeople,
            String carNumber,
            String requests
    ) {

        LocalDate defaultStartDate = LocalDate.now();
        LocalDate defaultEndDate = defaultStartDate.plusDays(1);
        String defaultSiteNumber = "A-1";
        String defaultCustomerName = "홍길동";
        String defaultPhoneNumber = "010-1234-5678";
        String defaultCarNumber = "12가3456";
        String defaultRequests = "없음";
        int defaultNumberOfPeople = 4;

        return new ReservationRequest(
                customerName != null ? customerName : defaultCustomerName,
                startDate != null ? startDate : defaultStartDate,
                endDate != null ? endDate : defaultEndDate,
                siteNumber != null ? siteNumber : defaultSiteNumber,
                phoneNumber != null ? phoneNumber : defaultPhoneNumber,
                numberOfPeople != null ? numberOfPeople : defaultNumberOfPeople,
                carNumber != null ? carNumber : defaultCarNumber,
                requests != null ? requests : defaultRequests
        );
    }
}
