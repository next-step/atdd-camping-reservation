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

    boolean existsByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual(Campsite campsite, LocalDate endDate, LocalDate startDate);

    List<Reservation> findByCustomerName(String customerName);

    List<Reservation> findByCustomerNameAndPhoneNumber(String customerName, String phoneNumber);

    boolean existsByCampsiteAndReservationDate(Campsite campsite, LocalDate date);


    @Query("""
        SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
        FROM Reservation r
        WHERE r.campsite = :campsite
              AND r.startDate <= :endDate
              AND r.endDate >= :startDate
              AND r.id != :excludeId
    """)
    boolean existsOverlappingReservation(
            @Param("campsite") Campsite campsite,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeId") Long excludeId);

    @Query("""
        SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
        FROM Reservation r
        WHERE r.campsite = :campsite
        AND r.startDate <= :endDate
        AND r.endDate >= :startDate
    """)
    boolean existsOverlappingReservation(
            @Param("campsite") Campsite campsite,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Campsite c WHERE c.siteNumber = :siteNumber")
    Optional<Campsite> findBySiteNumberWithLock(@Param("siteNumber") String siteNumber);

}