package com.camping.acceptance.site;

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

import static com.camping.acceptance.site.SiteSteps.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("사이트 검색")
class SiteSearchAcceptanceTest extends AcceptanceTest {

    @Autowired
    private SiteFixture siteFixture;

    @Autowired
    private ReservationFixture reservationFixture;

    private Campsite 대형사이트A1;
    private Campsite 대형사이트A2;
    private Campsite 소형사이트B1;
    private LocalDate 시작일;
    private LocalDate 종료일;

    private static final String 기본_고객명 = "홍길동";
    private static final String 기본_연락처 = "010-1234-5678";

    @BeforeEach
    void setUpFixture() {
        대형사이트A1 = siteFixture.대형_사이트_생성("A-1");
        대형사이트A2 = siteFixture.대형_사이트_생성("A-2");
        소형사이트B1 = siteFixture.소형_사이트_생성("B-1");
        시작일 = LocalDate.now().plusDays(1);
        종료일 = LocalDate.now().plusDays(5);
    }

    @Test
    @DisplayName("비어있는 사이트만 검색 결과에 표시된다")
    void 비어있는_사이트만_검색_결과에_표시된다() {
        // given
        reservationFixture.예약_생성(대형사이트A1, 기본_고객명, 기본_연락처,
                시작일, 종료일, "ABC123");

        // when
        ExtractableResponse<Response> response = 사이트_검색_요청(시작일, 종료일);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        List<String> siteNumbers = response.jsonPath().getList("siteNumber");
        assertThat(siteNumbers).contains("A-2", "B-1");
        assertThat(siteNumbers).doesNotContain("A-1");
    }

    @Test
    @DisplayName("대형 사이트만 검색할 수 있다")
    void 대형_사이트만_검색할_수_있다() {
        // when
        ExtractableResponse<Response> response = 사이트_검색_요청(시작일, 종료일, "대형");

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        List<String> siteNumbers = response.jsonPath().getList("siteNumber");
        assertThat(siteNumbers).contains("A-1", "A-2");
        assertThat(siteNumbers).doesNotContain("B-1");
    }

    @Test
    @DisplayName("소형 사이트만 검색할 수 있다")
    void 소형_사이트만_검색할_수_있다() {
        // when
        ExtractableResponse<Response> response = 사이트_검색_요청(시작일, 종료일, "소형");

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        List<String> siteNumbers = response.jsonPath().getList("siteNumber");
        assertThat(siteNumbers).contains("B-1");
        assertThat(siteNumbers).doesNotContain("A-1", "A-2");
    }

    @Test
    @DisplayName("지난 날짜로는 검색할 수 없다")
    void 지난_날짜로는_검색할_수_없다() {
        // given
        LocalDate 어제 = LocalDate.now().minusDays(1);

        // when
        ExtractableResponse<Response> response = 사이트_검색_요청(어제, 종료일);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @Test
    @DisplayName("퇴실일이 입실일보다 빠르면 검색할 수 없다")
    void 퇴실일이_입실일보다_빠르면_검색할_수_없다() {
        // given
        LocalDate 입실일 = LocalDate.now().plusDays(5);
        LocalDate 퇴실일 = LocalDate.now().plusDays(3);

        // when
        ExtractableResponse<Response> response = 사이트_검색_요청(입실일, 퇴실일);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @Test
    @DisplayName("검색 기간을 선택하지 않으면 검색할 수 없다")
    void 검색_기간을_선택하지_않으면_검색할_수_없다() {
        // when
        ExtractableResponse<Response> response = 사이트_검색_요청_날짜없이();

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("모든 사이트가 예약된 경우 검색 결과가 없다")
    void 모든_사이트가_예약된_경우_검색_결과가_없다() {
        // given
        reservationFixture.예약_생성(대형사이트A1, "홍길동", "010-1234-5678",
                시작일, 종료일, "ABC123");
        reservationFixture.예약_생성(대형사이트A2, "김철수", "010-2222-2222",
                시작일, 종료일, "DEF456");
        reservationFixture.예약_생성(소형사이트B1, "박영희", "010-3333-3333",
                시작일, 종료일, "GHI789");

        // when
        ExtractableResponse<Response> response = 사이트_검색_요청(시작일, 종료일);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        List<String> siteNumbers = response.jsonPath().getList("siteNumber");
        assertThat(siteNumbers).isEmpty();
    }

    @Test
    @DisplayName("취소된 예약이 있는 사이트는 검색 결과에 표시된다")
    void 취소된_예약이_있는_사이트는_검색_결과에_표시된다() {
        // given
        reservationFixture.취소된_예약_생성(대형사이트A1, 기본_고객명, 기본_연락처,
                시작일, 종료일);

        // when
        ExtractableResponse<Response> response = 사이트_검색_요청(시작일, 종료일);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        List<String> siteNumbers = response.jsonPath().getList("siteNumber");
        assertThat(siteNumbers).contains("A-1");
    }

    @Test
    @DisplayName("검색 기간 중간에 예약이 있는 사이트는 제외된다")
    void 검색_기간_중간에_예약이_있는_사이트는_제외된다() {
        // given - 검색 기간: 1일~10일, 기존 예약: 5일~7일 (중간에 있음)
        LocalDate 검색_시작 = LocalDate.now().plusDays(1);
        LocalDate 검색_종료 = LocalDate.now().plusDays(10);
        LocalDate 예약_시작 = LocalDate.now().plusDays(5);
        LocalDate 예약_종료 = LocalDate.now().plusDays(7);

        reservationFixture.예약_생성(대형사이트A1, 기본_고객명, 기본_연락처,
                예약_시작, 예약_종료, "ABC123");

        // when
        ExtractableResponse<Response> response = 사이트_검색_요청(검색_시작, 검색_종료);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        List<String> siteNumbers = response.jsonPath().getList("siteNumber");
        assertThat(siteNumbers).contains("A-2", "B-1");
        assertThat(siteNumbers).doesNotContain("A-1");
    }

    @Test
    @DisplayName("오늘 입실로 검색할 수 있다")
    void 오늘_입실로_검색할_수_있다() {
        // given
        LocalDate 오늘 = LocalDate.now();
        LocalDate 내일 = 오늘.plusDays(1);

        // when
        ExtractableResponse<Response> response = 사이트_검색_요청(오늘, 내일);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        List<String> siteNumbers = response.jsonPath().getList("siteNumber");
        assertThat(siteNumbers).contains("A-1", "A-2", "B-1");
    }
}
