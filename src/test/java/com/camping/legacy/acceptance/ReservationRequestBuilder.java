package com.camping.legacy.acceptance;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static com.camping.legacy.acceptance.TestFixture.*;

public class ReservationRequestBuilder {

    public static ReservationRequestBuilder aReservation() {
        return new ReservationRequestBuilder();
    }

    private String customerName = DEFAULT_CUSTOMER;
    private String phoneNumber = DEFAULT_PHONE;
    private String siteNumber = SITE_A1;
    private LocalDate startDate = LocalDate.now().plusDays(1);
    private LocalDate endDate = LocalDate.now().plusDays(3);

    public ReservationRequestBuilder withName(String name) {
        this.customerName = name;
        return this;
    }

    public ReservationRequestBuilder withPhone(String phone) {
        this.phoneNumber = phone;
        return this;
    }

    public ReservationRequestBuilder withSite(String site) {
        this.siteNumber = site;
        return this;
    }

    public ReservationRequestBuilder withPeriod(int startDaysFromNow, int endDaysFromNow) {
        this.startDate = LocalDate.now().plusDays(startDaysFromNow);
        this.endDate = LocalDate.now().plusDays(endDaysFromNow);
        return this;
    }

    public ReservationRequestBuilder withStartDate(LocalDate startDate) {
        this.startDate = startDate;
        return this;
    }

    public ReservationRequestBuilder withEndDate(LocalDate endDate) {
        this.endDate = endDate;
        return this;
    }

    public Map<String, Object> build() {
        Map<String, Object> request = new HashMap<>();
        request.put("customerName", customerName);
        request.put("phoneNumber", phoneNumber);
        request.put("siteNumber", siteNumber);
        request.put("startDate", startDate.toString());
        request.put("endDate", endDate.toString());
        return request;
    }
}
