package com.camping.legacy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
@Builder
public class ReservationRequest {
    
    private String customerName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String siteNumber;
    private String phoneNumber;
    private Integer numberOfPeople;
    private String carNumber;
    private String requests;

    public void validate() {
        LocalDate today = LocalDate.now();

        if (startDate == null || endDate == null) {
            throw new RuntimeException("예약 기간을 선택해주세요.");
        }

        // 시작일이 오늘보다 이전일 수 없음
        if (startDate.isBefore(today) || endDate.isBefore(today)) {
            throw new RuntimeException("시작일과 종료일은 오늘보다 이전일 수 없습니다.");
        }

        if (endDate.isBefore(startDate)) {
            throw new RuntimeException("종료일이 시작일보다 이전일 수 없습니다.");
        }

        // 시작일이 오늘 기준으로 30일 초과 불가
        if (startDate.isAfter(today.plusDays(MAX_RESERVATION_DAYS))) {
            throw new RuntimeException("시작일은 오늘부터 최대 30일 이내여야 합니다.");
        }

        if (customerName == null || customerName.trim().isEmpty()) {
            throw new RuntimeException("예약자 이름을 입력해주세요.");
        }
    }

    private static final int MAX_RESERVATION_DAYS = 30;
}