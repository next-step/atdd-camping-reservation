package com.camping.legacy;

import static com.camping.legacy.fixture.ReservationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.fixture.ReservationRequestBuilder;
import com.camping.legacy.repository.CampsiteRepository;
import com.camping.legacy.service.ReservationService;
import com.camping.legacy.utils.ConcurrencyTestHelper;
import com.camping.legacy.utils.DatabaseCleaner;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
public class ReservationServiceConcurrencyTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private CampsiteRepository campsiteRepository;

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @BeforeEach
    void setUp() {
        databaseCleaner.execute();

        campsiteRepository.save(new Campsite(SITE_A1, "Large Site", 6));
    }

    @DisplayName("동일한 사이트와 기간에 대해 중복 예약 시도를 하면 한명만 예약된다 (동시성 제어)")
    @Test
    void 동일한_사이트와_기간에_대해_중복_예약_시도를_하면_한명만_예약된다() throws InterruptedException {
        // given
        AtomicInteger successCount = new AtomicInteger(0);
        var request = ReservationRequestBuilder.aReservationRequest()
                .withStartDate(10)
                .withEndDate(11);

        // when
        ConcurrencyTestHelper.execute(
            () -> {
                try {
                    reservationService.createReservation(request.build());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // ignore
                }
            },
            () -> {
                try {
                    reservationService.createReservation(request.withCustomerName("홍길동").build());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // ignore
                }
            },
            () -> {
                try {
                    reservationService.createReservation(request.withCustomerName("이순신").build());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // ignore
                }
            }
        );

        // then
        assertThat(successCount.get()).isEqualTo(1);
    }
}
