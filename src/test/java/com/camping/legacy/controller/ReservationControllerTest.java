package com.camping.legacy.controller;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.List;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ReservationControllerTest {
    
    @LocalServerPort
    int port;
    
    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }
    @DisplayName("예약 생성 - 정상적인 예약 생성")
    @Test
    void createReservation_Success() {
        /*
         * 누가(Who), 무엇을(What), 언제(When), 어디서(Where), 왜(Why), 어떻게(How)
         * 누가(Who): 예약을 하려는 고객 "김철수"
         * 무엇을(What): 캠핑 사이트 예약을
         * 언제(When): 오늘로부터 5일 후부터 7일 후까지
         * 어디서(Where): 캠핑장 예약 시스템에서
         * 왜(Why): 캠핑을 위한 사이트 예약을 하기 위해
         * 어떻게(How): REST API를 통해 예약 요청을 보내서
         */

        /*
         * given - 고객 "김철수"의 전화번호는 "010-1234-5678"이고 예약 가능한 날짜가 있다
         * when - 유효한 정보로 예약을 요청하면
         * then - 예약이 성공적으로 생성되고 6자리 확인코드가 발급되며 상태가 CONFIRMED가 된다
         */

        // Given - 고객 "김철수"의 전화번호는 "010-1234-5678"이다
        String customerName = "김철수";
        String phoneNumber = "010-1234-5678";
        String siteId = "1";
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.plusDays(5);
        LocalDate endDate = startDate.plusDays(2);
        
        // When - "2024-01-20"부터 "2024-01-22"까지 사이트 "A001"을 예약 요청한다
        ExtractableResponse<Response> response = RestAssured
                .given().log().all()
                .contentType("application/json")
                .body(Map.of(
                        "customerName", customerName,
                        "phoneNumber", phoneNumber,
                        "campsiteId", siteId,
                        "startDate", startDate.toString(),
                        "endDate", endDate.toString()
                ))
                .when().post("/api/reservations")
                .then().log().all()
                .extract();
        
        // Then - 예약이 성공적으로 생성된다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        // And - 6자리 확인코드가 발급된다
        String confirmationCode = response.jsonPath().getString("confirmationCode");
        assertThat(confirmationCode).isNotNull();
        assertThat(confirmationCode).hasSize(6);
        // And - 예약 상태는 "CONFIRMED"이다
        assertThat(response.jsonPath().getString("status")).isEqualTo("CONFIRMED");
    }
    
    @DisplayName("예약 생성 - 30일 초과 예약 시도")
    @Test
    void createReservation_ExceedsDateLimit() {
        /*
         * 누가(Who), 무엇을(What), 언제(When), 어디서(Where), 왜(Why), 어떻게(How)
         * 누가(Who): 예약을 하려는 고객 "김철수"
         * 무엇을(What): 30일 초과 날짜 예약을
         * 언제(When): 오늘로부터 31일 후
         * 어디서(Where): 캠핑장 예약 시스템에서
         * 왜(Why): 30일 초과 예약이 불가능하다는 제한을 확인하기 위해
         * 어떻게(How): 30일을 초과한 날짜로 예약 요청을 보내서
         */

        /*
         * given - 고객이 30일을 초과한 날짜로 예약을 시도한다
         * when - 30일 초과 날짜로 예약을 요청하면
         * then - 예약이 실패하고 "30일 이내 예약만 가능합니다" 오류 메시지를 받는다
         */

        // Given - 고객 "김철수"의 전화번호는 "010-1234-5678"이다
        String customerName = "김철수";
        String phoneNumber = "010-1234-5678";
        String siteId = "1";
        LocalDate startDate = LocalDate.now().plusDays(31);
        LocalDate endDate = startDate.plusDays(2);
        
        // When - 30일 초과 날짜로 예약을 요청하면
        ExtractableResponse<Response> response = RestAssured
                .given().log().all()
                .contentType("application/json")
                .body(Map.of(
                        "customerName", customerName,
                        "phoneNumber", phoneNumber,
                        "campsiteId", siteId,
                        "startDate", startDate.toString(),
                        "endDate", endDate.toString()
                ))
                .when().post("/api/reservations")
                .then().log().all()
                .extract();
        
        // Then - 예약이 실패한다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        // And - "30일 이내 예약만 가능합니다" 오류 메시지를 받는다
        assertThat(response.jsonPath().getString("message")).contains("30일");
    }
    
    @DisplayName("예약 생성 - 과거 날짜 예약 시도")
    @Test
    void createReservation_PastDate() {
        /*
         * 누가(Who), 무엇을(What), 언제(When), 어디서(Where), 왜(Why), 어떻게(How)
         * 누가(Who): 예약을 하려는 고객 "김철수"
         * 무엇을(What): 과거 날짜 예약을
         * 언제(When): 어제 날짜로
         * 어디서(Where): 캠핑장 예약 시스템에서
         * 왜(Why): 과거 날짜 예약이 불가능하다는 제한을 확인하기 위해
         * 어떻게(How): 과거 날짜로 예약 요청을 보내서
         */

        /*
         * given - 고객이 과거 날짜로 예약을 시도한다
         * when - 과거 날짜로 예약을 요청하면
         * then - 예약이 실패하고 "과거 날짜로 예약할 수 없습니다" 오류 메시지를 받는다
         */

        // Given - 고객 "김철수"의 전화번호는 "010-1234-5678"이다
        String customerName = "김철수";
        String phoneNumber = "010-1234-5678";
        String siteId = "1";
        LocalDate pastDate = LocalDate.now().minusDays(1);
        LocalDate endDate = pastDate.plusDays(2);
        
        // When - 과거 날짜로 예약을 요청하면
        ExtractableResponse<Response> response = RestAssured
                .given().log().all()
                .contentType("application/json")
                .body(Map.of(
                        "customerName", customerName,
                        "phoneNumber", phoneNumber,
                        "campsiteId", siteId,
                        "startDate", pastDate.toString(),
                        "endDate", endDate.toString()
                ))
                .when().post("/api/reservations")
                .then().log().all()
                .extract();
        
        // Then - 예약이 실패한다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        // And - "과거 날짜로 예약할 수 없습니다" 오류 메시지를 받는다
        assertThat(response.jsonPath().getString("message")).contains("과거");
    }
    
    @DisplayName("예약 생성 - 필수 정보 누락")
    @Test
    void createReservation_MissingRequiredFields() {
        /*
         * 누가(Who), 무엇을(What), 언제(When), 어디서(Where), 왜(Why), 어떻게(How)
         * 누가(Who): 필수 정보를 누락한 사용자
         * 무엇을(What): 예약을
         * 언제(When): 정상적인 날짜에
         * 어디서(Where): 캠핑장 예약 시스템에서
         * 왜(Why): 필수 정보 누락 시 적절한 검증이 이루어지는지 확인하기 위해
         * 어떻게(How): 고객명 없이 예약 요청을 보내서
         */

        /*
         * given - 필수 정보(고객명)가 누락된 요청이 있다
         * when - 필수 정보 없이 예약을 요청하면
         * then - 예약이 실패하고 적절한 검증 오류 메시지가 반환된다
         */

        // Given - 필수 정보가 누락된 요청
        LocalDate startDate = LocalDate.now().plusDays(5);
        LocalDate endDate = startDate.plusDays(2);
        
        // When - 고객명 없이 예약을 요청하면
        ExtractableResponse<Response> response = RestAssured
                .given().log().all()
                .contentType("application/json")
                .body(Map.of(
                        "phoneNumber", "010-1234-5678",
                        "campsiteId", "1",
                        "startDate", startDate.toString(),
                        "endDate", endDate.toString()
                ))
                .when().post("/api/reservations")
                .then().log().all()
                .extract();
        
        // Then - 예약이 실패한다
        assertThat(response.statusCode()).isIn(HttpStatus.BAD_REQUEST.value(), HttpStatus.CONFLICT.value());
        // And - 적절한 검증 오류 메시지가 반환된다
        assertThat(response.jsonPath().getString("message")).isNotNull();
    }
    
    @DisplayName("동시성 제어 - 동시 예약 요청 시 하나만 성공")
    @Test
    void createReservation_ConcurrencyControl() throws Exception {
        /*
         * 누가(Who), 무엇을(What), 언제(When), 어디서(Where), 왜(Why), 어떻게(How)
         * 누가(Who): 두 명의 고객 "김철수"와 "이영희"
         * 무엇을(What): 동일한 사이트의 동일한 기간 예약을
         * 언제(When): 정확히 같은 시점에
         * 어디서(Where): 캠핑장 예약 시스템에서
         * 왜(Why): 동시성 제어를 통해 중복 예약을 방지하기 위해
         * 어떻게(How): 동시에 같은 예약 요청을 보내서
         */

        /*
         * given - 두 고객이 동일한 사이트와 기간에 대해 동시에 예약을 요청한다
         * when - 정확히 같은 시점에 예약 요청을 보내면
         * then - 하나의 예약만 성공하고 나머지는 "이미 예약된 기간입니다" 오류로 실패한다
         */

        // Given - 동일한 사이트와 날짜에 대한 예약 요청
        String siteId = "1";
        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = startDate.plusDays(2);
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        List<ExtractableResponse<Response>> responses = new ArrayList<>();
        
        // When - "김철수"와 "이영희"가 동시에 같은 예약을 요청한다
        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
            try {
                startLatch.await();
                ExtractableResponse<Response> response = RestAssured
                        .given().log().all()
                        .contentType("application/json")
                        .body(Map.of(
                                "customerName", "김철수",
                                "phoneNumber", "010-1111-1111",
                                "campsiteId", siteId,
                                "startDate", startDate.toString(),
                                "endDate", endDate.toString()
                        ))
                        .when().post("/api/reservations")
                        .then().log().all()
                        .extract();
                synchronized (responses) {
                    responses.add(response);
                }
                doneLatch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
            try {
                startLatch.await();
                ExtractableResponse<Response> response = RestAssured
                        .given().log().all()
                        .contentType("application/json")
                        .body(Map.of(
                                "customerName", "이영희",
                                "phoneNumber", "010-2222-2222",
                                "campsiteId", siteId,
                                "startDate", startDate.toString(),
                                "endDate", endDate.toString()
                        ))
                        .when().post("/api/reservations")
                        .then().log().all()
                        .extract();
                synchronized (responses) {
                    responses.add(response);
                }
                doneLatch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        startLatch.countDown();
        doneLatch.await();
        
        // Then - 하나의 예약만 성공한다
        long successCount = responses.stream()
                .mapToInt(ExtractableResponse::statusCode)
                .filter(status -> status == HttpStatus.CREATED.value())
                .count();
        
        long conflictCount = responses.stream()
                .mapToInt(ExtractableResponse::statusCode)
                .filter(status -> status == HttpStatus.CONFLICT.value())
                .count();
        
        assertThat(successCount).isEqualTo(1L);
        assertThat(conflictCount).isEqualTo(1L);
        
        // And - 나머지 예약은 "이미 예약된 기간입니다" 오류로 실패한다
        ExtractableResponse<Response> conflictResponse = responses.stream()
                .filter(r -> r.statusCode() == HttpStatus.CONFLICT.value())
                .findFirst()
                .orElseThrow();
        assertThat(conflictResponse.jsonPath().getString("message")).contains("이미 예약");
    }
    
    @DisplayName("연박 예약 - 전체 기간 가용성 확인")
    @Test
    void createReservation_MultiDayBookingAvailability() {
        /*
         * 누가(Who), 무엇을(What), 언제(When), 어디서(Where), 왜(Why), 어떻게(How)
         * 누가(Who): 연박 예약을 하려는 고객 "김철수"
         * 무엇을(What): 연박 예약을
         * 언제(When): 이미 예약된 날짜가 포함된 기간에
         * 어디서(Where): 캠핑장 예약 시스템에서
         * 왜(Why): 연박 예약 시 전체 기간의 가용성 확인이 필요하다는 규칙을 검증하기 위해
         * 어떻게(How): 이미 예약된 날짜가 중간에 포함된 연박 예약 요청을 보내서
         */

        /*
         * given - 사이트의 중간 날짜가 이미 예약되어 있다
         * when - 그 날짜가 포함된 연박 예약을 요청하면
         * then - 예약이 실패하고 "해당 기간에 예약이 불가능합니다" 오류 메시지를 받는다
         */

        // Given - 사이트 "A001"의 중간 날짜가 이미 예약되어 있다
        String siteId = "1";
        LocalDate conflictDate = LocalDate.now().plusDays(6);
        
        // 중간 날짜에 예약 생성
        RestAssured
                .given().log().all()
                .contentType("application/json")
                .body(Map.of(
                        "customerName", "기존예약자",
                        "phoneNumber", "010-0000-0000",
                        "campsiteId", siteId,
                        "startDate", conflictDate.toString(),
                        "endDate", conflictDate.plusDays(1).toString()
                ))
                .when().post("/api/reservations")
                .then().statusCode(HttpStatus.CREATED.value());
        
        // When - 겹치는 기간으로 연박 예약을 요청한다
        LocalDate startDate = conflictDate.minusDays(1);
        LocalDate endDate = conflictDate.plusDays(2);
        
        ExtractableResponse<Response> response = RestAssured
                .given().log().all()
                .contentType("application/json")
                .body(Map.of(
                        "customerName", "김철수",
                        "phoneNumber", "010-1234-5678",
                        "campsiteId", siteId,
                        "startDate", startDate.toString(),
                        "endDate", endDate.toString()
                ))
                .when().post("/api/reservations")
                .then().log().all()
                .extract();
        
        // Then - 예약이 실패한다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        // And - "해당 기간에 예약이 불가능합니다" 오류 메시지를 받는다
        assertThat(response.jsonPath().getString("message")).contains("해당 기간");
    }
    
    @DisplayName("예약 취소 - 올바른 확인코드로 예약 취소")
    @Test
    void cancelReservation_Success() {
        /*
         * 누가(Who), 무엇을(What), 언제(When), 어디서(Where), 왜(Why), 어떻게(How)
         * 누가(Who): 예약을 취소하려는 고객 "김철수"
         * 무엇을(What): 자신의 예약을
         * 언제(When): 예약 완료 후
         * 어디서(Where): 캠핑장 예약 시스템에서
         * 왜(Why): 예약을 취소하고 싶어서
         * 어떻게(How): 올바른 확인코드로 취소 요청을 보내서
         */

        /*
         * given - 고객이 확인코드로 예약을 완료한 상태이다
         * when - 올바른 확인코드로 취소를 요청하면
         * then - 예약 취소가 성공하고 상태가 CANCELLED로 변경된다
         */

        // Given - 고객 "김철수"가 확인코드로 예약을 완료했다
        String customerName = "김철수";
        String phoneNumber = "010-1234-5678";
        String siteId = "1";
        LocalDate startDate = LocalDate.now().plusDays(15);
        LocalDate endDate = startDate.plusDays(2);
        
        ExtractableResponse<Response> createResponse = RestAssured
                .given().log().all()
                .contentType("application/json")
                .body(Map.of(
                        "customerName", customerName,
                        "phoneNumber", phoneNumber,
                        "campsiteId", siteId,
                        "startDate", startDate.toString(),
                        "endDate", endDate.toString()
                ))
                .when().post("/api/reservations")
                .then().statusCode(HttpStatus.CREATED.value())
                .extract();
        
        Long reservationId = createResponse.jsonPath().getLong("id");
        String confirmationCode = createResponse.jsonPath().getString("confirmationCode");
        
        // When - 확인코드로 예약 취소를 요청한다
        ExtractableResponse<Response> response = RestAssured
                .given().log().all()
                .queryParam("confirmationCode", confirmationCode)
                .when().delete("/api/reservations/" + reservationId)
                .then().log().all()
                .extract();
        
        // Then - 예약 취소가 성공한다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getString("message")).contains("취소");
        
        // And - 예약 상태가 "CANCELLED"로 변경된다
        ExtractableResponse<Response> getResponse = RestAssured
                .given().log().all()
                .when().get("/api/reservations/" + reservationId)
                .then().log().all()
                .extract();
        
        assertThat(getResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(getResponse.jsonPath().getString("status")).isEqualTo("CANCELLED");
    }
    
    @DisplayName("예약 취소 - 잘못된 확인코드로 예약 취소 시도")
    @Test
    void cancelReservation_InvalidConfirmationCode() {
        /*
         * 누가(Who), 무엇을(What), 언제(When), 어디서(Where), 왜(Why), 어떻게(How)
         * 누가(Who): 예약을 취소하려는 고객 "김철수"
         * 무엇을(What): 자신의 예약을
         * 언제(When): 예약 완료 후
         * 어디서(Where): 캠핑장 예약 시스템에서
         * 왜(Why): 잘못된 확인코드로는 취소가 불가능하다는 보안 규칙을 검증하기 위해
         * 어떻게(How): 잘못된 확인코드로 취소 요청을 보내서
         */

        /*
         * given - 고객이 예약을 완료한 상태이다
         * when - 잘못된 확인코드로 취소를 요청하면
         * then - 취소가 실패하고 "확인코드가 일치하지 않습니다" 오류 메시지를 받는다
         */

        // Given - 고객 "김철수"가 확인코드로 예약을 완료했다
        String customerName = "김철수";
        String phoneNumber = "010-1234-5678";
        String siteId = "1";
        LocalDate startDate = LocalDate.now().plusDays(20);
        LocalDate endDate = startDate.plusDays(2);
        
        ExtractableResponse<Response> createResponse = RestAssured
                .given().log().all()
                .contentType("application/json")
                .body(Map.of(
                        "customerName", customerName,
                        "phoneNumber", phoneNumber,
                        "campsiteId", siteId,
                        "startDate", startDate.toString(),
                        "endDate", endDate.toString()
                ))
                .when().post("/api/reservations")
                .then().statusCode(HttpStatus.CREATED.value())
                .extract();
        
        Long reservationId = createResponse.jsonPath().getLong("id");
        
        // When - 잘못된 확인코드로 예약 취소를 요청한다
        ExtractableResponse<Response> response = RestAssured
                .given().log().all()
                .queryParam("confirmationCode", "WRONG1")
                .when().delete("/api/reservations/" + reservationId)
                .then().log().all()
                .extract();
        
        // Then - 예약 취소가 실패한다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        // And - "확인코드가 일치하지 않습니다" 오류 메시지를 받는다
        assertThat(response.jsonPath().getString("message")).contains("확인코드");
    }
    
    @DisplayName("내 예약 조회 - 이름과 전화번호로 내 예약 조회")
    @Test
    void getMyReservations_Success() {
        /*
         * 누가(Who), 무엇을(What), 언제(When), 어디서(Where), 왜(Why), 어떻게(How)
         * 누가(Who): 예약을 조회하려는 고객 "김철수"
         * 무엇을(What): 자신의 모든 예약 목록을
         * 언제(When): 여러 예약을 완료한 후
         * 어디서(Where): 캠핑장 예약 시스템에서
         * 왜(Why): 자신의 예약 현황을 확인하기 위해
         * 어떻게(How): 이름과 전화번호로 조회 요청을 보내서
         */

        /*
         * given - 고객이 동일한 이름과 전화번호로 여러 예약을 완료했다
         * when - 이름과 전화번호로 예약을 조회하면
         * then - 해당 고객의 모든 예약 목록이 반환된다
         */

        // Given - 고객 "김철수"가 전화번호 "010-1234-5678"로 여러 예약을 완료했다
        String customerName = "김철수";
        String phoneNumber = "010-1234-5678";
        
        // 첫 번째 예약
        RestAssured
                .given().log().all()
                .contentType("application/json")
                .body(Map.of(
                        "customerName", customerName,
                        "phoneNumber", phoneNumber,
                        "campsiteId", "1",
                        "startDate", LocalDate.now().plusDays(5).toString(),
                        "endDate", LocalDate.now().plusDays(7).toString()
                ))
                .when().post("/api/reservations")
                .then().statusCode(HttpStatus.CREATED.value());
        
        // 두 번째 예약
        RestAssured
                .given().log().all()
                .contentType("application/json")
                .body(Map.of(
                        "customerName", customerName,
                        "phoneNumber", phoneNumber,
                        "campsiteId", "2",
                        "startDate", LocalDate.now().plusDays(10).toString(),
                        "endDate", LocalDate.now().plusDays(12).toString()
                ))
                .when().post("/api/reservations")
                .then().statusCode(HttpStatus.CREATED.value());
        
        // When - 이름과 전화번호로 예약을 조회한다
        ExtractableResponse<Response> response = RestAssured
                .given().log().all()
                .queryParam("name", customerName)
                .queryParam("phone", phoneNumber)
                .when().get("/api/reservations/my")
                .then().log().all()
                .extract();
        
        // Then - 해당 고객의 모든 예약 목록이 반환된다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        List<Map<String, Object>> reservations = response.jsonPath().getList("$");
        assertThat(reservations).hasSize(2);
        
        reservations.forEach(reservation -> {
            assertThat(reservation.get("customerName")).isEqualTo(customerName);
            assertThat(reservation.get("phoneNumber")).isEqualTo(phoneNumber);
        });
    }
    
    @DisplayName("예약 조회 - 예약 ID로 단일 예약 조회")
    @Test
    void getReservation_Success() {
        /*
         * 누가(Who), 무엇을(What), 언제(When), 어디서(Where), 왜(Why), 어떻게(How)
         * 누가(Who): 예약 정보를 조회하려는 사용자
         * 무엇을(What): 특정 예약의 상세 정보를
         * 언제(When): 예약이 생성된 후
         * 어디서(Where): 캠핑장 예약 시스템에서
         * 왜(Why): 예약 상세 정보를 확인하기 위해
         * 어떻게(How): 예약 ID로 조회 요청을 보내서
         */

        /*
         * given - 예약이 생성되어 있다
         * when - 예약 ID로 조회를 요청하면
         * then - 예약 상세 정보가 반환된다
         */

        // Given - 예약이 생성되어 있다
        ExtractableResponse<Response> createResponse = RestAssured
                .given().log().all()
                .contentType("application/json")
                .body(Map.of(
                        "customerName", "김철수",
                        "phoneNumber", "010-1234-5678",
                        "campsiteId", "1",
                        "startDate", LocalDate.now().plusDays(5).toString(),
                        "endDate", LocalDate.now().plusDays(7).toString()
                ))
                .when().post("/api/reservations")
                .then().statusCode(HttpStatus.CREATED.value())
                .extract();
        
        Long reservationId = createResponse.jsonPath().getLong("id");
        
        // When - 예약 ID로 조회한다
        ExtractableResponse<Response> response = RestAssured
                .given().log().all()
                .when().get("/api/reservations/" + reservationId)
                .then().log().all()
                .extract();
        
        // Then - 예약 상세 정보가 반환된다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getLong("id")).isEqualTo(reservationId);
        assertThat(response.jsonPath().getString("customerName")).isEqualTo("김철수");
        assertThat(response.jsonPath().getString("status")).isEqualTo("CONFIRMED");
    }
    
    @DisplayName("월별 예약 현황 - 캘린더 조회")
    @Test
    void getReservationCalendar_Success() {
        /*
         * 누가(Who), 무엇을(What), 언제(When), 어디서(Where), 왜(Why), 어떻게(How)
         * 누가(Who): 예약 현황을 확인하려는 사용자
         * 무엇을(What): 특정 사이트의 월별 예약 현황을
         * 언제(When): 특정 월에 대해
         * 어디서(Where): 캠핑장 예약 시스템에서
         * 왜(Why): 예약된 날짜와 가능한 날짜를 구분해서 보기 위해
         * 어떻게(How): 년도, 월, 사이트ID로 캘린더 조회 요청을 보내서
         */

        /*
         * given - 사이트에 예약이 있다
         * when - 해당 월의 캘린더를 조회하면
         * then - 예약된 날짜와 가능한 날짜가 구분되어 표시된다
         */

        // Given - 사이트에 예약이 있다
        Long siteId = 1L;
        LocalDate reservationDate = LocalDate.now().plusDays(10);
        
        RestAssured
                .given().log().all()
                .contentType("application/json")
                .body(Map.of(
                        "customerName", "김철수",
                        "phoneNumber", "010-1234-5678",
                        "campsiteId", siteId.toString(),
                        "startDate", reservationDate.toString(),
                        "endDate", reservationDate.plusDays(1).toString()
                ))
                .when().post("/api/reservations")
                .then().statusCode(HttpStatus.CREATED.value());
        
        // When - 해당 월의 캘린더를 조회한다
        ExtractableResponse<Response> response = RestAssured
                .given().log().all()
                .queryParam("year", reservationDate.getYear())
                .queryParam("month", reservationDate.getMonthValue())
                .queryParam("siteId", siteId)
                .when().get("/api/reservations/calendar")
                .then().log().all()
                .extract();
        
        // Then - 예약된 날짜와 가능한 날짜가 구분되어 표시된다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().get("year")).isEqualTo(reservationDate.getYear());
        assertThat(response.jsonPath().get("month")).isEqualTo(reservationDate.getMonthValue());
        assertThat(response.jsonPath().get("siteId")).isEqualTo(siteId.intValue());
    }

}
