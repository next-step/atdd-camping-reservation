package com.camping.acceptance.sample;

import com.camping.acceptance.common.AcceptanceTest;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static com.camping.acceptance.sample.SampleSteps.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("샘플 관리")
class SampleAcceptanceTest extends AcceptanceTest {

    @Test
    @DisplayName("생성 - 유효한 요청이면 생성된다")
    void 유효한_요청이면_생성된다() {
        // given
        Map<String, Object> request = Map.of("name", "테스트");

        // when
        ExtractableResponse<Response> response = 생성_요청(request);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(response.jsonPath().getLong("id")).isPositive();
    }

    @Test
    @DisplayName("조회 - 존재하는 ID로 조회하면 성공한다")
    void 존재하는_ID로_조회하면_성공한다() {
        // given
        Long id = 생성_요청(Map.of("name", "테스트")).jsonPath().getLong("id");

        // when
        ExtractableResponse<Response> response = 조회_요청(id);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getString("name")).isEqualTo("테스트");
    }

    @Test
    @DisplayName("수정 - 존재하는 리소스를 수정하면 성공한다")
    void 존재하는_리소스를_수정하면_성공한다() {
        // given
        Long id = 생성_요청(Map.of("name", "원본")).jsonPath().getLong("id");
        Map<String, Object> updateRequest = Map.of("name", "수정됨");

        // when
        ExtractableResponse<Response> response = 수정_요청(id, updateRequest);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getString("name")).isEqualTo("수정됨");
    }

    @Test
    @DisplayName("삭제 - 존재하는 리소스를 삭제하면 성공한다")
    void 존재하는_리소스를_삭제하면_성공한다() {
        // given
        Long id = 생성_요청(Map.of("name", "삭제대상")).jsonPath().getLong("id");

        // when
        ExtractableResponse<Response> response = 삭제_요청(id);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    }
}