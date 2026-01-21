package com.camping.legacy.repository;

import com.camping.legacy.domain.Campsite;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CampsiteRepository extends JpaRepository<Campsite, Long> {

    Optional<Campsite> findBySiteNumber(String siteNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Campsite c WHERE c.siteNumber = :siteNumber")
    @QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "3000")})
    Optional<Campsite> findBySiteNumberWithLock(@Param("siteNumber") String siteNumber);
}
