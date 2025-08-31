package com.camping.legacy.dto;

import com.camping.legacy.domain.Campsite;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteResponse {
    private Long id;
    private String siteNumber;
    private String description;
    private Integer maxPeople;
    private String size;
    private Boolean hasElectricity;
    private Integer toiletDistance;
    private String facilities;
    private String rules;
    
    public static SiteResponse from(Campsite campsite) {
        return SiteResponse.builder()
                .id(campsite.getId())
                .siteNumber(campsite.getSiteNumber())
                .description(campsite.getDescription())
                .maxPeople(campsite.getMaxPeople())
                .size(campsite.getSiteNumber().startsWith("A") ? "대형" : "소형")
                .hasElectricity(true) // 모든 구역은 전기 사용 가능
                .toiletDistance(Integer.parseInt(campsite.getSiteNumber().split("-")[1]) * 10)
                .facilities("화장실, 샤워장, 개수대")
                .rules("22시 이후 소음 금지, 직화 금지")
                .build();
    }
}