package com.camping.legacy.domain;

import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Setter
class CampsiteFixture {

    private String siteNumber = "A-1";
    private String description = "대형 사이트 - 전기 있음, 화장실 인근";
    private Integer maxPeople = 6;

    public Campsite build() {
        return new Campsite(siteNumber, description, maxPeople);
    }
}
