package com.camping.legacy.stub;

import com.camping.legacy.dto.ReservationRequest;

import java.time.LocalDate;

/**
 * 테스트 전용 ReservationRequest Test Data Builder.
 */
public class ReservationRequestTestDataBuilder {

    // 기본값
    private String name = "홍길동";
    private String siteNumber = "A-1";
    private LocalDate startDate = LocalDate.now();
    private LocalDate endDate = LocalDate.now().plusDays(1);
    private String phone = "010-0000-0000";
    private Integer numberOfPeople = 4;
    private String carNumber = "12가3456";
    private String requests = "없음";

    public ReservationRequestTestDataBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public ReservationRequestTestDataBuilder withSiteNumber(String siteNumber) {
        this.siteNumber = siteNumber;
        return this;
    }

    public ReservationRequestTestDataBuilder withStartDate(LocalDate startDate) {
        this.startDate = startDate;
        return this;
    }

    public ReservationRequestTestDataBuilder withEndDate(LocalDate endDate) {
        this.endDate = endDate;
        return this;
    }

    public ReservationRequestTestDataBuilder withPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public ReservationRequestTestDataBuilder withNumberOfPeople(Integer numberOfPeople) {
        this.numberOfPeople = numberOfPeople;
        return this;
    }

    public ReservationRequestTestDataBuilder withCarNumber(String carNumber) {
        this.carNumber = carNumber;
        return this;
    }

    public ReservationRequestTestDataBuilder withRequests(String requests) {
        this.requests = requests;
        return this;
    }

    public ReservationRequest build() {
        return new ReservationRequest(
                name,
                startDate,
                endDate,
                siteNumber,
                phone,
                numberOfPeople,
                carNumber,
                requests
        );
    }
}
