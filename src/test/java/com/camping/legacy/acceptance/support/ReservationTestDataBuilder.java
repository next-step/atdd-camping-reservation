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
    
    public Map<String, Object> build() {
        Map<String, Object> request = new HashMap<>();
        request.put("siteNumber", siteNumber);
        request.put("startDate", startDate.toString());
        request.put("endDate", endDate.toString());
        request.put("customerName", name);
        request.put("phoneNumber", phone);
        return request;
    }
}
