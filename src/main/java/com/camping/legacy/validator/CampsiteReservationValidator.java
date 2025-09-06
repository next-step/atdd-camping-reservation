package com.camping.legacy.validator;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.domain.Reservation;
import com.camping.legacy.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CampsiteReservationValidator {

    private final ReservationRepository reservationRepository;

    public void validateCampsiteAvailability(Campsite campsite, LocalDate startDate, LocalDate endDate) {
        List<Reservation> reservations = reservationRepository.findByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                campsite, endDate, startDate
        );

        int confirmedReservationSize = reservations.stream()
                .filter(Reservation::isConfirmed)
                .toList()
                .size();

        if (confirmedReservationSize != 0) {
            throw new RuntimeException("해당 기간에 이미 예약이 존재합니다.");
        }
    }
}