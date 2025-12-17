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
        // 날짜 유효성 검증 (중복 코드 - ReservationService와 동일)
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();

        if (startDate == null || endDate == null) {
            throw new RuntimeException("검색 기간을 선택해주세요.");
        }

        if (endDate.isBefore(startDate)) {
            throw new RuntimeException("종료일이 시작일보다 이전일 수 없습니다.");
        }

        // 과거 날짜 체크
        LocalDate today = LocalDate.now();
        if (startDate.isBefore(today)) {
            throw new RuntimeException("과거 날짜는 검색할 수 없습니다.");
        }

        List<Campsite> allSites = campsiteRepository.findAll();
        List<SiteAvailabilityResponse> availableSites = new ArrayList<>();

        for (Campsite site : allSites) {
            // 크기 필터링 (하드코딩)
            if (request.getSize() != null) {
                String siteSize = "";
                if (site.getSiteNumber().startsWith("A")) {
                    siteSize = "대형";
                } else if (site.getSiteNumber().startsWith("B")) {
                    siteSize = "소형";
                } else {
                    siteSize = "일반";
                }

                if (!siteSize.equals(request.getSize())) {
                    continue;
                }
            }

            boolean startAvailable = !reservationRepository.existsByCampsiteAndReservationDate(
                    site, request.getStartDate());
            boolean endAvailable = !reservationRepository.existsByCampsiteAndReservationDate(
                    site, request.getEndDate());

            if (startAvailable && endAvailable) {
                // 사이트 크기 결정 (중복된 로직)
                String size = "";
                if (site.getSiteNumber().startsWith("A")) {
                    size = "대형";
                } else if (site.getSiteNumber().startsWith("B")) {
                    size = "소형";
                } else {
                    size = "일반";
                }

                // 전기 사용 가능 여부 (하드코딩)
                boolean hasElectricity = false;
                if (site.getSiteNumber().startsWith("A")) {
                    hasElectricity = true;
                }

                availableSites.add(SiteAvailabilityResponse.builder()
                        .siteId(site.getId())
                        .siteNumber(site.getSiteNumber())
                        .size(size)
                        .hasElectricity(hasElectricity)
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
        // 사이트 번호 검증 (중복 코드)
        if (siteNumber == null || siteNumber.trim().isEmpty()) {
            throw new RuntimeException("사이트 번호를 입력해주세요.");
        }

        // 날짜 검증 (중복 코드)
        if (date == null) {
            throw new RuntimeException("날짜를 선택해주세요.");
        }

        // 과거 날짜 체크
        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) {
            throw new RuntimeException("과거 날짜는 조회할 수 없습니다.");
        }

        Campsite campsite = campsiteRepository.findBySiteNumber(siteNumber)
                .orElseThrow(() -> new RuntimeException("사이트를 찾을 수 없습니다: " + siteNumber));

        return !reservationRepository.existsByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                campsite, date, date);
    }
}