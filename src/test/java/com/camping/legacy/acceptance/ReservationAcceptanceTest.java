package com.camping.legacy.acceptance;

import static com.camping.legacy.acceptance.helper.ReservationTestHelper.createReservation;
import static com.camping.legacy.acceptance.helper.ReservationTestHelper.reservationRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.Map;

import static com.camping.legacy.acceptance.helper.ReservationTestHelper.reservationRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class ReservationAcceptanceTest extends AcceptanceTestBase {

    private static final String REGEX_CONFIRM_CODE = "^[A-Za-z0-9]{6}$";

    @DisplayName("정상적인 예약이 생성된다. 예약 완료 시 예약 확인 코드를 받는다.")
    @Test
    void reservationSuccessTest() {
        // given - 일반적인 예약 패턴
        Map<String, String> request = reservationRequest()
                .withStartDate(LocalDate.of(2025, 9, 5).toString())
                .withEndDate(LocalDate.of(2025, 9, 8).toString())
                .build();

        // when - 정상적인 패턴으로 예약한다.
        ExtractableResponse<Response> extract = RestAssured
                .given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(request)
                .when()
                .post("/api/reservations")
                .then().log().all()
                .extract();

        // then - 성공 코드와 메세지를 받는다.
        assertAll(
                () -> assertThat(extract.statusCode()).isEqualTo(HttpStatus.CREATED.value()),
                () -> assertThat(extract.jsonPath().getString("confirmationCode")).matches(REGEX_CONFIRM_CODE)
        );
    }

    @DisplayName("예약은 오늘로부터 30일 이내에만 가능해야 한다.")
    @Test
    void reservationDateLimitTest() {
        // given - 30일 이후가 포함된 예약 요청
        String startDate = TODAY.plusDays(28).toString();
        String endDate = TODAY.plusDays(31).toString();

        Map<String, String> request = reservationRequest()
                .withStartDate(startDate)
                .withEndDate(endDate)
                .build();

        // when - 30일 이후의 날짜 예약을 요청하면
        ExtractableResponse<Response> extract = createReservation(request);

        // then - 예약에 실패하고, 30일 이후의 날짜는 예약할 수 없다는 에러메시지를 받는다.
        assertAll(
                () -> assertThat(extract.statusCode()).isEqualTo(HttpStatus.CONFLICT.value()),
                () -> assertThat(extract.jsonPath().getString("message")).isEqualTo("30일 이후의 날짜는 예약할 수 없습니다.")
        );
    }

    @DisplayName("과거 날짜 예약 시도시 예약이 거부되어야 한다.")
    @Test
    void reservationPastDateTest() {
        // given
        LocalDate startDate = TODAY.minusDays(1);

        Map<String, String> request = reservationRequest()
                .withStartDate(startDate.toString())
                .withEndDate(TODAY.toString())
                .build();

        // when - 과거의 날짜로 예약을 요청하면
        ExtractableResponse<Response> extract = createReservation(request);

        // then - 예약에 실패하고, 과거 날짜는 예약할 수 없습니다 메시지가 반환된다.
        assertThat(extract.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(extract.jsonPath().getString("message")).isEqualTo("과거 날짜는 예약할 수 없습니다.");
    }

    @DisplayName("종료일이 시작일보다 빠른 경우에는 예약이 거부되어야 한다.")
    @Test
    void reservationEndDateBeforeStartDateTest() {
        // given
        LocalDate startDate = TODAY.plusDays(3);
        LocalDate endDate = TODAY.plusDays(1);

        Map<String, String> request = reservationRequest()
                .withStartDate(startDate.toString())
                .withEndDate(endDate.toString())
                .build();

        // when
        ExtractableResponse<Response> extract = createReservation(request);

        // then
        assertThat(extract.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(extract.jsonPath().getString("message")).isEqualTo("종료일이 시작일보다 이전일 수 없습니다.");
    }

    @DisplayName("필수 정보인 예약자 이름이 누락 되면 예약이 실패해야 한다.")
    @Test
    void reservationMissingCustomerNameTest() {
        // given
        LocalDate startDate = TODAY.plusDays(1);
        LocalDate endDate = TODAY.plusDays(3);

        Map<String, String> request = reservationRequest()
                .withCustomerName("")
                .withStartDate(startDate.toString())
                .withEndDate(endDate.toString())
                .build();

        // when
        ExtractableResponse<Response> extract = createReservation(request);

        // then
        assertThat(extract.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(extract.jsonPath().getString("message")).isEqualTo("예약자 이름을 입력해주세요.");
    }

    @DisplayName("필수 정보인 예약자 전화번호가 누락 되면 예약이 실패해야 한다.")
    @Test
    void reservationMissingPhoneNumberTest() {
        // given
        LocalDate startDate = TODAY.plusDays(1);
        LocalDate endDate = TODAY.plusDays(3);

        Map<String, String> request = reservationRequest()
                .withStartDate(startDate.toString())
                .withEndDate(endDate.toString())
                .withPhoneNumber("")
                .build();

        // when
        ExtractableResponse<Response> extract = createReservation(request);

        // then
        assertThat(extract.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(extract.jsonPath().getString("message")).isEqualTo("예약자 전화번호를 입력해주세요.");
    }

    @DisplayName("동일 사이트, 동일 기간 중복 예약이 방지되어야한다.")
    @Test
    void reservationDuplicateTest() {
        // given
        LocalDate startDate = TODAY.plusDays(1);
        LocalDate endDate = TODAY.plusDays(3);

        Map<String, String> request = reservationRequest()
                .withStartDate(startDate.toString())
                .withEndDate(endDate.toString())
                .build();

        createReservation(request);

        Map<String, String> anotherRequest = reservationRequest()
                .withCustomerName("이영희")
                .withStartDate(startDate.toString())
                .withEndDate(endDate.toString())
                .withPhoneNumber("010-9876-5432")
                .build();
        // when
        ExtractableResponse<Response> extract = createReservation(anotherRequest);

        // then
        assertThat(extract.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(extract.jsonPath().getString("message")).isEqualTo("해당 기간에 이미 예약이 존재합니다.");
    }
}
