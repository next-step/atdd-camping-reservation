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
public interface JpaCampsiteRepository extends JpaRepository<Campsite, Long>, CampsiteRepository {

    @Override
    Optional<Campsite> findBySiteNumber(String siteNumber);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Campsite c WHERE c.siteNumber = :siteNumber")
    Optional<Campsite> findBySiteNumberWithLock(@Param("siteNumber") String siteNumber);
}
