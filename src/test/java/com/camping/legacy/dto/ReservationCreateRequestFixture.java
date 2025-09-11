package com.camping.legacy.dto;

import lombok.*;

import java.time.LocalDate;

public class ReservationCreateRequestFixture {
    public static ReservationCreateRequestBuilder builder() {
        return ReservationCreateRequestFixture.innerBuilder()
                .customerName("홍길동")
                .startDate(LocalDate.of(2024, 7, 1))
                .endDate(LocalDate.of(2024, 7, 3))
                .siteNumber("A-1")
                .phoneNumber("010-1234-5678")
                .numberOfPeople(4)
                .carNumber("12가 3456")
                .request("조용히 사용해주세요.");
    }

    @Builder(builderMethodName = "innerBuilder")
    private static ReservationCreateRequest builder(
            String customerName,
            LocalDate startDate,
            LocalDate endDate,
            String siteNumber,
            String phoneNumber,
            Integer numberOfPeople,
            String carNumber,
            String request
    ) {
        return new ReservationCreateRequest(
                customerName,
                startDate,
                endDate,
                siteNumber,
                phoneNumber,
                numberOfPeople,
                carNumber,
                request
        );
    }
}
