package com.camping.legacy.controller;

import com.camping.legacy.DatabaseCleaner;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AcceptanceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        databaseCleaner.clean();
    }

    protected ValidatableResponse post(String url, Map<String, Object> body) {
        return RestAssured.given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .when().post(url)
                .then().log().all();
    }

    protected ValidatableResponse get(String url) {
        return RestAssured.given().log().all()
                .when().get(url)
                .then().log().all();
    }

    protected ValidatableResponse get(String url, Map<String, ?> queryParams) {
        return RestAssured.given().log().all()
                .queryParams(queryParams)
                .when().get(url)
                .then().log().all();
    }

    protected ValidatableResponse delete(String url, Map<String, ?> queryParams) {
        return RestAssured.given().log().all()
                .queryParams(queryParams)
                .when().delete(url)
                .then().log().all();
    }

    protected ValidatableResponse put(String url, Map<String, Object> body, Map<String, ?> queryParams) {
        return RestAssured.given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .queryParams(queryParams)
                .body(body)
                .when().put(url)
                .then().log().all();
    }
}

