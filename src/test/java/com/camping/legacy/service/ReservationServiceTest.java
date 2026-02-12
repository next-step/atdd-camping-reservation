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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

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
    @DisplayName("정상 - 오늘로부터 30일째 되는 날 예약 가능")
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
    @DisplayName("실패 - 오늘로부터 31일 이후 예약 불가")
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
    @DisplayName("정상 - 당일 예약 가능 (startDate == now)")
    void createReservationForToday() {
        ReservationRequest request = new ReservationRequest(
                "홍길동",
                LocalDate.of(2030, 2, 1),
                LocalDate.of(2030, 2, 3),
                "A-1", "010-1234-5678",
                null, null, null
        );

        ReservationResponse response = reservationService.createReservation(request, NOW);

        assertThat(response.getStartDate()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("실패 - 하루 전 과거 날짜 (startDate == now - 1)")
    void failWhenStartDateIsYesterday() {
        ReservationRequest request = new ReservationRequest(
                "홍길동",
                LocalDate.of(2030, 1, 31),
                LocalDate.of(2030, 2, 2),
                "A-1", "010-1234-5678",
                null, null, null
        );

        assertThatThrownBy(() -> reservationService.createReservation(request, NOW))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("과거 날짜로 예약할 수 없습니다.");
    }

    @Test
    @DisplayName("실패 - 오늘로부터 정확히 31일 뒤 예약 불가 (경계값)")
    void failWhenStartDate31DaysLater() {
        ReservationRequest request = new ReservationRequest(
                "홍길동",
                LocalDate.of(2030, 3, 4),
                LocalDate.of(2030, 3, 4),
                "A-1", "010-1234-5678",
                null, null, null
        );

        assertThatThrownBy(() -> reservationService.createReservation(request, NOW))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("오늘로부터 30일 이내에만 예약 가능합니다.");
    }

    @Test
    @DisplayName("정상 - 총 예약 기간 정확히 30일 (경계값)")
    void createReservationWithExactly30DaysPeriod() {
        ReservationRequest request = new ReservationRequest(
                "홍길동",
                LocalDate.of(2030, 2, 1),
                LocalDate.of(2030, 3, 3),
                "A-1", "010-1234-5678",
                null, null, null
        );

        ReservationResponse response = reservationService.createReservation(request, NOW);

        assertThat(response.getConfirmationCode()).hasSize(6);
    }

    @Test
    @DisplayName("실패 - 총 예약 기간 31일 초과 (경계값)")
    void failWhenTotalPeriodExceeds30Days() {
        ReservationRequest request = new ReservationRequest(
                "홍길동",
                LocalDate.of(2030, 2, 1),
                LocalDate.of(2030, 3, 4),
                "A-1", "010-1234-5678",
                null, null, null
        );

        assertThatThrownBy(() -> reservationService.createReservation(request, NOW))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("총 예약 기간은 30일을 초과할 수 없습니다.");
    }

    @Test
    @DisplayName("정상 - now가 달라지면 같은 날짜도 예약 가능")
    void createReservationWithDifferentNow() {
        LocalDate laterNow = LocalDate.of(2030, 3, 1);
        ReservationRequest request = new ReservationRequest(
                "홍길동",
                LocalDate.of(2030, 3, 4),
                LocalDate.of(2030, 3, 6),
                "A-1", "010-1234-5678",
                null, null, null
        );

        ReservationResponse response = reservationService.createReservation(request, laterNow);

        assertThat(response.getStartDate()).isEqualTo(LocalDate.of(2030, 3, 4));
    }

    @Test
    @DisplayName("실패 - startDate와 endDate가 같은 날인데 endDate가 이전 (같은 날은 허용)")
    void createReservationSameDay() {
        ReservationRequest request = new ReservationRequest(
                "홍길동",
                LocalDate.of(2030, 2, 5),
                LocalDate.of(2030, 2, 5),
                "A-1", "010-1234-5678",
                null, null, null
        );

        ReservationResponse response = reservationService.createReservation(request, NOW);

        assertThat(response.getStartDate()).isEqualTo(response.getEndDate());
    }

    @Test
    @DisplayName("실패 - endDate만 과거 (startDate 유효, endDate < startDate)")
    void failWhenEndDateBeforeStartDateByOneDay() {
        ReservationRequest request = new ReservationRequest(
                "홍길동",
                LocalDate.of(2030, 2, 10),
                LocalDate.of(2030, 2, 9),
                "A-1", "010-1234-5678",
                null, null, null
        );

        assertThatThrownBy(() -> reservationService.createReservation(request, NOW))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("종료일이 시작일보다 이전일 수 없습니다.");
    }

    @Test
    @DisplayName("실패 - startDate null, endDate 유효")
    void failWhenStartDateNull() {
        ReservationRequest request = new ReservationRequest(
                "홍길동",
                null,
                LocalDate.of(2030, 2, 7),
                "A-1", "010-1234-5678",
                null, null, null
        );

        assertThatThrownBy(() -> reservationService.createReservation(request, NOW))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("예약 기간을 선택해주세요.");
    }

    @Test
    @DisplayName("실패 - startDate 유효, endDate null")
    void failWhenEndDateNull() {
        ReservationRequest request = new ReservationRequest(
                "홍길동",
                LocalDate.of(2030, 2, 5),
                null,
                "A-1", "010-1234-5678",
                null, null, null
        );

        assertThatThrownBy(() -> reservationService.createReservation(request, NOW))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("예약 기간을 선택해주세요.");
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

    // =============================================
    // Null/Empty 엣지 케이스
    // =============================================

    @Test
    @DisplayName("실패 - 사이트 번호 null")
    void failWhenSiteNumberNull() {
        ReservationRequest request = new ReservationRequest(
                "홍길동",
                LocalDate.of(2030, 2, 5),
                LocalDate.of(2030, 2, 7),
                null, "010-1234-5678",
                null, null, null
        );

        assertThatThrownBy(() -> reservationService.createReservation(request, NOW))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("사이트 번호를 입력해주세요.");
    }

    @Test
    @DisplayName("실패 - 사이트 번호 공백만 입력")
    void failWhenSiteNumberBlank() {
        ReservationRequest request = new ReservationRequest(
                "홍길동",
                LocalDate.of(2030, 2, 5),
                LocalDate.of(2030, 2, 7),
                "   ", "010-1234-5678",
                null, null, null
        );

        assertThatThrownBy(() -> reservationService.createReservation(request, NOW))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("사이트 번호를 입력해주세요.");
    }

    @Test
    @DisplayName("실패 - 예약자 이름 null")
    void failWhenCustomerNameNull() {
        ReservationRequest request = new ReservationRequest(
                null,
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
    @DisplayName("실패 - 예약자 이름 공백만 입력")
    void failWhenCustomerNameBlank() {
        ReservationRequest request = new ReservationRequest(
                "   ",
                LocalDate.of(2030, 2, 5),
                LocalDate.of(2030, 2, 7),
                "A-1", "010-1234-5678",
                null, null, null
        );

        assertThatThrownBy(() -> reservationService.createReservation(request, NOW))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("예약자 이름을 입력해주세요.");
    }

    // =============================================
    // 경계값 - 이름 길이
    // =============================================

    @Test
    @DisplayName("정상 - 이름 정확히 2자 (최소 경계)")
    void createReservationWithMinNameLength() {
        ReservationRequest request = new ReservationRequest(
                "홍길",
                LocalDate.of(2030, 2, 5),
                LocalDate.of(2030, 2, 7),
                "A-1", "010-1234-5678",
                null, null, null
        );

        ReservationResponse response = reservationService.createReservation(request, NOW);

        assertThat(response.getCustomerName()).isEqualTo("홍길");
    }

    @Test
    @DisplayName("정상 - 이름 정확히 20자 (최대 경계)")
    void createReservationWithMaxNameLength() {
        String name = "가나다라마바사아자차카타파하갸냐댜랴마";  // 20자
        ReservationRequest request = new ReservationRequest(
                name,
                LocalDate.of(2030, 2, 5),
                LocalDate.of(2030, 2, 7),
                "A-1", "010-1234-5678",
                null, null, null
        );

        ReservationResponse response = reservationService.createReservation(request, NOW);

        assertThat(response.getCustomerName()).isEqualTo(name);
    }

//    @Test
//    @DisplayName("실패 - 이름 21자 (최대 초과)")
//    void failWhenCustomerNameTooLong() {
//        String name = "가나다라마바사아자차카타파하갸냐댜랴마바"; // 21자
//        ReservationRequest request = new ReservationRequest(
//                name,
//                LocalDate.of(2030, 2, 5),
//                LocalDate.of(2030, 2, 7),
//                "A-1", "010-1234-5678",
//                null, null, null
//        );
//
//        assertThatThrownBy(() -> reservationService.createReservation(request, NOW))
//                .isInstanceOf(RuntimeException.class)
//                .hasMessage("예약자 이름은 최대 20자까지 가능합니다.");
//    }

    // =============================================
    // 경계값 - 전화번호
    // =============================================

    @Test
    @DisplayName("정상 - 전화번호 null 허용")
    void createReservationWithNullPhone() {
        ReservationRequest request = new ReservationRequest(
                "홍길동",
                LocalDate.of(2030, 2, 5),
                LocalDate.of(2030, 2, 7),
                "A-1", null,
                null, null, null
        );

        ReservationResponse response = reservationService.createReservation(request, NOW);

        assertThat(response.getPhoneNumber()).isNull();
    }

    @Test
    @DisplayName("정상 - 전화번호 빈 문자열 허용")
    void createReservationWithEmptyPhone() {
        ReservationRequest request = new ReservationRequest(
                "홍길동",
                LocalDate.of(2030, 2, 5),
                LocalDate.of(2030, 2, 7),
                "A-1", "",
                null, null, null
        );

        ReservationResponse response = reservationService.createReservation(request, NOW);

        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("정상 - 전화번호 10자리 (하이픈 없이)")
    void createReservationWithPhone10Digits() {
        ReservationRequest request = new ReservationRequest(
                "홍길동",
                LocalDate.of(2030, 2, 5),
                LocalDate.of(2030, 2, 7),
                "A-1", "0101234567",
                null, null, null
        );

        ReservationResponse response = reservationService.createReservation(request, NOW);

        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("정상 - 전화번호 11자리 (하이픈 포함)")
    void createReservationWithPhone11DigitsWithDash() {
        ReservationRequest request = new ReservationRequest(
                "홍길동",
                LocalDate.of(2030, 2, 5),
                LocalDate.of(2030, 2, 7),
                "A-1", "010-1234-5678",
                null, null, null
        );

        ReservationResponse response = reservationService.createReservation(request, NOW);

        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("실패 - 전화번호 9자리 (최소 미만)")
    void failWhenPhoneTooShort() {
        ReservationRequest request = new ReservationRequest(
                "홍길동",
                LocalDate.of(2030, 2, 5),
                LocalDate.of(2030, 2, 7),
                "A-1", "010123456",
                null, null, null
        );

        assertThatThrownBy(() -> reservationService.createReservation(request, NOW))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("전화번호 형식이 올바르지 않습니다.");
    }

    @Test
    @DisplayName("실패 - 전화번호 12자리 (최대 초과)")
    void failWhenPhoneTooLong() {
        ReservationRequest request = new ReservationRequest(
                "홍길동",
                LocalDate.of(2030, 2, 5),
                LocalDate.of(2030, 2, 7),
                "A-1", "010123456789",
                null, null, null
        );

        assertThatThrownBy(() -> reservationService.createReservation(request, NOW))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("전화번호 형식이 올바르지 않습니다.");
    }

    @Test
    @DisplayName("실패 - 전화번호에 문자 포함")
    void failWhenPhoneContainsLetters() {
        ReservationRequest request = new ReservationRequest(
                "홍길동",
                LocalDate.of(2030, 2, 5),
                LocalDate.of(2030, 2, 7),
                "A-1", "010-abcd-5678",
                null, null, null
        );

        assertThatThrownBy(() -> reservationService.createReservation(request, NOW))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("전화번호는 숫자만 입력 가능합니다.");
    }

    // =============================================
    // 중복 데이터 - 예약 겹침 경계
    // =============================================

    @Test
    @DisplayName("실패 - 기존 예약 시작일에 정확히 겹침 (endDate == 기존 startDate)")
    void failWhenNewEndDateEqualsExistingStartDate() {
        reservationService.createReservation(new ReservationRequest(
                "홍길동",
                LocalDate.of(2030, 2, 10),
                LocalDate.of(2030, 2, 12),
                "A-1", "010-1234-5678",
                null, null, null
        ), NOW);

        ReservationRequest request = new ReservationRequest(
                "김철수",
                LocalDate.of(2030, 2, 8),
                LocalDate.of(2030, 2, 10),
                "A-1", "010-2222-3333",
                null, null, null
        );

        assertThatThrownBy(() -> reservationService.createReservation(request, NOW))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("해당 기간에 이미 예약이 존재합니다.");
    }

    @Test
    @DisplayName("실패 - 기존 예약 종료일에 정확히 겹침 (startDate == 기존 endDate)")
    void failWhenNewStartDateEqualsExistingEndDate() {
        reservationService.createReservation(new ReservationRequest(
                "홍길동",
                LocalDate.of(2030, 2, 10),
                LocalDate.of(2030, 2, 12),
                "A-1", "010-1234-5678",
                null, null, null
        ), NOW);

        ReservationRequest request = new ReservationRequest(
                "김철수",
                LocalDate.of(2030, 2, 12),
                LocalDate.of(2030, 2, 14),
                "A-1", "010-2222-3333",
                null, null, null
        );

        assertThatThrownBy(() -> reservationService.createReservation(request, NOW))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("해당 기간에 이미 예약이 존재합니다.");
    }

    @Test
    @DisplayName("정상 - 기존 예약 다음 날부터 시작 (겹치지 않음)")
    void createReservationRightAfterExisting() {
        reservationService.createReservation(new ReservationRequest(
                "홍길동",
                LocalDate.of(2030, 2, 10),
                LocalDate.of(2030, 2, 12),
                "A-1", "010-1234-5678",
                null, null, null
        ), NOW);

        ReservationRequest request = new ReservationRequest(
                "김철수",
                LocalDate.of(2030, 2, 13),
                LocalDate.of(2030, 2, 15),
                "A-1", "010-2222-3333",
                null, null, null
        );

        ReservationResponse response = reservationService.createReservation(request, NOW);

        assertThat(response.getStartDate()).isEqualTo(LocalDate.of(2030, 2, 13));
    }

    @Test
    @DisplayName("정상 - 기존 예약 전날까지 (겹치지 않음)")
    void createReservationRightBeforeExisting() {
        reservationService.createReservation(new ReservationRequest(
                "홍길동",
                LocalDate.of(2030, 2, 10),
                LocalDate.of(2030, 2, 12),
                "A-1", "010-1234-5678",
                null, null, null
        ), NOW);

        ReservationRequest request = new ReservationRequest(
                "김철수",
                LocalDate.of(2030, 2, 5),
                LocalDate.of(2030, 2, 9),
                "A-1", "010-2222-3333",
                null, null, null
        );

        ReservationResponse response = reservationService.createReservation(request, NOW);

        assertThat(response.getEndDate()).isEqualTo(LocalDate.of(2030, 2, 9));
    }

    @Test
    @DisplayName("실패 - 기존 예약을 완전히 포함하는 기간")
    void failWhenNewReservationContainsExisting() {
        reservationService.createReservation(new ReservationRequest(
                "홍길동",
                LocalDate.of(2030, 2, 10),
                LocalDate.of(2030, 2, 12),
                "A-1", "010-1234-5678",
                null, null, null
        ), NOW);

        ReservationRequest request = new ReservationRequest(
                "김철수",
                LocalDate.of(2030, 2, 8),
                LocalDate.of(2030, 2, 14),
                "A-1", "010-2222-3333",
                null, null, null
        );

        assertThatThrownBy(() -> reservationService.createReservation(request, NOW))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("해당 기간에 이미 예약이 존재합니다.");
    }

    @Test
    @DisplayName("실패 - 기존 예약 안에 완전히 포함되는 기간")
    void failWhenNewReservationInsideExisting() {
        reservationService.createReservation(new ReservationRequest(
                "홍길동",
                LocalDate.of(2030, 2, 5),
                LocalDate.of(2030, 2, 15),
                "A-1", "010-1234-5678",
                null, null, null
        ), NOW);

        ReservationRequest request = new ReservationRequest(
                "김철수",
                LocalDate.of(2030, 2, 8),
                LocalDate.of(2030, 2, 10),
                "A-1", "010-2222-3333",
                null, null, null
        );

        assertThatThrownBy(() -> reservationService.createReservation(request, NOW))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("해당 기간에 이미 예약이 존재합니다.");
    }

    // =============================================
    // 날짜 경계 - 월/년 경계
    // =============================================

    @Test
    @DisplayName("정상 - 월 경계를 넘는 예약 (2월 말 ~ 3월 초)")
    void createReservationAcrossMonthBoundary() {
        LocalDate now = LocalDate.of(2030, 2, 20);
        ReservationRequest request = new ReservationRequest(
                "홍길동",
                LocalDate.of(2030, 2, 28),
                LocalDate.of(2030, 3, 2),
                "A-1", "010-1234-5678",
                null, null, null
        );

        ReservationResponse response = reservationService.createReservation(request, now);

        assertThat(response.getStartDate().getMonthValue()).isEqualTo(2);
        assertThat(response.getEndDate().getMonthValue()).isEqualTo(3);
    }

    @Test
    @DisplayName("정상 - 연도 경계를 넘는 예약 (12월 말 ~ 1월 초)")
    void createReservationAcrossYearBoundary() {
        LocalDate now = LocalDate.of(2030, 12, 20);
        ReservationRequest request = new ReservationRequest(
                "홍길동",
                LocalDate.of(2030, 12, 30),
                LocalDate.of(2031, 1, 2),
                "A-1", "010-1234-5678",
                null, null, null
        );

        ReservationResponse response = reservationService.createReservation(request, now);

        assertThat(response.getStartDate().getYear()).isEqualTo(2030);
        assertThat(response.getEndDate().getYear()).isEqualTo(2031);
    }

    @Test
    @DisplayName("정상 - 윤년 2월 29일 포함 예약")
    void createReservationOnLeapYearDay() {
        LocalDate now = LocalDate.of(2032, 2, 20);
        ReservationRequest request = new ReservationRequest(
                "홍길동",
                LocalDate.of(2032, 2, 28),
                LocalDate.of(2032, 3, 1),
                "A-1", "010-1234-5678",
                null, null, null
        );

        ReservationResponse response = reservationService.createReservation(request, now);

        assertThat(response.getStartDate()).isEqualTo(LocalDate.of(2032, 2, 28));
        assertThat(response.getEndDate()).isEqualTo(LocalDate.of(2032, 3, 1));
    }

    // =============================================
    // cancelReservation 엣지 케이스
    // =============================================

    @Test
    @DisplayName("취소 - 정상 취소 (사전 취소)")
    void cancelReservation() {
        ReservationResponse created = reservationService.createReservation(new ReservationRequest(
                "홍길동",
                LocalDate.of(2030, 2, 10),
                LocalDate.of(2030, 2, 12),
                "A-1", "010-1234-5678",
                null, null, null
        ), NOW);

        reservationService.cancelReservation(created.getId(), created.getConfirmationCode(), NOW);

        ReservationResponse cancelled = reservationService.getReservation(created.getId());
        assertThat(cancelled.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("취소 - 당일 취소 시 CANCELLED_SAME_DAY 상태")
    void cancelReservationSameDay() {
        LocalDate now = LocalDate.of(2030, 2, 10);
        ReservationResponse created = reservationService.createReservation(new ReservationRequest(
                "홍길동",
                LocalDate.of(2030, 2, 10),
                LocalDate.of(2030, 2, 12),
                "A-1", "010-1234-5678",
                null, null, null
        ), now);

        reservationService.cancelReservation(created.getId(), created.getConfirmationCode(), now);

        ReservationResponse cancelled = reservationService.getReservation(created.getId());
        assertThat(cancelled.getStatus()).isEqualTo("CANCELLED_SAME_DAY");
    }

    @Test
    @DisplayName("취소 실패 - 존재하지 않는 예약 ID")
    void failCancelWhenReservationNotFound() {
        assertThatThrownBy(() -> reservationService.cancelReservation(99999L, "ABC123", NOW))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("예약을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("취소 실패 - 확인 코드 불일치")
    void failCancelWhenWrongConfirmationCode() {
        ReservationResponse created = reservationService.createReservation(new ReservationRequest(
                "홍길동",
                LocalDate.of(2030, 2, 10),
                LocalDate.of(2030, 2, 12),
                "A-1", "010-1234-5678",
                null, null, null
        ), NOW);

        assertThatThrownBy(() -> reservationService.cancelReservation(created.getId(), "WRONG1", NOW))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("확인 코드가 일치하지 않습니다.");
    }

    // =============================================
    // getReservation 엣지 케이스
    // =============================================

    @Test
    @DisplayName("조회 실패 - 존재하지 않는 예약 ID")
    void failGetReservationNotFound() {
        assertThatThrownBy(() -> reservationService.getReservation(99999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("예약을 찾을 수 없습니다.");
    }
}
