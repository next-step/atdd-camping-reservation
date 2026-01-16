package com.camping.legacy.acceptance.reservation;

import com.camping.legacy.acceptance.AcceptanceTestBase;
import com.camping.legacy.dto.ReservationRequest;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.camping.legacy.acceptance.reservation.ReservationApiExtractableResponse.*;
import static com.camping.legacy.acceptance.reservation.SiteApiExtractableResponse.사이트를_조회한다;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("예약 관련 기능")
@Sql({"/truncate.sql", "/data.sql"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ReservationAcceptanceTest extends AcceptanceTestBase {

    private static final String 사이트번호_A_1 = "A-1";
    private static final String 사이트_크기_대형 = "대형";

    private static final String 기존예약_시작일 = "2026-02-01";
    private static final String 기존예약_종료일 = "2026-02-03";

    private static final String 중복예약_시작일 = "2026-02-02";
    private static final String 중복예약_종료일 = "2026-02-04";

    private static final String 변경예약_시작일 = "2026-02-05";
    private static final String 변경예약_종료일 = "2026-02-07";

    private static final String 홍길동 = "홍길동";
    private static final String 김철수 = "김철수";
    private static final String 연락처 = "010-1234-1234";
    private static final int 인원수_5명 = 5;
    private static final String 차량번호 = "가1234";
    private static final String 요청사항 = "1시간 일찍 입실 예정";

    private static final int 확인코드_길이_6자리 = 6;
    private static final String 예약상태_예약완료 = "CONFIRMED";
    private static final String 예약상태_사전취소 = "CANCELLED";
    private static final String 예약상태_당일취소 = "CANCELLED_SAME_DAY";

    private static final String 잘못된_확인코드 = "WRONG1";

    /**
     * Given: A-1 사이트가 2026년 2월 1일부터 2월 3일까지 예약 가능한 상태이다.
     * When: 홍길동이 A-1 사이트를 2026년 2월 1일부터 2월 3일까지 예약한다.
     * Then: 예약이 성공적으로 생성된다.
     * And: 6자리 확인 코드가 발급된다.
     * And: 예약 상태가 "CONFIRMED"로 설정된다.
     */
    @DisplayName("정상적으로 예약을 생성한다.")
    @Test
    void 정상적으로_예약을_생성() {

        // Given
        var 사이트정보 = 사이트를_조회한다(기존예약_시작일, 기존예약_종료일, 사이트_크기_대형);
        사이트가_존재한다(사이트정보, 사이트번호_A_1);

        // When
        var 홍길동_예약요청 = 예약요청_생성(홍길동, 기존예약_시작일, 기존예약_종료일, 사이트번호_A_1);
        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);

        // Then
        예약이_되었다(홍길동_예약결과정보);
        확인코드가_발급되었다(홍길동_예약결과정보, 확인코드_길이_6자리);
        예약상태가_확정이다(홍길동_예약결과정보, 예약상태_예약완료);
    }

    /**
     * Given: A-1 사이트가 2026년 2월 1일부터 2월 3일까지 홍길동에게 예약되어 있다.
     * When: 김철수가 A-1 사이트를 2026년 2월 2일부터 2월 4일까지 예약 시도한다.
     * Then: 예약이 거부된다.
     */
    @DisplayName("이미 예약된 사이트는 예약되지 않는다.")
    @Test
    void 중복_예약은_불가() {

        // Given
        var 홍길동_예약요청 = 예약요청_생성(홍길동, 기존예약_시작일, 기존예약_종료일, 사이트번호_A_1);
        예약을_생성한다(홍길동_예약요청);

        // When
        var 김철수_예약요청 = 예약요청_생성(김철수, 중복예약_시작일, 중복예약_종료일, 사이트번호_A_1);
        var 김철수_예약결과정보 = 예약을_생성한다(김철수_예약요청);

        // Then
        예약이_되지않았다(김철수_예약결과정보);
    }

    /**
     * Given: A-1 사이트가 2026년 2월 1일에 예약 가능한 상태이다.
     * When: 홍길동과 김철수가 동시에 같은 날짜로 A-1 사이트 예약을 요청한다.
     * Then: 한 명의 예약만 성공한다.
     * And: 나머지 한 명은 예약이 거부된다.
     */
    @DisplayName("동시에 같은 날짜/사이트로 예약 요청하면 한 건만 성공한다.")
    @Test
    void 동시_예약은_한건만_성공한다() throws InterruptedException {

        // Given
        var 사이트정보 = 사이트를_조회한다(기존예약_시작일, 기존예약_종료일, 사이트_크기_대형);
        사이트가_존재한다(사이트정보, 사이트번호_A_1);

        var 홍길동_예약요청 = 예약요청_생성(홍길동, 기존예약_시작일, 기존예약_종료일, 사이트번호_A_1);
        var 김철수_예약요청 = 예약요청_생성(김철수, 기존예약_시작일, 기존예약_종료일, 사이트번호_A_1);

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

        // 두 스레드가 "출발선"에 모두 도착할 때까지 대기 후 동시에 출발
        준비_완료.await();
        동시에_시작.countDown();

        // 두 요청이 끝날 때까지 대기
        종료_대기.await();
        executor.shutdownNow();

        // Then
        // - 두 요청 모두 HTTP 응답으로 귀결되어야 함 (예외로 터지면 테스트 자체가 불안정)
        assertThat(예외목록).isEmpty();
        assertThat(응답목록).hasSize(동시요청_개수);

        long 성공건수 = 응답목록.stream().filter(r -> r.statusCode() == 201).count();
        long 실패건수 = 응답목록.stream().filter(r -> r.statusCode() == 409).count();

        assertThat(성공건수).isEqualTo(1);
        assertThat(실패건수).isEqualTo(1);
    }

    /**
     * Given: 홍길동이 A-1 사이트를 2026년 2월 1일~3일로 예약을 한다.
     * When: 홍길동이 확인 코드를 입력하고 날짜를 2월 5일~7일로 변경한다.
     * Then: 예약 날짜가 2월 5일~7일로 수정된다.
     * And: 기존 확인 코드 "ABC123"은 유지된다.
     */
    @DisplayName("정상적으로 예약을 수정한다.")
    @Test
    void 정상적으로_예약을_변경() {

        // Given
        var 홍길동_예약요청 = 예약요청_생성(홍길동, 기존예약_시작일, 기존예약_종료일, 사이트번호_A_1);
        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);
        var 예약ID = 예약정보에서_예약ID_조회(홍길동_예약결과정보);
        var 확인코드 = 예약정보에서_확인코드_조회(홍길동_예약결과정보);

        // When
        var 홍길동_예약변경요청 = 예약요청_생성(홍길동, 변경예약_시작일, 변경예약_종료일, 사이트번호_A_1);
        var 예약수정정보 = 예약을_수정한다(예약ID, 확인코드, 홍길동_예약변경요청);

        // Then
        예약이_수정되었다(예약수정정보);
        예약날짜가_변경되었다(예약수정정보, 변경예약_시작일, 변경예약_종료일);
        확인코드가_유지되었다(예약수정정보, 확인코드);
    }

    /**
     * Given: A-1 사이트가 2026년 2월 1일부터 2월 3일까지 홍길동에게 예약되어 있다.
     * When: 틀린 확인 코드 "WRONG1"을 입력하고 예약 수정을 시도한다.
     * Then: 예약이 거부된다.
     */
    @DisplayName("틀린 확인코드를 입력할 경우 예약이 수정되지 않는다.")
    @Test
    void 틀린_확인코드로_예약_수정_불가() {

        // Given
        var 홍길동_예약요청 = 예약요청_생성(홍길동, 기존예약_시작일, 기존예약_종료일, 사이트번호_A_1);
        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);
        var 예약ID = 예약정보에서_예약ID_조회(홍길동_예약결과정보);

        // When
        var 홍길동_예약변경요청 = 예약요청_생성(홍길동, 변경예약_시작일, 변경예약_종료일, 사이트번호_A_1);
        var 예약수정정보 = 예약을_수정한다(예약ID, 잘못된_확인코드, 홍길동_예약변경요청);

        // Then
        예약이_수정되지않았다(예약수정정보);
    }

    /**
     * Given: 홍길동이 A-1 사이트를 2월 1일~3일로 예약했다.
     * And: 김철수가 A-1 사이트를 2월 5일~7일로 예약했다.
     * When: 홍길동이 자신의 예약을 2월 5일~7일로 변경 시도한다.
     * Then: 수정이 거부된다.
     */
    @DisplayName("이미 예약된 날짜로는 예약이 수정되지 않는다.")
    @Test
    void 이미_예약된_날짜로_예약_수정_불가() {

        // Given
        var 홍길동_예약요청 = 예약요청_생성(홍길동, 기존예약_시작일, 기존예약_종료일, 사이트번호_A_1);
        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);
        var 예약ID = 예약정보에서_예약ID_조회(홍길동_예약결과정보);
        var 확인코드 = 예약정보에서_확인코드_조회(홍길동_예약결과정보);

        var 김철수_예약요청 = 예약요청_생성(김철수, 변경예약_시작일, 변경예약_종료일, 사이트번호_A_1);
        예약을_생성한다(김철수_예약요청);

        // When
        var 홍길동_예약변경요청 = 예약요청_생성(홍길동, 변경예약_시작일, 변경예약_종료일, 사이트번호_A_1);
        var 예약수정정보 = 예약을_수정한다(예약ID, 확인코드, 홍길동_예약변경요청);

        // Then
        예약이_수정되지않았다(예약수정정보);
    }

    /**
     * Given: 홍길동이 A-1 사이트를 2026년 2월 5일~7일로 예약했다.
     * When: 홍길동이 올바른 확인 코드로 예약 취소를 요청한다.
     * Then: 예약이 취소된다.
     * And: A-1 사이트가 2월 5일~7일에 예약 가능해진다.
     * And: 예약 상태가 '사전 취소' 상태로 변경된다.
     */
    @DisplayName("정상적으로 예약을 취소한다.")
    @Test
    void 정상적으로_예약을_취소() {

        // Given
        var 홍길동_예약요청 = 예약요청_생성(홍길동, 변경예약_시작일, 변경예약_종료일, 사이트번호_A_1);
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
     * Given: 홍길동이 A-1 사이트를 2026년 2월 1일~3일로 예약했다.
     * And: 오늘 날짜는 2026년 2월 1일이다 (예약 시작일).
     * When: 홍길동이 올바른 확인 코드로 예약 취소를 요청한다.
     * Then: 예약이 취소된다.
     * And: 예약 상태가 '당일 취소' 상태로 변경된다.
     */
    @DisplayName("당일에 예약을 취소하면 당일예약취소 상태가 된다.")
    @Test
    void 당일에_예약을_취소하면_당일예약취소_상태로_변경() {
        // Given
        var 당일예약_시작일 = LocalDate.now().toString();
        var 당일예약_종료일 = LocalDate.now().plusDays(3).toString();

        var 홍길동_예약요청 = 예약요청_생성(홍길동, 당일예약_시작일, 당일예약_종료일, 사이트번호_A_1);
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
     * Given: 홍길동이 A-1 사이트를 2026년 2월 5일~7일로 예약했다.
     * When: 확인 코드 "WRONG1"을 입력하고 예약 취소를 시도한다.
     * Then: 취소가 거부된다.
     */
    @DisplayName("틀린 확인코드를 입력할 경우 예약이 취소되지 않는다.")
    @Test
    void 틀린_확인코드로_예약_취소_불가() {

        // Given
        var 홍길동_예약요청 = 예약요청_생성(홍길동, 변경예약_시작일, 변경예약_종료일, 사이트번호_A_1);
        var 홍길동_예약결과정보 = 예약을_생성한다(홍길동_예약요청);
        var 예약ID = 예약정보에서_예약ID_조회(홍길동_예약결과정보);

        // When
        var 예약취소정보 = 예약을_취소한다(예약ID, 잘못된_확인코드);

        // Then
        예약이_취소되지않았다(예약취소정보);
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
        String confirmationCode = response.jsonPath().getString("status");
        assertThat(confirmationCode).isEqualTo(예약상태_사전취소);
    }

    private void 당일예약_취소_상태이다(ExtractableResponse<Response> response) {
        String confirmationCode = response.jsonPath().getString("status");
        assertThat(confirmationCode).isEqualTo(예약상태_당일취소);
    }

    private void 예약이_취소되지않았다(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(400);
    }

}
