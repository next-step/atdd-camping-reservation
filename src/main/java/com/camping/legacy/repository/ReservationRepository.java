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

    List<Reservation> findByCustomerName(String customerName);
    
    List<Reservation> findByCustomerNameAndPhoneNumber(String customerName, String phoneNumber);

    @Query("select count(r) > 0 from Reservation r where r.campsite = :campsite and r.startDate <= :endDate and r.endDate >= :startDate and (r.status is null or r.status not like 'CANCELLED%')")
    boolean existsByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual(@Param("campsite") Campsite campsite, @Param("endDate") LocalDate endDate, @Param("startDate") LocalDate startDate);

    @Query("select count(r) > 0 from Reservation r where r.campsite = :campsite and r.startDate <= :endDate and r.endDate >= :startDate and r.id <> :id and (r.status is null or r.status not like 'CANCELLED%')")
    boolean existsByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndIdNot(@Param("campsite") Campsite campsite, @Param("endDate") LocalDate endDate, @Param("startDate") LocalDate startDate, @Param("id") Long id);

    boolean existsByCampsiteAndReservationDate(Campsite campsite, LocalDate date);

}