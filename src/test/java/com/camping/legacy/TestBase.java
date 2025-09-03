package com.camping.legacy;


import com.camping.legacy.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

//@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD) -> 현재 단계에서는 필요없고, 속도를 느리게 하기 때문에 주석 처리
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
