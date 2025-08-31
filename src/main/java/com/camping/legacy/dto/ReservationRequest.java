package com.camping.legacy.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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