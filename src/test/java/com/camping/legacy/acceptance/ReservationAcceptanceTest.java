package com.camping.legacy.acceptance;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("예약 생성 인수 테스트")
class ReservationAcceptanceTest extends AcceptanceTestBase {

    @Test
    @DisplayName("고객이 30일 이내 유효한 기간으로 예약을 신청하면, 예약이 확정되고 6자리 확인코드가 발급된다")
    void scenario_예약_생성_30일이내_확정_및_확인코드_발급된다() {
        /*
         * 5W1H
         * Who(누가): 캠핑을 예약하려는 고객 "홍길동"
         * What(무엇을): 캠핑 사이트 예약
         * When(언제): 오늘로부터 5일 뒤부터 2박 3일
         * Where(어디서): 초록 캠핑장 예약 시스템
         * Why(왜): 캠핑 이용을 위해 예약을 확정하려고
         * How(어떻게): 이름/전화번호/사이트/기간을 입력해 예약을 신청한다
         */

        // Given: 시드 데이터로 등록된 사이트 중 하나가 존재하고, 고객 정보와 30일 이내의 예약 기간이 준비되어 있다
        String siteNumber = anySiteNumber();  // A001 또는 B001
        String customerName = "홍길동";
        String phoneNumber = "010-1111-2222";
        LocalDate start = LocalDate.now().plusDays(5);
        LocalDate end   = start.plusDays(2);

        // When: 고객이 유효한 정보로 예약을 신청한다
        ExtractableResponse<Response> res = createReservation(customerName, phoneNumber, siteNumber, start, end);

        // Then: 예약이 성공으로 확정되고, 6자리 확인코드가 함께 반환된다
        assertThat(res.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        Long id = res.jsonPath().getLong("id");
        String returnedSite = res.jsonPath().getString("siteNumber");
        String status = res.jsonPath().getString("status");
        String confirmationCode = res.jsonPath().getString("confirmationCode");

        assertThat(id).as("예약 식별자는 발급되어야 한다").isNotNull();
        assertThat(returnedSite).as("요청한 사이트로 예약이 확정되어야 한다").isEqualTo(siteNumber);
        assertThat(status).as("예약 상태는 확정이어야 한다").isEqualTo("CONFIRMED");
        assertThat(confirmationCode)
                .as("확정 시 6자리 확인코드가 발급되어야 한다")
                .isNotBlank()
                .hasSize(6);

        // And: 바로 조회하면 동일한 정보가 확인된다(추가 안전망)
        var getRes = getReservation(id);
        assertThat(getRes.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(getRes.jsonPath().getString("siteNumber")).isEqualTo(siteNumber);
        assertThat(getRes.jsonPath().getString("confirmationCode")).isEqualTo(confirmationCode);
    }

    @Test
    @DisplayName("종료일이 시작일보다 이전이면, 예약이 거부된다")
    void scenario_종료일이_시작일보다_이전이면_예약_거부된다() {
        /*
         * 5W1H
         * Who(누가): 캠핑을 예약하려는 고객 "김철수"
         * What(무엇을): 캠핑 사이트 예약
         * When(언제): 시작일보다 더 이른 날짜를 종료일로 잘못 입력했을 때
         * Where(어디서): 초록 캠핑장 예약 시스템
         * Why(왜): 예약 기간은 시작일 ≤ 종료일 규칙을 따라야 하기 때문
         * How(어떻게): 이름/전화번호/사이트를 입력하되, 종료일을 시작일보다 이전으로 설정해 예약을 신청한다
         */

        // Given: 시드된 사이트 중 하나와 고객 정보가 준비되어 있고, 종료일이 시작일보다 이른 비정상 기간을 만든다
        String siteNumber = anySiteNumber();
        String customerName = "김철수";
        String phoneNumber = "010-2222-3333";
        LocalDate start = LocalDate.now().plusDays(10);
        LocalDate end   = start.minusDays(1); // 종료일 < 시작일

        // When: 종료일이 시작일보다 이른 상태로 예약을 신청한다
        var res = createReservation(customerName, phoneNumber, siteNumber, start, end);

        // Then: 예약은 거부(409)되고, 사유가 메시지에 포함된다
        assertThat(res.statusCode()).isEqualTo(org.springframework.http.HttpStatus.CONFLICT.value());
        assertThat(res.jsonPath().getString("message")).contains("종료일이 시작일보다 이전");
    }

    @Test
    @DisplayName("동일 사이트의 동일 기간에 중복으로 예약을 신청하면, 두 번째 요청은 거부된다")
    void scenario_동일_사이트_동일_기간_중복예약_거부된다() {
        /*
         * 5W1H
         * Who(누가): 서로 다른 두 고객 "김철수"와 "이영희"
         * What(무엇을): 같은 사이트의 같은 기간을 예약
         * When(언제): 첫 예약이 확정된 직후에
         * Where(어디서): 초록 캠핑장 예약 시스템
         * Why(왜): 동일 사이트·동일 기간에 중복 예약을 방지해야 하므로
         * How(어떻게): 먼저 정상 예약을 생성한 뒤, 같은 기간으로 또 예약을 신청한다
         */

        // Given: 같은 사이트와 기간
        String siteNumber = anySiteNumber();
        LocalDate start = LocalDate.now().plusDays(7);
        LocalDate end   = start.plusDays(1);

        // 1차 예약(성공)
        var first = createReservation("김철수", "010-1111-2222", siteNumber, start, end);
        assertThat(first.statusCode()).isEqualTo(org.springframework.http.HttpStatus.CREATED.value());

        // When: 같은 사이트·기간으로 2차 예약을 시도한다
        var second = createReservation("이영희", "010-2222-3333", siteNumber, start, end);

        // Then: 두 번째 요청은 거부되고, 중복을 알리는 메시지가 포함된다
        assertThat(second.statusCode()).isEqualTo(org.springframework.http.HttpStatus.CONFLICT.value());
        assertThat(second.jsonPath().getString("message")).contains("이미 예약");
    }

    @Test
    @DisplayName("두 고객이 동시에 같은 사이트와 기간을 예약하면, 단 한 건만 확정된다")
    void scenario_동시에_중복_예약하면_오직_한건만_성공한다() throws Exception {
        /*
         * 5W1H
         * Who(누가): 두 고객 "김철수"와 "이영희"
         * What(무엇을): 같은 사이트의 같은 기간 예약
         * When(언제): 정확히 같은 시점에 동시에
         * Where(어디서): 초록 캠핑장 예약 시스템
         * Why(왜): 동시성 제어로 중복 예약을 방지해야 하므로
         * How(어떻게): 두 요청을 동시에 발사해 결과를 비교한다
         */

        // Given: 같은 사이트와 기간을 준비한다
        String siteNumber = anySiteNumber();
        LocalDate start = LocalDate.now().plusDays(12);
        LocalDate end   = start.plusDays(1);

        var startLatch = new java.util.concurrent.CountDownLatch(1);
        var doneLatch  = new java.util.concurrent.CountDownLatch(2);
        var results = java.util.Collections.synchronizedList(new java.util.ArrayList<io.restassured.response.ExtractableResponse<Response>>());

        // When: 두 고객이 동시에 같은 기간/사이트로 예약을 신청한다
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                startLatch.await();
                results.add(createReservation("김철수", "010-1111-1111", siteNumber, start, end));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });

        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                startLatch.await();
                results.add(createReservation("이영희", "010-2222-2222", siteNumber, start, end));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        doneLatch.await();

        // Then: 오직 한 건만 확정이고, 나머지는 중복으로 실패해야 한다
        long success = results.stream().filter(r -> r.statusCode() == org.springframework.http.HttpStatus.CREATED.value()).count();
        long conflict = results.stream().filter(r -> r.statusCode() == org.springframework.http.HttpStatus.CONFLICT.value()).count();

        assertThat(success).isEqualTo(1L);
        assertThat(conflict).isEqualTo(1L);

        // 실패 응답 메시지에 중복 안내가 포함된다
        // 꼭 필요한진 모르겠으나, 테스트 결과에 메시지가 포함되는 게 더 가독성 측면에서 좋을 것 같기도 해서 추가하였습니다.
        results.stream()
                .filter(r -> r.statusCode() == org.springframework.http.HttpStatus.CONFLICT.value())
                .findFirst()
                .ifPresent(r -> assertThat(r.jsonPath().getString("message")).contains("이미 예약"));
    }

