package com.camping.legacy.acceptance.fixture;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static com.camping.legacy.acceptance.fixture.TestFixture.*;

/**
 * 예약 요청 Map을 생성하는 빌더
 */
public class ReservationRequestBuilder {

    private String siteNumber = 사이트_A1;
    private String customerName = 홍길동;
    private String phoneNumber = 홍길동_전화번호;
    private LocalDate startDate;
    private LocalDate endDate;
    private int numberOfPeople = 기본_인원수;

    public static ReservationRequestBuilder aReservation() {
        return new ReservationRequestBuilder();
    }

    public ReservationRequestBuilder withSiteNumber(String siteNumber) {
        this.siteNumber = siteNumber;
        return this;
    }

    public ReservationRequestBuilder withCustomerName(String customerName) {
        this.customerName = customerName;
        return this;
    }

    public ReservationRequestBuilder withPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
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

    public ReservationRequestBuilder withNumberOfPeople(int numberOfPeople) {
        this.numberOfPeople = numberOfPeople;
        return this;
    }

    public ReservationRequestBuilder withDates(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
        return this;
    }

    public Map<String, Object> build() {
        Map<String, Object> request = new HashMap<>();
        request.put("siteNumber", siteNumber);
        request.put("customerName", customerName);
        request.put("phoneNumber", phoneNumber);
        request.put("startDate", startDate.toString());
        request.put("endDate", endDate.toString());
        request.put("numberOfPeople", numberOfPeople);
        return request;
    }
}
