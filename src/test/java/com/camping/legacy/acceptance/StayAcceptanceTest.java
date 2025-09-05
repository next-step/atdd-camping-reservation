package com.camping.legacy.acceptance;

import static com.camping.legacy.acceptance.helper.ReservationTestHelper.createReservation;
import static com.camping.legacy.acceptance.helper.ReservationTestHelper.reservationRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class StayAcceptanceTest extends AcceptanceTestBase {

    private static final String REGEX_CONFIRM_CODE = "^[A-Za-z0-9]{6}$";

    @DisplayName("해당 기간 내 모든 날짜가 예약 가능하면 연박 예약에 성공한다.")
    @Test
    void staySuccessTest() {
        // given
        LocalDate startDate = LocalDate.of(2025, 9, 15);
        LocalDate endDate = startDate.plusDays(5);

        Map<String, String> request = reservationRequest()
                .withCustomerName("김철수")
                .withStartDate(startDate.toString())
                .withEndDate(endDate.toString())
                .build();

        // when
        ExtractableResponse<Response> extract =
                createReservation(request);

        // then
        assertAll(() -> assertThat(extract.statusCode()).isEqualTo(HttpStatus.CREATED.value()),
                () -> assertThat(extract.jsonPath().getString("confirmationCode")).matches(REGEX_CONFIRM_CODE));
    }

    @DisplayName("연박 기간 중 일부 날짜가 이미 예약된 경우 어떤 날짜도 예약되지 않는다.")
    @Test
    void stayDuplicateTest() {
        // given
        LocalDate startDate = LocalDate.of(2025, 9, 15);
        LocalDate endDate = startDate.plusDays(5);

        Map<String, String> request = reservationRequest()
                .withCustomerName("김철수")
                .withStartDate(startDate.toString())
                .withEndDate(endDate.toString())
                .build();

        createReservation(request);

        // when
        Map<String, String> anotherRequest = reservationRequest()
                .withCustomerName("박영희")
                .withPhoneNumber("010-9876-5432")
                .withStartDate(startDate.plusDays(2).toString())
                .withEndDate(startDate.plusDays(2).toString())
                .build();

        ExtractableResponse<Response> extract = createReservation(anotherRequest);

        // then
        assertAll(
                () -> assertThat(extract.statusCode()).isEqualTo(HttpStatus.CONFLICT.value()),
                () -> assertThat(extract.jsonPath().getString("message")).isEqualTo("해당 기간에 이미 예약이 존재합니다.")
        );
    }

    @DisplayName("최대 예약 가능 기간을 초과하는 연박 예약은 실패된다.")
    @Test
    void stayExceedMaxDaysTest() {
        // given
        LocalDate endDate = TODAY.plusDays(31);

        Map<String, String> request = reservationRequest()
                .withCustomerName("김철수")
                .withStartDate(TODAY.toString())
                .withEndDate(endDate.toString())
                .build();
        // when
        ExtractableResponse<Response> extract = createReservation(request);

        // then
        assertThat(extract.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(extract.jsonPath().getString("message")).isEqualTo("최대 예약 가능 기간을 초과했습니다.");
    }

    @DisplayName("연박 예약 취소 시, 관련된 모든 날짜의 예약 상태가 정상적으로 취소된다.")
    @Test
    void cancelStayTest() {
        // given
        LocalDate startDate = TODAY.plusDays(1);
        LocalDate endDate = TODAY.plusDays(5);

        Map<String, String> request = reservationRequest()
                .withCustomerName("김철수")
                .withStartDate(startDate.toString())
                .withEndDate(endDate.toString())
                .build();

        ExtractableResponse<Response> createResponse = createReservation(request);

        String cancelId = createResponse.jsonPath().getString("id");
        String confirmationCode = createResponse.jsonPath().getString("confirmationCode");

        ExtractableResponse<Response> extract = RestAssured
                .given().log().all()
                .param("confirmationCode", confirmationCode)
                .when()
                .delete("/api/reservations/" + cancelId)
                .then().log().all()
                .extract();

        // then
        assertAll(
                () -> assertThat(extract.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value()),
                () -> assertThat(extract.jsonPath().getString("message")).isEqualTo("예약이 취소되었습니다.")
        );
    }
}
