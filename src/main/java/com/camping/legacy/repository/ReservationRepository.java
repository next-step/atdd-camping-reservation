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
    
    List<Reservation> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate endDate, LocalDate startDate);
    
    List<Reservation> findByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual(Campsite campsite, LocalDate endDate, LocalDate startDate);
    
    Optional<Reservation> findByCampsiteIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(Long campsiteId, LocalDate endDate, LocalDate startDate);
    
    // [BUG FIX] 기존 코드: 취소된 예약도 중복 체크에 포함됨
    // boolean existsByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual(Campsite campsite, LocalDate endDate, LocalDate startDate);

    // [NEW] 취소된 예약(CANCELLED, CANCELLED_SAME_DAY) 제외하고 중복 체크
    @Query("SELECT COUNT(r) > 0 FROM Reservation r " +
           "WHERE r.campsite = :campsite " +
           "AND r.startDate <= :endDate " +
           "AND r.endDate >= :startDate " +
           "AND (r.status IS NULL OR (r.status <> 'CANCELLED' AND r.status <> 'CANCELLED_SAME_DAY'))")
    boolean existsByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            @Param("campsite") Campsite campsite,
            @Param("endDate") LocalDate endDate,
            @Param("startDate") LocalDate startDate);
    
    List<Reservation> findByCustomerName(String customerName);
    
    List<Reservation> findByCustomerNameAndPhoneNumber(String customerName, String phoneNumber);
    
    // [BUG FIX] 기존 코드: 취소된 예약도 가용성 조회에 포함됨
    // boolean existsByCampsiteAndReservationDate(Campsite campsite, LocalDate date);

    // [NEW] 취소된 예약 제외하고 가용성 체크
    @Query("SELECT COUNT(r) > 0 FROM Reservation r " +
           "WHERE r.campsite = :campsite " +
           "AND r.startDate <= :date " +
           "AND r.endDate >= :date " +
           "AND (r.status IS NULL OR (r.status <> 'CANCELLED' AND r.status <> 'CANCELLED_SAME_DAY'))")
    boolean existsByCampsiteAndReservationDate(
            @Param("campsite") Campsite campsite,
            @Param("date") LocalDate date);
}