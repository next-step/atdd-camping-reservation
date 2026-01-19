package com.camping.acceptance;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.dto.ReservationResponse;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.camping.acceptance.ReservationSteps.*;
import static com.camping.acceptance.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;


@DisplayName("캠핑장 예약 인수 테스트")
public class ReservationAcceptanceTest extends AcceptanceTest {

    private Campsite siteA1;
    private Campsite siteB2;
    private Campsite siteC3;
    private Campsite siteD4;
    private Campsite siteE5;

    @BeforeEach
    void setUp() {
        super.setUp();
        siteA1 = campsiteRepository.save(createCampsite(SITE_A1_NUMBER));
        siteB2 = campsiteRepository.save(createCampsite(SITE_B2_NUMBER));
        siteC3 = campsiteRepository.save(createCampsite(SITE_C3_NUMBER));
        siteD4 = campsiteRepository.save(createCampsite(SITE_D4_NUMBER));
        siteE5 = campsiteRepository.save(createCampsite(SITE_E5_NUMBER));
    }

    @Test
    @DisplayName("여러_사용자가_동시에_예약을_시도할_경우_오직_하나만_성공한다")
    void concurrencyControl_OnlyOneReservationSucceeds() throws InterruptedException {
        // given
        var 동시_요청_결과 = 동시에_예약을_요청한다(10, SITE_A1_NUMBER, START_DATE, END_DATE);

        // then
        assertThat(성공한_예약_개수(동시_요청_결과)).isEqualTo(1);
        assertThat(실패한_예약_개수(동시_요청_결과)).isEqualTo(9);
    }

    @Test
    @DisplayName("예약_직후_월별_캘린더에_즉시_반영된다")
    void calendarReflectsReservationImmediately() {
        // given
        var request = 예약_요청_생성(SITE_B2_NUMBER, START_DATE, END_DATE, CUSTOMER_KIM, "010-1234-5678");
        예약을_생성한다(request);

        // then
        var 예약_조회_결과 = 캘린터에서_특정사이트_예약을_조회한다(START_DATE.getYear(), START_DATE.getMonthValue(), siteB2.getId());
        var 특정_날짜_예약_조회_결과 = 특정_날짜_예약_응답_생성한다(예약_조회_결과, START_DATE.toString());

        assertThat(특정_날짜_예약_조회_결과.get("available")).isEqualTo(false);
        assertThat(특정_날짜_예약_조회_결과.get("customerName")).isEqualTo(CUSTOMER_KIM);
    }

    @Test
    @DisplayName("예약_취소_직후_예약_가능_목록에_즉시_반영된다")
    void siteBecomesAvailableAfterCancellation() {
        // given
        var 예약요청 = 예약_요청_생성(SITE_C3_NUMBER, START_DATE, END_DATE, CUSTOMER_LEE, "010-1234-5678");
        var 예약 = 예약을_생성한다(예약요청).body().as(ReservationResponse.class);

        // when
        정확한_확인_코드로_예약을_취소한다(예약.getConfirmationCode(), 예약.getId());

        // then
        var 예약_조회_결과 = 날짜로_예약을_조회한다(START_DATE, END_DATE);
        var 예약_가능_사이트 = 예약_조회_결과.jsonPath().getList("siteNumber");
        assertThat(예약_가능_사이트).contains(SITE_C3_NUMBER);
    }

    @Test
    @DisplayName("연박_예약_시_중간_날짜가_이미_예약된_경우_조회되지_않는다")
    void searchingForOverlappingDatesExcludesSite() {
        // given
        var 예약_정보 = 예약_요청_생성(SITE_D4_NUMBER, START_DATE.plusDays(1), START_DATE.plusDays(2), CUSTOMER_PARK, "010-1234-5678");
        예약을_생성한다(예약_정보);

        // when
        var 예약_조회_결과 = 날짜로_예약을_조회한다(START_DATE, END_DATE);

        // then
        var 예약_가능_사이트 = 예약_조회_결과.jsonPath().getList("siteNumber");
        assertThat(예약_가능_사이트).doesNotContain(SITE_D4_NUMBER);
    }

