package com.camping.legacy.domain;

import com.camping.legacy.domain.dto.ReservationParams;
import com.camping.legacy.dto.ReservationResponse;
import io.restassured.RestAssured;
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

import static org.assertj.core.api.Assertions.assertThat;

@Sql(scripts = "/cleanup-and-reinit.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReservationTest {

    static final String API_RESERVATIONS = "/api/reservations";
    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }


    @Test
    @DisplayName("예약은 오늘로부터 30일 이내만 가능해야 한다.")
    void reservationDateLimit() {
        // NOTE: 서비스 코드에 해당 기능 구현 필요.

        // given
        LocalDate startDate = LocalDate.now().plusDays(31);
        LocalDate endDate = LocalDate.now().plusDays(33);
        final var params = ReservationParams.of(startDate, endDate);

        // when
        ExtractableResponse<Response> response = ReservationRequestSender.send(API_RESERVATIONS, params);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }


    @Test
    @DisplayName("종료일은 시작일보다 이전일 수 없다.")
    void endDate_cannot_be_before_startDate() {
        // given
        LocalDate startDate = LocalDate.now().plusDays(5);
        LocalDate endDate = LocalDate.now().plusDays(4); // 종료일이 시작일보다 빠름
        final var params = ReservationParams.of(startDate, endDate);

        // when
        ExtractableResponse<Response> response = ReservationRequestSender.send(API_RESERVATIONS, params);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("이름과 전화번호는 필수 입력값이다.")
    void 이름과_전화번호는_필수_입력값이다() {
        // given
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(4);
        ReservationParams params = new ReservationParams(null, null, "A-1", startDate.toString(), endDate.toString());

        // when
        final var response = ReservationRequestSender.send(API_RESERVATIONS, params);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("동일 사이트, 동일 기간에 중복 예약이 불가능하다")
    void 동일_사이트_동일_기간에_중복_예약이_불가능하다() {
        // given
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(4);
        final var params = ReservationParams.of(startDate, endDate);

        // when
        ExtractableResponse<Response> response = ReservationRequestSender.send(API_RESERVATIONS, params);
        ExtractableResponse<Response> duplicateReservationResponse = ReservationRequestSender.send(API_RESERVATIONS, params);

        // then
        org.junit.jupiter.api.Assertions.assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value()),
                () -> assertThat(duplicateReservationResponse.statusCode()).isEqualTo(HttpStatus.CONFLICT.value())
        );
    }

    @Test
    @DisplayName("예약 완료 시 6자리 영숫자 확인 코드가 발급된다")
    void 예약_완료_시_6자리_영숫자_확인_코드가_발급된다() {
        // given
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(4);
        final var params = ReservationParams.of(startDate, endDate);

        // when
        ExtractableResponse<Response> response = ReservationRequestSender.send(API_RESERVATIONS, params);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        ReservationResponse reservationResponse = response.body().as(ReservationResponse.class);
        assertThat(reservationResponse.getConfirmationCode()).matches("^[A-Za-z0-9]{6}$");
    }
}