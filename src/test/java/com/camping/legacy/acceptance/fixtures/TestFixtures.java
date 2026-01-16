package com.camping.legacy.acceptance.fixtures;

import java.time.LocalDate;

@SuppressWarnings("NonAsciiCharacters")
public class TestFixtures {

    public static final ReservationRequest 기본_예약_요청 = ReservationRequest.builder()
            .customerName("홍길동")
            .startDate(LocalDate.now().plusDays(10))
            .endDate(LocalDate.now().plusDays(12))
            .siteNumber("A-1")
            .build();

    public static final ReservationRequest 종료일이_시작일_보다_빠른_예약 = ReservationRequest.builder()
            .customerName("김철수")
            .startDate(LocalDate.now().plusDays(12))
            .endDate(LocalDate.now().plusDays(12 - 2))
            .siteNumber("A-1")
            .build();

    public static final ReservationRequest 과거_시간의_예약 = ReservationRequest.builder()
            .customerName("김철수")
            .startDate(LocalDate.now().minusDays(2))
            .endDate(LocalDate.now().minusDays(1))
            .siteNumber("A-1")
            .build();

    public static final ReservationRequest 기간이_30일_초과된_예약 = ReservationRequest.builder()
            .customerName("김철수")
            .startDate(LocalDate.now().plusDays(12))
            .endDate(LocalDate.now().plusDays(12 + 31))
            .siteNumber("A-1")
            .build();

}
