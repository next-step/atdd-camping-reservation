package com.camping.acceptance.reservation;

import com.camping.acceptance.common.AcceptanceTest;
import com.camping.acceptance.common.ReservationFixture;
import com.camping.acceptance.common.SiteFixture;
import com.camping.legacy.domain.Campsite;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.camping.acceptance.reservation.ReservationSteps.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("예약 생성")
class ReservationCreateAcceptanceTest extends AcceptanceTest {

    @Autowired
    private SiteFixture siteFixture;

    @Autowired
    private ReservationFixture reservationFixture;

    private Campsite 대형사이트;
    private LocalDate 시작일;
    private LocalDate 종료일;
    private String 대형사이트_번호 = "A-1";

    @BeforeEach
    void setUpFixture() {
        대형사이트 = siteFixture.대형_사이트_생성(대형사이트_번호);
        시작일 = LocalDate.now().plusDays(1);
        종료일 = LocalDate.now().plusDays(3);
    }

    @Test
    @DisplayName("빈 사이트를 예약하면 확인 코드를 받는다")
    void 빈_사이트를_예약하면_확인_코드를_받는다() {
        // when
        ExtractableResponse<Response> response = 예약_생성_요청(
                대형사이트_번호, "홍길동", "010-1234-5678", 시작일, 종료일);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(response.jsonPath().getString("confirmationCode")).hasSize(6);
        assertThat(response.jsonPath().getString("status")).isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("예약자 이름 없이 예약할 수 없다")
    void 예약자_이름_없이_예약할_수_없다() {
        // given
        Map<String, Object> request = Map.of(
                "siteNumber", "A-1",
                "phoneNumber", "010-1234-5678",
                "startDate", 시작일.toString(),
                "endDate", 종료일.toString()
        );

        // when
        ExtractableResponse<Response> response = 예약_생성_요청(request);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.jsonPath().getString("message")).contains("예약자");
    }