    @Test
    @DisplayName("예약이 취소되면, 동일 사이트·기간으로 즉시 재예약할 수 있다")
    void scenario_취소_후_즉시_재예약_가능하다() {
        /*
         * 5W1H
         * Who(누가): 기존 예약자 "김철수"와 새 예약자 "이영희"
         * What(무엇을): 동일한 사이트·동일한 기간 재예약
         * When(언제): 기존 예약을 취소한 직후
         * Where(어디서): 초록 캠핑장 예약 시스템
         * Why(왜): 취소된 예약은 중복 체크에서 제외되어야 하므로
         * How(어떻게): 예약→취소→같은 조건으로 새 예약 순서로 요청한다
         */

        // Given: 같은 사이트와 기간으로 먼저 예약을 하나 만든다
        String siteNumber = anySiteNumber();
        LocalDate start = LocalDate.now().plusDays(18);
        LocalDate end   = start.plusDays(1);

        var created = createReservation("김철수", "010-1111-1111", siteNumber, start, end);
        assertThat(created.statusCode()).isEqualTo(org.springframework.http.HttpStatus.CREATED.value());

        Long reservationId = created.jsonPath().getLong("id");
        String confirmationCode = created.jsonPath().getString("confirmationCode");

        // And: 곧바로 해당 예약을 취소한다
        var cancelled = cancelReservation(reservationId, confirmationCode);
        assertThat(cancelled.statusCode()).isEqualTo(org.springframework.http.HttpStatus.OK.value());

        // When: 같은 사이트·같은 기간으로 다른 고객이 즉시 재예약을 시도한다
        var rebook = createReservation("이영희", "010-2222-2222", siteNumber, start, end);

        // Then: 재예약은 허용되어야 한다
        // 현재 구현한 방식은 상태값을 고려하지 않고 중복 체크를 하므로 CONFLICT가 나올 수 있다고 생각합니다.
        assertThat(rebook.statusCode())
                .as("취소된 예약은 중복 체크에서 제외되어, 동일 조건으로 즉시 재예약이 가능해야 한다")
                .isEqualTo(org.springframework.http.HttpStatus.CREATED.value());
    }

