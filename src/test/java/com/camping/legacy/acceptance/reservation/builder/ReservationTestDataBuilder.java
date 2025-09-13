package com.camping.legacy.acceptance.reservation.builder;

import java.time.LocalDate;
import java.util.Map;

public class ReservationTestDataBuilder {
    private String customerName = "홍길동";
    private String phoneNumber = "010-1234-5678";
    private String siteNumber = "A-1";
    private LocalDate startDate = LocalDate.now().plusDays(7);
    private LocalDate endDate = LocalDate.now().plusDays(9);

    public ReservationTestDataBuilder withCustomerName(String name) {
        this.customerName = name;
        return this;
    }

    public ReservationTestDataBuilder withDatesInFuture(int startDays, int endDays) {
        this.startDate = LocalDate.now().plusDays(startDays);
        this.endDate = LocalDate.now().plusDays(endDays);
        return this;
    }

    public ReservationTestDataBuilder withSiteNumber(String siteNumber) {
        this.siteNumber = siteNumber;
        return this;
    }

    public Map<String, String> buildRequestMap() {
        return Map.of(
                "customerName", customerName,
                "phoneNumber", phoneNumber,
                "siteNumber", siteNumber,
                "startDate", startDate.toString(),
                "endDate", endDate.toString()
        );
    }
}
