package com.camping.legacy.repository;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    boolean existsByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual(Campsite campsite, LocalDate endDate, LocalDate startDate);

    boolean existsByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndStatus(Campsite campsite, LocalDate endDate, LocalDate startDate, String status);
    
    List<Reservation> findByCustomerName(String customerName);
    
    List<Reservation> findByCustomerNameAndPhoneNumber(String customerName, String phoneNumber);
    
    boolean existsByCampsiteAndReservationDate(Campsite campsite, LocalDate date);

    boolean existsByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndStatusIn(
            Campsite campsite, LocalDate endDate, LocalDate startDate, List<String> statuses);
}