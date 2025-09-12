package com.camping.legacy.acceptance;

import com.camping.legacy.common.ClockProvider;
import com.camping.legacy.config.TestClockConfiguration;
import com.camping.legacy.repository.ReservationRepository;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestClockConfiguration.class)
@ActiveProfiles("test")
class BaseAcceptanceTest {
    
    @LocalServerPort
    protected int port;
    
    @Autowired
    protected ReservationRepository reservationRepository;
    
    @Autowired
    protected ClockProvider clockProvider;
    
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
