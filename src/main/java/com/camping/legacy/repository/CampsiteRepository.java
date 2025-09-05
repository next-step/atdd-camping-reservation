package com.camping.legacy.repository;

import com.camping.legacy.domain.Campsite;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

@Repository
public interface CampsiteRepository extends JpaRepository<Campsite, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Campsite> findBySiteNumber(String siteNumber);
}
