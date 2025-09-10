package com.camping.legacy.acceptance.reservation.support.fixture;

import com.camping.legacy.dto.ReservationRequest;

import java.time.LocalDate;

public final class ReservationRequestFixture {
    private String customerName = "TEST";
    private LocalDate startDate = LocalDate.now().plusDays(1);
    private LocalDate endDate = LocalDate.now().plusDays(2);
    private String siteNumber = "A-1";
    private String phoneNumber = "010-0000-0000";
    private Integer numberOfPeople = 2;
    private String carNumber = "00가0000";
    private String requests = "동시성테스트";

    public static ReservationRequestFixture builder() {
        return new ReservationRequestFixture();
    }

    public ReservationRequestFixture customerName(String val) {
        this.customerName = val;
        return this;
    }

    public ReservationRequestFixture startDate(LocalDate val) {
        this.startDate = val;
        return this;
    }

    public ReservationRequestFixture endDate(LocalDate val) {
        this.endDate = val;
        return this;
    }

    public ReservationRequestFixture siteNumber(String val) {
        this.siteNumber = val;
        return this;
    }

    public ReservationRequestFixture phoneNumber(String val) {
        this.phoneNumber = val;
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
