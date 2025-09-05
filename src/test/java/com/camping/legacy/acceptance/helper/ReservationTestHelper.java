package com.camping.legacy.acceptance.helper;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.MediaType;

public class ReservationTestHelper {

    public static ExtractableResponse<Response> createReservation(Map<String, String> request) {
        return RestAssured
                .given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(request)
                .when()
                .post("/api/reservations")
                .then().log().all()
                .extract();
    }

    public static ReservationRequestBuilder reservationRequest() {
        return new ReservationRequestBuilder();
    }

    public static class ReservationRequestBuilder {
        private String customerName = "홍길동";
        private String startDate = "2025-09-05";
        private String endDate = "2025-09-06";
        private String siteNumber = "A-1";
        private String phoneNumber = "010-1234-5678";
        private String numberOfPeople = "2";
        private String carNumber = "15하-1234";
        private String requests = "어메니티 제공해주세요.";

        public ReservationRequestBuilder withCustomerName(String customerName) {
            this.customerName = customerName;
            return this;
        }

        public ReservationRequestBuilder withStartDate(String startDate) {
            this.startDate = startDate;
            return this;
        }

        public ReservationRequestBuilder withEndDate(String endDate) {
            this.endDate = endDate;
            return this;
        }

        public ReservationRequestBuilder withSiteNumber(String siteNumber) {
            this.siteNumber = siteNumber;
            return this;
        }

        public ReservationRequestBuilder withPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public ReservationRequestBuilder withNumberOfPeople(String numberOfPeople) {
            this.numberOfPeople = numberOfPeople;
            return this;
        }

        public ReservationRequestBuilder withCarNumber(String carNumber) {
            this.carNumber = carNumber;
            return this;
        }

        public ReservationRequestBuilder withRequests(String requests) {
            this.requests = requests;
            return this;
        }

        public Map<String, String> build() {
            Map<String, String> request = new HashMap<>();
            request.put("customerName", customerName);
            request.put("startDate", startDate);
            request.put("endDate", endDate);
            request.put("siteNumber", siteNumber);
            request.put("phoneNumber", phoneNumber);
            request.put("numberOfPeople", numberOfPeople);
            request.put("carNumber", carNumber);
            request.put("requests", requests);
            return request;
        }
    }
}
