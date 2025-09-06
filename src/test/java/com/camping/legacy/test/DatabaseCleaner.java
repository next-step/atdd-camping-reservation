package com.camping.legacy.test;

import com.camping.legacy.repository.CampsiteRepository;
import com.camping.legacy.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class DatabaseCleaner {

    private final CampsiteRepository campsiteRepository;
    private final ReservationRepository reservationRepository;

    public void clear() {
        reservationRepository.deleteAllInBatch();
        log.info("Reservation data cleared.");

        campsiteRepository.deleteAllInBatch();
        log.info("Campsite data cleared.");
    }
}
