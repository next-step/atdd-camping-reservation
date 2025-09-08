package com.camping.legacy.acceptance;

import com.camping.legacy.DatabaseCleanup;
import com.camping.legacy.config.TimeProvider;
import io.restassured.RestAssured;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class AcceptanceTestBase {
    public static final LocalDate TODAY = LocalDate.of(2025, 9, 7);

    private static final String BASE_URI = "http://localhost";

    @LocalServerPort
    private int port;

    @Autowired
    private DatabaseCleanup databaseCleanup;

    @Autowired
    private TimeProvider timeProvider;


    @BeforeEach
    void setUp() {
        RestAssured.baseURI = BASE_URI;
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // 테스트용 고정 시간 설정
        timeProvider.setClock(Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC));

        databaseCleanup.execute();
    }
}
