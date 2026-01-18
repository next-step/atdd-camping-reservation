package com.camping.legacy.acceptance;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.repository.CampsiteRepository;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@SuppressWarnings("NonAsciiCharacters")
public abstract class AcceptanceTestBase {

    protected static final String 예약_API = "/api/reservations";
    protected static final String 사이트_예약_가능_여부_API = "/api/sites/%s/availability";
    protected static final String 예약_가능_목록_조회_API = "/api/sites/available";
    protected static final String 검색_API = "/api/sites/search";

    @LocalServerPort
    int port;

    @Autowired
    private DatabaseCleanup databaseCleanup;

    @Autowired
    private CampsiteRepository campsiteRepository;

    @BeforeEach
    void setUpRestAssured() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        databaseCleanup.execute();
    }

    protected void 사이트를_생성한다(String siteNumber) {
        campsiteRepository.save(new Campsite(siteNumber, "테스트용 사이트", 4));
    }
}
