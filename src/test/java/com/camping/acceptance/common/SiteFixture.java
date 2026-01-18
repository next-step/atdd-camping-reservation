package com.camping.acceptance.common;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.repository.CampsiteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SiteFixture {

    @Autowired
    private CampsiteRepository campsiteRepository;

    public Campsite 대형_사이트_생성(String siteNumber) {
        return campsiteRepository.save(new Campsite(siteNumber, "대형 사이트", 6));
    }

    public Campsite 소형_사이트_생성(String siteNumber) {
        return campsiteRepository.save(new Campsite(siteNumber, "소형 사이트", 4));
    }

    public List<Campsite> 대형_사이트_여러개_생성(String... siteNumbers) {
        List<Campsite> sites = new ArrayList<>();
        for (String siteNumber : siteNumbers) {
            sites.add(대형_사이트_생성(siteNumber));
        }
        return sites;
    }

    public List<Campsite> 소형_사이트_여러개_생성(String... siteNumbers) {
        List<Campsite> sites = new ArrayList<>();
        for (String siteNumber : siteNumbers) {
            sites.add(소형_사이트_생성(siteNumber));
        }
        return sites;
    }

    public void 기본_사이트_설정() {
        대형_사이트_생성("A-1");
        대형_사이트_생성("A-2");
        소형_사이트_생성("B-1");
        소형_사이트_생성("B-2");
    }
}