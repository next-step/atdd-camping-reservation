package com.camping.legacy.service;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.repository.CampsiteRepository;
import com.camping.legacy.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampsiteService {
    
    private final CampsiteRepository campsiteRepository;
    private final ReservationRepository reservationRepository;
    
    public List<Campsite> getAllCampsites() {
        return campsiteRepository.findAll();
    }
    
    public Campsite getCampsiteById(Long id) {
        return campsiteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("캠핑장을 찾을 수 없습니다."));
    }
    
    public Campsite getCampsiteBySiteNumber(String siteNumber) {
        return campsiteRepository.findBySiteNumber(siteNumber)
                .orElseThrow(() -> new RuntimeException("캠핑장을 찾을 수 없습니다."));
    }
    
    public boolean isAvailable(String siteNumber, LocalDate date) {
        Campsite campsite = getCampsiteBySiteNumber(siteNumber);
        return !reservationRepository.existsByCampsiteAndReservationDate(campsite, date);
    }
}