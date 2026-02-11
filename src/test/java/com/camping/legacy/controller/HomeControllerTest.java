package com.camping.legacy.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;

public class HomeControllerTest extends AcceptanceTest {

    @DisplayName("메인 페이지를 요청하면 정상 응답을 반환한다")
    @Test
    void home_ReturnsIndexHtml() {
        get("/")
                .statusCode(HttpStatus.OK.value())
                .contentType(MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
                .body(containsString("초록 캠핑장"));
    }
}
