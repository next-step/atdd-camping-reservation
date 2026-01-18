package com.camping.legacy.acceptance.fixture;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.repository.CampsiteRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CampsiteFixture {

    private final CampsiteRepository campsiteRepository;

    public CampsiteFixture(CampsiteRepository campsiteRepository) {
        this.campsiteRepository = campsiteRepository;
    }

    public void setUp() {
        // A 구역: 대형 사이트
        campsiteRepository.saveAll(List.of(
                대형_사이트("A-1", "대형 사이트 - 전기 있음, 화장실 인근"),
                대형_사이트("A-2", "대형 사이트 - 전기 있음, 화장실 인근"),
                대형_사이트("A-3", "대형 사이트 - 전기 있음, 화장실 인근"),
                대형_사이트("A-4", "대형 사이트 - 전기 있음, 개수대 인근"),
                대형_사이트("A-5", "대형 사이트 - 전기 있음, 개수대 인근"),
                대형_사이트("A-6", "대형 사이트 - 전기 있음, 놀이터 인근"),
                대형_사이트("A-7", "대형 사이트 - 전기 있음, 놀이터 인근"),
                대형_사이트("A-8", "대형 사이트 - 전기 있음, 계곡 전망"),
                대형_사이트("A-9", "대형 사이트 - 전기 있음, 계곡 전망"),
                대형_사이트("A-10", "대형 사이트 - 전기 있음, 계곡 전망"),
                대형_사이트("A-11", "대형 사이트 - 전기 있음, 산 전망"),
                대형_사이트("A-12", "대형 사이트 - 전기 있음, 산 전망"),
                대형_사이트("A-13", "대형 사이트 - 전기 있음, 산 전망"),
                대형_사이트("A-14", "대형 사이트 - 전기 있음, 중앙 위치"),
                대형_사이트("A-15", "대형 사이트 - 전기 있음, 중앙 위치"),
                대형_사이트("A-16", "대형 사이트 - 전기 있음, 중앙 위치"),
                대형_사이트("A-17", "대형 사이트 - 전기 있음, 조용한 위치"),
                대형_사이트("A-18", "대형 사이트 - 전기 있음, 조용한 위치"),
                대형_사이트("A-19", "대형 사이트 - 전기 있음, 조용한 위치"),
                대형_사이트("A-20", "대형 사이트 - 전기 있음, 조용한 위치")
        ));

        // B 구역: 소형 사이트
        campsiteRepository.saveAll(List.of(
                소형_사이트("B-1", "소형 사이트 - 전기 있음, 매점 인근"),
                소형_사이트("B-2", "소형 사이트 - 전기 있음, 매점 인근"),
                소형_사이트("B-3", "소형 사이트 - 전기 있음, 매점 인근"),
                소형_사이트("B-4", "소형 사이트 - 전기 있음, 주차장 인근"),
                소형_사이트("B-5", "소형 사이트 - 전기 있음, 주차장 인근"),
                소형_사이트("B-6", "소형 사이트 - 전기 있음, 주차장 인근"),
                소형_사이트("B-7", "소형 사이트 - 전기 있음, 샤워장 인근"),
                소형_사이트("B-8", "소형 사이트 - 전기 있음, 샤워장 인근"),
                소형_사이트("B-9", "소형 사이트 - 전기 있음, 샤워장 인근"),
                소형_사이트("B-10", "소형 사이트 - 전기 있음, 샤워장 인근"),
                소형_사이트("B-11", "소형 사이트 - 전기 있음, 바비큐장 인근"),
                소형_사이트("B-12", "소형 사이트 - 전기 있음, 바비큐장 인근"),
                소형_사이트("B-13", "소형 사이트 - 전기 있음, 바비큐장 인근"),
                소형_사이트("B-14", "소형 사이트 - 전기 있음, 운동장 인근"),
                소형_사이트("B-15", "소형 사이트 - 전기 있음, 운동장 인근")
        ));
    }

    private Campsite 대형_사이트(String siteNumber, String description) {
        return new Campsite(siteNumber, description, 6);
    }

    private Campsite 소형_사이트(String siteNumber, String description) {
        return new Campsite(siteNumber, description, 6);
    }
}