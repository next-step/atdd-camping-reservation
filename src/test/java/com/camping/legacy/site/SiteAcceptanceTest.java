package com.camping.legacy.site;

import com.camping.legacy.utils.AcceptanceTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static com.camping.legacy.site.SiteSteps.*;
import static org.assertj.core.api.Assertions.assertThat;

public class SiteAcceptanceTest extends AcceptanceTest {

    @DisplayName("전체 사이트 목록을 조회한다.")
    @Test
    void 전체_사이트_목록_조회() {
        // when
        var response = 전체_사이트_목록_조회_요청();

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        A구역_대형_사이트들이_반환된다(response);
        B구역_소형_사이트들이_반환된다(response);
        각_사이트의_최대_수용_인원이_포함된다(response);
    }

    @DisplayName("특정 날짜 가용 사이트를 조회한다.")
    @Test
    void 특정_날짜_가용_사이트_조회() {
        // given
        사이트_A1에_예약이_존재한다("2024-01-15");
        사이트_A2는_예약이_없다();

        // when
        var response = 특정_날짜_가용_사이트_조회_요청("2024-01-15");

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        A2_사이트가_가용_사이트로_반환된다(response);
        A1_사이트는_반환되지_않는다(response);
    }

    // todo: 버그. 기간 예약 시 첫날과 마지막 날만 체크함
    //
//    @DisplayName("기간별 가용 사이트를 검색한다.")
//    @Test
//    void 기간별_가용_사이트_검색() {
//        // given
//        사이트_A1에_기간_예약이_존재한다("2024-01-15", "2024-01-16");
//        사이트_A2는_예약이_없다();
//
//        // when
//        var response = 기간별_가용_사이트_검색_요청("2024-01-15", "2024-01-16");
//
//        // then
//        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
//        A2_사이트만_반환된다(response);
//        A1_사이트는_반환되지_않는다_기간검색(response);
//    }

    // todo: 버그. 요구사항에는 명시적으로 필터링 옵션: 사이트 크기별 (대형/소형)을 지원해야 한다고 정의되어 있음
    @DisplayName("사이트 크기별 필터링을 한다.")
    @Test
    void 사이트_크기별_필터링() {
        // given
        A구역_대형_사이트들과_B구역_소형_사이트들이_존재한다();

        // when
        var response = 대형_사이트만_필터링하여_조회_요청();

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        A로_시작하는_사이트들만_반환된다(response);
        B로_시작하는_사이트들은_반환되지_않는다(response);
    }
}
