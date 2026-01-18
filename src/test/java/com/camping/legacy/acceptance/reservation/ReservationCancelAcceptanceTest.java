package com.camping.legacy.acceptance.reservation;

import com.camping.legacy.acceptance.AcceptanceTestBase;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static com.camping.legacy.acceptance.reservation.apiExtractableresponse.ReservationApiExtractableResponse.*;
import static com.camping.legacy.acceptance.reservation.builder.ReservationRequestBuilder.Reservation;
import static com.camping.legacy.acceptance.reservation.ReservationTestConstants.*;
import static com.camping.legacy.acceptance.reservation.apiExtractableresponse.SiteApiExtractableResponse.사이트를_조회한다;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("예약 취소 기능")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ReservationCancelAcceptanceTest extends AcceptanceTestBase {

    /**
     * Given 홍길동이 A-1 사이트를 2026년 2월 5일~7일로 예약했고
     * When 홍길동이 올바른 확인 코드로 예약 취소를 요청하면
     * Then 예약이 취소되고
     * And A-1 사이트가 2월 5일~7일에 예약 가능해지고
     * And 예약 상태가 '사전 취소' 상태로 변경된다.
     */
    @DisplayName("[예약/취소] 정상적으로 예약을 취소한다.")
    @Test
    void 정상적으로_예약을_취소() {

        // Given
        var 홍길동_예약요청 = Reservation()
                .reserver(홍길동)
                .period(변경예약_시작일, 변경예약_종료일)
                .site(사이트번호_A_1)
                .build();

        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);
        var 예약ID = 예약정보에서_예약ID_조회(홍길동_예약결과정보);
        var 확인코드 = 예약정보에서_확인코드_조회(홍길동_예약결과정보);

        // When
        var 예약취소정보 = 예약을_취소한다(예약ID, 확인코드);
        var 예약정보 = 예약을_조회한다(예약ID);

        // Then
        예약이_취소되었다(예약취소정보);
        사전예약_취소_상태이다(예약정보);

        var 사이트정보 = 사이트를_조회한다(변경예약_시작일, 변경예약_종료일, 사이트_크기_대형);
        사이트가_존재한다(사이트정보, 사이트번호_A_1);
    }

    /**
     * Given 홍길동이 A-1 사이트를 2026년 2월 1일~3일로 예약했고
     * And 오늘 날짜는 2026년 2월 1일이다 (예약 시작일)
     * When 홍길동이 올바른 확인 코드로 예약 취소를 요청하면
     * Then 예약이 취소되고
     * And 예약 상태가 '당일 취소' 상태로 변경된다.
     */
    @DisplayName("[예약/취소] 당일에 예약을 취소하면 당일예약취소 상태가 된다.")
    @Test
    void 당일에_예약을_취소하면_당일예약취소_상태로_변경() {

        // Given
        var 당일예약_시작일 = LocalDate.now().toString();
        var 당일예약_종료일 = LocalDate.now().plusDays(3).toString();

        var 홍길동_예약요청 = Reservation()
                .reserver(홍길동)
                .period(당일예약_시작일, 당일예약_종료일)
                .site(사이트번호_A_1)
                .build();

        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);
        var 예약ID = 예약정보에서_예약ID_조회(홍길동_예약결과정보);
        var 확인코드 = 예약정보에서_확인코드_조회(홍길동_예약결과정보);

        // When
        var 예약취소정보 = 예약을_취소한다(예약ID, 확인코드);
        var 예약정보 = 예약을_조회한다(예약ID);

        // Then
        예약이_취소되었다(예약취소정보);
        당일예약_취소_상태이다(예약정보);
    }

    /**
     * Given 홍길동이 A-1 사이트를 2026년 2월 5일~7일로 예약했고
     * When 틀린 확인 코드를 입력하고 예약 취소를 시도하면
     * Then 취소가 거부된다.
     */
    @DisplayName("[예약/취소] 틀린 확인코드를 입력할 경우 예약이 취소되지 않는다.")
    @Test
    void 틀린_확인코드로_예약_취소_불가() {

        // Given
        var 홍길동_예약요청 = Reservation()
                .reserver(홍길동)
                .period(변경예약_시작일, 변경예약_종료일)
                .site(사이트번호_A_1)
                .build();

        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);
        var 예약ID = 예약정보에서_예약ID_조회(홍길동_예약결과정보);

        // When
        var 예약취소정보 = 예약을_취소한다(예약ID, 잘못된_확인코드);

        // Then
        예약이_취소되지않았다(예약취소정보);
    }

    // =====================================================
    // Helpers
    // =====================================================

    private String 예약정보에서_확인코드_조회(ExtractableResponse<Response> response) {
        return response.jsonPath().getString("confirmationCode");
    }

    private Long 예약정보에서_예약ID_조회(ExtractableResponse<Response> response) {
        return response.jsonPath().getLong("id");
    }

    private void 사이트가_존재한다(ExtractableResponse<Response> response, String expectedSiteNumber) {
        List<String> siteNumbers = response.jsonPath().getList("siteNumber", String.class);
        assertThat(siteNumbers).contains(expectedSiteNumber);
    }

    private void 예약이_취소되었다(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(200);
    }

    private void 사전예약_취소_상태이다(ExtractableResponse<Response> response) {
        String status = response.jsonPath().getString("status");
        assertThat(status).isEqualTo(예약상태_사전취소);
    }

    private void 당일예약_취소_상태이다(ExtractableResponse<Response> response) {
        String status = response.jsonPath().getString("status");
        assertThat(status).isEqualTo(예약상태_당일취소);
    }

    private void 예약이_취소되지않았다(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(400);
    }
}