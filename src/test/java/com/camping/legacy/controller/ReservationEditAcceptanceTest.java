package com.camping.legacy.controller;

import com.camping.legacy.AcceptanceTest;
import com.camping.legacy.domain.Campsite;
import com.camping.legacy.repository.CampsiteRepository;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.camping.legacy.controller.ReservationTestHelper.*;
import static org.assertj.core.api.Assertions.assertThat;

public class ReservationEditAcceptanceTest extends AcceptanceTest {
    @Autowired
    private CampsiteRepository campsiteRepository;

    @BeforeEach
    void setUpData() {
        campsiteRepository.save(new Campsite("A-1", "A-1", 3));
        campsiteRepository.save(new Campsite("B-1", "B-1", 3));
    }

    @Test
    void 예약_변경_성공() {
        // given
        ExtractableResponse<Response> response = sendReservationCreateRequest(reservationCreateRequest());
        Long reservationId = response.jsonPath().getLong("id");
        String confirmationCode = response.jsonPath().getString("confirmationCode");

        // when
        Map<String, Object> editRequest = reservationEditRequest();
        ExtractableResponse<Response> editResponse = sendReservationEditRequest(reservationId, confirmationCode, editRequest);

        // then
        assertThat(editResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertReservationSuccess(editResponse, editRequest);
    }

    @Test
    void 확인코드가_일치하지_않으면_변경_실패() {
        // given
        ExtractableResponse<Response> response = sendReservationCreateRequest(reservationCreateRequest());
        Long reservationId = response.jsonPath().getLong("id");

        // when
        Map<String, Object> editRequest = reservationEditRequest();
        ExtractableResponse<Response> editResponse = sendReservationEditRequest(reservationId, "XXXXXX", editRequest);

        // then
        assertThat(editResponse.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(editResponse.jsonPath().getString("message")).isEqualTo("확인 코드가 일치하지 않습니다.");
    }

    /**
     * Scenario: 존재하지 않는 예약 ID로 변경 실패
     * Given 예약 ID "1"는 존재하지 않는다
     * When 사용자가 예약("1")을 변경한다
     * Then "예약을 찾을 수 없습니다." 메시지를 반환해야 한다
     */
    @Test
    void 존재하지_않는_예약_ID로_변경_실패() {
        // given
        // 아무 예약이 없다.

        // when
        Map<String, Object> editRequest = reservationEditRequest();
        ExtractableResponse<Response> editResponse = sendReservationEditRequest(1L, "ABC123", editRequest);

        // then
        assertThat(editResponse.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(editResponse.jsonPath().getString("message")).isEqualTo("예약을 찾을 수 없습니다.");
    }

    @Test
    void 취소된_예약은_변경_실패() {
        // given
        ExtractableResponse<Response> response = sendReservationCreateRequest(reservationCreateRequest());
        Long reservationId = response.jsonPath().getLong("id");
        String confirmationCode = response.jsonPath().getString("confirmationCode");
        sendCancelRequest(confirmationCode, reservationId);

        // when
        Map<String, Object> editRequest = reservationEditRequest();
        ExtractableResponse<Response> editResponse = sendReservationEditRequest(reservationId, confirmationCode, editRequest);

        // then
        assertThat(editResponse.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(editResponse.jsonPath().getString("message")).isEqualTo("취소된 예약은 변경할 수 없습니다.");
    }

    @Test
    void 과거_날짜로_변경불가() {
        // given
        // TODO: mock today to 2025-09-01
        ExtractableResponse<Response> response = sendReservationCreateRequest(reservationCreateRequest());
        Long reservationId = response.jsonPath().getLong("id");
        String confirmationCode = response.jsonPath().getString("confirmationCode");

        // when
        Map<String, Object> editRequest = reservationEditRequest();
        editRequest.put("startDate", "2025-08-29");
        editRequest.put("endDate", "2025-08-30");
        ExtractableResponse<Response> editResponse = sendReservationEditRequest(reservationId, confirmationCode, editRequest);

        // then
        assertThat(editResponse.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(editResponse.jsonPath().getString("message")).isEqualTo("과거 날짜로는 예약할 수 없습니다.");
    }

    @Test
    void 예약_날짜는_오늘부터_30일_이내여야_한다() {
        // given
        // TODO: mock today to 2025-09-01
        ExtractableResponse<Response> response = sendReservationCreateRequest(reservationCreateRequest());
        Long reservationId = response.jsonPath().getLong("id");
        String confirmationCode = response.jsonPath().getString("confirmationCode");

        // when
        Map<String, Object> editRequest = reservationEditRequest();
        editRequest.put("startDate", "2025-09-31");
        editRequest.put("endDate", "2025-10-01");
        ExtractableResponse<Response> editResponse = sendReservationEditRequest(reservationId, confirmationCode, editRequest);

        // then
        assertThat(editResponse.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(editResponse.jsonPath().getString("message")).isEqualTo("예약은 오늘부터 30일 이내의 날짜로만 가능합니다.");
    }

    @Test
    void 동일_사이트_동일_기간_중복_예약_불가() {
        // given
        sendReservationCreateRequest(reservationCreateRequest());

        Map<String, Object> createRequest = reservationCreateRequest();
        createRequest.put("siteNumber", "B-1");
        createRequest.put("startDate", "2025-09-13");
        createRequest.put("endDate", "2025-09-14");
        ExtractableResponse<Response> createResponse = sendReservationCreateRequest(createRequest);
        Long reservationId = createResponse.jsonPath().getLong("id");
        String confirmationCode = createResponse.jsonPath().getString("confirmationCode");

        // when
        Map<String, Object> editRequest = reservationEditRequest();
        editRequest.put("siteNumber", "B-1");
        editRequest.put("startDate", "2025-09-13");
        editRequest.put("endDate", "2025-09-14");
        ExtractableResponse<Response> editResponse =
                sendReservationEditRequest(reservationId, confirmationCode, editRequest);

        // then
        assertThat(editResponse.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(editResponse.jsonPath().getString("message")).isEqualTo("해당 기간에 이미 예약이 존재합니다.");
    }

    @Test
    void 취소된_예약이_있는_날_변경_성공() {
        // given
        ExtractableResponse<Response> createCanceledReservationRequest = sendReservationCreateRequest(reservationCreateRequest());
        Long canceledReservationId = createCanceledReservationRequest.jsonPath().getLong("id");
        String canceledConfirmationCode = createCanceledReservationRequest.jsonPath().getString("confirmationCode");
        sendCancelRequest(canceledConfirmationCode, canceledReservationId);

        Map<String, Object> createRequest = reservationCreateRequest();
        createRequest.put("siteNumber", "B-1");
        createRequest.put("startDate", "2025-09-13");
        createRequest.put("endDate", "2025-09-14");
        ExtractableResponse<Response> createResponse = sendReservationCreateRequest(createRequest);
        Long reservationId = createResponse.jsonPath().getLong("id");
        String confirmationCode = createResponse.jsonPath().getString("confirmationCode");

        // when
        Map<String, Object> editRequest = reservationEditRequest();
        editRequest.put("siteNumber", "B-1");
        editRequest.put("startDate", "2025-09-13");
        editRequest.put("endDate", "2025-09-14");
        ExtractableResponse<Response> editResponse =
                sendReservationEditRequest(reservationId, confirmationCode, editRequest);

        // then
        assertThat(editResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertReservationSuccess(editResponse, editRequest);
    }

    @Test
    void 동시에_여러_요청이_들어온_경우_단_하나만_성공() {
        // given
        ExtractableResponse<Response> firstCreateResponse = sendReservationCreateRequest(reservationCreateRequest());
        Long firstReservationId = firstCreateResponse.jsonPath().getLong("id");
        String firstConfirmationCode = firstCreateResponse.jsonPath().getString("confirmationCode");

        Map<String, Object> secondCreateRequest = reservationCreateRequest();
        secondCreateRequest.put("customerName", "고길동");
        secondCreateRequest.put("startDate", "2025-09-13");
        secondCreateRequest.put("endDate", "2025-09-14");
        ExtractableResponse<Response> secondCreateResponse = sendReservationCreateRequest(secondCreateRequest);
        Long secondReservationId = secondCreateResponse.jsonPath().getLong("id");
        String secondConfirmationCode = secondCreateResponse.jsonPath().getString("confirmationCode");

        // when
        Map<String, Object> editRequest = reservationEditRequest();
        editRequest.put("siteNumber", "B-1");
        editRequest.put("startDate", "2025-09-13");
        editRequest.put("endDate", "2025-09-14");

        var futures = List.of(
                CompletableFuture.supplyAsync(() -> sendReservationEditRequest(firstReservationId, firstConfirmationCode, editRequest)),
                CompletableFuture.supplyAsync(() -> sendReservationEditRequest(secondReservationId, secondConfirmationCode, editRequest))
        );
        var responses = futures.stream().map(CompletableFuture::join).toList();

        // then
        assertThat(responses).extracting("statusCode")
                .containsExactlyInAnyOrderElementsOf(Arrays.asList(HttpStatus.OK.value(), HttpStatus.BAD_REQUEST.value()));
        var successResponses = responses.stream()
                .filter(res -> res.statusCode() == HttpStatus.OK.value())
                .toList();
        assertReservationSuccess(successResponses.get(0), editRequest);
    }

    @Test
    void 캠핑_사이트가_존재하지_않으면_변경_실패() {
        // given
        ExtractableResponse<Response> response = sendReservationCreateRequest(reservationCreateRequest());
        Long reservationId = response.jsonPath().getLong("id");
        String confirmationCode = response.jsonPath().getString("confirmationCode");

        // when
        Map<String, Object> editRequest = reservationEditRequest();
        editRequest.put("siteNumber", "X-1");
        ExtractableResponse<Response> editResponse = sendReservationEditRequest(reservationId, confirmationCode, editRequest);

        // then
        assertThat(editResponse.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(editResponse.jsonPath().getString("message")).isEqualTo("존재하지 않는 캠핑장입니다.");
    }


    private static Map<String, Object> reservationCreateRequest() {
        Map<String, String> body = Map.of(
                "customerName", "홍길동",
                "startDate", "2025-09-10",
                "endDate", "2025-09-12",
                "siteNumber", "A-1",
                "phoneNumber", "010-1234-5678"
        );
        return new HashMap<>(body);
    }

    private static Map<String, Object> reservationEditRequest() {
        Map<String, String> body = Map.of(
                "customerName", "고길동",
                "startDate", "2025-09-13",
                "endDate", "2025-09-15",
                "siteNumber", "B-1",
                "phoneNumber", "010-2222-2222"
        );
        return new HashMap<>(body);
    }
}
