package com.camping.legacy.repository;

import com.camping.legacy.domain.Campsite;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CampsiteRepository extends JpaRepository<Campsite, Long> {

    Optional<Campsite> findBySiteNumber(String siteNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Campsite c where c.siteNumber = :siteNumber")
    Optional<Campsite> findBySiteNumberForUpdate(@Param("siteNumber") String siteNumber);
}