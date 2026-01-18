package com.camping.legacy.acceptance.reservation;

import com.camping.legacy.acceptance.AcceptanceTestBase;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.camping.legacy.acceptance.reservation.apiExtractableresponse.ReservationApiExtractableResponse.*;
import static com.camping.legacy.acceptance.reservation.builder.ReservationRequestBuilder.Reservation;
import static com.camping.legacy.acceptance.reservation.ReservationTestConstants.*;
import static com.camping.legacy.acceptance.reservation.apiExtractableresponse.SiteApiExtractableResponse.사이트를_조회한다;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("예약 관련 기능")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ReservationAcceptanceTest extends AcceptanceTestBase {

    // =====================================================
    // 1. 예약 생성
    // =====================================================

    /**
     * Given A-1 사이트가 2026년 2월 1일부터 2월 3일까지 예약 가능한 상태이고
     * When 홍길동이 A-1 사이트를 2026년 2월 1일부터 2월 3일까지 예약하면
     * Then 예약이 성공적으로 생성되고
     * And 6자리 확인 코드가 발급되고
     * And 예약 상태가 "CONFIRMED"로 설정된다.
     */
    @DisplayName("[예약/생성] 정상적으로 예약을 생성한다.")
    @Test
    void 정상적으로_예약을_생성() {

        // Given
        var 사이트정보 = 사이트를_조회한다(기존예약_시작일, 기존예약_종료일, 사이트_크기_대형);
        사이트가_존재한다(사이트정보, 사이트번호_A_1);

        // When
        var 홍길동_예약요청 = Reservation()
                .reserver(홍길동)
                .period(기존예약_시작일, 기존예약_종료일)
                .site(사이트번호_A_1)
                .build();

        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);

        // Then
        예약이_되었다(홍길동_예약결과정보);
        확인코드가_발급되었다(홍길동_예약결과정보, 확인코드_길이_6자리);
        예약상태가_확정이다(홍길동_예약결과정보, 예약상태_예약완료);
    }

    /**
     * Given A-1 사이트가 예약 가능한 상태이고
     * When 홍길동이 A-1 사이트를 2026-02-01부터 2026-03-05까지 (32일) 예약 시도하면
     * Then 예약은 생성되지 않는다.
     */
    @DisplayName("[예약/생성] 예약 기간이 30일을 초과하면 예약이 거부된다.")
    @Test
    void 예약기간_30일_초과시_예약_거부() {

        // Given
        var 사이트정보 = 사이트를_조회한다(장기예약_시작일, 장기예약_종료일_32일, 사이트_크기_대형);
        사이트가_존재한다(사이트정보, 사이트번호_A_1);

        // When
        var 홍길동_예약요청 = Reservation()
                .reserver(홍길동)
                .period(장기예약_시작일, 장기예약_종료일_32일)
                .site(사이트번호_A_1)
                .build();

        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);

        // Then
        예약이_되지않았다(홍길동_예약결과정보);
    }

    /**
     * Given A-1 사이트가 예약 가능한 상태이고
     * When 홍길동이 A-1 사이트를 2026-02-01부터 2026-03-02까지 (30일) 예약하면
     * Then 예약이 생성된다.
     */
    @DisplayName("[예약/생성] 예약 기간이 정확히 30일이면 예약이 성공한다.")
    @Test
    void 예약기간_정확히_30일이면_예약_성공() {

        // Given
        var 사이트정보 = 사이트를_조회한다(장기예약_시작일, 장기예약_종료일_30일, 사이트_크기_대형);
        사이트가_존재한다(사이트정보, 사이트번호_A_1);

        // When
        var 홍길동_예약요청 = Reservation()
                .reserver(홍길동)
                .period(장기예약_시작일, 장기예약_종료일_30일)
                .site(사이트번호_A_1)
                .build();

        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);

        // Then
        예약이_되었다(홍길동_예약결과정보);
    }

    /**
     * Given A-1 사이트의 최대 수용 인원은 6명이고
     * When 홍길동이 A-1 사이트를 7명으로 2026-02-01부터 2026-02-03까지 예약 시도하면
     * Then 예약은 생성되지 않는다.
     */
    @DisplayName("[예약/생성] 최대 수용 인원을 초과하면 예약이 거부된다.")
    @Test
    void 최대_수용_인원_초과시_예약_거부() {

        // Given
        var 사이트정보 = 사이트를_조회한다(기존예약_시작일, 기존예약_종료일, 사이트_크기_대형);
        사이트가_존재한다(사이트정보, 사이트번호_A_1);

        // When
        var 홍길동_예약요청 = Reservation()
                .reserver(홍길동)
                .period(기존예약_시작일, 기존예약_종료일)
                .site(사이트번호_A_1)
                .headCount(인원수_7명)
                .build();

        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);

        // Then
        예약이_되지않았다(홍길동_예약결과정보);
    }

    /**
     * Given A-1 사이트의 최대 수용 인원은 6명이고
     * When 홍길동이 A-1 사이트를 6명으로 2026-02-01부터 2026-02-03까지 예약하면
     * Then 예약이 생성된다.
     */
    @DisplayName("[예약/생성] 최대 수용 인원과 동일하면 예약이 성공한다.")
    @Test
    void 최대_수용_인원과_동일하면_예약_성공() {

        // Given
        var 사이트정보 = 사이트를_조회한다(기존예약_시작일, 기존예약_종료일, 사이트_크기_대형);
        사이트가_존재한다(사이트정보, 사이트번호_A_1);

        // When
        var 홍길동_예약요청 = Reservation()
                .reserver(홍길동)
                .period(기존예약_시작일, 기존예약_종료일)
                .site(사이트번호_A_1)
                .headCount(인원수_6명)
                .build();

        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);

        // Then
        예약이_되었다(홍길동_예약결과정보);
    }

    /**
     * Given A-1 사이트가 예약 가능한 상태이고
     * When 홍길동이 A-1 사이트를 0명으로 2026-02-01부터 2026-02-03까지 예약 시도하면
     * Then 예약은 생성되지 않는다.
     */
    @DisplayName("[예약/생성] 예약 인원이 0명이면 예약이 거부된다.")
    @Test
    void 인원수_0명으로_예약시_예약_거부() {

        // Given
        var 사이트정보 = 사이트를_조회한다(기존예약_시작일, 기존예약_종료일, 사이트_크기_대형);
        사이트가_존재한다(사이트정보, 사이트번호_A_1);

        // When
        var 홍길동_예약요청 = Reservation()
                .reserver(홍길동)
                .period(기존예약_시작일, 기존예약_종료일)
                .site(사이트번호_A_1)
                .headCount(인원수_0명)
                .build();

        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);

        // Then
        예약이_되지않았다(홍길동_예약결과정보);
    }

    /**
     * Given 오늘 날짜 기준으로 과거 날짜이고
     * When 홍길동이 A-1 사이트를 과거 날짜로 예약 시도하면
     * Then 예약은 생성되지 않는다.
     */
    @DisplayName("[예약/생성] 과거 날짜로 예약 시도하면 거부된다.")
    @Test
    void 과거_날짜로_예약_시도시_거부() {

        // Given
        var 과거예약_시작일 = LocalDate.now().minusDays(2).toString();
        var 과거예약_종료일 = LocalDate.now().minusDays(1).toString();

        // When
        var 홍길동_예약요청 = Reservation()
                .reserver(홍길동)
                .period(과거예약_시작일, 과거예약_종료일)
                .site(사이트번호_A_1)
                .build();

        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);

        // Then
        예약이_되지않았다(홍길동_예약결과정보);
    }

    /**
     * Given A-1 사이트가 예약 가능한 상태이고
     * When 홍길동이 A-1 사이트를 시작일 2026-02-05, 종료일 2026-02-03으로 예약 시도하면
     * Then 예약은 생성되지 않는다.
     */
    @DisplayName("[예약/생성] 종료일이 시작일보다 이전이면 예약이 거부된다.")
    @Test
    void 종료일이_시작일보다_이전이면_예약_거부() {

        // Given
        var 사이트정보 = 사이트를_조회한다(기존예약_시작일, 기존예약_종료일, 사이트_크기_대형);
        사이트가_존재한다(사이트정보, 사이트번호_A_1);

        // When
        var 홍길동_예약요청 = Reservation()
                .reserver(홍길동)
                .period(역전예약_시작일, 역전예약_종료일)
                .site(사이트번호_A_1)
                .build();

        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);

        // Then
        예약이_되지않았다(홍길동_예약결과정보);
    }

    // =====================================================
    // 2. 예약 중복/경계 (기간 겹침 규칙)
    // =====================================================

    /**
     * Given A-1 사이트가 2026년 2월 1일부터 2월 3일까지 홍길동에게 예약되어 있고
     * When 김철수가 A-1 사이트를 2026년 2월 2일부터 2월 4일까지 예약 시도하면
     * Then 예약이 거부된다.
     */
    @DisplayName("[예약/중복] 이미 예약된 사이트는 예약되지 않는다.")
    @Test
    void 중복_예약은_불가() {

        // Given
        var 홍길동_예약요청 = Reservation()
                .reserver(홍길동)
                .period(기존예약_시작일, 기존예약_종료일)
                .site(사이트번호_A_1)
                .build();
        예약을_생성한다(홍길동_예약요청);

        // When
        var 김철수_예약요청 = Reservation()
                .reserver(김철수)
                .period(중복예약_시작일, 중복예약_종료일)
                .site(사이트번호_A_1)
                .build();

        var 김철수_예약결과정보 = 예약을_생성한다(김철수_예약요청);

        // Then
        예약이_되지않았다(김철수_예약결과정보);
    }

    /**
     * Given 홍길동이 A-1 사이트를 2026-02-01부터 2026-02-03까지 예약했고
     * When 김철수가 A-1 사이트를 2026-02-03부터 2026-02-05까지 예약 시도하면
     * Then 예약은 생성되지 않는다.
     */
    @DisplayName("[예약/중복] 기존 예약 종료일과 새 예약 시작일이 같으면 중복으로 거부된다.")
    @Test
    void 기존_예약_종료일과_새_예약_시작일이_같으면_중복으로_거부() {

        // Given
        var 홍길동_예약요청 = Reservation()
                .reserver(홍길동)
                .period(기존예약_시작일, 기존예약_종료일)
                .site(사이트번호_A_1)
                .build();
        예약을_생성한다(홍길동_예약요청);

        // When
        var 김철수_예약요청 = Reservation()
                .reserver(김철수)
                .period(경계_예약_시작일, 경계_예약_종료일)
                .site(사이트번호_A_1)
                .build();

        var 김철수_예약결과정보 = 예약을_생성한다(김철수_예약요청);

        // Then
        예약이_되지않았다(김철수_예약결과정보);
    }

    /**
     * Given 홍길동이 A-1 사이트를 2026-02-01부터 2026-02-03까지 예약했고
     * When 김철수가 A-1 사이트를 2026-02-04부터 2026-02-06까지 예약하면
     * Then 예약이 생성된다.
     */
    @DisplayName("[예약/중복] 기존 예약 종료일 다음날부터 시작하면 예약이 성공한다.")
    @Test
    void 기존_예약_종료일_다음날부터_시작하면_예약_성공() {

        // Given
        var 홍길동_예약요청 = Reservation()
                .reserver(홍길동)
                .period(기존예약_시작일, 기존예약_종료일)
                .site(사이트번호_A_1)
                .build();
        예약을_생성한다(홍길동_예약요청);

        // When
        var 김철수_예약요청 = Reservation()
                .reserver(김철수)
                .period(연속_예약_시작일, 연속_예약_종료일)
                .site(사이트번호_A_1)
                .build();

        var 김철수_예약결과정보 = 예약을_생성한다(김철수_예약요청);

        // Then
        예약이_되었다(김철수_예약결과정보);
    }

    /**
     * Given 홍길동이 A-1 사이트를 2026-02-01부터 2026-02-03까지 예약했고
     * When 김철수가 A-1 사이트를 2026-02-01부터 2026-02-02까지 예약 시도하면
     * Then 예약은 생성되지 않는다.
     */
    @DisplayName("[예약/중복] 기존 예약 시작일을 포함하는 기간으로 예약하면 거부된다.")
    @Test
    void 기존_예약_시작일을_포함하는_기간으로_예약하면_거부() {

        // Given
        var 홍길동_예약요청 = Reservation()
                .reserver(홍길동)
                .period(기존예약_시작일, 기존예약_종료일)
                .site(사이트번호_A_1)
                .build();
        예약을_생성한다(홍길동_예약요청);

        // When
        var 김철수_예약요청 = Reservation()
                .reserver(김철수)
                .period(기존예약_시작일, 시작일_포함_예약_종료일)
                .site(사이트번호_A_1)
                .build();

        var 김철수_예약결과정보 = 예약을_생성한다(김철수_예약요청);

        // Then
        예약이_되지않았다(김철수_예약결과정보);
    }

    /**
     * Given 홍길동이 A-1 사이트를 2026-02-01부터 2026-02-03까지 예약했고
     * When 김철수가 A-1 사이트를 2026-02-02부터 2026-02-03까지 예약 시도하면
     * Then 예약은 생성되지 않는다.
     */
    @DisplayName("[예약/중복] 기존 예약 종료일을 포함하는 기간으로 예약하면 거부된다.")
    @Test
    void 기존_예약_종료일을_포함하는_기간으로_예약하면_거부() {

        // Given
        var 홍길동_예약요청 = Reservation()
                .reserver(홍길동)
                .period(기존예약_시작일, 기존예약_종료일)
                .site(사이트번호_A_1)
                .build();
        예약을_생성한다(홍길동_예약요청);

        // When
        var 김철수_예약요청 = Reservation()
                .reserver(김철수)
                .period(중복예약_시작일, 기존예약_종료일)
                .site(사이트번호_A_1)
                .build();

        var 김철수_예약결과정보 = 예약을_생성한다(김철수_예약요청);

        // Then
        예약이_되지않았다(김철수_예약결과정보);
    }

    /**
     * Given 홍길동이 A-1 사이트를 2026-02-02부터 2026-02-03까지 예약했고
     * When 김철수가 A-1 사이트를 2026-02-01부터 2026-02-04까지 예약 시도하면
     * Then 예약은 생성되지 않는다.
     */
    @DisplayName("[예약/중복] 기존 예약을 완전히 포함하는 기간으로 예약하면 거부된다.")
    @Test
    void 기존_예약을_완전히_포함하는_기간으로_예약하면_거부() {

        // Given
        var 홍길동_예약요청 = Reservation()
                .reserver(홍길동)
                .period(중복예약_시작일, 기존예약_종료일)
                .site(사이트번호_A_1)
                .build();
        예약을_생성한다(홍길동_예약요청);

        // When
        var 김철수_예약요청 = Reservation()
                .reserver(김철수)
                .period(기존예약_시작일, 중복예약_종료일)
                .site(사이트번호_A_1)
                .build();

        var 김철수_예약결과정보 = 예약을_생성한다(김철수_예약요청);

        // Then
        예약이_되지않았다(김철수_예약결과정보);
    }

    // =====================================================
    // 3. 동시성
    // =====================================================

    /**
     * Given A-1 사이트가 2026년 2월 1일에 예약 가능한 상태이고
     * When 홍길동과 김철수가 동시에 같은 날짜로 A-1 사이트 예약을 요청하면
     * Then 한 명의 예약만 성공하고
     * And 나머지 한 명은 예약이 거부된다.
     */
    @DisplayName("[예약/동시성] 동시에 같은 날짜/사이트로 예약 요청하면 한 건만 성공한다.")
    @Test
    void 동시_예약은_한건만_성공한다() throws InterruptedException {

        // Given
        var 사이트정보 = 사이트를_조회한다(기존예약_시작일, 기존예약_종료일, 사이트_크기_대형);
        사이트가_존재한다(사이트정보, 사이트번호_A_1);

        var 홍길동_예약요청 = Reservation()
                .reserver(홍길동)
                .period(기존예약_시작일, 기존예약_종료일)
                .site(사이트번호_A_1)
                .build();

        var 김철수_예약요청 = Reservation()
                .reserver(김철수)
                .period(기존예약_시작일, 기존예약_종료일)
                .site(사이트번호_A_1)
                .build();

        int 동시요청_개수 = 2;

        CountDownLatch 준비_완료 = new CountDownLatch(동시요청_개수);
        CountDownLatch 동시에_시작 = new CountDownLatch(1);
        CountDownLatch 종료_대기 = new CountDownLatch(동시요청_개수);

        CopyOnWriteArrayList<ExtractableResponse<Response>> 응답목록 = new CopyOnWriteArrayList<>();
        CopyOnWriteArrayList<Throwable> 예외목록 = new CopyOnWriteArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(동시요청_개수);

        // When
        executor.submit(() -> {
            try {
                준비_완료.countDown();
                동시에_시작.await();
                응답목록.add(예약을_생성한다(홍길동_예약요청));
            } catch (Throwable e) {
                예외목록.add(e);
            } finally {
                종료_대기.countDown();
            }
        });

        executor.submit(() -> {
            try {
                준비_완료.countDown();
                동시에_시작.await();
                응답목록.add(예약을_생성한다(김철수_예약요청));
            } catch (Throwable e) {
                예외목록.add(e);
            } finally {
                종료_대기.countDown();
            }
        });

        준비_완료.await();
        동시에_시작.countDown();

        종료_대기.await();
        executor.shutdownNow();

        // Then
        assertThat(예외목록).isEmpty();
        assertThat(응답목록).hasSize(동시요청_개수);

        long 성공건수 = 응답목록.stream().filter(r -> r.statusCode() == 201).count();
        long 실패건수 = 응답목록.stream().filter(r -> r.statusCode() == 409).count();

        assertThat(성공건수).isEqualTo(1);
        assertThat(실패건수).isEqualTo(1);
    }

    // =====================================================
    // 4. 예약 수정
    // =====================================================

    /**
     * Given 홍길동이 A-1 사이트를 2026년 2월 1일~3일로 예약을 하고
     * When 홍길동이 확인 코드를 입력하고 날짜를 2월 5일~7일로 변경하면
     * Then 예약 날짜가 2월 5일~7일로 수정되고
     * And 기존 확인 코드는 유지된다.
     */
    @DisplayName("[예약/수정] 정상적으로 예약을 수정한다.")
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
    @DisplayName("[예약/수정] 틀린 확인코드를 입력할 경우 예약이 수정되지 않는다.")
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
    @DisplayName("[예약/수정] 이미 예약된 날짜로는 예약이 수정되지 않는다.")
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
    // 5. 예약 취소
    // =====================================================

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

    private void 예약이_되었다(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(201);
    }

    private void 예약이_되지않았다(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(409);
    }

    private void 예약이_수정되었다(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(200);
    }

    private void 예약이_수정되지않았다(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(400);
    }

    private void 확인코드가_발급되었다(ExtractableResponse<Response> response, int expectedLength) {
        String confirmationCode = response.jsonPath().getString("confirmationCode");
        assertThat(confirmationCode).hasSize(expectedLength);
    }

    private void 예약상태가_확정이다(ExtractableResponse<Response> response, String expectedStatus) {
        String status = response.jsonPath().getString("status");
        assertThat(status).isEqualTo(expectedStatus);
    }

    private void 예약날짜가_변경되었다(ExtractableResponse<Response> response, String expectedStartDate, String expectedEndDate) {
        String startDate = response.jsonPath().getString("startDate");
        String endDate = response.jsonPath().getString("endDate");
        assertThat(startDate).isEqualTo(expectedStartDate);
        assertThat(endDate).isEqualTo(expectedEndDate);
    }

    private void 확인코드가_유지되었다(ExtractableResponse<Response> response, String expectedConfirmationCode) {
        String confirmationCode = response.jsonPath().getString("confirmationCode");
        assertThat(confirmationCode).isEqualTo(expectedConfirmationCode);
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
