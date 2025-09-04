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
}
