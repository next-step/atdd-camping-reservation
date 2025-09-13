package com.camping.legacy.acceptance.utils;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

public class ResponseAcceptanceFixture {

    public static void assertCreatedSuccessfully(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    }

    public static void assertUpdatedSuccessfully(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    }

    public static void assertDeletedSuccessfully(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
    }

    public static void assertFoundSuccessfully(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    }

    public static void assertBadRequestWithMessage(ExtractableResponse<Response> response, String expectedMessage) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.jsonPath().getString("message")).isEqualTo(expectedMessage);
    }

    public static void assertNotFound(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    public static void assertReservationDetails(ExtractableResponse<Response> response, String expectedSite, String expectedStatus) {
        assertFoundSuccessfully(response);
        assertThat(response.jsonPath().getString("siteNumber")).isEqualTo(expectedSite);
        assertThat(response.jsonPath().getString("status")).isEqualTo(expectedStatus);
    }

    public static void assertCustomerInfo(ExtractableResponse<Response> response, String expectedName, String expectedPhone) {
        assertFoundSuccessfully(response);
        assertThat(response.jsonPath().getString("customerName")).isEqualTo(expectedName);
        assertThat(response.jsonPath().getString("phoneNumber")).isEqualTo(expectedPhone);
    }

    public static void assertReservationDates(ExtractableResponse<Response> response, String expectedStartDate, String expectedEndDate) {
        assertFoundSuccessfully(response);
        assertThat(response.jsonPath().getString("startDate")).isEqualTo(expectedStartDate);
        assertThat(response.jsonPath().getString("endDate")).isEqualTo(expectedEndDate);
    }

    public static void assertConfirmationCodeExists(ExtractableResponse<Response> response) {
        assertFoundSuccessfully(response);
        String confirmationCode = response.jsonPath().getString("confirmationCode");
        assertThat(confirmationCode).isNotNull();
        assertThat(confirmationCode).hasSize(6);
        assertThat(confirmationCode).matches("[A-Z0-9]{6}");
    }
}
