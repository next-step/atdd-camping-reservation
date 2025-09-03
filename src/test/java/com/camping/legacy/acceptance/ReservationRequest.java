package com.camping.legacy.acceptance;

import java.time.LocalDate;

/**
 * 예약 요청 객체의 빌더 패턴 구현
 */
public class ReservationRequest {
    private String customerName;
    private String phoneNumber; 
    private String siteNumber;
    private LocalDate startDate;
    private LocalDate endDate;

    private ReservationRequest() {}

    public static ReservationRequest builder() {
        return new ReservationRequest();
    }

    public static ReservationRequest name(String customerName) {
        ReservationRequest request = new ReservationRequest();
        request.customerName = customerName;
        return request;
    }

    public ReservationRequest phoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        return this;
    }

    public ReservationRequest siteNumber(String siteNumber) {
        this.siteNumber = siteNumber;
        return this;
    }

    public ReservationRequest startDate(LocalDate startDate) {
        this.startDate = startDate;
        return this;
    }

    public ReservationRequest endDate(LocalDate endDate) {
        this.endDate = endDate;
        return this;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getSiteNumber() {
        return siteNumber;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }
}
