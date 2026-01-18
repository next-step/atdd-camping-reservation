package com.camping.legacy.acceptance.reservation;

import com.camping.legacy.acceptance.AcceptanceTestBase;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static com.camping.legacy.acceptance.reservation.apiExtractableresponse.ReservationApiExtractableResponse.예약을_생성한다;
import static com.camping.legacy.acceptance.reservation.builder.ReservationRequestBuilder.Reservation;
import static com.camping.legacy.acceptance.reservation.ReservationTestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("예약 금액 관련 기능")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ReservationPricingAcceptanceTest extends AcceptanceTestBase {

    // =====================================================
    // 1. 기본 요금
    // =====================================================

    /**
     * Given A-1 사이트가 예약 가능한 상태이고
     * When 홍길동이 A-1 사이트를 비수기 평일 1박으로 예약하면
     * Then 예약 금액은 80,000원이다.
     */
    @DisplayName("[기본요금] A 사이트(대형)의 기본 요금은 80,000원이다.")
    @Test
    void A_사이트_대형의_기본_요금은_80000원이다() {

        // When
        var 홍길동_예약요청 = Reservation()
                .reserver(홍길동)
                .period(비수기_평일_시작일, 비수기_평일_종료일)
                .site(사이트번호_A_1)
                .build();

        var 응답 = 예약을_생성한다(홍길동_예약요청);

        // Then
        예약이_되었다(응답);
        예약_금액이_일치한다(응답, A_사이트_기본요금);
    }

    // =====================================================
    // 2. 단일 조건 할증 (비수기 / 성수기 / 주말)
    // =====================================================

    /**
     * Given A-1 사이트가 예약 가능한 상태이고
     * When 홍길동이 A-1 사이트를 비수기 수요일 1박으로 예약하면
     * Then 예약 금액은 80,000원이다.
     */
    @DisplayName("[비수기/평일] 평일에는 할증이 적용되지 않는다.")
    @Test
    void 평일에는_할증이_적용되지_않는다() {

        // When
        var 요청 = Reservation()
                .reserver(홍길동)
                .period(비수기_수요일_시작일, 비수기_목요일_종료일)
                .site(사이트번호_A_1)
                .build();

        var 응답 = 예약을_생성한다(요청);

        // Then
        예약이_되었다(응답);
        예약_금액이_일치한다(응답, A_사이트_기본요금);
    }

    /**
     * Given A-1 사이트가 예약 가능한 상태이고
     * When 홍길동이 A-1 사이트를 비수기 토요일 1박으로 예약하면
     * Then 예약 금액은 104,000원이다. (80,000 * 1.3)
     */
    @DisplayName("[비수기/주말] 주말에는 30% 할증이 적용된다.")
    @Test
    void 주말에는_30퍼센트_할증이_적용된다() {

        // When
        var 요청 = Reservation()
                .reserver(홍길동)
                .period(비수기_토요일_시작일, 비수기_토요일_종료일)
                .site(사이트번호_A_1)
                .build();

        var 응답 = 예약을_생성한다(요청);

        // Then
        예약이_되었다(응답);
        예약_금액이_일치한다(응답, 주말_할증_요금);
    }

    /**
     * Given A-1 사이트가 예약 가능한 상태이고
     * When 홍길동이 A-1 사이트를 7월 평일 1박으로 예약하면
     * Then 예약 금액은 120,000원이다. (80,000 * 1.5)
     */
    @DisplayName("[성수기/평일] 성수기 평일에는 50% 할증이 적용된다.")
    @Test
    void 성수기_평일에는_50퍼센트_할증이_적용된다() {

        // When
        var 요청 = Reservation()
                .reserver(홍길동)
                .period(성수기_7월_평일_시작일, 성수기_7월_평일_종료일)
                .site(사이트번호_A_1)
                .build();

        var 응답 = 예약을_생성한다(요청);

        // Then
        예약이_되었다(응답);
        예약_금액이_일치한다(응답, 성수기_평일_할증_요금);
    }

    /**
     * Given A-1 사이트가 예약 가능한 상태이고
     * When 홍길동이 A-1 사이트를 8월 토요일 1박으로 예약하면
     * Then 예약 금액은 136,000원이다. (80,000 * 1.7)
     */
    @DisplayName("[성수기/주말] 성수기 주말에는 70% 할증이 적용된다.")
    @Test
    void 성수기_주말에는_70퍼센트_할증이_적용된다() {

        // When
        var 요청 = Reservation()
                .reserver(홍길동)
                .period(성수기_8월_토요일_시작일, 성수기_8월_토요일_종료일)
                .site(사이트번호_A_1)
                .build();

        var 응답 = 예약을_생성한다(요청);

        // Then
        예약이_되었다(응답);
        예약_금액이_일치한다(응답, 성수기_주말_할증_요금);
    }

    // =====================================================
    // 3. 복합 요금 계산 (여러 날짜)
    // =====================================================

    /**
     * Given A-1 사이트가 예약 가능한 상태이고
     * When 홍길동이 A-1 사이트를 비수기 월요일부터 수요일까지 2박으로 예약하면
     * Then 예약 금액은 160,000원이다. (80,000 * 2)
     */
    @DisplayName("[복합요금] 평일 2박 예약 시 일별 요금의 합계가 청구된다.")
    @Test
    void 평일_2박_예약_시_일별_요금의_합계가_청구된다() {

        // When
        var 요청 = Reservation()
                .reserver(홍길동)
                .period(비수기_월요일_시작일, 비수기_2박_수요일_종료일)
                .site(사이트번호_A_1)
                .build();

        var 응답 = 예약을_생성한다(요청);

        // Then
        예약이_되었다(응답);
        예약_금액이_일치한다(응답, 평일_2박_요금);
    }

    /**
     * Given A-1 사이트가 예약 가능한 상태이고
     * When 홍길동이 A-1 사이트를 비수기 금요일부터 일요일까지 2박으로 예약하면
     * Then 예약 금액은 184,000원이다. (금요일 80,000 + 토요일 104,000)
     */
    @DisplayName("[복합요금] 평일과 주말이 혼합된 예약은 각 날짜별 요금이 적용된다.")
    @Test
    void 평일과_주말이_혼합된_예약은_각_날짜별_요금이_적용된다() {

        // When
        var 요청 = Reservation()
                .reserver(홍길동)
                .period(비수기_금요일_시작일, 비수기_일요일_종료일)
                .site(사이트번호_A_1)
                .build();

        var 응답 = 예약을_생성한다(요청);

        // Then
        예약이_되었다(응답);
        예약_금액이_일치한다(응답, 평일_주말_혼합_요금);
    }

    /**
     * Given A-1 사이트가 예약 가능한 상태이고
     * When 홍길동이 A-1 사이트를 6월 30일부터 7월 2일까지 2박으로 예약하면
     * Then 예약 금액은 200,000원이다. (6월 30일 80,000 + 7월 1일 120,000)
     */
    @DisplayName("[복합요금] 성수기와 비수기가 혼합된 예약은 각 날짜별 요금이 적용된다.")
    @Test
    void 성수기와_비수기가_혼합된_예약은_각_날짜별_요금이_적용된다() {

        // When
        var 요청 = Reservation()
                .reserver(홍길동)
                .period(비수기_6월30일_시작일, 성수기_7월2일_종료일)
                .site(사이트번호_A_1)
                .build();

        var 응답 = 예약을_생성한다(요청);

        // Then
        예약이_되었다(응답);
        예약_금액이_일치한다(응답, 비수기_성수기_혼합_요금);
    }

    // =====================================================
    // 4. 성수기 경계값
    // =====================================================

    /**
     * Given A-1 사이트가 예약 가능한 상태이고
     * When 홍길동이 A-1 사이트를 6월 30일(평일) 1박으로 예약하면
     * Then 예약 금액은 80,000원이다.
     */
    @DisplayName("[성수기/경계] 성수기 전날(6월 30일)에는 비수기 요금이 적용된다.")
    @Test
    void 성수기_전날_6월_30일에는_비수기_요금이_적용된다() {

        // When
        var 요청 = Reservation()
                .reserver(홍길동)
                .period(성수기_전날_6월30일, 성수기_전날_7월1일)
                .site(사이트번호_A_1)
                .build();

        var 응답 = 예약을_생성한다(요청);

        // Then
        예약이_되었다(응답);
        예약_금액이_일치한다(응답, A_사이트_기본요금);
    }

    /**
     * Given A-1 사이트가 예약 가능한 상태이고
     * When 홍길동이 A-1 사이트를 7월 1일(평일) 1박으로 예약하면
     * Then 예약 금액은 120,000원이다.
     */
    @DisplayName("[성수기/경계] 성수기 시작일(7월 1일)에는 성수기 요금이 적용된다.")
    @Test
    void 성수기_시작일_7월_1일에는_성수기_요금이_적용된다() {

        // When
        var 요청 = Reservation()
                .reserver(홍길동)
                .period(성수기_시작일_7월1일, 성수기_시작일_7월2일)
                .site(사이트번호_A_1)
                .build();

        var 응답 = 예약을_생성한다(요청);

        // Then
        예약이_되었다(응답);
        예약_금액이_일치한다(응답, 성수기_평일_할증_요금);
    }

    /**
     * Given A-1 사이트가 예약 가능한 상태이고
     * When 홍길동이 A-1 사이트를 8월 31일(평일) 1박으로 예약하면
     * Then 예약 금액은 120,000원이다.
     */
    @DisplayName("[성수기/경계] 성수기 종료일(8월 31일)에는 성수기 요금이 적용된다.")
    @Test
    void 성수기_종료일_8월_31일에는_성수기_요금이_적용된다() {

        // When
        var 요청 = Reservation()
                .reserver(홍길동)
                .period(성수기_종료일_8월31일, 성수기_종료일_9월1일)
                .site(사이트번호_A_1)
                .build();

        var 응답 = 예약을_생성한다(요청);

        // Then
        예약이_되었다(응답);
        예약_금액이_일치한다(응답, 성수기_평일_할증_요금);
    }

    /**
     * Given A-1 사이트가 예약 가능한 상태이고
     * When 홍길동이 A-1 사이트를 9월 1일(평일) 1박으로 예약하면
     * Then 예약 금액은 80,000원이다.
     */
    @DisplayName("[성수기/경계] 성수기 다음날(9월 1일)에는 비수기 요금이 적용된다.")
    @Test
    void 성수기_다음날_9월_1일에는_비수기_요금이_적용된다() {

        // When
        var 요청 = Reservation()
                .reserver(홍길동)
                .period(성수기_다음날_9월1일, 성수기_다음날_9월2일)
                .site(사이트번호_A_1)
                .build();

        var 응답 = 예약을_생성한다(요청);

        // Then
        예약이_되었다(응답);
        예약_금액이_일치한다(응답, A_사이트_기본요금);
    }

    // =====================================================
    // 5. 포인트 적립
    // =====================================================

    /**
     * Given A-1 사이트가 예약 가능한 상태이고
     * When 홍길동이 A-1 사이트를 비수기 평일 1박으로 예약하면
     * Then 적립 포인트는 4,000포인트이다. (80,000 * 0.05)
     */
    @DisplayName("[포인트] 평일 예약 시 5% 포인트가 적립된다.")
    @Test
    void 평일_예약_시_5퍼센트_포인트가_적립된다() {

        // When
        var 요청 = Reservation()
                .reserver(홍길동)
                .period(비수기_평일_시작일, 비수기_평일_종료일)
                .site(사이트번호_A_1)
                .build();

        var 응답 = 예약을_생성한다(요청);

        // Then
        예약이_되었다(응답);
        포인트가_일치한다(응답, 평일_포인트);
    }

    /**
     * Given A-1 사이트가 예약 가능한 상태이고
     * When 홍길동이 A-1 사이트를 비수기 토요일 1박으로 예약하면
     * Then 적립 포인트는 10,400포인트이다. (104,000 * 0.10)
     */
    @DisplayName("[포인트] 주말 포함 예약 시 10% 포인트가 적립된다.")
    @Test
    void 주말_포함_예약_시_10퍼센트_포인트가_적립된다() {

        // When
        var 요청 = Reservation()
                .reserver(홍길동)
                .period(비수기_토요일_시작일, 비수기_토요일_종료일)
                .site(사이트번호_A_1)
                .build();

        var 응답 = 예약을_생성한다(요청);

        // Then
        예약이_되었다(응답);
        포인트가_일치한다(응답, 주말_포인트);
    }

    /**
     * Given A-1 사이트가 예약 가능한 상태이고
     * When 홍길동이 A-1 사이트를 성수기 평일 1박으로 예약하면
     * Then 적립 포인트는 3,600포인트이다. (120,000 * 0.03)
     */
    @DisplayName("[포인트] 성수기 예약 시 3% 포인트가 적립된다.")
    @Test
    void 성수기_예약_시_3퍼센트_포인트가_적립된다() {

        // When
        var 요청 = Reservation()
                .reserver(홍길동)
                .period(성수기_7월_평일_시작일, 성수기_7월_평일_종료일)
                .site(사이트번호_A_1)
                .build();

        var 응답 = 예약을_생성한다(요청);

        // Then
        예약이_되었다(응답);
        포인트가_일치한다(응답, 성수기_포인트);
    }

    // =====================================================
    // Assertion Helpers
    // =====================================================

    private void 예약이_되었다(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(201);
    }

    private void 예약_금액이_일치한다(ExtractableResponse<Response> response, int expectedPrice) {
        Integer totalPrice = response.jsonPath().getInt("totalPrice");
        assertThat(totalPrice).isEqualTo(expectedPrice);
    }

    private void 포인트가_일치한다(ExtractableResponse<Response> response, int expectedPoints) {
        Integer earnedPoints = response.jsonPath().getInt("earnedPoints");
        assertThat(earnedPoints).isEqualTo(expectedPoints);
    }
}
