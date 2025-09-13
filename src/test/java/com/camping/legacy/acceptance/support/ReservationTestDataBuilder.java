package com.camping.legacy.acceptance.support;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class ReservationTestDataBuilder {
    private String name = "기본이름";
    private String phone = "010-0000-0000";
    private String siteNumber = "A-1";
    private LocalDate startDate = LocalDate.now().plusDays(7);
    private LocalDate endDate = LocalDate.now().plusDays(9);
    
    public ReservationTestDataBuilder withName(String name) {
        this.name = name;
        return this;
    }
    
    public ReservationTestDataBuilder withPhone(String phone) {
        this.phone = phone;
        return this;
    }
    
    public ReservationTestDataBuilder withSiteNumber(String siteNumber) {
        this.siteNumber = siteNumber;
        return this;
    }
    
    public ReservationTestDataBuilder withDates(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
        return this;
    }
    
    public ReservationTestDataBuilder withDateRange(int startDaysFromNow, int endDaysFromNow) {
        this.startDate = LocalDate.now().plusDays(startDaysFromNow);
        this.endDate = LocalDate.now().plusDays(endDaysFromNow);
        return this;
    }
    
    public ReservationTestDataBuilder withSameDayReservation(LocalDate date) {
        this.startDate = date;
        this.endDate = date;
        return this;
    }
    
    public ReservationTestDataBuilder withValidFutureReservation() {
        return withDateRange(7, 9);
    }
    
    public ReservationTestDataBuilder withTodayReservation() {
        LocalDate today = LocalDate.now();
        this.startDate = today;
        this.endDate = today.plusDays(1);
        return this;
    }
    
    public ReservationTestDataBuilder withoutDates() {
        this.startDate = null;
        this.endDate = null;
        return this;
    }

    public Map<String, Object> build() {
        Map<String, Object> request = new HashMap<>();
        request.put("siteNumber", siteNumber);
        request.put("startDate", startDate != null ? startDate.toString() : null);
        request.put("endDate", endDate != null ? endDate.toString() : null);
        request.put("customerName", name);
        request.put("phoneNumber", phone);
        return request;
    }
}
