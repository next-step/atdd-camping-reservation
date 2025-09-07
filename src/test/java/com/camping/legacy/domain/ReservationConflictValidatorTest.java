package com.camping.legacy.domain;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.camping.legacy.repository.CampsiteRepository;
import com.camping.legacy.repository.ReservationRepository;
import com.camping.legacy.test.IntegrationTest;
import java.time.LocalDate;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

class ReservationConflictValidatorTest extends IntegrationTest {

    @Autowired
    private ReservationConflictValidator reservationConflictValidator;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private CampsiteRepository campsiteRepository;

    private LocalDate today = LocalDate.now();
    private Campsite campsite;

    @BeforeEach
    void setUp() {
        campsite = campsiteRepository.save(new CampsiteFixture().build());

        reservationRepository.save(
            new ReservationFixture()
                .campsite(campsite)
                .startDate(today)
                .endDate(today.plusDays(1))
                .build()
        );
    }

    @Nested
    class validateNoConflict {

        @Test
        void 예약_기간이_겹치지_않는_경우_검증에_통과한다() {
            assertThatNoException().isThrownBy(() ->
                reservationConflictValidator.validateNoConflict(
                    campsite,
                    today.plusDays(2),
                    today.plusDays(3)
                )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("conflictTestArguments")
        void 예약_기간이_겹치는_경우_검증에_실패한다(String testName, LocalDate startDate, LocalDate endDate) {
            assertThatThrownBy(() ->
                reservationConflictValidator.validateNoConflict(campsite, startDate, endDate)
            ).isInstanceOf(RuntimeException.class)
                .hasMessage("해당 기간에 이미 예약이 존재합니다.");
        }

        private static Stream<Arguments> conflictTestArguments() {
            LocalDate today = LocalDate.now();
            return Stream.of(
                Arguments.of("기존 예약 기간 시작일에 끝나는 경우", today.minusDays(1), today),
                Arguments.of("기존 예약 기간에 포함되는_경우", today, today),
                Arguments.of("기존 예약 기간 종료일에 시작되는 경우", today.plusDays(1), today.plusDays(2))
            );
        }
    }

    @Nested
    class validateNoConflictExcluding {

        // 기존 예약  : today    ~  today+1
        // 제외할 예약:             today+1  ~  today+2
        Reservation reservationToBeExcluded = null;

        @BeforeEach
        void setUp() {
            reservationToBeExcluded = reservationRepository.save(
                new ReservationFixture()
                    .campsite(campsite)
                    .startDate(today.plusDays(1))
                    .endDate(today.plusDays(2))
                    .build()
            );
        }

        @Test
        void 제외할_예약과_겹치지만_다른_예약과는_겹치지_않는_경우_검증에_통과한다() {
            // when & then
            assertThatNoException().isThrownBy(() ->
                reservationConflictValidator.validateNoConflictExcluding(
                    campsite,
                    reservationToBeExcluded,
                    today.plusDays(2),
                    today.plusDays(3)
                )
            );
        }

        @Test
        void 제외할_예약과_다른_예약과_겹치지는_경우_검증에_실패한다() {
            // when & then
            assertThatThrownBy(() ->
                reservationConflictValidator.validateNoConflictExcluding(
                    campsite,
                    reservationToBeExcluded,
                    today.plusDays(1),
                    today.plusDays(2)
                )
            ).isInstanceOf(RuntimeException.class)
                .hasMessage("해당 기간에 이미 예약이 존재합니다.");
        }
    }
}
