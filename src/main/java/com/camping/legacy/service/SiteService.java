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
            boolean isAvailable = !reservationRepository.existsByCampsiteAndReservationDate(site, date);
            
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
            
            boolean startAvailable = !reservationRepository.existsByCampsiteAndReservationDate(
                    site, request.getStartDate());
            boolean endAvailable = !reservationRepository.existsByCampsiteAndReservationDate(
                    site, request.getEndDate());
            
            if (startAvailable && endAvailable) {
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
        
        return !reservationRepository.existsByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                campsite, date, date);
    }
}