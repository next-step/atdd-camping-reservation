package com.camping.legacy.fixture;

import static com.camping.legacy.fixture.ReservationFixture.*;

import com.camping.legacy.dto.ReservationRequest;
import java.time.LocalDate;

public class ReservationRequestBuilder {
    private String customerName = CUSTOMER_NAME;
    private LocalDate startDate = LocalDate.now();
    private LocalDate endDate = startDate.plusDays(1);
    private String siteNumber = SITE_A1;
    private String phoneNumber = PHONE_NUMBER;
    private Integer numberOfPeople = 4;
    private String carNumber;
    private String requests = "잘 부탁드립니다";

    public static ReservationRequestBuilder aReservationRequest() {
        return new ReservationRequestBuilder();
    }

    public ReservationRequestBuilder withCustomerName(String customerName) {
        this.customerName = customerName;
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

    public ReservationRequestBuilder withSiteNumber(String siteNumber) {
        this.siteNumber = siteNumber;
        return this;
    }

    public ReservationRequestBuilder withPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        return this;
    }

    public ReservationRequestBuilder withNumberOfPeople(Integer numberOfPeople) {
        this.numberOfPeople = numberOfPeople;
        return this;
    }

    public ReservationRequestBuilder withCarNumber(String carNumber) {
        this.carNumber = carNumber;
        return this;
    }

    public ReservationRequestBuilder withRequests(String requests) {
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
