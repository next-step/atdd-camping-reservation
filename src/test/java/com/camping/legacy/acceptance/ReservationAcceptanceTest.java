package com.camping.legacy.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.camping.legacy.dto.ReservationRequest;
import com.camping.legacy.dto.ReservationResponse;
import com.camping.legacy.test_config.TestConfig;
import com.camping.legacy.test_utils.CleanUp;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Sql("/data.sql")
@ActiveProfiles("test")
@Import(TestConfig.class)
@DisplayNameGeneration(ReplaceUnderscores.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReservationAcceptanceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CleanUp cleanUp;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @AfterEach
    void tearDown() {
        cleanUp.all();
    }

    /**
     * When 사용자가 이름, 전화번호로 사이트 'A-1'에 '2025-09-10' 날짜로 예약을 요청하면
     * Then 예약은 성공적으로 처리된다.
     * And 응답에는 6자리의 영숫자 확인 코드가 포함되어야 한다.
     */
    @Test
    void 사용자가_유효한_정보로_예약을_성공적으로_생성한다() throws Exception {
        // when
        ReservationRequest request = ReservationRequest.builder()
                .customerName("김철수")
                .phoneNumber("010-1234-5678")
                .startDate(LocalDate.of(2025, 9, 10))
                .endDate(LocalDate.of(2025, 9, 11))
                .siteNumber("A-1")
                .numberOfPeople(2)
                .build();

        String responseBody = RestAssured
                .given()
                .log().all()
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .when()
                .post("/api/reservations")
                .then()
                .log().all()
                .statusCode(HttpStatus.CREATED.value())
                .extract().asString();

        // then
        ReservationResponse response = objectMapper.readValue(responseBody, ReservationResponse.class);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.getConfirmationCode()).hasSize(6);
            softly.assertThat(response.getConfirmationCode()).isNotNull();
        });
    }

    /**
     * Given 2025-09-10일에 대형 사이트 'A-1'에 이미 예약이 존재할 때
     * When 다른 사용자가 사이트 'A-1'에 대해 2025-09-10일을 포함하는 기간으로 예약을 요청하면
     * Then "이미 예약된 날짜입니다."와 같은 메시지와 함께 예약이 실패한다.
     */
    @Test
    void 이미_예약된_날짜에_중복으로_예약을_시도하면_실패한다() throws Exception {
        // Given: 2025-09-10일에 대형 사이트 'A-1'에 이미 예약이 존재할 때
        ReservationRequest initialRequest = ReservationRequest.builder()
                .customerName("홍길동")
                .phoneNumber("010-1111-2222")
                .startDate(LocalDate.of(2025, 9, 10))
                .endDate(LocalDate.of(2025, 9, 11))
                .siteNumber("A-1")
                .numberOfPeople(2)
                .build();

        // todo : 테스트 헬퍼 메서드 같은 걸로 분리해보기
        RestAssured
                .given()
                    .contentType(ContentType.JSON)
                    .body(objectMapper.writeValueAsString(initialRequest))
                .when()
                    .post("/api/reservations")
                .then()
                    .statusCode(HttpStatus.CREATED.value());

        // When: 다른 사용자가 사이트 'A-1'에 대해 2025-09-10일을 포함하는 기간으로 예약을 요청하면
        ReservationRequest conflictingRequest = ReservationRequest.builder()
                .customerName("김중복")
                .phoneNumber("010-3333-4444")
                .startDate(LocalDate.of(2025, 9, 10)) // 중복되는 날짜
                .endDate(LocalDate.of(2025, 9, 11))
                .siteNumber("A-1")
                .numberOfPeople(3)
                .build();

        String errorMessage = RestAssured
                .given()
                    .log().all()
                    .contentType(ContentType.JSON)
                    .body(objectMapper.writeValueAsString(conflictingRequest))
                .when()
                    .post("/api/reservations")
                .then()
                    .log().all()
                    .statusCode(HttpStatus.CONFLICT.value())
                    .extract().path("message");

        // Then: "해당 기간에 이미 예약이 존재합니다."와 같은 메시지와 함께 예약이 실패한다.
        assertThat(errorMessage).contains("해당 기간에 이미 예약이 존재합니다.");
    }

    /**
     * Scenario: 과거 날짜로 예약을 시도하면 실패한다.
     * When 사용자가 예약할 때, 시작 날짜를 오늘보다 과거로 지정하면
     * Then "유효하지 않은 날짜입니다."와 같은 메시지와 함께 예약은 실패한다.
     */
    @Test
    void 과거_날짜로_예약을_시도하면_실패한다() throws Exception {
        // When: 사용자가 예약할 때, 시작 날짜를 오늘보다 과거로 지정하면
        ReservationRequest request = ReservationRequest.builder()
                .customerName("김시간")
                .phoneNumber("010-9876-5432")
                .startDate(LocalDate.now().minusDays(1)) // 과거 날짜
                .endDate(LocalDate.now().plusDays(1))
                .siteNumber("A-1")
                .numberOfPeople(2)
                .build();

        String errorMessage = RestAssured
                .given()
                    .log().all()
                    .contentType(ContentType.JSON)
                    .body(objectMapper.writeValueAsString(request))
                .when()
                    .post("/api/reservations")
                .then()
                    .log().all()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                .extract().path("message");

        // Then: "유효하지 않은 날짜입니다."와 같은 메시지와 함께 예약은 실패한다.
        assertThat(errorMessage).contains("유효하지 않은 날짜입니다.");
    }


    /**
     * Scenario: 유효하지 않은 기간으로 예약을 시도하면 실패한다.
     * When 사용자가 예약할 때, 종료 날짜를 시작 날짜보다 이전으로 지정하면
     * Then "종료일이 시작일보다 이전일 수 없습니다."와 같은 메시지와 함께 예약이 실패한다.
     */
    @Test
    void 유효하지_않은_기간으로_예약을_시도하면_실패한다() throws Exception {
        // When: 사용자가 예약할 때, 종료 날짜를 시작 날짜보다 이전으로 지정하면
        ReservationRequest request = ReservationRequest.builder()
                .customerName("김기간")
                .phoneNumber("010-1234-5678")
                .startDate(LocalDate.of(2025, 9, 11)) // 시작일
                .endDate(LocalDate.of(2025, 9, 10))   // 종료일 (시작일보다 빠름)
                .siteNumber("A-1")
                .numberOfPeople(2)
                .build();

        String errorMessage = RestAssured
                .given()
                    .log().all()
                    .contentType(ContentType.JSON)
                    .body(objectMapper.writeValueAsString(request))
                .when()
                    .post("/api/reservations")
                .then()
                    .log().all()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                .extract().path("message");

        // Then: "종료일이 시작일보다 이전일 수 없습니다."와 같은 메시지와 함께 예약이 실패한다.
        assertThat(errorMessage).contains("종료일이 시작일보다 이전일 수 없습니다.");
    }

    /**
     * Scenario: 30일 이후 기간으로 예약을 시도하면 실패한다.
     * When 사용자가 예약할 때, 예약 기간에 30일 이후 날짜가 포함되면
     * Then "30일 이내 기간으로만 예약이 가능합니다."와 같은 메시지와 함께 예약이 실패한다.
     */
    @Test
    void 삼십일_이후_기간으로_예약을_시도하면_실패한다() throws Exception {
        // When: 사용자가 예약할 때, 예약 기간에 30일 이후 날짜가 포함되면
        ReservationRequest request = ReservationRequest.builder()
                .customerName("김미래")
                .phoneNumber("010-5555-6666")
                .startDate(LocalDate.now().plusDays(30))
                .endDate(LocalDate.now().plusDays(31))
                .siteNumber("A-1")
                .numberOfPeople(2)
                .build();

        String errorMessage = RestAssured
                .given()
                    .log().all()
                    .contentType(ContentType.JSON)
                    .body(objectMapper.writeValueAsString(request))
                .when()
                    .post("/api/reservations")
                .then()
                    .log().all()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .extract().path("message");

        // Then: "30일 이내 기간으로만 예약이 가능합니다."와 같은 메시지와 함께 예약이 실패한다.
        assertThat(errorMessage).contains("30일 이내 기간으로만 예약이 가능합니다.");
    }

    /**
     * Scenario: 취소된 예약 날짜에 새로운 예약을 성공적으로 생성한다.
     * Given 'A-1' 사이트에 특정 날짜로 예약이 생성되었다가 취소되었을 때
     * When 다른 사용자가 동일한 사이트와 날짜로 새로운 예약을 요청하면
     * Then 예약은 성공적으로 처리된다.
     * And 'A-1' 사이트의 취소된 예약 날짜에 새로운 사용자 정보로 예약이 생긴다.
     */
    @Test
    void 취소된_예약_날짜에_새로운_예약을_성공적으로_생성한다() throws Exception {
        // Given: 'A-1' 사이트에 특정 날짜로 예약이 생성되었다가 취소되었을 때
        // 1. 초기 예약 생성
        ReservationRequest initialRequest = ReservationRequest.builder()
                .customerName("김초기")
                .phoneNumber("010-0000-0000")
                .startDate(LocalDate.of(2025, 10, 1))
                .endDate(LocalDate.of(2025, 10, 2))
                .siteNumber("A-1")
                .numberOfPeople(2)
                .build();

        String initialResponseBody = RestAssured
                .given()
                    .contentType(ContentType.JSON)
                    .body(objectMapper.writeValueAsString(initialRequest))
                .when()
                    .post("/api/reservations")
                .then()
                    .statusCode(HttpStatus.CREATED.value())
                    .extract().asString();

        ReservationResponse initialResponse = objectMapper.readValue(initialResponseBody, ReservationResponse.class);
        Long reservationId = initialResponse.getId();
        String confirmationCode = initialResponse.getConfirmationCode();

        // 2. 예약 취소
        RestAssured
                .given().log().all()
                    .queryParam("confirmationCode", confirmationCode)
                .when()
                    .delete("/api/reservations/" + reservationId)
                .then().log().all()
                    .statusCode(HttpStatus.OK.value());

        // When: 다른 사용자가 동일한 사이트와 날짜로 새로운 예약을 요청하면
        ReservationRequest newRequest = ReservationRequest.builder()
                .customerName("박새롬")
                .phoneNumber("010-1111-1111")
                .startDate(LocalDate.of(2025, 10, 1))
                .endDate(LocalDate.of(2025, 10, 2))
                .siteNumber("A-1")
                .numberOfPeople(3)
                .build();

        String newResponseBody = RestAssured
                .given().log().all()
                    .contentType(ContentType.JSON)
                    .body(objectMapper.writeValueAsString(newRequest))
                .when()
                    .post("/api/reservations")
                .then().log().all()
                    .statusCode(HttpStatus.CREATED.value())
                    .extract().asString();

        // Then: 예약은 성공적으로 처리된다.
        ReservationResponse newResponse = objectMapper.readValue(newResponseBody, ReservationResponse.class);

        // And: 'A-1' 사이트의 취소된 예약 날짜에 새로운 사용자 정보로 예약이 생긴다.
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(newResponse.getConfirmationCode()).isNotNull();
            softly.assertThat(newResponse.getConfirmationCode()).isNotEqualTo(confirmationCode);
            softly.assertThat(newResponse.getCustomerName()).isEqualTo("박새롬");
        });
    }

    /**
     * Scenario: 여러 사용자가 동시에 동일한 예약을 요청할 경우, 단 하나의 요청만 성공한다.
     * When 여러 사용자가 동시에 'A-1' 사이트에 동일한 날짜로 예약을 요청하면
     * Then 오직 하나의 예약 요청만 성공하고, 나머지 요청은 모두 실패한다.
     * And 해당 날짜에는 하나의 예약만 생긴다.
     */
    @Test
    void 여러_사용자가_동시에_예약을_요청하면_하나만_성공한다() throws Exception {
        // When: 여러 사용자가 동시에 'A-1' 사이트에 동일한 날짜로 예약을 요청하면
        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numberOfThreads);
        List<Integer> statusCodes = new CopyOnWriteArrayList<>(); // Thread-safe list

        LocalDate reservationDate = LocalDate.of(2025, 11, 1);

        for (int i = 0; i < numberOfThreads; i++) {
            int userIndex = i;
            executorService.submit(() -> {
                try {
                    startLatch.await(); // Wait for the signal to start

                    ReservationRequest request = ReservationRequest.builder()
                            .customerName("User " + userIndex)
                            .phoneNumber("010-1234-" + String.format("%04d", userIndex))
                            .startDate(reservationDate)
                            .endDate(reservationDate.plusDays(1))
                            .siteNumber("A-1")
                            .numberOfPeople(2)
                            .build();

                    int statusCode = RestAssured
                            .given()
                            .contentType(ContentType.JSON)
                            .body(objectMapper.writeValueAsString(request))
                            .when()
                            .post("/api/reservations")
                            .then()
                            .extract().statusCode();

                    statusCodes.add(statusCode);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Signal all threads to start
        finishLatch.await(); // Wait for all threads to finish
        executorService.shutdown();

        // Then: 오직 하나의 예약 요청만 성공하고, 나머지 요청은 모두 실패한다.
        long successCount = statusCodes.stream().filter(code -> code == HttpStatus.CREATED.value()).count();
        long conflictCount = statusCodes.stream().filter(code -> code == HttpStatus.CONFLICT.value()).count();

        assertThat(successCount).isEqualTo(1);
        assertThat(conflictCount).isEqualTo(numberOfThreads - 1);

        // And: 해당 날짜에는 하나의 예약만 생긴다.
        List<ReservationResponse> reservations = RestAssured
                .given()
                .queryParam("date", reservationDate.toString())
                .when()
                .get("/api/reservations")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", ReservationResponse.class);

        assertThat(reservations).hasSize(1);
    }

    /**
     * Scenario: 사용자가 올바른 확인 코드로 자신의 예약을 성공적으로 취소한다.
     * When 사용자가 취소하고 싶은 예약의 예약 ID와 확인 코드로 취소를 요청하면
     * Then 예약 취소는 성공적으로 처리된다.
     * And 취소된 사이트와 날짜는 이제 다른 사용자가 예약할 수 있는 상태가 된다.
     */
    @Test
    void 사용자가_올바른_확인_코드로_자신의_예약을_성공적으로_취소한다() throws Exception {
        // Given: 특정 날짜에 예약이 생성되어 있음
        ReservationRequest initialRequest = ReservationRequest.builder()
                .customerName("김취소")
                .phoneNumber("010-4321-8765")
                .startDate(LocalDate.of(2025, 11, 5))
                .endDate(LocalDate.of(2025, 11, 6))
                .siteNumber("B-2")
                .numberOfPeople(4)
                .build();

        String responseBody = RestAssured
                .given()
                    .contentType(ContentType.JSON)
                    .body(objectMapper.writeValueAsString(initialRequest))
                .when()
                    .post("/api/reservations")
                .then()
                    .statusCode(HttpStatus.CREATED.value())
                    .extract().asString();

        ReservationResponse initialResponse = objectMapper.readValue(responseBody, ReservationResponse.class);
        Long reservationId = initialResponse.getId();
        String confirmationCode = initialResponse.getConfirmationCode();

        // When: 사용자가 예약 ID와 올바른 확인 코드로 취소를 요청하면
        RestAssured
                .given().log().all()
                    .queryParam("confirmationCode", confirmationCode)
                .when()
                    .delete("/api/reservations/" + reservationId)
                .then().log().all()
                    .statusCode(HttpStatus.OK.value());

        // Then: 예약 취소는 성공적으로 처리된다. (위의 statusCode(200) 검증으로 확인)

        // And: 취소된 사이트와 날짜는 이제 다른 사용자가 예약할 수 있는 상태가 된다.
        ReservationRequest newRequest = ReservationRequest.builder()
                .customerName("박새롬")
                .phoneNumber("010-1111-1111")
                .startDate(LocalDate.of(2025, 11, 5))
                .endDate(LocalDate.of(2025, 11, 6))
                .siteNumber("B-2")
                .numberOfPeople(3)
                .build();

        RestAssured
                .given().log().all()
                    .contentType(ContentType.JSON)
                    .body(objectMapper.writeValueAsString(newRequest))
                .when()
                    .post("/api/reservations")
                .then().log().all()
                    .statusCode(HttpStatus.CREATED.value());
    }

    /**
     * Scenario: 사용자가 잘못된 확인 코드로 예약을 취소하려 하면 실패한다.
     * Given 사용자가 사이트 'A-1'을 예약해둔 상태일 때
     * When 사용자가 해당 예약 ID와 유효하지 않은 확인 코드로 취소를 요청하면
     * Then "확인 코드가 일치하지 않습니다."와 같은 메시지와 함께 예약 취소가 실패한다.
     */
    @Test
    void 사용자가_잘못된_확인_코드로_예약_취소를_시도하면_실패한다() throws Exception {
        // Given: 사용자가 사이트 'A-1'을 예약해둔 상태일 때
        ReservationRequest initialRequest = ReservationRequest.builder()
                .customerName("김보안")
                .phoneNumber("010-9999-8888")
                .startDate(LocalDate.of(2025, 12, 1))
                .endDate(LocalDate.of(2025, 12, 2))
                .siteNumber("A-1")
                .numberOfPeople(2)
                .build();

        String responseBody = RestAssured
                .given()
                    .contentType(ContentType.JSON)
                    .body(objectMapper.writeValueAsString(initialRequest))
                .when()
                    .post("/api/reservations")
                .then()
                    .statusCode(HttpStatus.CREATED.value())
                    .extract().asString();

        ReservationResponse initialResponse = objectMapper.readValue(responseBody, ReservationResponse.class);
        Long reservationId = initialResponse.getId();

        // When: 사용자가 해당 예약 ID와 유효하지 않은 확인 코드로 취소를 요청하면
        String invalidConfirmationCode = "invalid-code";

        String errorMessage = RestAssured
                .given().log().all()
                    .queryParam("confirmationCode", invalidConfirmationCode)
                .when()
                    .delete("/api/reservations/" + reservationId)
                .then().log().all()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .extract().path("message");

        // Then: "확인 코드가 일치하지 않습니다."와 같은 메시지와 함께 예약 취소가 실패한다.
        assertThat(errorMessage).contains("확인 코드가 일치하지 않습니다.");
    }
}
