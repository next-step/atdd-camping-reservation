package com.camping.legacy.repository;

import com.camping.legacy.domain.Campsite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaCampsiteRepository extends JpaRepository<Campsite, Long>, CampsiteRepository {

    @Override
    Optional<Campsite> findBySiteNumber(String siteNumber);
}
