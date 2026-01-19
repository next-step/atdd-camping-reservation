package com.camping.legacy.repository;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.domain.Reservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    
    List<Reservation> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate endDate, LocalDate startDate);
    
    List<Reservation> findByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual(Campsite campsite, LocalDate endDate, LocalDate startDate);
    
    Optional<Reservation> findByCampsiteIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(Long campsiteId, LocalDate endDate, LocalDate startDate);
    @Query("SELECT COUNT(r) > 0 FROM Reservation r WHERE r.campsite = :campsite AND r.startDate < :endDate AND r.endDate > :startDate AND (r.status IS NULL OR r.status NOT LIKE 'CANCELLED%')")
    boolean existsByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual(@Param("campsite") Campsite campsite, @Param("endDate") LocalDate endDate, @Param("startDate") LocalDate startDate);

    @Query("SELECT COUNT(r) > 0 FROM Reservation r WHERE r.campsite = :campsite AND r.id <> :excludeId AND r.startDate < :endDate AND r.endDate > :startDate AND (r.status IS NULL OR r.status NOT LIKE 'CANCELLED%')")
    boolean existsByCampsiteAndDateRangeExcludingId(@Param("campsite") Campsite campsite, @Param("endDate") LocalDate endDate, @Param("startDate") LocalDate startDate, @Param("excludeId") Long excludeId);
    
    List<Reservation> findByCustomerName(String customerName);
    
    List<Reservation> findByCustomerNameAndPhoneNumber(String customerName, String phoneNumber);
    
    boolean existsByCampsiteAndReservationDate(Campsite campsite, LocalDate date);

    @Query("SELECT r FROM Reservation r WHERE r.status IS NULL OR r.status NOT LIKE 'CANCELLED%'")
    List<Reservation> findAllActive();
}