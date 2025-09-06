package com.camping.legacy.acceptance;

import com.camping.legacy.dto.ReservationRequest;

import java.time.LocalDate;

public class ReservationRequestBuilder {
    private static final LocalDate now = LocalDate.now();

    private String name = "김영희";
    private LocalDate startDate = now.plusDays(20);
    private LocalDate endDate = now.plusDays(20);
    private String siteName = "A-1";
    private String phoneNumber = "010-1234-5678";
    private int numberOfPeople = 2;
    private String carNumber = null;
    private String requests = null;

    public static ReservationRequestBuilder builder() {
        return new ReservationRequestBuilder();
    }

    public ReservationRequestBuilder name(String name) {
        this.name = name;
        return this;
    }

    public ReservationRequestBuilder startDate(LocalDate startDate) {
        this.startDate = startDate;
        return this;
    }

    public ReservationRequestBuilder endDate(LocalDate endDate) {
        this.endDate = endDate;
        return this;
    }

    public ReservationRequestBuilder siteName(String siteName) {
        this.siteName = siteName;
        return this;
    }

    public ReservationRequestBuilder phoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        return this;
    }

    public ReservationRequestBuilder numberOfPeople(int numberOfPeople) {
        this.numberOfPeople = numberOfPeople;
        return this;
    }

    public ReservationRequestBuilder carNumber(String carNumber) {
        this.carNumber = carNumber;
        return this;
    }

    public ReservationRequestBuilder requests(String requests) {
        this.requests = requests;
        return this;
    }

    public ReservationRequest build() {
        return new ReservationRequest(
                name,
                startDate,
                endDate,
                siteName,
                phoneNumber,
                numberOfPeople,
                carNumber,
                requests
        );
    }
}
