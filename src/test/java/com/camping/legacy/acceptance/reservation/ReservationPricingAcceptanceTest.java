package com.camping.legacy.acceptance.reservation;

import com.camping.legacy.acceptance.AcceptanceTestBase;
import com.camping.legacy.dto.ReservationRequest;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;

import static com.camping.legacy.acceptance.reservation.ReservationApiExtractableResponse.예약을_생성한다;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("예약 금액 관련 기능")
@Sql({"/truncate.sql", "/data.sql"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ReservationPricingAcceptanceTest extends AcceptanceTestBase {

    // ====== 사이트 정보 ======
    private static final String 사이트번호_A_1 = "A-1";

    // ====== 예약자 정보 ======
    private static final String 홍길동 = "홍길동";
    private static final String 연락처 = "010-1234-1234";
    private static final int 인원수_5명 = 5;
    private static final String 차량번호 = "가1234";
    private static final String 요청사항 = "1시간 일찍 입실 예정";

    // ====== 비수기 평일 1박 (2026년 2월 - 월:2, 화:3) ======
    private static final String 비수기_평일_시작일 = "2026-02-02";
    private static final String 비수기_평일_종료일 = "2026-02-03";

    // ====== 비수기 평일 1박 (2026년 2월 - 수:4, 목:5) ======
    private static final String 비수기_수요일_시작일 = "2026-02-04";
    private static final String 비수기_목요일_종료일 = "2026-02-05";

    // ====== 비수기 평일 2박 (2026년 2월 - 월:2, 수:4) ======
    private static final String 비수기_월요일_시작일 = "2026-02-02";
    private static final String 비수기_2박_수요일_종료일 = "2026-02-04";

    // ====== 비수기 주말 (2026년 2월 - 토:7, 일:8) ======
    private static final String 비수기_토요일_시작일 = "2026-02-07";
    private static final String 비수기_토요일_종료일 = "2026-02-08";

    // ====== 비수기 평일+주말 혼합 (2026년 2월 - 금:13, 일:15) ======
    private static final String 비수기_금요일_시작일 = "2026-02-13";
    private static final String 비수기_일요일_종료일 = "2026-02-15";

    // ====== 성수기 평일 (2026년 7월 - 월:6, 화:7) ======
    private static final String 성수기_7월_평일_시작일 = "2026-07-06";
    private static final String 성수기_7월_평일_종료일 = "2026-07-07";

    // ====== 성수기 주말 (2026년 8월 - 토:1, 일:2) ======
    private static final String 성수기_8월_토요일_시작일 = "2026-08-01";
    private static final String 성수기_8월_토요일_종료일 = "2026-08-02";

    // ====== 비수기+성수기 혼합 (2026년 6월30일~7월2일) ======
    private static final String 비수기_6월30일_시작일 = "2026-06-30";
    private static final String 성수기_7월2일_종료일 = "2026-07-02";

    // ====== 성수기 경계값 테스트 (2026년) ======
    // 2026-07-01은 수요일(성수기 시작일)
    private static final String 성수기_시작일_7월1일 = "2026-07-01";
    private static final String 성수기_시작일_7월2일 = "2026-07-02";

    // 2026-08-31은 월요일(성수기 종료일)
    private static final String 성수기_종료일_8월31일 = "2026-08-31";
    private static final String 성수기_종료일_9월1일 = "2026-09-01";

    // 2026-06-30은 화요일(성수기 전날)
    private static final String 성수기_전날_6월30일 = "2026-06-30";
    private static final String 성수기_전날_7월1일 = "2026-07-01";

    // 2026-09-01은 화요일(성수기 다음날)
    private static final String 성수기_다음날_9월1일 = "2026-09-01";
    private static final String 성수기_다음날_9월2일 = "2026-09-02";

    // ====== 기본 요금 ======
    private static final int A_사이트_기본요금 = 80000;
    private static final int 주말_할증_요금 = 104000;  // 80,000 * 1.3
    private static final int 성수기_평일_할증_요금 = 120000;  // 80,000 * 1.5
    private static final int 성수기_주말_할증_요금 = 136000;  // 80,000 * 1.7
    private static final int 평일_2박_요금 = 160000;  // 80,000 * 2
    private static final int 평일_주말_혼합_요금 = 184000;  // 80,000 + 104,000
    private static final int 비수기_성수기_혼합_요금 = 200000;  // 80,000 + 120,000
    private static final int 장기예약_30박_요금 = 2592000;  // 평일 22일(1,760,000) + 주말 8일(832,000)
    private static final int 평일_포인트 = 4000;  // 80,000 * 0.05
    private static final int 주말_포인트 = 10400;  // 104,000 * 0.10
    private static final int 성수기_포인트 = 3600;  // 120,000 * 0.03

    /**
     * Given: A-1 사이트가 예약 가능한 상태이다.
     * When: 홍길동이 A-1 사이트를 비수기 평일 1박으로 예약한다.
     * Then: 예약 금액은 80,000원이다.
     */
    @DisplayName("A 사이트(대형)의 기본 요금은 80,000원이다.")
    @Test
    void A_사이트_대형의_기본_요금은_80000원이다() {

        // Given
        // A-1 사이트가 예약 가능한 상태 (data.sql에서 설정됨)

        // When
        var 홍길동_예약요청 = 예약요청_생성(홍길동, 비수기_평일_시작일, 비수기_평일_종료일, 사이트번호_A_1);
        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);

        // Then
        예약이_되었다(홍길동_예약결과정보);
        예약_금액이_일치한다(홍길동_예약결과정보, A_사이트_기본요금);
    }

    /**
     * Given: A-1 사이트가 예약 가능한 상태이다.
     * When: 홍길동이 A-1 사이트를 비수기 토요일 1박으로 예약한다.
     * Then: 예약 금액은 104,000원이다. (80,000 * 1.3)
     */
    @DisplayName("주말에는 30% 할증이 적용된다.")
    @Test
    void 주말에는_30퍼센트_할증이_적용된다() {

        // Given
        // A-1 사이트가 예약 가능한 상태 (data.sql에서 설정됨)

        // When
        var 홍길동_예약요청 = 예약요청_생성(홍길동, 비수기_토요일_시작일, 비수기_토요일_종료일, 사이트번호_A_1);
        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);

        // Then
        예약이_되었다(홍길동_예약결과정보);
        예약_금액이_일치한다(홍길동_예약결과정보, 주말_할증_요금);
    }

    /**
     * Given: A-1 사이트가 예약 가능한 상태이다.
     * When: 홍길동이 A-1 사이트를 비수기 수요일 1박으로 예약한다.
     * Then: 예약 금액은 80,000원이다.
     */
    @DisplayName("평일에는 할증이 적용되지 않는다.")
    @Test
    void 평일에는_할증이_적용되지_않는다() {

        // Given
        // A-1 사이트가 예약 가능한 상태 (data.sql에서 설정됨)

        // When
        var 홍길동_예약요청 = 예약요청_생성(홍길동, 비수기_수요일_시작일, 비수기_목요일_종료일, 사이트번호_A_1);
        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);

        // Then
        예약이_되었다(홍길동_예약결과정보);
        예약_금액이_일치한다(홍길동_예약결과정보, A_사이트_기본요금);
    }

    /**
     * Given: A-1 사이트가 예약 가능한 상태이다.
     * When: 홍길동이 A-1 사이트를 7월 평일 1박으로 예약한다.
     * Then: 예약 금액은 120,000원이다. (80,000 * 1.5)
     */
    @DisplayName("성수기 평일에는 50% 할증이 적용된다.")
    @Test
    void 성수기_평일에는_50퍼센트_할증이_적용된다() {

        // Given
        // A-1 사이트가 예약 가능한 상태 (data.sql에서 설정됨)

        // When
        var 홍길동_예약요청 = 예약요청_생성(홍길동, 성수기_7월_평일_시작일, 성수기_7월_평일_종료일, 사이트번호_A_1);
        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);

        // Then
        예약이_되었다(홍길동_예약결과정보);
        예약_금액이_일치한다(홍길동_예약결과정보, 성수기_평일_할증_요금);
    }

    /**
     * Given: A-1 사이트가 예약 가능한 상태이다.
     * When: 홍길동이 A-1 사이트를 8월 토요일 1박으로 예약한다.
     * Then: 예약 금액은 136,000원이다. (80,000 * 1.7)
     */
    @DisplayName("성수기 주말에는 70% 할증이 적용된다.")
    @Test
    void 성수기_주말에는_70퍼센트_할증이_적용된다() {

        // Given
        // A-1 사이트가 예약 가능한 상태 (data.sql에서 설정됨)

        // When
        var 홍길동_예약요청 = 예약요청_생성(홍길동, 성수기_8월_토요일_시작일, 성수기_8월_토요일_종료일, 사이트번호_A_1);
        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);

        // Then
        예약이_되었다(홍길동_예약결과정보);
        예약_금액이_일치한다(홍길동_예약결과정보, 성수기_주말_할증_요금);
    }

    /**
     * Given: A-1 사이트가 예약 가능한 상태이다.
     * When: 홍길동이 A-1 사이트를 비수기 월요일부터 수요일까지 2박으로 예약한다.
     * Then: 예약 금액은 160,000원이다. (80,000 * 2)
     */
    @DisplayName("평일 2박 예약 시 일별 요금의 합계가 청구된다.")
    @Test
    void 평일_2박_예약_시_일별_요금의_합계가_청구된다() {

        // Given
        // A-1 사이트가 예약 가능한 상태 (data.sql에서 설정됨)

        // When
        var 홍길동_예약요청 = 예약요청_생성(홍길동, 비수기_월요일_시작일, 비수기_2박_수요일_종료일, 사이트번호_A_1);
        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);

        // Then
        예약이_되었다(홍길동_예약결과정보);
        예약_금액이_일치한다(홍길동_예약결과정보, 평일_2박_요금);
    }

    /**
     * Given: A-1 사이트가 예약 가능한 상태이다.
     * When: 홍길동이 A-1 사이트를 비수기 금요일부터 일요일까지 2박으로 예약한다.
     * Then: 예약 금액은 184,000원이다. (금요일 80,000 + 토요일 104,000)
     */
    @DisplayName("평일과 주말이 혼합된 예약은 각 날짜별 요금이 적용된다.")
    @Test
    void 평일과_주말이_혼합된_예약은_각_날짜별_요금이_적용된다() {

        // Given
        // A-1 사이트가 예약 가능한 상태 (data.sql에서 설정됨)

        // When
        var 홍길동_예약요청 = 예약요청_생성(홍길동, 비수기_금요일_시작일, 비수기_일요일_종료일, 사이트번호_A_1);
        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);

        // Then
        예약이_되었다(홍길동_예약결과정보);
        예약_금액이_일치한다(홍길동_예약결과정보, 평일_주말_혼합_요금);
    }

    /**
     * Given: A-1 사이트가 예약 가능한 상태이다.
     * When: 홍길동이 A-1 사이트를 6월 30일부터 7월 2일까지 2박으로 예약한다.
     * Then: 예약 금액은 200,000원이다. (6월 30일 80,000 + 7월 1일 120,000)
     */
    @DisplayName("성수기와 비수기가 혼합된 예약은 각 날짜별 요금이 적용된다.")
    @Test
    void 성수기와_비수기가_혼합된_예약은_각_날짜별_요금이_적용된다() {

        // Given
        // A-1 사이트가 예약 가능한 상태 (data.sql에서 설정됨)

        // When
        var 홍길동_예약요청 = 예약요청_생성(홍길동, 비수기_6월30일_시작일, 성수기_7월2일_종료일, 사이트번호_A_1);
        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);

        // Then
        예약이_되었다(홍길동_예약결과정보);
        예약_금액이_일치한다(홍길동_예약결과정보, 비수기_성수기_혼합_요금);
    }

    /**
     * Given: A-1 사이트가 예약 가능한 상태이다.
     * When: 홍길동이 A-1 사이트를 7월 1일(평일) 1박으로 예약한다.
     * Then: 예약 금액은 120,000원이다.
     */
    @DisplayName("성수기 시작일(7월 1일)에는 성수기 요금이 적용된다.")
    @Test
    void 성수기_시작일_7월_1일에는_성수기_요금이_적용된다() {

        // Given
        // A-1 사이트가 예약 가능한 상태 (data.sql에서 설정됨)

        // When
        var 홍길동_예약요청 = 예약요청_생성(홍길동, 성수기_시작일_7월1일, 성수기_시작일_7월2일, 사이트번호_A_1);
        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);

        // Then
        예약이_되었다(홍길동_예약결과정보);
        예약_금액이_일치한다(홍길동_예약결과정보, 성수기_평일_할증_요금);
    }

    /**
     * Given: A-1 사이트가 예약 가능한 상태이다.
     * When: 홍길동이 A-1 사이트를 8월 31일(평일) 1박으로 예약한다.
     * Then: 예약 금액은 120,000원이다.
     */
    @DisplayName("성수기 종료일(8월 31일)에는 성수기 요금이 적용된다.")
    @Test
    void 성수기_종료일_8월_31일에는_성수기_요금이_적용된다() {

        // Given
        // A-1 사이트가 예약 가능한 상태 (data.sql에서 설정됨)

        // When
        var 홍길동_예약요청 = 예약요청_생성(홍길동, 성수기_종료일_8월31일, 성수기_종료일_9월1일, 사이트번호_A_1);
        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);

        // Then
        예약이_되었다(홍길동_예약결과정보);
        예약_금액이_일치한다(홍길동_예약결과정보, 성수기_평일_할증_요금);
    }

    /**
     * Given: A-1 사이트가 예약 가능한 상태이다.
     * When: 홍길동이 A-1 사이트를 6월 30일(평일) 1박으로 예약한다.
     * Then: 예약 금액은 80,000원이다.
     */
    @DisplayName("성수기 전날(6월 30일)에는 비수기 요금이 적용된다.")
    @Test
    void 성수기_전날_6월_30일에는_비수기_요금이_적용된다() {

        // Given
        // A-1 사이트가 예약 가능한 상태 (data.sql에서 설정됨)

        // When
        var 홍길동_예약요청 = 예약요청_생성(홍길동, 성수기_전날_6월30일, 성수기_전날_7월1일, 사이트번호_A_1);
        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);

        // Then
        예약이_되었다(홍길동_예약결과정보);
        예약_금액이_일치한다(홍길동_예약결과정보, A_사이트_기본요금);
    }

    /**
     * Given: A-1 사이트가 예약 가능한 상태이다.
     * When: 홍길동이 A-1 사이트를 9월 1일(평일) 1박으로 예약한다.
     * Then: 예약 금액은 80,000원이다.
     */
    @DisplayName("성수기 다음날(9월 1일)에는 비수기 요금이 적용된다.")
    @Test
    void 성수기_다음날_9월_1일에는_비수기_요금이_적용된다() {

        // Given
        // A-1 사이트가 예약 가능한 상태 (data.sql에서 설정됨)

        // When
        var 홍길동_예약요청 = 예약요청_생성(홍길동, 성수기_다음날_9월1일, 성수기_다음날_9월2일, 사이트번호_A_1);
        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);

        // Then
        예약이_되었다(홍길동_예약결과정보);
        예약_금액이_일치한다(홍길동_예약결과정보, A_사이트_기본요금);
    }

    /**
     * Given: A-1 사이트가 예약 가능한 상태이다.
     * When: 홍길동이 A-1 사이트를 비수기 평일 1박으로 예약한다.
     * Then: 적립 포인트는 4,000포인트이다. (80,000 * 0.05)
     */
    @DisplayName("평일 예약 시 5% 포인트가 적립된다.")
    @Test
    void 평일_예약_시_5퍼센트_포인트가_적립된다() {

        // Given
        // A-1 사이트가 예약 가능한 상태 (data.sql에서 설정됨)

        // When
        var 홍길동_예약요청 = 예약요청_생성(홍길동, 비수기_평일_시작일, 비수기_평일_종료일, 사이트번호_A_1);
        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);

        // Then
        예약이_되었다(홍길동_예약결과정보);
        포인트가_일치한다(홍길동_예약결과정보, 평일_포인트);
    }

    /**
     * Given: A-1 사이트가 예약 가능한 상태이다.
     * When: 홍길동이 A-1 사이트를 비수기 토요일 1박으로 예약한다.
     * Then: 적립 포인트는 10,400포인트이다. (104,000 * 0.10)
     */
    @DisplayName("주말 포함 예약 시 10% 포인트가 적립된다.")
    @Test
    void 주말_포함_예약_시_10퍼센트_포인트가_적립된다() {

        // Given
        // A-1 사이트가 예약 가능한 상태 (data.sql에서 설정됨)

        // When
        var 홍길동_예약요청 = 예약요청_생성(홍길동, 비수기_토요일_시작일, 비수기_토요일_종료일, 사이트번호_A_1);
        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);

        // Then
        예약이_되었다(홍길동_예약결과정보);
        포인트가_일치한다(홍길동_예약결과정보, 주말_포인트);
    }

    /**
     * Given: A-1 사이트가 예약 가능한 상태이다.
     * When: 홍길동이 A-1 사이트를 성수기 평일 1박으로 예약한다.
     * Then: 적립 포인트는 3,600포인트이다. (120,000 * 0.03)
     */
    @DisplayName("성수기 예약 시 3% 포인트가 적립된다.")
    @Test
    void 성수기_예약_시_3퍼센트_포인트가_적립된다() {

        // Given
        // A-1 사이트가 예약 가능한 상태 (data.sql에서 설정됨)

        // When
        var 홍길동_예약요청 = 예약요청_생성(홍길동, 성수기_7월_평일_시작일, 성수기_7월_평일_종료일, 사이트번호_A_1);
        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);

        // Then
        예약이_되었다(홍길동_예약결과정보);
        포인트가_일치한다(홍길동_예약결과정보, 성수기_포인트);
    }

    private ReservationRequest 예약요청_생성(
            String 예약자명,
            String 시작일,
            String 종료일,
            String 사이트번호
    ) {
        return new ReservationRequest(
                예약자명,
                LocalDate.parse(시작일),
                LocalDate.parse(종료일),
                사이트번호,
                연락처,
                인원수_5명,
                차량번호,
                요청사항
        );
    }

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
