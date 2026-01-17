package com.camping.legacy.acceptance;

import com.camping.legacy.acceptance.fixtures.ReservationRequest;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

/**
 * RestAssured 기반 공통 API 호출을 모아둔 베이스.
 * - 예약/사이트 등 여러 인수테스트에서 재사용합니다.
 */
@SuppressWarnings("NonAsciiCharacters")
public abstract class ApiAcceptanceTestBase extends AcceptanceTestBase {

    public ExtractableResponse<Response> 예약을_생성한다(ReservationRequest request) {
        return given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(예약_API)
                .then()
                .extract();
    }

    public ExtractableResponse<Response> 예약을_수정한다(
            Long reservationId,
            String confirmationCode,
            ReservationRequest request
    ) {
        return given()
                .contentType(ContentType.JSON)
                .queryParam("confirmationCode", confirmationCode)
                .body(request)
                .when()
                .put(예약_API + "/" + reservationId)
                .then()
                .extract();
    }

    public ExtractableResponse<Response> 사이트_예약_가능_여부를_조회한다(String siteNumber, String date) {
        return given()
                .accept(ContentType.JSON)
                .queryParam("date", date)
                .when()
                .get(String.format(사이트_예약_가능_여부_API, siteNumber))
                .then()
                .extract();
    }

    public ExtractableResponse<Response> 특정_날짜에_예약_가능한_사이트_목록을_조회한다(String date) {
        return given()
                .accept(ContentType.JSON)
                .queryParam("date", date)
                .when()
                .get(예약_가능_목록_조회_API)
                .then()
                .extract();
    }

    public ExtractableResponse<Response> 기간으로_사이트를_검색한다(String startDate, String endDate) {
        return given()
                .accept(ContentType.JSON)
                .queryParam("startDate", startDate)
                .queryParam("endDate", endDate)
                .when()
                .get(검색_API)
                .then()
                .extract();
    }
}
