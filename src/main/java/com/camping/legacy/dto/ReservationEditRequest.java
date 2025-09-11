package com.camping.legacy.dto;

import java.time.LocalDate;

public record ReservationEditRequest(
        String customerName,
        LocalDate startDate,
        LocalDate endDate,
        String siteNumber,
        String phoneNumber,
        Integer numberOfPeople,
        String carNumber,
        String requests
) {
    public ReservationEditRequest {
        if (customerName != null && customerName.isBlank()) throw new RuntimeException("예약자 이름을 입력해주세요.");
        if (siteNumber != null && siteNumber.isBlank()) throw new RuntimeException("캠핑장 번호를 입력해주세요.");
        if (phoneNumber != null && !phoneNumber.matches("^01[016789]-?\\d{3,4}-?\\d{4}$"))
            throw new RuntimeException("전화번호를 입력해주세요.");

        if (startDate != null && endDate != null) {
            if (endDate.isBefore(startDate)) throw new RuntimeException("종료일이 시작일보다 이전일 수 없습니다.");
        }
    }
}