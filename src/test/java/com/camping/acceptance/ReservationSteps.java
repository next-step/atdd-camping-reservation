package com.camping.acceptance;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.http.MediaType;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class ReservationSteps {

    public static final String API_RESERVATIONS = "/api/reservations/";
    public static final String API_RESERVATIONS_FOR_POST = "/api/reservations";
    public static final String API_SITES_SEARCH = "/api/sites/search";
    public static final String API_RESERVATIONS_CALENDAR = "/api/reservations/calendar";

    public static ExtractableResponse<Response> 예약을_생성한다(Map<String, Object> request) {
        return RestAssured.given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(request)
                .when()
                .post(API_RESERVATIONS_FOR_POST)
                .then().log().all()
                .extract();
    }

    public static Map<String, Object> 예약_요청_생성(String 사이트_번호, LocalDate 시작날짜, LocalDate 마감날짜, String 이름, String 핸드폰번호) {
        Map<String, Object> request = new HashMap<>();
        request.put("siteNumber", 사이트_번호);
        request.put("startDate", 시작날짜.toString());
        request.put("endDate", 마감날짜.toString());
        request.put("customerName", 이름);
        request.put("phoneNumber", 핸드폰번호);
        return request;
    }

    public static ExtractableResponse<Response> 정확한_확인_코드로_예약을_취소한다(String confirmationCode, Long id) {
        return RestAssured
                .given().log().all()
                .queryParam("confirmationCode", confirmationCode)
                .when()
                .delete(API_RESERVATIONS + id)
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 예약ID로_예약을_조회한다(Long reservationId) {
        return RestAssured
                .when()
                .get(API_RESERVATIONS + reservationId)
                .then().log().all()
                .extract();
    }
    
    public static ExtractableResponse<Response> 잘못된_코드로_예약을_취소한다(String wrongCode, Long id) {
        return RestAssured
                .given().log().all()
                .queryParam("confirmationCode", wrongCode)
                .when()
                .delete(API_RESERVATIONS + id)
                .then().log().all()
                .extract();
    }
    
    public static ExtractableResponse<Response> 캘린터에서_특정사이트_예약을_조회한다(int 년도, int 월, Long 사이트_ID) {
        return RestAssured
                .given().log().all()
                .param("year", 년도)
                .param("month", 월)
                .param("siteId", 사이트_ID)
                .when()
                .get(API_RESERVATIONS_CALENDAR)
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 날짜로_예약을_조회한다(LocalDate reservationDate, LocalDate reservationEndDate) {
        return RestAssured
                .given().log().all()
                .param("startDate", reservationDate.toString())
                .param("endDate", reservationEndDate.toString())
                .when()
                .get(API_SITES_SEARCH)
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 이름과_전화번호로_예약을_조회한다(String name, String phone) {
        return RestAssured
                .given().log().all()
                .param("name", name)
                .param("phone", phone)
                .when()
                .get("/api/reservations/my")
                .then().log().all()
                .extract();
    }
}
