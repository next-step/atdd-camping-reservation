package com.camping.legacy.builder;

import com.camping.legacy.dto.ReservationRequest;

import java.time.LocalDate;

public class ReservationRequestBuilder {

    private String customerName = "홍길동";
    private LocalDate startDate = LocalDate.now().plusDays(7);
    private LocalDate endDate = LocalDate.now().plusDays(9);
    private String siteNumber = "A-1";
    private String phoneNumber = "01012345678";
    private Integer numberOfPeople = 2;
    private String carNumber = "12가3456";
    private String requests = null;

    public static ReservationRequestBuilder aReservation() {
        return new ReservationRequestBuilder();
    }

    public ReservationRequestBuilder customerName(String customerName) {
        this.customerName = customerName;
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

    public ReservationRequestBuilder period(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
        return this;
    }

    public ReservationRequestBuilder siteNumber(String siteNumber) {
        this.siteNumber = siteNumber;
        return this;
    }

    public ReservationRequestBuilder phoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        return this;
    }

    public ReservationRequestBuilder numberOfPeople(Integer numberOfPeople) {
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

    public ReservationRequestBuilder today() {
        LocalDate now = LocalDate.now();
        this.startDate = now;
        this.endDate = now;
        return this;
    }

    public ReservationRequestBuilder pastDate(int daysBefore) {
        LocalDate pastDate = LocalDate.now().minusDays(daysBefore);
        this.startDate = pastDate;
        this.endDate = pastDate.plusDays(2);
        return this;
    }

    public ReservationRequestBuilder daysAfter(int daysAfter, int duration) {
        this.startDate = LocalDate.now().plusDays(daysAfter);
        this.endDate = this.startDate.plusDays(duration);
        return this;
    }

    public ReservationRequestBuilder forConcurrencyTest(int index) {
        this.customerName = "고객" + index;
        this.phoneNumber = "0101234567" + index;
        this.carNumber = null;
        return this;
    }

    public ReservationRequest build() {
        return new ReservationRequest(
                customerName,
                startDate,
                endDate,
                siteNumber,
                phoneNumber,
                numberOfPeople,
                carNumber,
                requests
        );
    }
}
