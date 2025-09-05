package com.camping.legacy.test;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.web.server.LocalServerPort;

public abstract class AcceptanceTest extends IntegrationTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void acceptanceTestSetup() {
        if (RestAssured.requestSpecification != null) {
            return;
        }

        RestAssured.requestSpecification = new RequestSpecBuilder()
            .setPort(this.port)
            .setContentType(ContentType.JSON)
            .addFilters(
                List.of(
                    new RequestLoggingFilter(),
                    new ResponseLoggingFilter()
                )
            )
            .build();
    }
}
