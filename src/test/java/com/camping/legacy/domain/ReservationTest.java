package com.camping.legacy.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class ReservationTest {

    private Campsite campsite;

    @BeforeEach
    void setUp() {
        campsite = new CampsiteFixture().build();
    }

    @Nested
    @DisplayName("생성자")
    class constructor {

        @Test
        void 예약이_확정된_객체가_생성된다() {
            // when
            var result = Reservation.builder()
                .customerName("테스터")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now())
                .campsite(campsite)
                .phoneNumber("010-1111-2222")
                .build();

            // then
            assertThat(result.getCustomerName()).isEqualTo("테스터");
            assertThat(result.getStartDate()).isEqualTo(LocalDate.now());
            assertThat(result.getEndDate()).isEqualTo(LocalDate.now());
            assertThat(result.getCampsite()).isEqualTo(campsite);
            assertThat(result.getPhoneNumber()).isEqualTo("010-1111-2222");
            assertThat(result.getStatus()).isEqualTo("CONFIRMED");
        }

        @ParameterizedTest
        @NullAndEmptySource
        void 예약자_이름이_필수값이다(String customerName) {
            // when & then
            assertThatThrownBy(() ->
                Reservation.builder()
                    .customerName(customerName) // <==
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(2))
                    .campsite(campsite)
                    .phoneNumber("010-1111-2222")
                    .build()
            ).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("예약자 이름을 입력해주세요.");
        }

        @ParameterizedTest
        @NullAndEmptySource
        void 예약자_전화번호가_필수값이다(String phoneNumber) {
            // when & then
            assertThatThrownBy(() ->
                Reservation.builder()
                    .customerName("테스터")
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(2))
                    .campsite(campsite)
                    .phoneNumber(phoneNumber) // <==
                    .build()
            ).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("예약자 전화번호를 입력해주세요.");
        }

        @Test
        @DisplayName("과거 날짜로 예약이 불가능하다")
        void 과거_날짜로_예약이_불가능하다() {
            // when & then
            assertThatThrownBy(() ->
                Reservation.builder()
                    .customerName("테스터")
                    .startDate(LocalDate.now().minusDays(1)) // <==
                    .endDate(LocalDate.now())
                    .campsite(campsite)
                    .phoneNumber("010-1111-2222")
                    .build()
            ).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("예약 기간은 오늘 이후로 선택해주세요.");
        }

        @Test
        void 종료일이_시작일보다_이전일_수_없다() {
            // when & then
            assertThatThrownBy(() ->
                Reservation.builder()
                    .customerName("테스터")
                    .startDate(LocalDate.now().plusDays(2)) // <==
                    .endDate(LocalDate.now().plusDays(1)) // <==
                    .campsite(campsite)
                    .phoneNumber("010-1111-2222")
                    .build()
            ).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("종료일이 시작일보다 이전일 수 없습니다.");
        }

        @Nested
        class 예약은_오늘로부터_30일_이내에만_가능하다 {

            @Test
            void 예약을_30일_내로_하는_경우_예약이_가능하다() {
                // when & then
                assertThatNoException().isThrownBy(() -> Reservation.builder()
                    .customerName("테스터")
                    .startDate(LocalDate.now().plusDays(29)) // <==
                    .endDate(LocalDate.now().plusDays(30)) // <==
                    .campsite(campsite)
                    .phoneNumber("010-1111-2222")
                    .build());
            }

            @Test
            void 예약을_30일_이후로_하는_경우_예약이_불가능하다() {
                // when & then
                assertThatThrownBy(() ->
                    Reservation.builder()
                        .customerName("테스터")
                        .startDate(LocalDate.now().plusDays(30)) // <==
                        .endDate(LocalDate.now().plusDays(31)) // <==
                        .campsite(campsite)
                        .phoneNumber("010-1111-2222")
                        .build()
                ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("예약 기간은 오늘로부터 30일 이내에만 가능합니다.");
            }
        }

        @Test
        @DisplayName("캠핑장이 필수값이다")
        void 캠핑장이_필수값이다() {
            // when & then
            assertThatThrownBy(() -> Reservation.builder()
                .customerName("테스터")
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(2))
                .campsite(null) // <==
                .phoneNumber("010-1111-2222")
                .build()
            ).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("캠핑장을 선택해주세요.");
        }
    }

    @Nested
    @DisplayName("update")
    class update {

        private Reservation reservation;
        private Campsite newCampsite;

        @BeforeEach
        void setUp() {
            reservation = Reservation.builder()
                .customerName("원본고객")
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(2))
                .campsite(campsite)
                .phoneNumber("010-1111-2222")
                .build();
            reservation.setConfirmationCode("ABC123");

            newCampsite = new CampsiteFixture().siteNumber("B-1").build();
        }

        @Test
        void 예약이_내역이_수정된다() {
            // when
            reservation.update(
                "ABC123",
                newCampsite,
                LocalDate.now().plusDays(3),
                LocalDate.now().plusDays(4),
                "수정된고객",
                "010-9999-8888"
            );

            // then
            assertThat(reservation.getCampsite()).isEqualTo(newCampsite);
            assertThat(reservation.getStartDate()).isEqualTo(LocalDate.now().plusDays(3));
            assertThat(reservation.getEndDate()).isEqualTo(LocalDate.now().plusDays(4));
            assertThat(reservation.getCustomerName()).isEqualTo("수정된고객");
            assertThat(reservation.getPhoneNumber()).isEqualTo("010-9999-8888");
        }

        @Test
        void 확인코드가_일치하지_않으면_수정할_수_없다() {
            // when & then
            assertThatThrownBy(() ->
                reservation.update(
                    "WRONG123", // 잘못된 확인코드
                    newCampsite,
                    LocalDate.now().plusDays(3),
                    LocalDate.now().plusDays(4),
                    "수정된고객",
                    "010-9999-8888"
                )
            ).isInstanceOf(RuntimeException.class)
                .hasMessage("확인 코드가 일치하지 않습니다.");
        }

        @ParameterizedTest
        @NullAndEmptySource
        void 예약자_이름이_필수값이다(String customerName) {
            // when & then
            assertThatThrownBy(() ->
                reservation.update(
                    "ABC123",
                    newCampsite,
                    LocalDate.now().plusDays(3),
                    LocalDate.now().plusDays(4),
                    customerName, // <==
                    "010-9999-8888"
                )
            ).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("예약자 이름을 입력해주세요.");
        }

        @ParameterizedTest
        @NullAndEmptySource
        void 예약자_전화번호가_필수값이다(String phoneNumber) {
            // when & then
            assertThatThrownBy(() ->
                reservation.update(
                    "ABC123",
                    newCampsite,
                    LocalDate.now().plusDays(3),
                    LocalDate.now().plusDays(4),
                    "수정된고객",
                    phoneNumber // <==
                )
            ).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("예약자 전화번호를 입력해주세요.");
        }

        @Test
        @DisplayName("캠핑장은 필수값이다")
        void 캠핑장은_필수값이다() {
            // when & then
            assertThatThrownBy(() ->
                reservation.update(
                    "ABC123",
                    null, // 빈 캠핑장
                    LocalDate.now().plusDays(3),
                    LocalDate.now().plusDays(4),
                    "수정된고객",
                    "010-9999-8888"
                )
            ).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("캠핑장을 선택해주세요.");
        }

        @Test
        @DisplayName("과거 날짜로 수정할 수 없다")
        void 과거_날짜로_수정할_수_없다() {
            // when & then
            assertThatThrownBy(() ->
                reservation.update(
                    "ABC123",
                    newCampsite,
                    LocalDate.now().minusDays(1), // <==
                    LocalDate.now(),
                    "수정된고객",
                    "010-9999-8888"
                )
            ).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("예약 기간은 오늘 이후로 선택해주세요.");
        }

        @Test
        @DisplayName("종료일이 시작일보다 이전일 수 없다")
        void 종료일이_시작일보다_이전일_수_없다() {
            // when & then
            assertThatThrownBy(() ->
                reservation.update(
                    "ABC123",
                    newCampsite,
                    LocalDate.now().plusDays(4), // <==
                    LocalDate.now().plusDays(3), // <==
                    "수정된고객",
                    "010-9999-8888"
                )
            ).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("종료일이 시작일보다 이전일 수 없습니다.");
        }

        @Nested
        class 예약_수정은_오늘로부터_30일_이내에만_가능하다 {

            @Test
            void 예약_수정을_30일_내로_하는_경우_수정이_가능하다() {
                // when & then
                assertThatNoException().isThrownBy(() ->
                    reservation.update(
                        "ABC123",
                        newCampsite,
                        LocalDate.now().plusDays(29), // <==
                        LocalDate.now().plusDays(30), // <==
                        "수정된고객",
                        "010-9999-8888"
                    )
                );
            }

            @Test
            void 예약_수정을_30일_이후로_하는_경우_수정이_불가능하다() {
                // when & then
                assertThatThrownBy(() ->
                    reservation.update(
                        "ABC123",
                        newCampsite,
                        LocalDate.now().plusDays(30), // <==
                        LocalDate.now().plusDays(31), // <==
                        "수정된고객",
                        "010-9999-8888"
                    )
                ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("예약 기간은 오늘로부터 30일 이내에만 가능합니다.");
            }
        }
    }
}
