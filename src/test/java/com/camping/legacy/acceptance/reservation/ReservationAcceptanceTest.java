package com.camping.legacy.acceptance.reservation;

import com.camping.legacy.acceptance.reservation.builder.ReservationTestDataBuilder;
import com.camping.legacy.acceptance.utils.AcceptanceTestBase;
import com.camping.legacy.acceptance.utils.RequestAcceptanceFixture;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.Map;

import static com.camping.legacy.acceptance.utils.RequestAcceptanceFixture.*;
import static com.camping.legacy.acceptance.utils.RequestAcceptanceFixture.updateReservation;
import static com.camping.legacy.acceptance.utils.ResponseAcceptanceFixture.*;
import static org.assertj.core.api.Assertions.assertThat;

public class ReservationAcceptanceTest extends AcceptanceTestBase {
    LocalDate today = LocalDate.now();
    @DisplayName("유효한 정보를 통해 예약할 수 있다")
    @Test
    void reservationSuccessCase() {
        /**
         * 누가 : 예약하려는 사용자
         * 언제 : 오늘
         * 무엇을 : 30일내로 비어있는 날짜에 대한 예약을
         * 왜 : 유효한 정보로 예약에 성공하기 위해
         */
        // given - 오늘 날짜를 기준으로 30일 이내의 날짜를 계산하고
        ReservationTestDataBuilder builder = new ReservationTestDataBuilder();
        Map<String, String> requestMap = builder.withSiteNumber("B-1").buildRequestMap();

        // when - 30일 이내 날짜의 예약을 신청했을 때
        ExtractableResponse<Response> response = createReservation(requestMap);

        // then - 예약에 성공한다
        assertCreatedSuccessfully(response);
    }

    @DisplayName("오늘로부터 31일 이후의 날짜는 예약할 수 없다")
    @Test // 테스트가 뜰 때, Spring 부트 애플리케이션에 해당되는 컴포넌트들이 테스트 컨테이너에 Bean으로 같이 환경으로 딸려 올라감.
    // 이때, 딸려올라가는 웹 설정도 같이 해줄 수 있다.
    void reservationDateLimit() {
        /**
         * 누가 : 예약하려는 사용자
         * 언제 : 오늘
         * 무엇을 : 31일 이후의 날짜 예약을
         * 왜 : 31일 이후의 날짜는 예약할 수 없다는 안내를 받기 위해
         */
        // given - 오늘 날짜를 기준으로 30일 이후의 날짜를 계산하고
        ReservationTestDataBuilder builder = new ReservationTestDataBuilder();
        Map<String, String> requestMap = builder.withSiteNumber("B-1").withDatesInFuture(31, 32).buildRequestMap();

        // when - 30일 이후 날짜의 예약을 신청했을 때
        ExtractableResponse<Response> response = createReservation(requestMap);

        // then - 예약에 실패하고, "30일 이후의 날짜는 예약할 수 없다는 안내"를 받는다.
        assertBadRequestWithMessage(response, "오늘로부터 30일 이내로만 예약할 수 있습니다.");
    }

    @DisplayName("존재하는 캠핑장만 예약 수정할 수 있다")
    @Test
    void reservationExistCampsite() {
        /**
         * 누가 : 홍길동이
         * 무엇을 : 기존 예약 정보를
         * 어떻게 : 다른 캠핑장으로 예약 수정한다
         * 왜 : 존재하는 캠핑장에 대해 예약이 정상 수정되는지 확인하기 위해
         */
        // given - 기존 예약 정보에 수정하려는 캠핑장으로
        Long reservationId = 1L;
        ExtractableResponse<Response> previousReservation = getReservation(reservationId);

        ReservationTestDataBuilder builder = new ReservationTestDataBuilder();
        Map<String, String> afterRequest = builder.withSiteNumber("A-2").buildRequestMap();

        // when - 예약을 수정했을 때
        ExtractableResponse<Response> response = updateReservation(reservationId, previousReservation.jsonPath().getString("confirmationCode"), afterRequest);

        // then - 예약 수정에 성공한다
        assertUpdatedSuccessfully(response);
    }

    @DisplayName("존재하지 않는 캠핑장은 예약 수정이 불가능하다")
    @Test
    void reservationNotExistCampsite() {
        /**
         * 누가 : 홍길동이
         * 무엇을 : 기존 예약 정보를
         * 어떻게 : 존재하지 않는 캠핑장으로 수정한다
         * 왜 : 존재하는 캠핑장만 예약할 수 있다는 안내를 받기 위해
         */
        // given - 기존 예약 정보와 존재하지 않는 캠핑장으로
        Long reservationId = 1L;
        ExtractableResponse<Response> previousReservation = getReservation(reservationId);

        ReservationTestDataBuilder builder = new ReservationTestDataBuilder();
        Map<String, String> afterRequest = builder.withSiteNumber("X-999").buildRequestMap();

        // when - 예약을 수정했을 때
        ExtractableResponse<Response> response = updateReservation(reservationId, previousReservation.jsonPath().getString("confirmationCode"), afterRequest);

        // then - 예약에 실패하고, "존재하지 않는 캠핑장입니다"라는 안내를 받는다.
        assertBadRequestWithMessage(response, "존재하지 않는 캠핑장입니다.");
    }
}