    @Test
    @DisplayName("과거 날짜로는 예약할 수 없다")
    void scenario_과거_날짜_예약_거부된다() {
        /*
         * 5W1H
         * Who(누가): 기존 예약자 "김철수"와 새 예약자 "이영희"
         * What(무엇을): 동일한 사이트·동일한 기간 재예약
         * When(언제): 기존 예약을 취소한 직후
         * Where(어디서): 초록 캠핑장 예약 시스템
         * Why(왜): 취소된 예약은 중복 체크에서 제외되어야 하므로
         * How(어떻게): 예약→취소→같은 조건으로 새 예약 순서로 요청한다
         */
        String siteNumber = anySiteNumber();
        var start = LocalDate.now().minusDays(1);
        var end   = start.plusDays(1);

        var res = createReservation("홍길동", "010-9999-0000", siteNumber, start, end);

        // 기대: 거부 + "과거 날짜" 문구 (현 구현상 CREATED일 수 있어 Red가 나올 수 있다고 생각합니다.)
        assertThat(res.statusCode()).isIn(HttpStatus.BAD_REQUEST.value(), HttpStatus.CONFLICT.value());
        assertThat(res.jsonPath().getString("message")).contains("과거 날짜");
    }

    @Test
    @DisplayName("30일 제한을 초과하면 예약이 거부된다")
    void scenario_30일_제한_초과_예약_거부된다() {
        /*
         * 5W1H
         * Who(누가): 고객 "김철수"
         * What(무엇을): 정책 한도를 초과한 날짜로 예약 시도
         * When(언제): 오늘로부터 35일 뒤 시작 1박 2일
         * Where(어디서): 초록 캠핑장 예약 시스템
         * Why(왜): "오늘부터 30일 이내만 예약 가능" 정책 준수 여부를 검증하기 위해
         * How(어떻게): start/end를 30일 초과 시점으로 설정해 /api/reservations 예약 생성 요청을 보낸다
         */
        String siteNumber = anySiteNumber();
        var start = LocalDate.now().plusDays(35);
        var end   = start.plusDays(1);

        var res = createReservation("김철수", "010-1111-0000", siteNumber, start, end);

        // 기대: 거부 + "30일 이내" 문구  (현 구현상 CREATED일 수 있어 Red가 나올 수 있다고 생각합니다.)
        assertThat(res.statusCode()).isIn(HttpStatus.BAD_REQUEST.value(), HttpStatus.CONFLICT.value());
        assertThat(res.jsonPath().getString("message")).contains("30일");
    }


    @Test
    @DisplayName("전화번호가 없으면 예약할 수 없다")
    void scenario_전화번호_누락_예약_거부된다() {
        /*
         * 5W1H
         * Who(누가): 고객 "이영희"
         * What(무엇을): 전화번호 없이 예약 시도
         * When(언제): 정상 예약 가능 기간에
         * Where(어디서): 초록 캠핑장 예약 시스템
         * Why(왜): 전화번호가 필수 입력임을 검증하기 위해
         * How(어떻게): 요청 바디에 phoneNumber를 누락한 채 /api/reservations 예약 생성 요청을 보낸다
         */
        String siteNumber = anySiteNumber();
        var start = LocalDate.now().plusDays(3);
        var end   = start.plusDays(1);

        var body = java.util.Map.of(
                "customerName", "이영희",
                "siteNumber", siteNumber,
                "startDate", start.toString(),
                "endDate", end.toString()
                // phoneNumber 누락
        );

        var res = io.restassured.RestAssured
                .given().log().all()
                .contentType("application/json")
                .body(body)
                .when().post("/api/reservations")
                .then().log().all()
                .extract();

        // 기대: 거부 + "전화번호" 문구  (현 구현상 CREATED일 수 있어 Red가 나올 수 있다고 생각합니다.)
        assertThat(res.statusCode()).isIn(HttpStatus.BAD_REQUEST.value(), HttpStatus.CONFLICT.value());
        assertThat(res.jsonPath().getString("message")).contains("전화번호");
    }

}
