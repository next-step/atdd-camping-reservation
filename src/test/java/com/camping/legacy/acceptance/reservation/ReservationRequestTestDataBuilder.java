package com.camping.legacy.acceptance.reservation;

import com.camping.legacy.dto.ReservationRequest;

import java.time.LocalDate;

public class ReservationRequestTestDataBuilder {
    private String customerName = "홍길동";
    private LocalDate startDate = LocalDate.now().plusDays(1);
    private LocalDate endDate = LocalDate.now().plusDays(2);
    private String siteNumber = "A-1";
    private String phoneNumber = "010-1234-5678";
    private Integer numberOfPeople = 2;
    private String carNumber = null;
    private String requests = null;

    public ReservationRequestTestDataBuilder withDates(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
        return this;
    }

    public ReservationRequestTestDataBuilder withCustomerName(String customerName) {
        this.customerName = customerName;
        return this;
    }

    public ReservationRequestTestDataBuilder withPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
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
