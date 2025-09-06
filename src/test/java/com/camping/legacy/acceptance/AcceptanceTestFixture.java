package com.camping.legacy.acceptance;

import com.camping.legacy.dto.ReservationRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

public class AcceptanceTestFixture {
    private static final LocalDate now = LocalDate.now();
    private static final LocalDate startDate = now.plusDays(20);

    public static ReservationRequest createReservationRequest() {
        return ReservationRequestBuilder.builder()
                .build();
    }

    public static ReservationRequest createSameReservationRequest() {
        return ReservationRequestBuilder.builder()
                .name("박철수")
                .phoneNumber("010-9876-5432")
                .build();
    }

    public static ReservationRequest createCancelledReservationRequest() {
        예약_취소_성공();
        return ReservationRequestBuilder.builder()
                .build();
    }

    public static ReservationRequest createCancelledSameReservationRequest() {
        예약_취소_성공();
        return ReservationRequestBuilder.builder()
                .name("박철수")
                .phoneNumber("010-9876-5432")
                .build();
    }

    public static ReservationRequest createWrongReservationRequest() {
        return ReservationRequestBuilder.builder()
                .startDate(now.plusDays(30))
                .endDate(now.plusDays(30))
                .build();
    }

    public static ReservationRequest createBookedReservationRequest() {
        return ReservationRequestBuilder.builder()
                .build();
    }

    public static ExtractableResponse<Response> 예약_생성_성공() {
        ReservationRequest request = createReservationRequest();

        ExtractableResponse<Response> response = RestAssured
                .given().log().all()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/reservations")
                .then().log().all()
                .extract();

        assertThat(response.statusCode()).isEqualTo(201);
        return response;
    }

    public static ExtractableResponse<Response> 예약_취소_성공() {
        ExtractableResponse<Response> successResponse = 예약_생성_성공();

        ExtractableResponse<Response> response = RestAssured
                .given().log().all()
                .contentType(ContentType.JSON)
                .param("confirmationCode", successResponse.jsonPath().getString("confirmationCode"))
                .when()
                .delete("/api/reservations/" + successResponse.jsonPath().getLong("id"))
                .then().log().all()
                .extract();

        assertThat(response.statusCode()).isEqualTo(200);
        return response;
    }

    public static ReservationRequest createConsecutiveReservationRequest() {
        예약_취소_성공();
        return ReservationRequestBuilder.builder()
                .name("김연박")
                .endDate(startDate.plusDays(2))
                .siteName("B-2")
                .phoneNumber("010-1111-2222")
                .numberOfPeople(4)
                .build();
    }

    public static ReservationRequest createConsecutiveWithCancelledReservationRequest() {
        예약_취소_성공();
        return ReservationRequestBuilder.builder()
                .name("박연박")
                .endDate(startDate.plusDays(2))
                .siteName("C-3")
                .phoneNumber("010-3333-4444")
                .numberOfPeople(3)
                .build();
    }

    public static ReservationRequest createBlockedConsecutiveReservationRequest() {
        return ReservationRequestBuilder.builder()
                .name("이연박")
                .startDate(now.plusDays(5))
                .endDate(now.plusDays(7))
                .phoneNumber("010-5555-6666")
                .build();
    }

    public static ReservationRequest createExistingReservationInConsecutivePeriod() {
        return ReservationRequestBuilder.builder()
                .name("최기존")
                .startDate(now.plusDays(6))
                .endDate(now.plusDays(6))
                .phoneNumber("010-7777-8888")
                .build();
    }

    public static ExtractableResponse<Response> getCreateReservationApiResponse(ReservationRequest request) {
        return RestAssured
                .given().log().all()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/reservations")
                .then().log().all()
                .extract();
    }
}
