package com.camping.legacy.fixture;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.repository.CampsiteRepository;

import java.util.List;

public class CampsiteFixture {

    public static void createDefaultSites(CampsiteRepository repository) {
        repository.saveAll(List.of(
                new Campsite("A-1", "대형 사이트 - 전기 있음, 화장실 인근", 6),
                new Campsite("A-2", "대형 사이트 - 전기 있음, 화장실 인근", 6)
        ));
    }

    public static void createAllSites(CampsiteRepository repository) {
        repository.saveAll(List.of(
                new Campsite("A-1", "대형 사이트 - 전기 있음, 화장실 인근", 6),
                new Campsite("A-2", "대형 사이트 - 전기 있음, 화장실 인근", 6),
                new Campsite("A-3", "대형 사이트 - 전기 있음, 화장실 인근", 6),
                new Campsite("B-1", "소형 사이트 - 전기 있음, 매점 인근", 4),
                new Campsite("B-2", "소형 사이트 - 전기 있음, 매점 인근", 4)
        ));
    }
}
