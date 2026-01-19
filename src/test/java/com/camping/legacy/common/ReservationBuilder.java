package com.camping.legacy.common;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

import static com.camping.legacy.common.TestFixture.오늘부터_N일_후;

/**
 * 예약 생성 요청을 위한 빌더 클래스
 * 기본값을 제공하여 테스트에서 필요한 값만 설정할 수 있음
 */
public class ReservationBuilder {

    private String siteNumber = "A-1";
    private String startDate = 오늘부터_N일_후(30);
    private String endDate = 오늘부터_N일_후(32);
    private String customerName = "테스트고객";
    private int numberOfPeople = 4;
    private String phoneNumber = "010-1234-5678";

    public static ReservationBuilder 예약() {
        return new ReservationBuilder();
    }

    public ReservationBuilder 사이트(String siteNumber) {
        this.siteNumber = siteNumber;
        return this;
    }

    public ReservationBuilder 기간(String startDate, String endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
        return this;
    }

    public ReservationBuilder 시작일(String startDate) {
        this.startDate = startDate;
        return this;
    }

    public ReservationBuilder 종료일(String endDate) {
        this.endDate = endDate;
        return this;
    }

    public ReservationBuilder 고객명(String customerName) {
        this.customerName = customerName;
        return this;
    }

    public ReservationBuilder 인원(int numberOfPeople) {
        this.numberOfPeople = numberOfPeople;
        return this;
    }

    public ReservationBuilder 연락처(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        return this;
    }

    public ExtractableResponse<Response> 생성_요청() {
        Map<String, Object> params = new HashMap<>();
        params.put("siteNumber", siteNumber);
        params.put("startDate", startDate);
        params.put("endDate", endDate);
        params.put("customerName", customerName);
        params.put("numberOfPeople", numberOfPeople);
        params.put("phoneNumber", phoneNumber);

        return RestAssured
                .given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(params)
                .when().post("/api/reservations")
                .then().log().all()
                .extract();
    }

    /**
     * 예약 생성 후 결과를 담는 클래스
     */
    public static class ReservationResult {
        private final ExtractableResponse<Response> response;
        private final Long id;
        private final String confirmationCode;

        public ReservationResult(ExtractableResponse<Response> response) {
            this.response = response;
            this.id = response.jsonPath().getLong("id");
            this.confirmationCode = response.jsonPath().getString("confirmationCode");
        }

        public ExtractableResponse<Response> 응답() {
            return response;
        }

        public Long ID() {
            return id;
        }

        public String 확인코드() {
            return confirmationCode;
        }
    }

    public ReservationResult 생성() {
        return new ReservationResult(생성_요청());
    }
}