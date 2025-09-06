package com.camping.legacy.acceptance.site;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.repository.CampsiteRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SiteAcceptanceTestSteps {

    private static CampsiteRepository campsiteRepository;

    @Autowired
    private CampsiteRepository repository;

    @PostConstruct
    public void init() {
        campsiteRepository = repository;
    }

    public static Campsite 사이트가_존재한다(String siteNumber) {
        return campsiteRepository.save(
            new Campsite(siteNumber, "테스트 사이트 - 전기 있음, 화장실 인근", 6)
        );
    }
}
