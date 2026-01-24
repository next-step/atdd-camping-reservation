package com.camping.legacy.fixture;

import com.camping.legacy.repository.CampsiteRepository;

import java.util.List;

import static com.camping.legacy.builder.CampsiteBuilder.aCampsite;

public class CampsiteFixture {

    public static void 기본_사이트_생성(CampsiteRepository repository) {
        repository.saveAll(List.of(
                aCampsite().largeSite("1").build(),
                aCampsite().largeSite("2").build()
        ));
    }

    public static void 전체_사이트_생성(CampsiteRepository repository) {
        repository.saveAll(List.of(
                aCampsite().largeSite("1").build(),
                aCampsite().largeSite("2").build(),
                aCampsite().largeSite("3").build(),
                aCampsite().smallSite("1").build(),
                aCampsite().smallSite("2").build()
        ));
    }
}
