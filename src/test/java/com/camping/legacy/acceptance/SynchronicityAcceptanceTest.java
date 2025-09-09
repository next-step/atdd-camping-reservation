package com.camping.legacy.acceptance;

import com.camping.legacy.acceptance.fixture.ReservationRequestFixture;
import com.camping.legacy.dto.ReservationRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment =  SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(statements = {
        "TRUNCATE TABLE reservations",
        "ALTER TABLE reservations ALTER COLUMN id RESTART WITH 1"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class SynchronicityAcceptanceTest {

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    @DisplayName("여러 사용자가 동시에 같은 사이트를 예약하면 하나의 예약만 잘 성공하는지")
    @Test
    @Timeout(10)
    void reservationSynchronicityTest() throws InterruptedException {
        //when: 서른 명의 사용자가 동일한 캠핑 사이트와 날짜로 예약을 시도한다
        final int userCount = 30;
        final String siteNumber = "A-1";
        final LocalDate start = LocalDate.now();
        final LocalDate end = start.plusDays(1);

        final CyclicBarrier barrier = new CyclicBarrier(userCount);

        final ExecutorService pool = Executors.newFixedThreadPool(userCount);

        try {
            var tasks = IntStream.range(0, userCount)
                    .mapToObj(i -> (Callable<Integer>) () -> {
                        ReservationRequest req = ReservationRequestFixture.builder()
                                .siteNumber(siteNumber)
                                .startDate(start)
                                .endDate(end)
                                .customerName("TEST-" + (i + 1))
                                .build();

                        barrier.await();

                        return RestAssured.given()
                                .contentType(ContentType.JSON)
                                .body(req)
                                .when()
                                .post("/api/reservations")
                                .then()
                                .extract()
                                .statusCode();
                    })
                    .toList();

            var futures = pool.invokeAll(tasks);
            long created = 0, conflict = 0, other = 0;

            for (var f : futures) {
                try {
                    int code = f.get();
                    if (code == 201) created++;
                    else if (code == 409) conflict++;
                    else other++;
                } catch (Exception e) {
                    other++;
                }
            }

            //then: 하나의 예약만 성공하고 나머지 29개의 예약은 실패한다
            assertThat(created).as("성공(201)은 정확히 1건").isEqualTo(1);
            assertThat(conflict).as("실패(409)는 나머지 전부(29건)").isEqualTo(userCount - 1);
            assertThat(other).as("예상 밖 상태코드/예외").isZero();
        } finally {
            pool.shutdownNow();
        }
    }
}
