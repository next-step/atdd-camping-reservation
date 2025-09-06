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
}