package com.camping.legacy.repository;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    
    List<Reservation> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate endDate, LocalDate startDate);
    
    List<Reservation> findByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual(Campsite campsite, LocalDate endDate, LocalDate startDate);
    
    Optional<Reservation> findByCampsiteIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(Long campsiteId, LocalDate endDate, LocalDate startDate);
    
    boolean existsByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual(Campsite campsite, LocalDate endDate, LocalDate startDate);
    
    List<Reservation> findByCustomerName(String customerName);
    
    List<Reservation> findByCustomerNameAndPhoneNumber(String customerName, String phoneNumber);
    
    boolean existsByCampsiteAndReservationDate(Campsite campsite, LocalDate date);
}