    @Test
    @DisplayName("연락처 없이 예약할 수 없다")
    void 연락처_없이_예약할_수_없다() {
        // given
        Map<String, Object> request = Map.of(
                "customerName", "홍길동",
                "siteNumber", "A-1",
                "startDate", 시작일.toString(),
                "endDate", 종료일.toString()
        );

        // when
        ExtractableResponse<Response> response = 예약_생성_요청(request);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.jsonPath().getString("message")).contains("전화번호");
    }

    @Test
    @DisplayName("사이트를 선택하지 않으면 예약할 수 없다")
    void 사이트를_선택하지_않으면_예약할_수_없다() {
        // given
        Map<String, Object> request = Map.of(
                "customerName", "홍길동",
                "phoneNumber", "010-1234-5678",
                "startDate", 시작일.toString(),
                "endDate", 종료일.toString()
        );

        // when
        ExtractableResponse<Response> response = 예약_생성_요청(request);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.jsonPath().getString("message")).contains("사이트");
    }

    @Test
    @DisplayName("날짜를 선택하지 않으면 예약할 수 없다")
    void 날짜를_선택하지_않으면_예약할_수_없다() {
        // given
        Map<String, Object> request = Map.of(
                "customerName", "홍길동",
                "phoneNumber", "010-1234-5678",
                "siteNumber", "A-1"
        );

        // when
        ExtractableResponse<Response> response = 예약_생성_요청(request);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.jsonPath().getString("message")).contains("예약 기간");
    }

    @Test
    @DisplayName("지난 날짜로는 예약할 수 없다")
    void 지난_날짜로는_예약할_수_없다() {
        // given
        LocalDate 어제 = LocalDate.now().minusDays(1);

        // when
        ExtractableResponse<Response> response = 예약_생성_요청(
                "A-1", "홍길동", "010-1234-5678", 어제, 종료일);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.jsonPath().getString("message")).contains("과거");
    }

    @Test
    @DisplayName("30일을 초과하는 예약은 할 수 없다")
    void 삼십일을_초과하는_예약은_할_수_없다() {
        // given
        LocalDate 장기_종료일 = 시작일.plusDays(35);

        // when
        ExtractableResponse<Response> response = 예약_생성_요청(
                "A-1", "홍길동", "010-1234-5678", 시작일, 장기_종료일);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.jsonPath().getString("message")).contains("30");
    }

    @Test
    @DisplayName("퇴실일이 입실일보다 빠를 수 없다")
    void 퇴실일이_입실일보다_빠를_수_없다() {
        // given
        LocalDate 입실일 = LocalDate.now().plusDays(5);
        LocalDate 퇴실일 = LocalDate.now().plusDays(3);

        // when
        ExtractableResponse<Response> response = 예약_생성_요청(
                "A-1", "홍길동", "010-1234-5678", 입실일, 퇴실일);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.jsonPath().getString("message")).contains("종료일");
    }

    @Test
    @DisplayName("존재하지 않는 사이트는 예약할 수 없다")
    void 존재하지_않는_사이트는_예약할_수_없다() {
        // when
        ExtractableResponse<Response> response = 예약_생성_요청(
                "Z-99", "홍길동", "010-1234-5678", 시작일, 종료일);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.jsonPath().getString("message")).contains("존재");
    }

    @Test
    @DisplayName("이미 예약된 사이트는 같은 기간에 다시 예약할 수 없다")
    void 이미_예약된_기간에는_예약할_수_없다() {
        // given
        reservationFixture.예약_생성(대형사이트, "김철수", "010-9999-9999",
                시작일, 종료일, "ABC123");

        // when
        ExtractableResponse<Response> response = 예약_생성_요청(
                대형사이트_번호, "홍길동", "010-1234-5678", 시작일, 종료일);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.jsonPath().getString("message")).contains("예약");
    }

    @Test
    @DisplayName("기존 예약과 일부 날짜가 겹치면 예약할 수 없다")
    void 기존_예약과_일부_날짜가_겹치면_예약할_수_없다() {
        // given
        reservationFixture.예약_생성(대형사이트, "김철수", "010-9999-9999",
                시작일, 종료일, "ABC123");
        LocalDate 겹치는_시작일 = 시작일.plusDays(1);
        LocalDate 겹치는_종료일 = 종료일.plusDays(2);

        // when
        ExtractableResponse<Response> response = 예약_생성_요청(
                대형사이트_번호, "홍길동", "010-1234-5678", 겹치는_시작일, 겹치는_종료일);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.jsonPath().getString("message")).contains("예약");
    }

    @Test
    @DisplayName("취소된 예약이 있는 날짜는 다시 예약할 수 있다")
    void 취소된_예약이_있는_날짜는_다시_예약할_수_있다() {
        // given
        reservationFixture.취소된_예약_생성(대형사이트, "김철수", "010-9999-9999",
                시작일, 종료일);

        // when
        ExtractableResponse<Response> response = 예약_생성_요청(
                대형사이트_번호, "홍길동", "010-1234-5678", 시작일, 종료일);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(response.jsonPath().getString("confirmationCode")).hasSize(6);
    }

    @Test
    @DisplayName("두 고객이 동시에 같은 사이트를 예약하면 한 명만 성공한다")
    @DirtiesContext
    void 두_고객이_동시에_같은_사이트를_예약하면_한_명만_성공한다() throws InterruptedException {
        // given
        int 동시_요청_수 = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(동시_요청_수);
        CountDownLatch 준비완료 = new CountDownLatch(동시_요청_수);
        CountDownLatch 시작신호 = new CountDownLatch(1);

        List<Future<ExtractableResponse<Response>>> futures = new ArrayList<>();

        // when - 두 고객이 동시에 예약 요청
        for (int i = 0; i < 동시_요청_수; i++) {
            String 고객명 = (i == 0) ? "홍길동" : "김철수";
            String 연락처 = (i == 0) ? "010-1111-1111" : "010-2222-2222";

            Future<ExtractableResponse<Response>> future = executorService.submit(() -> {
                준비완료.countDown();
                시작신호.await(); // 모든 스레드가 동시에 시작하도록 대기
                return 예약_생성_요청(대형사이트_번호, 고객명, 연락처, 시작일, 종료일);
            });
            futures.add(future);
        }

        준비완료.await(); // 모든 스레드가 준비될 때까지 대기
        시작신호.countDown(); // 동시에 시작

        // then - 결과 수집 및 검증
        List<Integer> statusCodes = new ArrayList<>();
        for (Future<ExtractableResponse<Response>> future : futures) {
            try {
                ExtractableResponse<Response> response = future.get();
                statusCodes.add(response.statusCode());
            } catch (Exception e) {
                // 예외 발생 시 실패로 처리
            }
        }

        executorService.shutdown();

        // 한 명만 성공(201), 다른 한 명은 실패(409)해야 함
        long 성공_횟수 = statusCodes.stream()
                .filter(code -> code == HttpStatus.CREATED.value())
                .count();
        long 실패_횟수 = statusCodes.stream()
                .filter(code -> code == HttpStatus.CONFLICT.value())
                .count();

        assertThat(성공_횟수).isEqualTo(1);
        assertThat(실패_횟수).isEqualTo(1);
    }
}
