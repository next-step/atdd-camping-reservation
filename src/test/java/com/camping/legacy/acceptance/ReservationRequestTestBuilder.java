package com.camping.legacy.acceptance;

import com.camping.legacy.dto.ReservationRequest;
import java.time.LocalDate;

public class ReservationRequestTestBuilder {

    private String customerName = "김철수";
    private LocalDate startDate = LocalDate.now().plusDays(1);
    private LocalDate endDate = LocalDate.now().plusDays(3);
    private String siteNumber = "A-1";
    private String phoneNumber = "010-1234-5678";
    private Integer numberOfPeople = 2;
    private String carNumber = "12가1234";
    private String requests = "조용한 자리 원합니다.";

    public static ReservationRequestTestBuilder builder() {
        return new ReservationRequestTestBuilder();
    }

    public ReservationRequestTestBuilder withCustomerName(String customerName) {
        this.customerName = customerName;
        return this;
    }

    public ReservationRequestTestBuilder withStartDate(LocalDate startDate) {
        this.startDate = startDate;
        return this;
    }

    public ReservationRequestTestBuilder withEndDate(LocalDate endDate) {
        this.endDate = endDate;
        return this;
    }

    public ReservationRequestTestBuilder withSiteNumber(String siteNumber) {
        this.siteNumber = siteNumber;
        return this;
    }

    public ReservationRequestTestBuilder withPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        return this;
    }

    public ReservationRequestTestBuilder withNumberOfPeople(Integer numberOfPeople) {
        this.numberOfPeople = numberOfPeople;
        return this;
    }

    public ReservationRequestTestBuilder withCarNumber(String carNumber) {
        this.carNumber = carNumber;
        return this;
    }

    public ReservationRequestTestBuilder withRequests(String requests) {
        this.requests = requests;
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
