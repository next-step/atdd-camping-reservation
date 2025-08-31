package com.camping.legacy.service;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.domain.Reservation;
import com.camping.legacy.dto.ReservationRequest;
import com.camping.legacy.dto.ReservationResponse;
import com.camping.legacy.repository.CampsiteRepository;
import com.camping.legacy.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationService {
    
    private final ReservationRepository reservationRepository;
    private final CampsiteRepository campsiteRepository;
    
    private static final int MAX_RESERVATION_DAYS = 30;
    
    public ReservationResponse createReservation(ReservationRequest request) {
        String siteNumber = request.getSiteNumber();
        Campsite campsite = campsiteRepository.findBySiteNumber(siteNumber)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 캠핑장입니다."));
        
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();
        
        if (startDate == null || endDate == null) {
            throw new RuntimeException("예약 기간을 선택해주세요.");
        }
        
        if (endDate.isBefore(startDate)) {
            throw new RuntimeException("종료일이 시작일보다 이전일 수 없습니다.");
        }
        
        if (request.getCustomerName() == null || request.getCustomerName().trim().isEmpty()) {
            throw new RuntimeException("예약자 이름을 입력해주세요.");
        }
        
        boolean hasConflict = reservationRepository.existsByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                campsite, endDate, startDate);
        if (hasConflict) {
            throw new RuntimeException("해당 기간에 이미 예약이 존재합니다.");
        }
        
        try {
            Thread.sleep(100); // 100ms 지연으로 동시성 문제 재현 가능성 증가
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Reservation reservation = new Reservation();
        reservation.setCustomerName(request.getCustomerName());
        reservation.setStartDate(startDate);
        reservation.setEndDate(endDate);
        reservation.setReservationDate(startDate);
        reservation.setCampsite(campsite);
        reservation.setPhoneNumber(request.getPhoneNumber());
        
        reservation.setConfirmationCode(generateConfirmationCode());
        
        Reservation saved = reservationRepository.save(reservation);
        
        return ReservationResponse.from(saved);
    }
    
    @Transactional(readOnly = true)
    public ReservationResponse getReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("예약을 찾을 수 없습니다."));
        return ReservationResponse.from(reservation);
    }
    
    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsByDate(LocalDate date) {
        List<Reservation> reservations = reservationRepository.findAll().stream()
                .filter(r -> r.getStartDate() != null && r.getEndDate() != null)
                .filter(r -> !date.isBefore(r.getStartDate()) && !date.isAfter(r.getEndDate()))
                .collect(Collectors.toList());
        
        return reservations.stream()
                .map(ReservationResponse::from)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ReservationResponse> getAllReservations() {
        return reservationRepository.findAll().stream()
                .map(ReservationResponse::from)
                .collect(Collectors.toList());
    }
    
    public void cancelReservation(Long id, String confirmationCode) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("예약을 찾을 수 없습니다."));
        
        if (!reservation.getConfirmationCode().equals(confirmationCode)) {
            throw new RuntimeException("확인 코드가 일치하지 않습니다.");
        }
        
        LocalDate today = LocalDate.now();
        if (reservation.getStartDate().equals(today)) {
            reservation.setStatus("CANCELLED_SAME_DAY");
        } else {
            reservation.setStatus("CANCELLED");
        }
        
        reservationRepository.save(reservation);
    }
    
    // 고객 이름으로 예약 조회
    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsByCustomerName(String customerName) {
        return reservationRepository.findByCustomerName(customerName).stream()
                .map(ReservationResponse::from)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ReservationResponse> searchReservations(String keyword) {
        return reservationRepository.findAll().stream()
                .filter(r -> r.getCustomerName().contains(keyword) || 
                           (r.getPhoneNumber() != null && r.getPhoneNumber().contains(keyword)))
                .map(ReservationResponse::from)
                .collect(Collectors.toList());
    }
    
    public ReservationResponse updateReservation(Long id, ReservationRequest request, String confirmationCode) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("예약을 찾을 수 없습니다."));
        
        if (!reservation.getConfirmationCode().equals(confirmationCode)) {
            throw new RuntimeException("확인 코드가 일치하지 않습니다.");
        }
        
        if (request.getSiteNumber() != null) {
            Campsite campsite = campsiteRepository.findBySiteNumber(request.getSiteNumber())
                    .orElseThrow(() -> new RuntimeException("존재하지 않는 캠핑장입니다."));
            reservation.setCampsite(campsite);
        }
        
        if (request.getStartDate() != null) {
            reservation.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            reservation.setEndDate(request.getEndDate());
        }
        
        if (request.getCustomerName() != null) {
            reservation.setCustomerName(request.getCustomerName());
        }
        if (request.getPhoneNumber() != null) {
            reservation.setPhoneNumber(request.getPhoneNumber());
        }
        
        Reservation updated = reservationRepository.save(reservation);
        return ReservationResponse.from(updated);
    }
    
    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsByNameAndPhone(String name, String phone) {
        return reservationRepository.findByCustomerNameAndPhoneNumber(name, phone).stream()
                .map(ReservationResponse::from)
                .collect(Collectors.toList());
    }
    
    private String generateConfirmationCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }
}