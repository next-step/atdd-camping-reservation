package com.camping.legacy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarResponse {
    private Integer year;
    private Integer month;
    private Long siteId;
    private String siteNumber;
    private List<DayStatus> days;
    private Map<String, Integer> summary;
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DayStatus {
        private LocalDate date;
        private Boolean available;
        private String customerName;
        private Long reservationId;
    }
}