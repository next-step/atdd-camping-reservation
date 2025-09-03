package com.camping.legacy.acceptance;

import com.camping.legacy.DataInitializer;
import com.camping.legacy.DatabaseCleanup;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AcceptanceTestBase {

    private static final String BASE_URI = "http://localhost";

    @LocalServerPort
    private int port;

    @Autowired
    private DatabaseCleanup databaseCleanup;

    @Autowired
    private DataInitializer dataInitializer;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = BASE_URI;
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        databaseCleanup.execute();
        dataInitializer.execute();
    }
}
