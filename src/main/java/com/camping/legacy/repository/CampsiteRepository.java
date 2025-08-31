package com.camping.legacy.repository;

import com.camping.legacy.domain.Campsite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CampsiteRepository extends JpaRepository<Campsite, Long> {
    
    Optional<Campsite> findBySiteNumber(String siteNumber);
}