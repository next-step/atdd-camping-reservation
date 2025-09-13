package com.camping.legacy.repository;

import com.camping.legacy.domain.Campsite;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CampsiteRepository extends JpaRepository<Campsite, Long> {
    @Query("SELECT c FROM Campsite c WHERE c.siteNumber = :siteNumber")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Campsite> findBySiteNumberForUpdate(String siteNumber);

    Optional<Campsite> findBySiteNumber(String siteNumber);
}