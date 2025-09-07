package com.camping.legacy.repository;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    boolean existsByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual(Campsite campsite, LocalDate endDate, LocalDate startDate);

    List<Reservation> findByCustomerName(String customerName);
    
    List<Reservation> findByCustomerNameAndPhoneNumber(String customerName, String phoneNumber);
    
    boolean existsByCampsiteAndReservationDate(Campsite campsite, LocalDate date);

    /**
     * 특정 캠핑장에서 날짜 범위에 겹치는 예약이 있는지 확인한다.
     */
    @Query("""
        SELECT COUNT(r) > 0
        FROM Reservation r
        WHERE r.campsite = :campsite
          AND r.startDate <= :endDate
          AND r.endDate >= :startDate
        """)
    boolean hasConflictingReservation(
        @Param("campsite") Campsite campsite,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * 특정 캠핑장에서 특정 예약을 제외하고 날짜 범위에 겹치는 예약이 있는지 확인한다.
     */
    @Query("""
        SELECT COUNT(r) > 0
        FROM Reservation r
        WHERE r.campsite = :campsite
          AND r.id != :excludeReservationId
          AND r.startDate <= :endDate
          AND r.endDate >= :startDate
        """)
    boolean hasConflictingReservationExcluding(
        @Param("campsite") Campsite campsite,
        @Param("excludeReservationId") Long excludeReservationId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
