package com.camping.legacy.domain;

import com.camping.legacy.repository.ReservationRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReservationConflictValidator {

    private final ReservationRepository reservationRepository;

    /**
     * 특정 사이트에 대해 입력 기간 동안 예약이 존재하는지 확인한다.
     */
    public void validateNoConflict(Campsite campsite, LocalDate startDate, LocalDate endDate) {
        if (reservationRepository.hasConflictingReservation(campsite, startDate, endDate)) {
            throw new RuntimeException("해당 기간에 이미 예약이 존재합니다.");
        }
    }

    /**
     * 특정 예약을 제외하고 특정 사이트에 대해 입력 기간 동안 예약이 존재하는지 확인한다. (예약 수정 시 사용)
     */
    public void validateNoConflictExcluding(
        Campsite campsite, Reservation excludeReservation,
        LocalDate startDate, LocalDate endDate
    ) {
        if (reservationRepository.hasConflictingReservationExcluding(
            campsite, excludeReservation.getId(),
            startDate, endDate
        )) {
            throw new RuntimeException("해당 기간에 이미 예약이 존재합니다.");
        }
    }
}
