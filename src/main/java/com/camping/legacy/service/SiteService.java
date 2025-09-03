package com.camping.legacy.service;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.dto.SiteAvailabilityResponse;
import com.camping.legacy.dto.SiteResponse;
import com.camping.legacy.dto.SiteSearchRequest;
import com.camping.legacy.repository.CampsiteRepository;
import com.camping.legacy.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SiteService {
    
    private final CampsiteRepository campsiteRepository;
    private final ReservationRepository reservationRepository;
    
    public List<SiteResponse> getAllSites() {
        return campsiteRepository.findAll().stream()
                .map(SiteResponse::from)
                .collect(Collectors.toList());
    }
    
    public SiteResponse getSiteById(Long siteId) {
        Campsite campsite = campsiteRepository.findById(siteId)
                .orElseThrow(() -> new RuntimeException("사이트를 찾을 수 없습니다."));
        return SiteResponse.from(campsite);
    }
    
    public List<SiteAvailabilityResponse> getAvailableSites(LocalDate date) {
        List<Campsite> allSites = campsiteRepository.findAll();
        List<SiteAvailabilityResponse> responses = new ArrayList<>();
        
        for (Campsite site : allSites) {
            // 취소된 예약을 제외하고 가용성 확인
            boolean isAvailable = reservationRepository.findByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                    site, date, date).stream()
                    .noneMatch(reservation -> reservation.getStatus().isActive());
            
            responses.add(SiteAvailabilityResponse.builder()
                    .siteId(site.getId())
                    .siteNumber(site.getSiteNumber())
                    .size(site.getSiteNumber().startsWith("A") ? "대형" : "소형")
                    .hasElectricity(site.getSiteNumber().startsWith("A"))
                    .date(date)
                    .available(isAvailable)
                    .maxPeople(site.getMaxPeople())
                    .description(site.getDescription())
                    .build());
        }
        
        return responses.stream()
                .filter(SiteAvailabilityResponse::getAvailable)
                .collect(Collectors.toList());
    }
    
    public List<SiteAvailabilityResponse> searchAvailableSites(SiteSearchRequest request) {
        List<Campsite> allSites = campsiteRepository.findAll();
        List<SiteAvailabilityResponse> availableSites = new ArrayList<>();
        
        for (Campsite site : allSites) {
            // 크기 필터링
            if (request.getSize() != null) {
                String siteSize = site.getSiteNumber().startsWith("A") ? "대형" : "소형";
                if (!siteSize.equals(request.getSize())) {
                    continue;
                }
            }
            
            // 기간 동안의 가용성 확인 - 취소된 예약은 제외
            boolean isAvailable = reservationRepository.findByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                    site, request.getEndDate(), request.getStartDate()).stream()
                    .noneMatch(reservation -> reservation.getStatus().isActive());
            
            if (isAvailable) {
                availableSites.add(SiteAvailabilityResponse.builder()
                        .siteId(site.getId())
                        .siteNumber(site.getSiteNumber())
                        .size(site.getSiteNumber().startsWith("A") ? "대형" : "소형")
                        .hasElectricity(site.getSiteNumber().startsWith("A"))
                        .date(request.getStartDate())
                        .available(true)
                        .maxPeople(site.getMaxPeople())
                        .description(site.getDescription())
                        .build());
            }
        }
        
        return availableSites;
    }
    
    public boolean isAvailable(String siteNumber, LocalDate date) {
        Campsite campsite = campsiteRepository.findBySiteNumber(siteNumber)
                .orElseThrow(() -> new RuntimeException("사이트를 찾을 수 없습니다: " + siteNumber));
        
        // 취소된 예약을 제외하고 해당 날짜의 가용성 확인
        return reservationRepository.findByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                campsite, date, date).stream()
                .noneMatch(reservation -> reservation.getStatus().isActive());
    }
}
