package com.camping.legacy.service;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.domain.Reservation;
import com.camping.legacy.domain.ReservationStatus;
import com.camping.legacy.dto.ReservationRequest;
import com.camping.legacy.dto.ReservationResponse;
import com.camping.legacy.exception.BadRequestException;
import com.camping.legacy.exception.ConflictException;
import com.camping.legacy.exception.NotFoundException;
import com.camping.legacy.repository.CampsiteRepository;
import com.camping.legacy.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final CampsiteRepository campsiteRepository;

    public synchronized ReservationResponse createReservation(ReservationRequest request) {
        String siteNumber = request.getSiteNumber();
        Campsite campsite = campsiteRepository.findBySiteNumber(siteNumber)
                .orElseThrow(() -> new BadRequestException("존재하지 않는 캠핑장입니다."));

        // 도메인 객체에서 유효성 검증 수행
        Reservation.validateReservationPeriod(request.getStartDate(), request.getEndDate());

        // 기존 예약 중복 체크 - 캠프사이트 도메인에서 처리
        List<Reservation> conflictingReservations = reservationRepository
                .findByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        campsite, request.getEndDate(), request.getStartDate());
        
        boolean hasConflict = conflictingReservations.stream()
                .anyMatch(reservation -> reservation.getStatus().isActive());
        if (hasConflict) {
            throw new ConflictException("해당 기간에 이미 예약이 존재합니다.");
        }

        try {
            Thread.sleep(100); // 100ms 지연으로 동시성 문제 재현 가능성 증가
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 도메인 팩토리 메서드 사용
        Reservation reservation = Reservation.create(request, campsite);
        Reservation saved = reservationRepository.save(reservation);

        return ReservationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("예약을 찾을 수 없습니다."));
        return ReservationResponse.from(reservation);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsByDate(LocalDate date) {
        List<Reservation> reservations = reservationRepository.findAll().stream()
                .filter(reservation -> reservation.isWithinDateRange(date))
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
                .orElseThrow(() -> new NotFoundException("예약을 찾을 수 없습니다."));

        // 도메인 객체에서 확인 코드 검증
        reservation.validateConfirmationCode(confirmationCode);

        // 도메인 객체에서 취소 로직 처리
        reservation.cancel();

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
                .filter(reservation -> reservation.matchesKeyword(keyword))
                .map(ReservationResponse::from)
                .collect(Collectors.toList());
    }

    public ReservationResponse updateReservation(Long id, ReservationRequest request, String confirmationCode) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("예약을 찾을 수 없습니다."));

        // 도메인 객체에서 확인 코드 검증
        reservation.validateConfirmationCode(confirmationCode);

        Campsite newCampsite = null;
        if (request.getSiteNumber() != null) {
            newCampsite = campsiteRepository.findBySiteNumber(request.getSiteNumber())
                    .orElseThrow(() -> new BadRequestException("존재하지 않는 캠핑장입니다."));
        }

        // 도메인 객체에서 업데이트 로직 처리
        reservation.updateReservation(request, newCampsite);

        Reservation updated = reservationRepository.save(reservation);
        return ReservationResponse.from(updated);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsByNameAndPhone(String name, String phone) {
        return reservationRepository.findAll().stream()
                .filter(reservation -> reservation.matchesCustomer(name, phone))
                .map(ReservationResponse::from)
                .collect(Collectors.toList());
    }

}
