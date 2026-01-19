package com.camping.legacy.acceptance;

import com.camping.legacy.acceptance.fixture.CampsiteFixture;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

public abstract class AcceptanceTestBase {

    @LocalServerPort
    protected int port;

    @Autowired
    private DatabaseCleanup databaseCleanup;

    @Autowired
    private CampsiteFixture campsiteFixture;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;

        // 1. DB 초기화 (INFORMATION_SCHEMA로 모든 테이블 자동 TRUNCATE)
        databaseCleanup.execute();

        // 2. 테스트 데이터 세팅 (SQL 대신 자바 코드로 캡슐화)
        campsiteFixture.setUp();
    }
}