package com.camping.acceptance.reservation;

import com.camping.acceptance.common.AcceptanceTest;
import com.camping.acceptance.common.ReservationFixture;
import com.camping.acceptance.common.SiteFixture;
import com.camping.legacy.domain.Campsite;
import com.camping.legacy.domain.Reservation;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Map;

import static com.camping.acceptance.reservation.ReservationSteps.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("예약 수정")
class ReservationUpdateAcceptanceTest extends AcceptanceTest {

    @Autowired
    private SiteFixture siteFixture;

    @Autowired
    private ReservationFixture reservationFixture;

    private Campsite 사이트A1;
    private Campsite 사이트A2;
    private Reservation 기존예약;
    private LocalDate 시작일;
    private LocalDate 종료일;

    private static final String 기본_고객명 = "홍길동";
    private static final String 기본_연락처 = "010-1234-5678";
    private static final String 다른_고객명 = "김철수";
    private static final String 다른_연락처 = "010-9999-9999";

    @BeforeEach
    void setUpFixture() {
        사이트A1 = siteFixture.대형_사이트_생성("A-1");
        사이트A2 = siteFixture.대형_사이트_생성("A-2");
        시작일 = LocalDate.now().plusDays(1);
        종료일 = LocalDate.now().plusDays(3);
        기존예약 = reservationFixture.예약_생성(사이트A1, 기본_고객명, 기본_연락처, 시작일, 종료일, "ABC123");
    }

