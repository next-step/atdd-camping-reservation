package com.camping.legacy.domain.dto;

import java.time.LocalDate;

public record ReservationParams(String customerName, String phoneNumber, String siteNumber, String startDate,
                         String endDate) {
    public static ReservationParams of(LocalDate startDate, LocalDate endDate) {
        return new ReservationParams(
                "홍길동",
                "010-1234-5678",
                "A-3",
                startDate.toString(),
                endDate.toString()
        );
    }

    public static ReservationParams ofWithSiteNumber(String siteNumber, LocalDate startDate, LocalDate endDate) {
        return new ReservationParams(
                "홍길동",
                "010-1234-5678",
                siteNumber,
                startDate.toString(),
                endDate.toString()
        );
    }
}