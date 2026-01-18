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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.camping.acceptance.reservation.ReservationSteps.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("캘린더 조회")
class CalendarAcceptanceTest extends AcceptanceTest {

    @Autowired
    private SiteFixture siteFixture;

    @Autowired
    private ReservationFixture reservationFixture;

    private Campsite 사이트;
    private LocalDate 시작일;
    private LocalDate 종료일;

    private static final String 기본_고객명 = "홍길동";

    @BeforeEach
    void setUpFixture() {
        사이트 = siteFixture.대형_사이트_생성("A-1");
        시작일 = LocalDate.now().plusDays(1);
        종료일 = LocalDate.now().plusDays(3);
    }

    @Test
    @DisplayName("취소된 예약은 캘린더에 예약으로 표시되지 않는다")
    void 취소된_예약은_캘린더에_예약으로_표시되지_않는다() {
        // given
        reservationFixture.취소된_예약_생성(사이트, 시작일, 종료일);

        // when
        ExtractableResponse<Response> response = 캘린더_조회_요청(
                시작일.getYear(), 시작일.getMonthValue(), 사이트.getId());

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());

        List<Map<String, Object>> days = response.jsonPath().getList("days");
        Map<String, Object> 예약일_상태 = days.stream()
                .filter(day -> 시작일.toString().equals(day.get("date")))
                .findFirst()
                .orElseThrow();

        assertThat(예약일_상태.get("available")).isEqualTo(true);
        assertThat(예약일_상태.get("customerName")).isNull();
    }

    @Test
    @DisplayName("활성 예약이 있는 날짜는 캘린더에 예약으로 표시된다")
    void 활성_예약이_있는_날짜는_캘린더에_예약으로_표시된다() {
        // given
        reservationFixture.예약_생성(사이트, 기본_고객명, 시작일, 종료일);

        // when
        ExtractableResponse<Response> response = 캘린더_조회_요청(
                시작일.getYear(), 시작일.getMonthValue(), 사이트.getId());

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());

        List<Map<String, Object>> days = response.jsonPath().getList("days");
        Map<String, Object> 예약일_상태 = days.stream()
                .filter(day -> 시작일.toString().equals(day.get("date")))
                .findFirst()
                .orElseThrow();

        assertThat(예약일_상태.get("available")).isEqualTo(false);
        assertThat(예약일_상태.get("customerName")).isEqualTo(기본_고객명);
    }
}