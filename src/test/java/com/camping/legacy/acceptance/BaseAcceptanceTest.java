package com.camping.legacy.acceptance;

import com.camping.legacy.repository.ReservationRepository;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BaseAcceptanceTest {
    
    @LocalServerPort
    protected int port;
    
    @Autowired
    protected ReservationRepository reservationRepository;
    
    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        
        clearTestData();
    }
    
    protected void clearTestData() {
        reservationRepository.deleteAll();
    }
}