    @Test
    @DisplayName("정확한_확인_코드를_입력해야만_예약을_취소할_수_있다")
    void cancellationRequiresCorrectConfirmationCode() {
        // given
        var 예약_정보 = 예약_요청_생성(SITE_E5_NUMBER, START_DATE, END_DATE, "박서준", "010-1234-5678");
        var 예약 = 예약을_생성한다(예약_정보).body().as(ReservationResponse.class);

        // when
        var 잘못된_확인코드_응답 = 잘못된_코드로_예약을_취소한다(WRONG_CONFIRMATION_CODE, 예약.getId());
        var 올바른_확인_코드_응답 = 정확한_확인_코드로_예약을_취소한다(예약.getConfirmationCode(), 예약.getId());

        // then
        assertThat(잘못된_확인코드_응답.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(올바른_확인_코드_응답.statusCode()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("과거_날짜로_예약하면_실패한다")
    void createReservation_WithPastDate_ShouldFail() {
        var request = 예약_요청_생성(SITE_A1_NUMBER, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), CUSTOMER_KIM, "010-0000-0000");
        var response = 예약을_생성한다(request);
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    @DisplayName("종료일이_시작일보다_빠르면_예약에_실패한다")
    void createReservation_WithEndDateBeforeStartDate_ShouldFail() {
        var request = 예약_요청_생성(SITE_A1_NUMBER, START_DATE, START_DATE.minusDays(1), CUSTOMER_KIM, "010-0000-0000");
        var response = 예약을_생성한다(request);
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    }
    
    @Test
    @DisplayName("이름과_전화번호로_내_예약을_조회한다")
    void findMyReservations_ByNameAndPhone() {
        // given
        var name = "김조회";
        var phone = "010-9876-5432";
        예약을_미리_만든다(SITE_B2_NUMBER, START_DATE, END_DATE, name, phone);

        // when
        var response = 이름과_전화번호로_예약을_조회한다(name, phone);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getList("")).hasSize(1);
        assertThat(response.jsonPath().getString("[0].customerName")).isEqualTo(name);
    }

    // --- Private Helper Methods ---
    
    private ReservationResponse 예약을_미리_만든다(String siteNumber, LocalDate startDate, LocalDate endDate, String name, String phone) {
        var request = 예약_요청_생성(siteNumber, startDate, endDate, name, phone);
        return 예약을_생성한다(request).as(ReservationResponse.class);
    }

    private ConcurrentLinkedQueue<Response> 동시에_예약을_요청한다(int numberOfUsers, String site, LocalDate 예약_시작_날짜, LocalDate 예약_마감_날짜) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfUsers);
        CountDownLatch latch = new CountDownLatch(numberOfUsers);
        ConcurrentLinkedQueue<Response> responses = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < numberOfUsers; i++) {
            int userIndex = i;
            executorService.submit(() -> {
                try {
                    Map<String, Object> request = 예약_요청_생성(SITE_A1_NUMBER, 예약_시작_날짜, 예약_마감_날짜, "사용자-" + userIndex, "010-1234-567" + userIndex);
                    responses.add(예약을_생성한다(request).response());
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();
        return responses;
    }

    private static Map<String, Object> 특정_날짜_예약_응답_생성한다(ExtractableResponse<Response> response, String 확인날짜) {
        List<Map<String, Object>> days = response.jsonPath().getList("days");
        return days.stream().filter(d -> d.get("date").equals(확인날짜)).findFirst().orElseThrow();
    }

    private static long 실패한_예약_개수(ConcurrentLinkedQueue<Response> responses) {
        return responses.stream().filter(r -> r.statusCode() == HttpStatus.CONFLICT.value()).count();
    }

    private static long 성공한_예약_개수(ConcurrentLinkedQueue<Response> responses) {
        return responses.stream().filter(r -> r.statusCode() == HttpStatus.CREATED.value()).count();
    }
}

