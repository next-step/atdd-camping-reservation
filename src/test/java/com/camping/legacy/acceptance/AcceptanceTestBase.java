package com.camping.legacy.acceptance;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * 인수 테스트(ATDD) 스켈레톤 베이스.
 *
 * - Spring Boot를 테스트 환경에서 RANDOM_PORT로 실행
 * - RestAssured가 해당 포트로 요청을 보내도록 설정
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AcceptanceTestBase {

    @LocalServerPort
    int port;

    @BeforeEach
    void setUpRestAssured() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }
}
