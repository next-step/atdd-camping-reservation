package com.camping.legacy.controller;

import com.camping.legacy.AcceptanceTest;
import com.camping.legacy.domain.Campsite;
import com.camping.legacy.repository.CampsiteRepository;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import static com.camping.legacy.controller.ReservationTestHelper.*;
import static com.camping.legacy.util.DateTimeUtil.yyyyMMdd;
import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;

class ReservationCreateAcceptanceTest extends AcceptanceTest {
    @Autowired
    private CampsiteRepository campsiteRepository;
    private final LocalDate today = LocalDate.of(2025, 9, 1);

    @BeforeEach
    void setUpData() {
        campsiteRepository.save(new Campsite("A-1", "A-1", 3));
        Mockito.when(clock.instant())
                .thenReturn(today.atStartOfDay().toInstant(ZoneOffset.UTC));
    }

    @Test
    void 예약_생성_성공() {
        // given
        Map<String, Object> body = defaultCreateRequest();

        // when
        ExtractableResponse<Response> response = sendReservationCreateRequest(body);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertReservationSuccess(response, body);
    }

    @Test
    void 과거_날짜로_예약불가() {
        // given
        Map<String, Object> body = defaultCreateRequest();
        body.put("startDate", "2025-08-30");
        body.put("endDate", "2025-08-31");

        // when
        ExtractableResponse<Response> response = sendReservationCreateRequest(body);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.jsonPath().getString("message")).isEqualTo("과거 날짜로 예약할 수 없습니다.");
    }

    @Test
    void 예약_날짜는_오늘부터_30일_이내여야_한다() {
        // given
        LocalDate today = LocalDate.of(2025, 9, 1);
        Map<String, Object> body = defaultCreateRequest();
        body.put("startDate", today.plusDays(28).format(yyyyMMdd));
        body.put("endDate", today.plusDays(31).format(yyyyMMdd));

        // when
        ExtractableResponse<Response> response = sendReservationCreateRequest(body);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.jsonPath().getString("message")).isEqualTo("예약은 오늘부터 30일 이내의 날짜로만 가능합니다.");
    }

    @Test
    void 동일_사이트_동일_기간_중복_예약_불가() {
        // given
        Map<String, Object> firstRequest = defaultCreateRequest();
        sendReservationCreateRequest(firstRequest);

        // when
        Map<String, Object> secondRequest = defaultCreateRequest();
        secondRequest.put("customerName", "김철수");
        ExtractableResponse<Response> response = sendReservationCreateRequest(secondRequest);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.jsonPath().getString("message")).isEqualTo("해당 기간에 이미 예약이 존재합니다.");
    }

    @Test
    void 취소된_예약이_있는_날_예약_성공() {
        // given
        Map<String, Object> firstRequest = defaultCreateRequest();
        ExtractableResponse<Response> firstExtract = sendReservationCreateRequest(firstRequest);
        Long reservationId = firstExtract.jsonPath().getLong("id");
        String confirmationCode = firstExtract.jsonPath().getString("confirmationCode");
        sendCancelRequest(confirmationCode, reservationId);

        // when
        Map<String, Object> secondReservationRequest = defaultCreateRequest();
        ExtractableResponse<Response> secondExtract = sendReservationCreateRequest(firstRequest);

        // then
        assertThat(secondExtract.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertReservationSuccess(secondExtract, secondReservationRequest);
    }

    @Test
    void 동시에_여러_요청이_들어온_경우_단_하나만_성공() {
        // given
        Map<String, Object> body = defaultCreateRequest();

        // when
        List<CompletableFuture<ExtractableResponse<Response>>> futures =
                IntStream.range(0, 2)
                        .mapToObj(i -> CompletableFuture.supplyAsync(() -> sendReservationCreateRequest(body)))
                        .toList();

        // then
        List<ExtractableResponse<Response>> responseList = futures.stream().map(CompletableFuture::join).toList();
        assertThat(responseList).extracting("statusCode")
                .containsExactlyInAnyOrderElementsOf(Arrays.asList(HttpStatus.CREATED.value(), HttpStatus.CONFLICT.value()));
        List<ExtractableResponse<Response>> successList = responseList.stream()
                .filter(res -> res.statusCode() == HttpStatus.CREATED.value())
                .toList();
        assertThat(successList).hasSize(1);
        assertReservationSuccess(successList.get(0), body);
    }

    @Test
    void 캠핑_사이트가_존재하지_않으면_예약_실패() {
        // given
        Map<String, Object> body = defaultCreateRequest();
        body.put("siteNumber", "B-1");

        // when
        ExtractableResponse<Response> response = sendReservationCreateRequest(body);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.jsonPath().getString("message")).isEqualTo("존재하지 않는 캠핑장입니다.");
    }

    private static Map<String, Object> defaultCreateRequest() {
        Map<String, String> body = Map.of(
                "customerName", "홍길동",
                "startDate", "2025-09-10",
                "endDate", "2025-09-12",
                "siteNumber", "A-1",
                "phoneNumber", "010-1234-5678"
        );
        return new HashMap<>(body);
    }
}