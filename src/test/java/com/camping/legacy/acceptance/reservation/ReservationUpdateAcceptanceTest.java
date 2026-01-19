package com.camping.legacy.acceptance.reservation;

import com.camping.legacy.acceptance.AcceptanceTestBase;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static com.camping.legacy.acceptance.reservation.apiExtractableresponse.ReservationApiExtractableResponse.*;
import static com.camping.legacy.acceptance.reservation.builder.ReservationRequestBuilder.Reservation;
import static com.camping.legacy.acceptance.reservation.ReservationTestConstants.*;
import static com.camping.legacy.acceptance.reservation.ReservationTestHelpers.*;

@DisplayName("예약 수정 기능")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ReservationUpdateAcceptanceTest extends AcceptanceTestBase {

    /**
     * Given 홍길동이 A-1 사이트를 2026년 2월 1일~3일로 예약을 하고
     * When 홍길동이 확인 코드를 입력하고 날짜를 2월 5일~7일로 변경하면
     * Then 예약 날짜가 2월 5일~7일로 수정되고
     * And 기존 확인 코드는 유지된다.
     */
    @DisplayName("정상적으로 예약을 수정한다")
    @Test
    void 정상적으로_예약을_변경() {

        // Given
        var 홍길동_예약요청 = Reservation()
                .reserver(홍길동)
                .period(기존예약_시작일, 기존예약_종료일)
                .site(사이트번호_A_1)
                .build();

        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);
        var 예약ID = 예약정보에서_예약ID_조회(홍길동_예약결과정보);
        var 확인코드 = 예약정보에서_확인코드_조회(홍길동_예약결과정보);

        // When
        var 홍길동_예약변경요청 = Reservation()
                .reserver(홍길동)
                .period(변경예약_시작일, 변경예약_종료일)
                .site(사이트번호_A_1)
                .build();

        var 예약수정정보 = 예약을_수정한다(예약ID, 확인코드, 홍길동_예약변경요청);

        // Then
        예약이_수정되었다(예약수정정보);
        예약날짜가_변경되었다(예약수정정보, 변경예약_시작일, 변경예약_종료일);
        확인코드가_유지되었다(예약수정정보, 확인코드);
    }

    /**
     * Given A-1 사이트가 2026년 2월 1일부터 2월 3일까지 홍길동에게 예약되어 있고
     * When 틀린 확인 코드를 입력하고 예약 수정을 시도하면
     * Then 예약이 거부된다.
     */
    @DisplayName("틀린 확인코드를 입력할 경우 예약이 수정되지 않는다")
    @Test
    void 틀린_확인코드로_예약_수정_불가() {

        // Given
        var 홍길동_예약요청 = Reservation()
                .reserver(홍길동)
                .period(기존예약_시작일, 기존예약_종료일)
                .site(사이트번호_A_1)
                .build();

        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);
        var 예약ID = 예약정보에서_예약ID_조회(홍길동_예약결과정보);

        // When
        var 홍길동_예약변경요청 = Reservation()
                .reserver(홍길동)
                .period(변경예약_시작일, 변경예약_종료일)
                .site(사이트번호_A_1)
                .build();

        var 예약수정정보 = 예약을_수정한다(예약ID, 잘못된_확인코드, 홍길동_예약변경요청);

        // Then
        예약이_수정되지않았다(예약수정정보);
    }

    /**
     * Given 홍길동이 A-1 사이트를 2월 1일~3일로 예약했고
     * And 김철수가 A-1 사이트를 2월 5일~7일로 예약했고
     * When 홍길동이 자신의 예약을 2월 5일~7일로 변경 시도하면
     * Then 수정이 거부된다.
     */
    @DisplayName("이미 예약된 날짜로는 예약이 수정되지 않는다")
    @Test
    void 이미_예약된_날짜로_예약_수정_불가() {

        // Given
        var 홍길동_예약요청 = Reservation()
                .reserver(홍길동)
                .period(기존예약_시작일, 기존예약_종료일)
                .site(사이트번호_A_1)
                .build();

        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);
        var 예약ID = 예약정보에서_예약ID_조회(홍길동_예약결과정보);
        var 확인코드 = 예약정보에서_확인코드_조회(홍길동_예약결과정보);

        var 김철수_예약요청 = Reservation()
                .reserver(김철수)
                .period(변경예약_시작일, 변경예약_종료일)
                .site(사이트번호_A_1)
                .build();
        예약을_생성한다(김철수_예약요청);

        // When
        var 홍길동_예약변경요청 = Reservation()
                .reserver(홍길동)
                .period(변경예약_시작일, 변경예약_종료일)
                .site(사이트번호_A_1)
                .build();

        var 예약수정정보 = 예약을_수정한다(예약ID, 확인코드, 홍길동_예약변경요청);

        // Then
        예약이_수정되지않았다(예약수정정보);
    }

    // =====================================================
    // 2. 예약 수정 - 날짜 관련
    // =====================================================

    /**
     * Given 홍길동이 A-1 사이트를 2026년 2월 1일~3일로 예약했고
     * When 예약 기간을 32일로 수정 시도하면
     * Then 수정이 거부된다.
     */
    @DisplayName("예약 기간이 30일을 초과하면 수정이 거부된다")
    @Test
    void 예약기간_30일_초과시_수정_거부() {

        // Given
        var 홍길동_예약요청 = Reservation()
                .reserver(홍길동)
                .period(기존예약_시작일, 기존예약_종료일)
                .site(사이트번호_A_1)
                .build();

        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);
        var 예약ID = 예약정보에서_예약ID_조회(홍길동_예약결과정보);
        var 확인코드 = 예약정보에서_확인코드_조회(홍길동_예약결과정보);

        // When
        var 홍길동_예약변경요청 = Reservation()
                .reserver(홍길동)
                .period(장기예약_시작일, 장기예약_종료일_32일)
                .site(사이트번호_A_1)
                .build();

        var 예약수정정보 = 예약을_수정한다(예약ID, 확인코드, 홍길동_예약변경요청);

        // Then
        예약이_수정되지않았다(예약수정정보);
    }

    /**
     * Given 홍길동이 A-1 사이트를 2026년 2월 1일~3일로 예약했고
     * When 예약 기간을 정확히 30일(3월 1일~30일)로 수정하면
     * Then 수정이 성공한다.
     */
    @DisplayName("예약 기간이 정확히 30일이면 수정이 성공한다")
    @Test
    void 예약기간_정확히_30일이면_수정_성공() {

        // Given
        var 홍길동_예약요청 = Reservation()
                .reserver(홍길동)
                .period(기존예약_시작일, 기존예약_종료일)
                .site(사이트번호_A_1)
                .build();

        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);
        var 예약ID = 예약정보에서_예약ID_조회(홍길동_예약결과정보);
        var 확인코드 = 예약정보에서_확인코드_조회(홍길동_예약결과정보);

        // When - 기존 예약과 겹치지 않는 30일 기간으로 수정
        var 수정_시작일 = "2026-03-01";
        var 수정_종료일 = "2026-03-30";

        var 홍길동_예약변경요청 = Reservation()
                .reserver(홍길동)
                .period(수정_시작일, 수정_종료일)
                .site(사이트번호_A_1)
                .build();

        var 예약수정정보 = 예약을_수정한다(예약ID, 확인코드, 홍길동_예약변경요청);

        // Then
        예약이_수정되었다(예약수정정보);
    }

    /**
     * Given 홍길동이 A-1 사이트를 2026년 2월 1일~3일로 예약했고
     * When 과거 날짜로 수정 시도하면
     * Then 수정이 거부된다.
     */
    @DisplayName("과거 날짜로 수정 시도하면 거부된다")
    @Test
    void 과거_날짜로_수정_시도시_거부() {

        // Given
        var 미래예약_시작일 = LocalDate.now().plusDays(10).toString();
        var 미래예약_종료일 = LocalDate.now().plusDays(12).toString();

        var 홍길동_예약요청 = Reservation()
                .reserver(홍길동)
                .period(미래예약_시작일, 미래예약_종료일)
                .site(사이트번호_A_1)
                .build();

        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);
        var 예약ID = 예약정보에서_예약ID_조회(홍길동_예약결과정보);
        var 확인코드 = 예약정보에서_확인코드_조회(홍길동_예약결과정보);

        // When
        var 과거예약_시작일 = LocalDate.now().minusDays(2).toString();
        var 과거예약_종료일 = LocalDate.now().minusDays(1).toString();

        var 홍길동_예약변경요청 = Reservation()
                .reserver(홍길동)
                .period(과거예약_시작일, 과거예약_종료일)
                .site(사이트번호_A_1)
                .build();

        var 예약수정정보 = 예약을_수정한다(예약ID, 확인코드, 홍길동_예약변경요청);

        // Then
        예약이_수정되지않았다(예약수정정보);
    }

    /**
     * Given 홍길동이 A-1 사이트를 2026년 2월 1일~3일로 예약했고
     * When 종료일이 시작일보다 이전인 날짜로 수정 시도하면
     * Then 수정이 거부된다.
     */
    @DisplayName("종료일이 시작일보다 이전이면 수정이 거부된다")
    @Test
    void 종료일이_시작일보다_이전이면_수정_거부() {

        // Given
        var 홍길동_예약요청 = Reservation()
                .reserver(홍길동)
                .period(기존예약_시작일, 기존예약_종료일)
                .site(사이트번호_A_1)
                .build();

        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);
        var 예약ID = 예약정보에서_예약ID_조회(홍길동_예약결과정보);
        var 확인코드 = 예약정보에서_확인코드_조회(홍길동_예약결과정보);

        // When
        var 홍길동_예약변경요청 = Reservation()
                .reserver(홍길동)
                .period(역전예약_시작일, 역전예약_종료일)
                .site(사이트번호_A_1)
                .build();

        var 예약수정정보 = 예약을_수정한다(예약ID, 확인코드, 홍길동_예약변경요청);

        // Then
        예약이_수정되지않았다(예약수정정보);
    }

    // =====================================================
    // 3. 예약 수정 - 수용 인원 관련
    // =====================================================

    /**
     * Given 홍길동이 A-1 사이트를 2026년 2월 1일~3일로 예약했고
     * When 인원을 7명으로 수정 시도하면
     * Then 수정이 거부된다.
     */
    @DisplayName("최대 수용 인원을 초과하면 수정이 거부된다")
    @Test
    void 최대_수용_인원_초과시_수정_거부() {

        // Given
        var 홍길동_예약요청 = Reservation()
                .reserver(홍길동)
                .period(기존예약_시작일, 기존예약_종료일)
                .site(사이트번호_A_1)
                .build();

        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);
        var 예약ID = 예약정보에서_예약ID_조회(홍길동_예약결과정보);
        var 확인코드 = 예약정보에서_확인코드_조회(홍길동_예약결과정보);

        // When
        var 홍길동_예약변경요청 = Reservation()
                .reserver(홍길동)
                .period(기존예약_시작일, 기존예약_종료일)
                .site(사이트번호_A_1)
                .headCount(인원수_7명)
                .build();

        var 예약수정정보 = 예약을_수정한다(예약ID, 확인코드, 홍길동_예약변경요청);

        // Then
        예약이_수정되지않았다(예약수정정보);
    }

    /**
     * Given 홍길동이 A-1 사이트를 2026년 2월 1일~3일로 예약했고
     * When 인원을 6명으로 수정하면
     * Then 수정이 성공한다.
     */
    @DisplayName("최대 수용 인원과 동일하면 수정이 성공한다")
    @Test
    void 최대_수용_인원과_동일하면_수정_성공() {

        // Given
        var 홍길동_예약요청 = Reservation()
                .reserver(홍길동)
                .period(기존예약_시작일, 기존예약_종료일)
                .site(사이트번호_A_1)
                .build();

        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);
        var 예약ID = 예약정보에서_예약ID_조회(홍길동_예약결과정보);
        var 확인코드 = 예약정보에서_확인코드_조회(홍길동_예약결과정보);

        // When
        var 홍길동_예약변경요청 = Reservation()
                .reserver(홍길동)
                .period(기존예약_시작일, 기존예약_종료일)
                .site(사이트번호_A_1)
                .headCount(인원수_6명)
                .build();

        var 예약수정정보 = 예약을_수정한다(예약ID, 확인코드, 홍길동_예약변경요청);

        // Then
        예약이_수정되었다(예약수정정보);
    }

    /**
     * Given 홍길동이 A-1 사이트를 2026년 2월 1일~3일로 예약했고
     * When 인원을 0명으로 수정 시도하면
     * Then 수정이 거부된다.
     */
    @DisplayName("예약 인원이 0명이면 수정이 거부된다")
    @Test
    void 인원수_0명으로_수정시_거부() {

        // Given
        var 홍길동_예약요청 = Reservation()
                .reserver(홍길동)
                .period(기존예약_시작일, 기존예약_종료일)
                .site(사이트번호_A_1)
                .build();

        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);
        var 예약ID = 예약정보에서_예약ID_조회(홍길동_예약결과정보);
        var 확인코드 = 예약정보에서_확인코드_조회(홍길동_예약결과정보);

        // When
        var 홍길동_예약변경요청 = Reservation()
                .reserver(홍길동)
                .period(기존예약_시작일, 기존예약_종료일)
                .site(사이트번호_A_1)
                .headCount(인원수_0명)
                .build();

        var 예약수정정보 = 예약을_수정한다(예약ID, 확인코드, 홍길동_예약변경요청);

        // Then
        예약이_수정되지않았다(예약수정정보);
    }

    // =====================================================
    // 4. 예약 수정 - 요금 재계산
    // =====================================================

    /**
     * Given 홍길동이 A-1 사이트를 비수기 평일 1박으로 예약했고
     * When 비수기 주말로 날짜를 수정하면
     * Then 수정된 요금은 104,000원이다. (80,000 * 1.3)
     */
    @DisplayName("평일에서 주말로 수정하면 주말 할증 요금이 적용된다")
    @Test
    void 평일에서_주말로_수정하면_주말_할증_요금이_적용된다() {

        // Given
        var 홍길동_예약요청 = Reservation()
                .reserver(홍길동)
                .period(비수기_평일_시작일, 비수기_평일_종료일)
                .site(사이트번호_A_1)
                .build();

        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);
        var 예약ID = 예약정보에서_예약ID_조회(홍길동_예약결과정보);
        var 확인코드 = 예약정보에서_확인코드_조회(홍길동_예약결과정보);

        // When
        var 홍길동_예약변경요청 = Reservation()
                .reserver(홍길동)
                .period(비수기_토요일_시작일, 비수기_토요일_종료일)
                .site(사이트번호_A_1)
                .build();

        var 예약수정정보 = 예약을_수정한다(예약ID, 확인코드, 홍길동_예약변경요청);

        // Then
        예약이_수정되었다(예약수정정보);
        예약_금액이_일치한다(예약수정정보, 주말_할증_요금);
    }

    /**
     * Given 홍길동이 A-1 사이트를 비수기 평일 1박으로 예약했고
     * When 성수기 평일로 날짜를 수정하면
     * Then 수정된 요금은 120,000원이다. (80,000 * 1.5)
     */
    @DisplayName("비수기에서 성수기로 수정하면 성수기 할증 요금이 적용된다")
    @Test
    void 비수기에서_성수기로_수정하면_성수기_할증_요금이_적용된다() {

        // Given
        var 홍길동_예약요청 = Reservation()
                .reserver(홍길동)
                .period(비수기_평일_시작일, 비수기_평일_종료일)
                .site(사이트번호_A_1)
                .build();

        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);
        var 예약ID = 예약정보에서_예약ID_조회(홍길동_예약결과정보);
        var 확인코드 = 예약정보에서_확인코드_조회(홍길동_예약결과정보);

        // When
        var 홍길동_예약변경요청 = Reservation()
                .reserver(홍길동)
                .period(성수기_7월_평일_시작일, 성수기_7월_평일_종료일)
                .site(사이트번호_A_1)
                .build();

        var 예약수정정보 = 예약을_수정한다(예약ID, 확인코드, 홍길동_예약변경요청);

        // Then
        예약이_수정되었다(예약수정정보);
        예약_금액이_일치한다(예약수정정보, 성수기_평일_할증_요금);
    }

    /**
     * Given 홍길동이 A-1 사이트를 비수기 평일 1박으로 예약했고
     * When 성수기 주말로 날짜를 수정하면
     * Then 수정된 요금은 136,000원이다. (80,000 * 1.7)
     */
    @DisplayName("비수기 평일에서 성수기 주말로 수정하면 성수기 주말 할증 요금이 적용된다")
    @Test
    void 비수기_평일에서_성수기_주말로_수정하면_성수기_주말_할증_요금이_적용된다() {

        // Given
        var 홍길동_예약요청 = Reservation()
                .reserver(홍길동)
                .period(비수기_평일_시작일, 비수기_평일_종료일)
                .site(사이트번호_A_1)
                .build();

        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);
        var 예약ID = 예약정보에서_예약ID_조회(홍길동_예약결과정보);
        var 확인코드 = 예약정보에서_확인코드_조회(홍길동_예약결과정보);

        // When
        var 홍길동_예약변경요청 = Reservation()
                .reserver(홍길동)
                .period(성수기_8월_토요일_시작일, 성수기_8월_토요일_종료일)
                .site(사이트번호_A_1)
                .build();

        var 예약수정정보 = 예약을_수정한다(예약ID, 확인코드, 홍길동_예약변경요청);

        // Then
        예약이_수정되었다(예약수정정보);
        예약_금액이_일치한다(예약수정정보, 성수기_주말_할증_요금);
    }

    // =====================================================
    // 5. 예약 수정 - 포인트 재계산
    // =====================================================

    /**
     * Given 홍길동이 A-1 사이트를 비수기 평일 1박으로 예약했고
     * When 비수기 주말로 날짜를 수정하면
     * Then 수정된 포인트는 10,400포인트이다. (104,000 * 0.10)
     */
    @DisplayName("평일에서 주말로 수정하면 주말 포인트가 적립된다")
    @Test
    void 평일에서_주말로_수정하면_주말_포인트가_적립된다() {

        // Given
        var 홍길동_예약요청 = Reservation()
                .reserver(홍길동)
                .period(비수기_평일_시작일, 비수기_평일_종료일)
                .site(사이트번호_A_1)
                .build();

        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);
        var 예약ID = 예약정보에서_예약ID_조회(홍길동_예약결과정보);
        var 확인코드 = 예약정보에서_확인코드_조회(홍길동_예약결과정보);

        // When
        var 홍길동_예약변경요청 = Reservation()
                .reserver(홍길동)
                .period(비수기_토요일_시작일, 비수기_토요일_종료일)
                .site(사이트번호_A_1)
                .build();

        var 예약수정정보 = 예약을_수정한다(예약ID, 확인코드, 홍길동_예약변경요청);

        // Then
        예약이_수정되었다(예약수정정보);
        포인트가_일치한다(예약수정정보, 주말_포인트);
    }

    /**
     * Given 홍길동이 A-1 사이트를 비수기 평일 1박으로 예약했고
     * When 성수기 평일로 날짜를 수정하면
     * Then 수정된 포인트는 3,600포인트이다. (120,000 * 0.03)
     */
    @DisplayName("비수기에서 성수기로 수정하면 성수기 포인트가 적립된다")
    @Test
    void 비수기에서_성수기로_수정하면_성수기_포인트가_적립된다() {

        // Given
        var 홍길동_예약요청 = Reservation()
                .reserver(홍길동)
                .period(비수기_평일_시작일, 비수기_평일_종료일)
                .site(사이트번호_A_1)
                .build();

        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);
        var 예약ID = 예약정보에서_예약ID_조회(홍길동_예약결과정보);
        var 확인코드 = 예약정보에서_확인코드_조회(홍길동_예약결과정보);

        // When
        var 홍길동_예약변경요청 = Reservation()
                .reserver(홍길동)
                .period(성수기_7월_평일_시작일, 성수기_7월_평일_종료일)
                .site(사이트번호_A_1)
                .build();

        var 예약수정정보 = 예약을_수정한다(예약ID, 확인코드, 홍길동_예약변경요청);

        // Then
        예약이_수정되었다(예약수정정보);
        포인트가_일치한다(예약수정정보, 성수기_포인트);
    }
}
