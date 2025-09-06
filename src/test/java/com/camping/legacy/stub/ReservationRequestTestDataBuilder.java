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

    // with 메서드들
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

    // 기존 사용 패턴과 동일한 정적 팩토리 메서드 제공
    public static ReservationRequest get(
            String customerName,
            String siteNumber,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return new ReservationRequestTestDataBuilder()
                .withName(customerName)
                .withSiteNumber(siteNumber)
                .withStartDate(startDate)
                .withEndDate(endDate)
                .build();
    }

    public static ReservationRequest get(
            String customerName,
            String siteNumber,
            LocalDate startDate,
            LocalDate endDate,
            String phoneNumber,
            Integer numberOfPeople,
            String carNumber,
            String requests
    ) {
        ReservationRequestTestDataBuilder b = new ReservationRequestTestDataBuilder();
        if (customerName != null) b.withName(customerName);
        if (siteNumber != null) b.withSiteNumber(siteNumber);
        if (startDate != null) b.withStartDate(startDate);
        if (endDate != null) b.withEndDate(endDate);
        if (phoneNumber != null) b.withPhone(phoneNumber);
        if (numberOfPeople != null) b.withNumberOfPeople(numberOfPeople);
        if (carNumber != null) b.withCarNumber(carNumber);
        if (requests != null) b.withRequests(requests);
        return b.build();
    }
}
