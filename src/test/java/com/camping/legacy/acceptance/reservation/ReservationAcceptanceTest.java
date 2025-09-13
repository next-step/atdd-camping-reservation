package com.camping.legacy.acceptance.reservation;

import com.camping.legacy.acceptance.utils.AcceptanceTestBase;
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

        Map<String, String> map = Map.of(
                "customerName", "홍길동"
                , "startDate", today.plusDays(7).toString()
                , "endDate", today.plusDays(8).toString()
                , "siteNumber", "B-1"
                , "phoneNumber", "010-1234-1234"
        );

        // when - 30일 이내 날짜의 예약을 신청했을 때
        ExtractableResponse<Response> response = RestAssured
                .given()
                    .body(map)
                .when()
                    .post("/api/reservations")
                .then()
                .extract();

        // then - 예약에 성공한다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    }

    @DisplayName("오늘로부터 31일 이후의 날짜는 예약할 수 없다")
    @Test // 테스트가 뜰 때, Spring 부트 애플리케이션에 해당되는 컴포넌트들이 테스트 컨테이너에 Bean으로 같이 환경으로 딸려 올라감.
    // 이때, 딸려올라가는 웹 설정도 같이 해줄 수 있다.
    void reservationDateLimit() {
        /**
         * 누가 : 예약하려는 사용자
         * 언제 : 오늘
         * 무엇을 : 30일 이후의 날짜 예약을
         * 왜 : 30일 이후의 날짜는 예약할 수 없다는 안내를 받기 위해
         */
        // given - 오늘 날짜를 기준으로 30일 이후의 날짜를 계산하고
        Map<String, String> map = Map.of(
              "customerName", "홍길동"
            , "startDate", today.plusDays(31).toString()
            , "endDate", today.plusDays(32).toString()
            , "siteNumber", "B-1"
            , "phoneNumber", "010-1234-1234"
        );

        // when - 30일 이후 날짜의 예약을 신청했을 때
        ExtractableResponse<Response> response = RestAssured
                .given()
                    .body(map)
                .when()
                    .post("/api/reservations")
                .then()
                .extract();

        // then - 예약에 실패하고, "30일 이후의 날짜는 예약할 수 없다는 안내"를 받는다.
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.jsonPath().getString("message")).isEqualTo("오늘로부터 30일 이내로만 예약할 수 있습니다.");
    }

    @DisplayName("존재하는 캠핑장만 예약 수정할 수 있다")
    @Test
    void reservationExistCampsite() {
        /**
         * 누가 : 홍길동이
         * 무엇을 : B-1 캠핑장을
         * 어떻게 : 예약 1을 확인 코드 "ABC123"으로 캠핑장 수정
         * 왜 : 존재하는 캠핑장에 대해 예약이 정상 수정되는지 확인하기 위해
         */
        // given - 예약 번호, 존재하는 캠핑장 번호가 담긴 예약 정보와 생성된 확인코드로
        Long id = 1L;
        String baseURI = "/api/reservations/";
        ExtractableResponse<Response> reservationInfo = RestAssured
                .given()
                .when()
                .get(baseURI + id)
                .then()
                .extract();

        Long reservationId = Long.valueOf(reservationInfo.jsonPath().getString("id"));
        String customerName = reservationInfo.jsonPath().getString("customerName");
        String startDate = reservationInfo.jsonPath().getString("startDate");
        String endDate = reservationInfo.jsonPath().getString("endDate");
        String phoneNumber = reservationInfo.jsonPath().getString("phoneNumber");
        String confirmationCode = reservationInfo.jsonPath().getString("confirmationCode");

        Map<String, String> map = Map.of(
                "customerName", customerName
                , "startDate", startDate
                , "endDate", endDate
                , "siteNumber", "B-1"
                , "phoneNumber", phoneNumber
        );


        // when - 존재하는 캠핑장으로 예약을 수정했을 때
        ExtractableResponse<Response> response = RestAssured
                .given()
                    .body(map)
                .when()
                    .put("/api/reservations/" + reservationId + "?confirmationCode=" + confirmationCode)
                .then()
                .extract();

        // then - 예약에 성공한다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    }

    @DisplayName("존재하지 않는 캠핑장은 예약 수정이 불가능하다")
    @Test
    void reservationNotExistCampsite() {
        /**
         * 누가 : 홍길동이
         * 무엇을 : x-999 캠핑장을
         * 어떻게 : 예약 1을 확인 코드 "ABC123"으로 캠핑장 수정
         * 왜 : 존재하는 캠핑장만 예약할 수 있다는 안내를 받기 위해
         */
        // given - 예약 번호, 존재하지 않는 캠핑장 번호가 담긴 예약 정보와 생성된 확인코드로
        Long id = 1L;
        String baseURI = "/api/reservations/";
        ExtractableResponse<Response> reservationInfo = RestAssured
                .given()
                .when()
                    .get(baseURI + id)
                .then()
                .extract();

        Long reservationId = Long.valueOf(reservationInfo.jsonPath().getString("id"));
        String customerName = reservationInfo.jsonPath().getString("customerName");
        String startDate = reservationInfo.jsonPath().getString("startDate");
        String endDate = reservationInfo.jsonPath().getString("endDate");
        String phoneNumber = reservationInfo.jsonPath().getString("phoneNumber");
        String confirmationCode = reservationInfo.jsonPath().getString("confirmationCode");

        Map<String, String> map = Map.of(
                "customerName", customerName
                , "startDate", startDate
                , "endDate", endDate
                , "siteNumber", "x-999"
                , "phoneNumber", phoneNumber
        );

        // when - 존재하지 않는 캠핑장으로 예약을 수정했을 때
        ExtractableResponse<Response> response = RestAssured
                .given()
                    .body(map)
                .when()
                    .put("/api/reservations/" + reservationId + "?confirmationCode=" + confirmationCode)
                .then()
                .extract();

        // then - 예약에 실패하고, "존재하지 않는 캠핑장입니다"라는 안내를 받는다.
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.jsonPath().getString("message")).isEqualTo("존재하지 않는 캠핑장입니다.");
    }

    @DisplayName("필수 정보 누락 시 예약이 실패한다")
    @Test
    void reservationFailsWithMissingRequiredInfo() {
        /**
         * 누가 : 예약하려는 사용자
         * 무엇을 : 불충분한 정보만 가지고 예약을
         * 왜 : 필수 정보 누락 시 적절한 오류 메시지를 받기 위해
         */
        // given - 예약자 이름이 누락된 요청 데이터
        Map<String, String> map = Map.of(
                "startDate", today.plusDays(7).toString(),
                "endDate", today.plusDays(8).toString(),
                "siteNumber", "A-1",
                "phoneNumber", "010-1234-5678"
        );

        // when - 불충분한 정보로 예약을 시도했을 때
        ExtractableResponse<Response> response = RestAssured
                .given()
                    .body(map)
                .when()
                    .post("/api/reservations")
                .then()
                .extract();

        // then - 예약이 실패하고 적절한 오류 메시지를 받는다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.jsonPath().getString("message")).isEqualTo("예약자 이름을 입력해주세요.");
    }

    @DisplayName("종료일이 시작일보다 이전인 경우 예약이 실패한다")
    @Test
    void reservationFailsWhenEndDateIsBeforeStartDate() {
        /**
         * 누가 : 예약하려는 사용자
         * 무엇을 : 종료일이 시작일보다 이전인 예약을
         * 왜 : 날짜 유효성 검증을 확인하기 위해
         */
        // given - 종료일이 시작일보다 이전인 요청 데이터
        Map<String, String> map = Map.of(
                "customerName", "홍길동",
                "startDate", today.plusDays(8).toString(),
                "endDate", today.plusDays(7).toString(), // 시작일보다 이전
                "siteNumber", "A-1",
                "phoneNumber", "010-1234-5678"
        );

        // when - 잘못된 날짜로 예약을 시도했을 때
        ExtractableResponse<Response> response = RestAssured
                .given()
                    .body(map)
                .when()
                    .post("/api/reservations")
                .then()
                .extract();

        // then - 예약이 실패하고 적절한 오류 메시지를 받는다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.jsonPath().getString("message")).isEqualTo("종료일이 시작일보다 이전일 수 없습니다.");
    }

    @DisplayName("30일 제한 규칙을 애매하게 넘는 경우 예약이 실패한다")
    @Test
    void reservationFailsWithBorderline30DayLimit() {
        /**
         * 누가 : 예약하려는 사용자
         * 무엇을 : 정확히 30일째 되는 날짜의 예약을
         * 왜 : 30일 제한 규칙의 경계값을 확인하기 위해
         */
        // given - 정확히 30일째와 그 이후 날짜
        Map<String, String> map = Map.of(
                "customerName", "홍길동",
                "startDate", today.plusDays(30).toString(), // 정확히 30일째
                "endDate", today.plusDays(32).toString(),   // 30일 초과
                "siteNumber", "A-1",
                "phoneNumber", "010-1234-5678"
        );

        // when - 경계값으로 예약을 시도했을 때
        ExtractableResponse<Response> response = RestAssured
                .given()
                    .body(map)
                .when()
                    .post("/api/reservations")
                .then()
                .extract();

        // then - 예약이 실패하고 적절한 오류 메시지를 받는다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.jsonPath().getString("message")).isEqualTo("오늘로부터 30일 이내로 예약할 수 있습니다.");
    }
}