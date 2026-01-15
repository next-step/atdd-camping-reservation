package com.camping.legacy.acceptance;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.web.server.LocalServerPort;

public abstract class AcceptanceTestBase {

    @LocalServerPort
    protected int port;

    @BeforeEach
    void setUpRestAssuredPort() {
        RestAssured.port = port;
    }
}
