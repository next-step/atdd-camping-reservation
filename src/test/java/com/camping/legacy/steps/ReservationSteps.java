package com.camping.legacy.steps;

import com.camping.legacy.dto.ReservationRequest;
import com.camping.legacy.dto.ReservationResponse;
import com.camping.legacy.client.ReservationClient;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

import static com.camping.legacy.builder.ReservationRequestBuilder.aReservation;
import static org.assertj.core.api.Assertions.assertThat;

public class ReservationSteps {

    public static ReservationResponse 예약_생성됨(String 고객명, String 사이트번호, LocalDate 시작일, LocalDate 종료일) {
        var request = aReservation()
                .customerName(고객명)
                .siteNumber(사이트번호)
                .period(시작일, 종료일)
                .build();
        var response = ReservationClient.예약_생성_API(request);
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        return response.as(ReservationResponse.class);
    }

    public static ReservationResponse 예약_생성됨(ReservationRequest request) {
        var response = ReservationClient.예약_생성_API(request);
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        return response.as(ReservationResponse.class);
    }

    public static void 예약_취소됨(ReservationResponse 예약) {
        ReservationClient.예약_취소_API(예약.getId(), 예약.getConfirmationCode());
    }

    public static ExtractableResponse<Response> 예약_생성_요청(ReservationRequest request) {
        return ReservationClient.예약_생성_API(request);
    }

    public static ExtractableResponse<Response> 예약_취소_요청(Long 예약ID, String 확인코드) {
        return ReservationClient.예약_취소_API(예약ID, 확인코드);
    }

    public static void 예약_생성_성공(ExtractableResponse<Response> 응답) {
        assertThat(응답.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    }

    public static void 예약_취소_성공(ExtractableResponse<Response> 응답) {
        assertThat(응답.statusCode()).isEqualTo(HttpStatus.OK.value());
    }

    public static void 예약_상태_확인(Long 예약ID, String 기대상태) {
        var 예약 = ReservationClient.예약_조회_API(예약ID).as(ReservationResponse.class);
        assertThat(예약.getStatus()).isEqualTo(기대상태);
    }

    public static void 에러_응답_확인(ExtractableResponse<Response> 응답, HttpStatus 상태코드, String 에러메시지) {
        assertThat(응답.statusCode()).isEqualTo(상태코드.value());
        assertThat(응답.jsonPath().getString("message")).isEqualTo(에러메시지);
    }

    public static ReservationResponse 예약_응답_추출(ExtractableResponse<Response> 응답) {
        return 응답.as(ReservationResponse.class);
    }

    public static LocalDate 오늘() {
        return LocalDate.now();
    }

    public static LocalDate 일_후(int days) {
        return LocalDate.now().plusDays(days);
    }

    public static LocalDate 일_전(int days) {
        return LocalDate.now().minusDays(days);
    }
}
