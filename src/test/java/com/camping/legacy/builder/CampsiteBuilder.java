package com.camping.legacy.builder;

import com.camping.legacy.domain.Campsite;

public class CampsiteBuilder {

    private String siteNumber = "A-1";
    private String description = "기본 사이트";
    private Integer maxPeople = 4;

    public static CampsiteBuilder aCampsite() {
        return new CampsiteBuilder();
    }

    public CampsiteBuilder siteNumber(String siteNumber) {
        this.siteNumber = siteNumber;
        return this;
    }

    public CampsiteBuilder description(String description) {
        this.description = description;
        return this;
    }

    public CampsiteBuilder maxPeople(Integer maxPeople) {
        this.maxPeople = maxPeople;
        return this;
    }

    public CampsiteBuilder largeSite(String number) {
        this.siteNumber = "A-" + number;
        this.description = "대형 사이트 - 전기 있음, 화장실 인근";
        this.maxPeople = 6;
        return this;
    }

    public CampsiteBuilder smallSite(String number) {
        this.siteNumber = "B-" + number;
        this.description = "소형 사이트 - 전기 있음, 매점 인근";
        this.maxPeople = 4;
        return this;
    }

    public Campsite build() {
        return new Campsite(siteNumber, description, maxPeople);
    }
}
