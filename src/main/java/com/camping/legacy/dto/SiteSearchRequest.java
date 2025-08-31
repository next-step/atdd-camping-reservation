package com.camping.legacy.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SiteSearchRequest {
    private LocalDate startDate;
    private LocalDate endDate;
    private String size;
}