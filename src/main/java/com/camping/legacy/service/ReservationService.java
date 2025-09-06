package com.camping.legacy.service;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.domain.Reservation;
import com.camping.legacy.domain.ReservationStatus;
import com.camping.legacy.dto.ReservationRequest;
import com.camping.legacy.dto.ReservationResponse;
import com.camping.legacy.repository.CampsiteRepository;
import com.camping.legacy.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
        checkReservationAvailable(request);
        Campsite campsite = campsiteRepository.findBySiteNumber(request.getSiteNumber())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 캠핑장입니다."));
        
        try {
            Thread.sleep(100); // 100ms 지연으로 동시성 문제 재현 가능성 증가
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 동시성 제어를 위해 다시 한 번 체크
        synchronized (this) {
            checkReservationExists(request.getSiteNumber(), request.getStartDate(), request.getEndDate());

            Reservation reservation = new Reservation(
                    request.getCustomerName(),
                    request.getStartDate(),
                    request.getEndDate(),
                    campsite,
                    request.getPhoneNumber(),
                    generateConfirmationCode()
            );
            Reservation saved = reservationRepository.save(reservation);

            return ReservationResponse.from(saved);
        }
    }

    private void checkReservationAvailable(ReservationRequest request) {
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();

        checkDatesAvailable(startDate, endDate);
        checkRequiredField(request.getCustomerName(), "예약자 이름을 입력해주세요.");
        
        if (!isValidPhoneNumber(request.getPhoneNumber())) {
            throw new RuntimeException("올바른 전화번호 형식이 아닙니다.");
        }
    }

    private void checkDatesAvailable(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new RuntimeException("예약 기간을 선택해주세요.");
        }

        if (endDate.isBefore(startDate)) {
            throw new RuntimeException("종료일이 시작일보다 이전일 수 없습니다.");
        }

        LocalDate today = LocalDate.now();
        if (startDate.isBefore(today) || endDate.isBefore(today)) {
            throw new RuntimeException("예약일이 과거일 수 없습니다.");
        }

        if (startDate.isAfter(today.plusDays(MAX_RESERVATION_DAYS)) || endDate.isAfter(today.plusDays(MAX_RESERVATION_DAYS))) {
            throw new RuntimeException("예약일이 오늘 기준 30일을 초과할 수 없습니다.");
        }
    }


    private void checkRequiredField(String value, String errorMessage) {
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException(errorMessage);
        }
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        checkRequiredField(phoneNumber, "전화번호를 입력해주세요.");

        // 한국 휴대폰 번호 형식: 010-xxxx-xxxx
        String pattern = "^010-\\d{4}-\\d{4}$";
        return phoneNumber.matches(pattern);
    }

    private void checkReservationExists(String siteNumber, LocalDate startDate, LocalDate endDate) {
        Reservation existingReservation =
                reservationRepository.findByCampsiteSiteNumberAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByCreatedAtDesc(siteNumber, startDate, endDate);

        if (existingReservation != null && existingReservation.isConfirmed()) {
            throw new RuntimeException("해당 기간에 이미 예약이 존재합니다.");
        }
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
            reservation.setStatus(ReservationStatus.CANCELLED_SAME_DAY);
        } else {
            reservation.setStatus(ReservationStatus.CANCELLED);
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
}
