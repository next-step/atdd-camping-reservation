package com.camping.legacy.service;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.dto.ReservationRequest;
import com.camping.legacy.dto.ReservationResponse;
import com.camping.legacy.fake.FakeCampsiteRepository;
import com.camping.legacy.fake.FakeReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ReservationService 단위 테스트")
class ReservationServiceTest {

    private ReservationService reservationService;
    private FakeCampsiteRepository campsiteRepository;
    private FakeReservationRepository reservationRepository;

    private static final LocalDate NOW = LocalDate.of(2030, 2, 1);

    @BeforeEach
    void setUp() {
        campsiteRepository = new FakeCampsiteRepository();
        reservationRepository = new FakeReservationRepository();
        reservationService = new ReservationService(reservationRepository, campsiteRepository);

        campsiteRepository.save(new Campsite("A-1", "대형 사이트", 8));
        campsiteRepository.save(new Campsite("B-1", "소형 사이트", 4));
    }

    @Test
    @DisplayName("정상 - 유효한 예약 생성")
    void createValidReservation() {
        ReservationRequest request = new ReservationRequest(
                "홍길동",
                LocalDate.of(2030, 2, 5),
                LocalDate.of(2030, 2, 7),
                "A-1", "010-1234-5678",
                null, null, null
        );

        ReservationResponse response = reservationService.createReservation(request, NOW);

        assertThat(response.getCustomerName()).isEqualTo("홍길동");
        assertThat(response.getStartDate()).isEqualTo(LocalDate.of(2030, 2, 5));
        assertThat(response.getEndDate()).isEqualTo(LocalDate.of(2030, 2, 7));
        assertThat(response.getSiteNumber()).isEqualTo("A-1");
        assertThat(response.getConfirmationCode()).hasSize(6);
        assertThat(response.getStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("정상 - 30일째 되는 날 예약 가능")
    void createReservationExactly30Days() {
        ReservationRequest request = new ReservationRequest(
                "홍길동",
                LocalDate.of(2030, 3, 3),
                LocalDate.of(2030, 3, 3),
                "A-1", "010-1234-5678",
                null, null, null
        );

        ReservationResponse response = reservationService.createReservation(request, NOW);

        assertThat(response.getConfirmationCode()).hasSize(6);
    }

    @Test
    @DisplayName("실패 - 30일 이후 예약 불가")
    void failWhenBeyond30Days() {
        ReservationRequest request = new ReservationRequest(
                "홍길동",
                LocalDate.of(2030, 3, 4),
                LocalDate.of(2030, 3, 6),
                "A-1", "010-1234-5678",
                null, null, null
        );

        assertThatThrownBy(() -> reservationService.createReservation(request, NOW))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("오늘로부터 30일 이내에만 예약 가능합니다.");
    }

    @Test
    @DisplayName("실패 - 과거 날짜 예약 불가")
    void failWhenPastDate() {
        ReservationRequest request = new ReservationRequest(
                "홍길동",
                LocalDate.of(2030, 1, 30),
                LocalDate.of(2030, 1, 31),
                "A-1", "010-1234-5678",
                null, null, null
        );

        assertThatThrownBy(() -> reservationService.createReservation(request, NOW))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("과거 날짜로 예약할 수 없습니다.");
    }

    @Test
    @DisplayName("실패 - 종료일이 시작일보다 이전")
    void failWhenEndDateBeforeStartDate() {
        ReservationRequest request = new ReservationRequest(
                "홍길동",
                LocalDate.of(2030, 2, 10),
                LocalDate.of(2030, 2, 5),
                "A-1", "010-1234-5678",
                null, null, null
        );

        assertThatThrownBy(() -> reservationService.createReservation(request, NOW))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("종료일이 시작일보다 이전일 수 없습니다.");
    }

    @Test
    @DisplayName("실패 - 기간 중복 예약 불가")
    void failWhenOverlapping() {
        ReservationRequest first = new ReservationRequest(
                "홍길동",
                LocalDate.of(2030, 2, 5),
                LocalDate.of(2030, 2, 7),
                "A-1", "010-1234-5678",
                null, null, null
        );
        reservationService.createReservation(first, NOW);

        ReservationRequest second = new ReservationRequest(
                "김철수",
                LocalDate.of(2030, 2, 6),
                LocalDate.of(2030, 2, 8),
                "A-1", "010-2222-3333",
                null, null, null
        );

        assertThatThrownBy(() -> reservationService.createReservation(second, NOW))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("해당 기간에 이미 예약이 존재합니다.");
    }

    @Test
    @DisplayName("실패 - 존재하지 않는 사이트")
    void failWhenSiteNotFound() {
        ReservationRequest request = new ReservationRequest(
                "홍길동",
                LocalDate.of(2030, 2, 5),
                LocalDate.of(2030, 2, 7),
                "Z-99", "010-1234-5678",
                null, null, null
        );

        assertThatThrownBy(() -> reservationService.createReservation(request, NOW))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("존재하지 않는 캠핑장입니다.");
    }

    @Test
    @DisplayName("실패 - 날짜 미입력")
    void failWhenDatesNull() {
        ReservationRequest request = new ReservationRequest(
                "홍길동", null, null,
                "A-1", "010-1234-5678",
                null, null, null
        );

        assertThatThrownBy(() -> reservationService.createReservation(request, NOW))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("예약 기간을 선택해주세요.");
    }

    @Test
    @DisplayName("실패 - 사이트 번호 미입력")
    void failWhenSiteNumberEmpty() {
        ReservationRequest request = new ReservationRequest(
                "홍길동",
                LocalDate.of(2030, 2, 5),
                LocalDate.of(2030, 2, 7),
                "", "010-1234-5678",
                null, null, null
        );

        assertThatThrownBy(() -> reservationService.createReservation(request, NOW))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("사이트 번호를 입력해주세요.");
    }

    @Test
    @DisplayName("실패 - 예약자 이름 미입력")
    void failWhenCustomerNameEmpty() {
        ReservationRequest request = new ReservationRequest(
                "",
                LocalDate.of(2030, 2, 5),
                LocalDate.of(2030, 2, 7),
                "A-1", "010-1234-5678",
                null, null, null
        );

        assertThatThrownBy(() -> reservationService.createReservation(request, NOW))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("예약자 이름을 입력해주세요.");
    }

    @Test
    @DisplayName("실패 - 예약자 이름 2자 미만")
    void failWhenCustomerNameTooShort() {
        ReservationRequest request = new ReservationRequest(
                "홍",
                LocalDate.of(2030, 2, 5),
                LocalDate.of(2030, 2, 7),
                "A-1", "010-1234-5678",
                null, null, null
        );

        assertThatThrownBy(() -> reservationService.createReservation(request, NOW))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("예약자 이름은 최소 2자 이상이어야 합니다.");
    }

    @Test
    @DisplayName("정상 - 다른 사이트에는 같은 기간 예약 가능")
    void createReservationOnDifferentSite() {
        ReservationRequest first = new ReservationRequest(
                "홍길동",
                LocalDate.of(2030, 2, 5),
                LocalDate.of(2030, 2, 7),
                "A-1", "010-1234-5678",
                null, null, null
        );
        reservationService.createReservation(first, NOW);

        ReservationRequest second = new ReservationRequest(
                "김철수",
                LocalDate.of(2030, 2, 5),
                LocalDate.of(2030, 2, 7),
                "B-1", "010-2222-3333",
                null, null, null
        );

        ReservationResponse response = reservationService.createReservation(second, NOW);

        assertThat(response.getSiteNumber()).isEqualTo("B-1");
    }
}
