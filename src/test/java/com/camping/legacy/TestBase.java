package com.camping.legacy;


import com.camping.legacy.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class TestBase {

    @LocalServerPort
    protected int port;

    @Autowired
    ReservationRepository reservationRepository;

    protected void clearDB() {
        reservationRepository.deleteAll();
    }
}
