package com.camping.legacy.repository;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JpaReservationRepository extends JpaRepository<Reservation, Long>, ReservationRepository {

    @Override
    List<Reservation> findByCustomerName(String customerName);

    @Override
    List<Reservation> findByCustomerNameAndPhoneNumber(String customerName, String phoneNumber);

    @Override
    boolean existsByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Campsite campsite, LocalDate endDate, LocalDate startDate);

    @Override
    boolean existsByCampsiteAndReservationDate(Campsite campsite, LocalDate date);
}
