package com.camping.legacy.repository;

import com.camping.legacy.domain.Campsite;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CampsiteRepository extends JpaRepository<Campsite, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Campsite> findBySiteNumber(String siteNumber);
}