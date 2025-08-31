package com.camping.legacy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteAvailabilityResponse {
    private Long siteId;
    private String siteNumber;
    private String size;
    private Boolean hasElectricity;
    private LocalDate date;
    private Boolean available;
    private Integer maxPeople;
    private String description;
}