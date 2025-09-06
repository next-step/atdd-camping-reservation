package com.camping.legacy.service;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.domain.Reservation;
import com.camping.legacy.dto.ReservationRequest;
import com.camping.legacy.dto.ReservationResponse;
import com.camping.legacy.generator.ConfirmationCodeGenerator;
import com.camping.legacy.repository.CampsiteRepository;
import com.camping.legacy.repository.ReservationRepository;
import com.camping.legacy.validator.CampsiteReservationValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final CampsiteRepository campsiteRepository;
    private final CampsiteReservationValidator campsiteReservationValidator;
    private final ConfirmationCodeGenerator confirmationCodeGenerator;

    public ReservationService(
            ReservationRepository reservationRepository,
            CampsiteRepository campsiteRepository,
            CampsiteReservationValidator campsiteReservationValidator,
            ConfirmationCodeGenerator confirmationCodeGenerator
    ) {
        this.reservationRepository = reservationRepository;
        this.campsiteRepository = campsiteRepository;
        this.campsiteReservationValidator = campsiteReservationValidator;
        this.confirmationCodeGenerator = confirmationCodeGenerator;
    }

    public ReservationResponse createReservation(ReservationRequest request) {
        String siteNumber = request.getSiteNumber();
        Campsite campsite = campsiteRepository.findBySiteNumberWithLock(siteNumber)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 캠핑장입니다."));

        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();

        campsiteReservationValidator.validateCampsiteAvailability(campsite, startDate, endDate);

        Reservation reservation = new Reservation(
                request.getCustomerName(),
                startDate,
                endDate,
                campsite,
                request.getPhoneNumber(),
                confirmationCodeGenerator.generateConfirmationCode()
        );

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
                .toList();

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

        Campsite campsite = null;
        if (request.getSiteNumber() != null) {
            campsite = campsiteRepository.findBySiteNumber(request.getSiteNumber())
                    .orElseThrow(() -> new RuntimeException("존재하지 않는 캠핑장입니다."));
        }

        reservation.updateReservation(
                request.getCustomerName(),
                request.getStartDate(),
                request.getEndDate(),
                campsite,
                request.getPhoneNumber()
        );

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