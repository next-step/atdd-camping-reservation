package com.camping.legacy.repository;

import com.camping.legacy.domain.Campsite;

import java.util.List;
import java.util.Optional;

public interface CampsiteRepository {

    Campsite save(Campsite campsite);

    Optional<Campsite> findById(Long id);

    Optional<Campsite> findBySiteNumber(String siteNumber);

    List<Campsite> findAll();

    void deleteAll();
}
