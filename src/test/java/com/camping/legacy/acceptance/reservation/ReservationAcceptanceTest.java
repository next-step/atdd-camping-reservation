package com.camping.legacy.acceptance.reservation;

import static org.assertj.core.api.Assertions.assertThat;

import com.camping.legacy.acceptance.BaseAcceptanceTest;
import com.camping.legacy.acceptance.reservation.support.db.CampsiteSeed;
import com.camping.legacy.acceptance.reservation.support.fixture.ReservationRequestFixture;
import com.camping.legacy.acceptance.reservation.support.http.ReservationApi;
import com.camping.legacy.dto.ReservationRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.springframework.http.HttpStatus;

class ReservationAcceptanceTest extends BaseAcceptanceTest {

    static final String DEFAULT_SITE_NUMBER = "A-1";

    @BeforeEach
    void setUpDefaultSite() {
        CampsiteSeed.ensure(jdbc, DEFAULT_SITE_NUMBER);
    }

    @DisplayName("예약 생성 API가 잘 동작하는지")
    @Test
    void reservationTest() {
        // when: 사용자가 필수 정보를 모두 입력하고 예약을 시도한다
        final LocalDate start = LocalDate.now();
        final LocalDate end = start.plusDays(1);
        final String customerName = "TEST";
        final String phoneNumber = "010-0000-0000";

        ReservationRequest request = ReservationRequestFixture.builder()
                .siteNumber(DEFAULT_SITE_NUMBER)
                .startDate(start)
                .endDate(end)
                .customerName(customerName)
                .phoneNumber(phoneNumber)
                .build();

        var response = ReservationApi.post(request);

        // then: 예약이 성공한다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    }

    @DisplayName("필수 정보(이름)가 누락된 상태로 예약을 시도하면 예약이 실패하는지")
    @ParameterizedTest
    @NullAndEmptySource
    void reservationTestWithMissingCustomerName(String missingCustomerName) {
        final LocalDate start = LocalDate.now();
        final LocalDate end = start.plusDays(1);
        final String phoneNumber = "010-0000-0000";

        ReservationRequest request = ReservationRequestFixture.builder()
                .siteNumber(DEFAULT_SITE_NUMBER)
                .startDate(start)
                .endDate(end)
                .customerName(missingCustomerName)
                .phoneNumber(phoneNumber)
                .build();

        var response = ReservationApi.post(request);

        // then: 예약이 실패한다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @DisplayName("필수 정보(날짜)가 누락된 상태로 예약을 시도하면 예약이 실패하는지")
    @ParameterizedTest
    @NullSource
    void reservationTestWithMissingInfo(LocalDate emptyDate) {
        // when: 사용자가 필수 정보(날짜)를 누락하고 예약을 시도한다
        final String customerName = "TEST";
        final String phoneNumber = "010-0000-0000";

        ReservationRequest request = ReservationRequestFixture.builder()
                .siteNumber(DEFAULT_SITE_NUMBER)
                .startDate(emptyDate)
                .endDate(emptyDate)
                .customerName(customerName)
                .phoneNumber(phoneNumber)
                .build();

        var response = ReservationApi.post(request);

        // then: 예약이 실패한다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @DisplayName("필수 정보(사이트 번호)가 누락된 상태로 예약을 시도하면 예약이 실패하는지")
    @ParameterizedTest
    @NullAndEmptySource
    void reservationTestWithMissingSiteNumber(String missingSiteNumber) {
        // when: 사용자가 필수 정보(사이트 번호)를 누락하고 예약을 시도한다
        final LocalDate start = LocalDate.now();
        final LocalDate end = start.plusDays(1);
        final String customerName = "TEST";
        final String phoneNumber = "010-0000-0000";

        ReservationRequest request = ReservationRequestFixture.builder()
                .siteNumber(missingSiteNumber)
                .startDate(start)
                .endDate(end)
                .customerName(customerName)
                .phoneNumber(phoneNumber)
                .build();

        var response = ReservationApi.post(request);

        // then: 예약이 실패한다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @DisplayName("필수 정보(전화번호)가 누락된 상태로 예약을 시도하면 예약이 실패하는지")
    @ParameterizedTest
    @NullSource
    void reservationTestWithMissingPhoneNumber(String missingPhoneNumber) {
        // when: 사용자가 필수 정보(전화번호)를 누락하고 예약을 시도한다
        final LocalDate start = LocalDate.now();
        final LocalDate end = start.plusDays(1);
        final String customerName = "TEST";

        ReservationRequest request = ReservationRequestFixture.builder()
                .siteNumber(DEFAULT_SITE_NUMBER)
                .startDate(start)
                .endDate(end)
                .customerName(customerName)
                .phoneNumber(missingPhoneNumber)
                .build();

        var response = ReservationApi.post(request);

        // then: 예약이 실패한다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @DisplayName("사용자가 오늘로부터 30일 이후 시점의 예약을 시도하면 예약이 실패하는지")
    @Test
    void reservationTestWithTooFarDate() {
        // when: 사용자가 필수 정보를 모두 입력하고 오늘로부터 30일 이후 시점에 예약을 시도한다
        final LocalDate start = LocalDate.now().plusDays(31);
        final LocalDate end = start.plusDays(1);
        final String customerName = "TEST";
        final String phoneNumber = "010-0000-0000";

        ReservationRequest request = ReservationRequestFixture.builder()
                .siteNumber(DEFAULT_SITE_NUMBER)
                .startDate(start)
                .endDate(end)
                .customerName(customerName)
                .phoneNumber(phoneNumber)
                .build();

        var response = ReservationApi.post(request);

        // then: 예약이 실패한다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @DisplayName("종료일이 시작일보다 이전인 날짜로 예약을 시도하면 예약이 실패하는지")
    @Test
    void reservationTestWithInvalidDateRange() {
        // when: 사용자가 필수 정보를 모두 입력하고 시작일이 종료일보다 이후인 날짜로 예약을 시도한다
        final LocalDate start = LocalDate.now().plusDays(5);
        final LocalDate end = start.minusDays(1);
        final String customerName = "TEST";
        final String phoneNumber = "010-0000-0000";

        ReservationRequest request = ReservationRequestFixture.builder()
                .siteNumber(DEFAULT_SITE_NUMBER)
                .startDate(start)
                .endDate(end)
                .customerName(customerName)
                .phoneNumber(phoneNumber)
                .build();

        var response = ReservationApi.post(request);

        // then: 예약이 실패한다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @DisplayName("동일한 사이트와 날짜에 중복 예약이 있을 때 예약이 실패하는지")
    @Test
    void reservationTestWithDuplicateBooking() {
        // given: 특정 사이트와 기간에 예약이 존재한다
        final LocalDate start = LocalDate.now();
        final LocalDate end = start.plusDays(1);

        ReservationRequest existingRequest = ReservationRequestFixture.builder()
                .siteNumber(DEFAULT_SITE_NUMBER)
                .startDate(start)
                .endDate(end)
                .customerName("EXISTING")
                .phoneNumber("010-0000-0000")
                .build();

        ReservationApi.post(existingRequest, HttpStatus.CREATED);

        // when: 사용자가 필수 정보를 모두 입력하고 예약을 시도한다
        ReservationRequest newRequest = ReservationRequestFixture.builder()
                .siteNumber(DEFAULT_SITE_NUMBER)
                .startDate(start)
                .endDate(end)
                .customerName("NEW")
                .phoneNumber("010-1111-1111")
                .build();

        var response = ReservationApi.post(newRequest);

        // then: 예약이 실패한다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @DisplayName("동일한 사이트와 날짜에 취소된 예약이 있을 때 새로운 예약이 성공하는지")
    @Test
    void reservationTestWithCancelledBooking() {
        // given: 특정 사이트와 기간에 취소된 예약이 존재한다
        final LocalDate start = LocalDate.now();
        final LocalDate end = start.plusDays(1);

        ReservationRequest existingRequest = ReservationRequestFixture.builder()
                .siteNumber(DEFAULT_SITE_NUMBER)
                .startDate(start)
                .endDate(end)
                .customerName("EXISTING")
                .phoneNumber("010-0000-0000")
                .build();

        var existingResponse = ReservationApi.post(existingRequest);

        Long reservationId = existingResponse.jsonPath().getLong("id");
        String confirmationCode = existingResponse.jsonPath().getString("confirmationCode");

        ReservationApi.delete(reservationId, confirmationCode, HttpStatus.OK);

        // when: 사용자가 필수 정보를 모두 입력하고 예약을 시도한다
        ReservationRequest newRequest = ReservationRequestFixture.builder()
                .siteNumber(DEFAULT_SITE_NUMBER)
                .startDate(start)
                .endDate(end)
                .customerName("NEW")
                .phoneNumber("010-1111-1111")
                .build();

        var response = ReservationApi.post(newRequest);

        // then: 예약이 성공한다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    }
}