    @Test
    @DisplayName("확인 코드 없이는 수정할 수 없다")
    void 확인_코드_없이는_수정할_수_없다() {
        // given
        Map<String, Object> request = Map.of("customerName", 다른_고객명);

        // when
        ExtractableResponse<Response> response = 예약_수정_요청(
                기존예약.getId(), "", request);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.jsonPath().getString("message")).contains("확인 코드");
    }

    @Test
    @DisplayName("잘못된 확인 코드로는 수정할 수 없다")
    void 잘못된_확인_코드로는_수정할_수_없다() {
        // given
        Map<String, Object> request = Map.of("customerName", 다른_고객명);

        // when
        ExtractableResponse<Response> response = 예약_수정_요청(
                기존예약.getId(), "WRONG1", request);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.jsonPath().getString("message")).contains("확인 코드");
    }

    @Test
    @DisplayName("올바른 확인 코드로 날짜를 변경할 수 있다")
    void 올바른_확인_코드로_날짜를_변경할_수_있다() {
        // given
        LocalDate 새시작일 = LocalDate.now().plusDays(10);
        LocalDate 새종료일 = LocalDate.now().plusDays(12);
        Map<String, Object> request = Map.of(
                "startDate", 새시작일.toString(),
                "endDate", 새종료일.toString()
        );

        // when
        ExtractableResponse<Response> response = 예약_수정_요청(
                기존예약.getId(), "ABC123", request);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getString("startDate")).isEqualTo(새시작일.toString());
    }

    @Test
    @DisplayName("다른 예약이 있는 날짜로 변경할 수 없다 (같은 사이트로 날짜를 변경하는 경우)")
    void 다른_예약이_있는_날짜로_변경할_수_없다() {
        // given
        LocalDate 충돌시작일 = LocalDate.now().plusDays(10);
        LocalDate 충돌종료일 = LocalDate.now().plusDays(12);
        reservationFixture.예약_생성(사이트A1, 다른_고객명, 충돌시작일, 충돌종료일);

        Map<String, Object> request = Map.of(
                "startDate", 충돌시작일.toString(),
                "endDate", 충돌종료일.toString()
        );

        // when
        ExtractableResponse<Response> response = 예약_수정_요청(
                기존예약.getId(), "ABC123", request);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("비어있는 다른 사이트로 변경할 수 있다")
    void 비어있는_다른_사이트로_변경할_수_있다() {
        // given
        Map<String, Object> request = Map.of("siteNumber", "A-2");

        // when
        ExtractableResponse<Response> response = 예약_수정_요청(
                기존예약.getId(), "ABC123", request);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getString("siteNumber")).isEqualTo("A-2");
    }

    @Test
    @DisplayName("예약자명을 변경할 수 있다")
    void 예약자명을_변경할_수_있다() {
        // given
        Map<String, Object> request = Map.of("customerName", 다른_고객명);

        // when
        ExtractableResponse<Response> response = 예약_수정_요청(
                기존예약.getId(), "ABC123", request);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getString("customerName")).isEqualTo(다른_고객명);
    }

    @Test
    @DisplayName("연락처를 변경할 수 있다")
    void 연락처를_변경할_수_있다() {
        // given
        Map<String, Object> request = Map.of("phoneNumber", 다른_연락처);

        // when
        ExtractableResponse<Response> response = 예약_수정_요청(
                기존예약.getId(), "ABC123", request);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getString("phoneNumber")).isEqualTo(다른_연락처);
    }

    @Test
    @DisplayName("존재하지 않는 예약은 수정할 수 없다")
    void 존재하지_않는_예약은_수정할_수_없다() {
        // given
        Map<String, Object> request = Map.of("customerName", "김철수");

        // when
        ExtractableResponse<Response> response = 예약_수정_요청(
                999999L, "ABC123", request);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.jsonPath().getString("message")).contains("예약");
    }

    @Test
    @DisplayName("취소된 예약은 수정할 수 없다")
    void 취소된_예약은_수정할_수_없다() {
        // given
        Reservation 취소된예약 = reservationFixture.취소된_예약_생성(사이트A2, 기본_고객명, 기본_연락처, 시작일, 종료일);
        Map<String, Object> request = Map.of("customerName", 다른_고객명);

        // when
        ExtractableResponse<Response> response = 예약_수정_요청(
                취소된예약.getId(), "CANCEL1", request);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("지난 날짜로는 예약을 변경할 수 없다")
    void 지난_날짜로는_예약을_변경할_수_없다() {
        // given
        LocalDate 어제 = LocalDate.now().minusDays(1);
        Map<String, Object> request = Map.of(
                "startDate", 어제.toString(),
                "endDate", 종료일.toString()
        );

        // when
        ExtractableResponse<Response> response = 예약_수정_요청(
                기존예약.getId(), "ABC123", request);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.jsonPath().getString("message")).contains("과거");
    }

    @Test
    @DisplayName("퇴실일이 입실일보다 빠르게 변경할 수 없다")
    void 퇴실일이_입실일보다_빠르게_변경할_수_없다() {
        // given
        LocalDate 새입실일 = LocalDate.now().plusDays(10);
        LocalDate 새퇴실일 = LocalDate.now().plusDays(8);
        Map<String, Object> request = Map.of(
                "startDate", 새입실일.toString(),
                "endDate", 새퇴실일.toString()
        );

        // when
        ExtractableResponse<Response> response = 예약_수정_요청(
                기존예약.getId(), "ABC123", request);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.jsonPath().getString("message")).contains("종료일");
    }

    @Test
    @DisplayName("해당 기간에 예약된 사이트로는 변경할 수 없다 (같은 날짜로 사이트만 변경하는 경우)")
    void 해당_기간에_예약된_사이트로는_변경할_수_없다() {
        // given
        reservationFixture.예약_생성(사이트A2, 다른_고객명, 시작일, 종료일);
        Map<String, Object> request = Map.of("siteNumber", 사이트A2.getSiteNumber());

        // when
        ExtractableResponse<Response> response = 예약_수정_요청(
                기존예약.getId(), "ABC123", request);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("존재하지 않는 사이트로는 변경할 수 없다")
    void 존재하지_않는_사이트로는_변경할_수_없다() {
        // given
        Map<String, Object> request = Map.of("siteNumber", "Z-99");

        // when
        ExtractableResponse<Response> response = 예약_수정_요청(
                기존예약.getId(), "ABC123", request);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.jsonPath().getString("message")).contains("존재");
    }

    @Test
    @DisplayName("취소된 예약이 있는 사이트로 변경할 수 있다")
    void 취소된_예약이_있는_사이트로_변경할_수_있다() {
        // given - A-2에 취소된 예약이 있음
        reservationFixture.취소된_예약_생성(사이트A2, 다른_고객명, 다른_연락처, 시작일, 종료일);
        Map<String, Object> request = Map.of("siteNumber", 사이트A2.getSiteNumber());

        // when
        ExtractableResponse<Response> response = 예약_수정_요청(
                기존예약.getId(), "ABC123", request);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getString("siteNumber")).isEqualTo("A-2");
    }

    @Test
    @DisplayName("기존 예약과 일부 날짜가 겹치면 변경할 수 없다")
    void 기존_예약과_일부_날짜가_겹치면_변경할_수_없다() {
        // given - A-1에 10일~12일 예약 존재
        LocalDate 기존_시작 = LocalDate.now().plusDays(10);
        LocalDate 기존_종료 = LocalDate.now().plusDays(12);
        reservationFixture.예약_생성(사이트A1, 다른_고객명, 기존_시작, 기존_종료);

        // when - 9일~11일로 변경 시도 (일부 겹침)
        LocalDate 새_시작 = LocalDate.now().plusDays(9);
        LocalDate 새_종료 = LocalDate.now().plusDays(11);
        Map<String, Object> request = Map.of(
                "startDate", 새_시작.toString(),
                "endDate", 새_종료.toString()
        );
        ExtractableResponse<Response> response = 예약_수정_요청(
                기존예약.getId(), "ABC123", request);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("사이트와 날짜를 동시에 변경할 수 있다")
    void 사이트와_날짜를_동시에_변경할_수_있다() {
        // given
        LocalDate 새시작일 = LocalDate.now().plusDays(10);
        LocalDate 새종료일 = LocalDate.now().plusDays(12);
        Map<String, Object> request = Map.of(
                "siteNumber", "A-2",
                "startDate", 새시작일.toString(),
                "endDate", 새종료일.toString()
        );

        // when
        ExtractableResponse<Response> response = 예약_수정_요청(
                기존예약.getId(), "ABC123", request);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getString("siteNumber")).isEqualTo("A-2");
        assertThat(response.jsonPath().getString("startDate")).isEqualTo(새시작일.toString());
    }
}
