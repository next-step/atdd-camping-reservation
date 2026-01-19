package com.camping.legacy.acceptance.reservation.builder;

import com.camping.legacy.dto.ReservationRequest;

import java.time.LocalDate;

import static com.camping.legacy.acceptance.reservation.ReservationTestConstants.*;

public class ReservationRequestBuilder {

    private String reserverName = 홍길동;
    private LocalDate startDate = LocalDate.parse(기존예약_시작일);
    private LocalDate endDate = LocalDate.parse(기존예약_종료일);
    private String siteNumber = 사이트번호_A_1;

    private String phone = 연락처;
    private int headCount = 인원수_5명;
    private String carNumber = 차량번호;
    private String memo = 요청사항;

    private ReservationRequestBuilder() {
    }

    public static ReservationRequestBuilder Reservation() {
        return new ReservationRequestBuilder();
    }

    public ReservationRequestBuilder reserver(String name) {
        this.reserverName = name;
        return this;
    }

    public ReservationRequestBuilder period(String startDate, String endDate) {
        this.startDate = LocalDate.parse(startDate);
        this.endDate = LocalDate.parse(endDate);
        return this;
    }

    public ReservationRequestBuilder site(String siteNumber) {
        this.siteNumber = siteNumber;
        return this;
    }

    public ReservationRequestBuilder headCount(int headCount) {
        this.headCount = headCount;
        return this;
    }

    public ReservationRequestBuilder phone(String phone) {
        this.phone = phone;
        return this;
    }

    public ReservationRequestBuilder carNumber(String carNumber) {
        this.carNumber = carNumber;
        return this;
    }

    public ReservationRequestBuilder memo(String memo) {
        this.memo = memo;
        return this;
    }

    public ReservationRequest build() {
        return new ReservationRequest(
                reserverName,
                startDate,
                endDate,
                siteNumber,
                phone,
                headCount,
                carNumber,
                memo
        );
    }
}
