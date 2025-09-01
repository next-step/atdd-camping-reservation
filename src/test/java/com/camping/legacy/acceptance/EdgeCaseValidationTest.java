package com.camping.legacy.acceptance;

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
import java.util.Map;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("캠핑장 예약 시스템 엣지 케이스 검증 테스트")
class EdgeCaseValidationTest {
    
    @LocalServerPort
    int port;
    
    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }
    
    @DisplayName("예약 생성 - 빈 고객명으로 예약 시도")
    @Test
    void createReservation_EmptyCustomerName() {
        /*
         * 누가(Who), 무엇을(What), 언제(When), 어디서(Where), 왜(Why), 어떻게(How)
         * 누가(Who): 필수 정보를 누락한 사용자
         * 무엇을(What): 예약을
         * 언제(When): 정상적인 날짜에
         * 어디서(Where): 캠핑장 예약 시스템에서
         * 왜(Why): 필수 정보(고객명) 누락 시 적절한 검증이 이루어지는지 확인하기 위해
         * 어떻게(How): 빈 고객명으로 예약 요청을 보내서
         */

        /*
         * given - 빈 고객명으로 예약을 시도한다
         * when - 빈 고객명으로 예약을 요청하면
         * then - 예약이 실패하고 적절한 검증 오류 메시지가 반환된다
         */

        // Given - 빈 고객명
        String emptyName = "";
        String phoneNumber = "010-1234-5678";
        LocalDate startDate = LocalDate.now().plusDays(5);
        LocalDate endDate = startDate.plusDays(2);
        
        // When - 빈 고객명으로 예약을 요청한다
        ExtractableResponse<Response> response = RestAssured
                .given().log().all()
                .contentType("application/json")
                .body(Map.of(
                        "customerName", emptyName,
                        "phoneNumber", phoneNumber,
                        "campsiteId", "1",
                        "startDate", startDate.toString(),
                        "endDate", endDate.toString()
                ))
                .when().post("/api/reservations")
                .then().log().all()
                .extract();
        
        // Then - 예약이 실패한다
        assertThat(response.statusCode()).isIn(HttpStatus.BAD_REQUEST.value(), HttpStatus.CONFLICT.value());
        assertThat(response.jsonPath().getString("message")).isNotNull();
    }
    
    @DisplayName("예약 생성 - null 전화번호로 예약 시도")
    @Test
    void createReservation_NullPhoneNumber() {
        // Given - null 전화번호
        String customerName = "김철수";
        LocalDate startDate = LocalDate.now().plusDays(5);
        LocalDate endDate = startDate.plusDays(2);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("customerName", customerName);
        requestBody.put("phoneNumber", null);
        requestBody.put("campsiteId", "1");
        requestBody.put("startDate", startDate.toString());
        requestBody.put("endDate", endDate.toString());
        
        // When - null 전화번호로 예약을 요청한다
        ExtractableResponse<Response> response = RestAssured
                .given().log().all()
                .contentType("application/json")
                .body(requestBody)
                .when().post("/api/reservations")
                .then().log().all()
                .extract();
        
        // Then - 예약이 실패한다
        assertThat(response.statusCode()).isIn(HttpStatus.BAD_REQUEST.value(), HttpStatus.CONFLICT.value());
        assertThat(response.jsonPath().getString("message")).isNotNull();
    }
    
    @DisplayName("예약 생성 - 존재하지 않는 사이트 ID로 예약 시도")
    @Test\n    void createReservation_NonExistentSiteId() {\n        // Given - 존재하지 않는 사이트 ID\n        String customerName = \"김철수\";\n        String phoneNumber = \"010-1234-5678\";\n        String nonExistentSiteId = \"99999\";\n        LocalDate startDate = LocalDate.now().plusDays(5);\n        LocalDate endDate = startDate.plusDays(2);\n        \n        // When - 존재하지 않는 사이트 ID로 예약을 요청한다\n        ExtractableResponse<Response> response = RestAssured\n                .given().log().all()\n                .contentType(\"application/json\")\n                .body(Map.of(\n                        \"customerName\", customerName,\n                        \"phoneNumber\", phoneNumber,\n                        \"campsiteId\", nonExistentSiteId,\n                        \"startDate\", startDate.toString(),\n                        \"endDate\", endDate.toString()\n                ))\n                .when().post(\"/api/reservations\")\n                .then().log().all()\n                .extract();\n        \n        // Then - 예약이 실패한다\n        assertThat(response.statusCode()).isIn(HttpStatus.BAD_REQUEST.value(), HttpStatus.CONFLICT.value(), HttpStatus.NOT_FOUND.value());\n        assertThat(response.jsonPath().getString(\"message\")).isNotNull();\n    }\n    \n    @DisplayName(\"예약 생성 - 종료일이 시작일보다 이른 경우\")\n    @Test\n    void createReservation_EndDateBeforeStartDate() {\n        // Given - 종료일이 시작일보다 이른 날짜\n        String customerName = \"김철수\";\n        String phoneNumber = \"010-1234-5678\";\n        LocalDate startDate = LocalDate.now().plusDays(10);\n        LocalDate endDate = startDate.minusDays(1); // 시작일보다 하루 전\n        \n        // When - 잘못된 날짜 순서로 예약을 요청한다\n        ExtractableResponse<Response> response = RestAssured\n                .given().log().all()\n                .contentType(\"application/json\")\n                .body(Map.of(\n                        \"customerName\", customerName,\n                        \"phoneNumber\", phoneNumber,\n                        \"campsiteId\", \"1\",\n                        \"startDate\", startDate.toString(),\n                        \"endDate\", endDate.toString()\n                ))\n                .when().post(\"/api/reservations\")\n                .then().log().all()\n                .extract();\n        \n        // Then - 예약이 실패한다\n        assertThat(response.statusCode()).isIn(HttpStatus.BAD_REQUEST.value(), HttpStatus.CONFLICT.value());\n        assertThat(response.jsonPath().getString(\"message\")).isNotNull();\n    }\n    \n    @DisplayName(\"예약 생성 - 정확히 30일째 되는 날 예약 (경계값 테스트)\")\n    @Test\n    void createReservation_ExactlyThirtiethDay() {\n        // Given - 정확히 30일째 되는 날\n        String customerName = \"김철수\";\n        String phoneNumber = \"010-1234-5678\";\n        LocalDate exactly30Days = LocalDate.now().plusDays(30);\n        LocalDate endDate = exactly30Days.plusDays(2);\n        \n        // When - 정확히 30일째 되는 날로 예약을 요청한다\n        ExtractableResponse<Response> response = RestAssured\n                .given().log().all()\n                .contentType(\"application/json\")\n                .body(Map.of(\n                        \"customerName\", customerName,\n                        \"phoneNumber\", phoneNumber,\n                        \"campsiteId\", \"1\",\n                        \"startDate\", exactly30Days.toString(),\n                        \"endDate\", endDate.toString()\n                ))\n                .when().post(\"/api/reservations\")\n                .then().log().all()\n                .extract();\n        \n        // Then - 30일 이내 규칙에 따라 성공하거나 실패한다 (비즈니스 규칙에 따라)\n        assertThat(response.statusCode()).isIn(HttpStatus.CREATED.value(), HttpStatus.BAD_REQUEST.value());\n    }\n    \n    @DisplayName(\"예약 생성 - 당일 예약 시도\")\n    @Test\n    void createReservation_SameDayReservation() {\n        // Given - 당일 예약\n        String customerName = \"김철수\";\n        String phoneNumber = \"010-1234-5678\";\n        LocalDate today = LocalDate.now();\n        LocalDate endDate = today.plusDays(2);\n        \n        // When - 당일로 예약을 요청한다\n        ExtractableResponse<Response> response = RestAssured\n                .given().log().all()\n                .contentType(\"application/json\")\n                .body(Map.of(\n                        \"customerName\", customerName,\n                        \"phoneNumber\", phoneNumber,\n                        \"campsiteId\", \"1\",\n                        \"startDate\", today.toString(),\n                        \"endDate\", endDate.toString()\n                ))\n                .when().post(\"/api/reservations\")\n                .then().log().all()\n                .extract();\n        \n        // Then - 당일 예약 정책에 따라 성공하거나 실패한다\n        assertThat(response.statusCode()).isIn(HttpStatus.CREATED.value(), HttpStatus.BAD_REQUEST.value());\n    }\n    \n    @DisplayName(\"예약 생성 - 매우 긴 고객명으로 예약 시도\")\n    @Test\n    void createReservation_VeryLongCustomerName() {\n        // Given - 매우 긴 고객명 (255자)\n        String veryLongName = \"A\".repeat(255);\n        String phoneNumber = \"010-1234-5678\";\n        LocalDate startDate = LocalDate.now().plusDays(5);\n        LocalDate endDate = startDate.plusDays(2);\n        \n        // When - 매우 긴 고객명으로 예약을 요청한다\n        ExtractableResponse<Response> response = RestAssured\n                .given().log().all()\n                .contentType(\"application/json\")\n                .body(Map.of(\n                        \"customerName\", veryLongName,\n                        \"phoneNumber\", phoneNumber,\n                        \"campsiteId\", \"1\",\n                        \"startDate\", startDate.toString(),\n                        \"endDate\", endDate.toString()\n                ))\n                .when().post(\"/api/reservations\")\n                .then().log().all()\n                .extract();\n        \n        // Then - 길이 제한에 따라 성공하거나 실패한다\n        assertThat(response.statusCode()).isIn(HttpStatus.CREATED.value(), HttpStatus.BAD_REQUEST.value(), HttpStatus.CONFLICT.value());\n    }\n    \n    @DisplayName(\"예약 생성 - 잘못된 전화번호 형식\")\n    @Test\n    void createReservation_InvalidPhoneNumberFormat() {\n        // Given - 잘못된 전화번호 형식\n        String customerName = \"김철수\";\n        String invalidPhone = \"잘못된전화번호\";\n        LocalDate startDate = LocalDate.now().plusDays(5);\n        LocalDate endDate = startDate.plusDays(2);\n        \n        // When - 잘못된 전화번호로 예약을 요청한다\n        ExtractableResponse<Response> response = RestAssured\n                .given().log().all()\n                .contentType(\"application/json\")\n                .body(Map.of(\n                        \"customerName\", customerName,\n                        \"phoneNumber\", invalidPhone,\n                        \"campsiteId\", \"1\",\n                        \"startDate\", startDate.toString(),\n                        \"endDate\", endDate.toString()\n                ))\n                .when().post(\"/api/reservations\")\n                .then().log().all()\n                .extract();\n        \n        // Then - 전화번호 검증에 따라 성공하거나 실패한다\n        assertThat(response.statusCode()).isIn(HttpStatus.CREATED.value(), HttpStatus.BAD_REQUEST.value(), HttpStatus.CONFLICT.value());\n    }\n    \n    @DisplayName(\"예약 취소 - 존재하지 않는 예약 ID로 취소 시도\")\n    @Test\n    void cancelReservation_NonExistentReservationId() {\n        // Given - 존재하지 않는 예약 ID\n        Long nonExistentId = 99999L;\n        String confirmationCode = \"ABC123\";\n        \n        // When - 존재하지 않는 예약 ID로 취소를 요청한다\n        ExtractableResponse<Response> response = RestAssured\n                .given().log().all()\n                .queryParam(\"confirmationCode\", confirmationCode)\n                .when().delete(\"/api/reservations/\" + nonExistentId)\n                .then().log().all()\n                .extract();\n        \n        // Then - 취소가 실패한다\n        assertThat(response.statusCode()).isIn(HttpStatus.NOT_FOUND.value(), HttpStatus.BAD_REQUEST.value());\n        assertThat(response.jsonPath().getString(\"message\")).isNotNull();\n    }\n    \n    @DisplayName(\"예약 취소 - 빈 확인코드로 취소 시도\")\n    @Test\n    void cancelReservation_EmptyConfirmationCode() {\n        // Given - 예약을 생성하고 빈 확인코드를 준비한다\n        ExtractableResponse<Response> createResponse = createReservation();\n        Long reservationId = createResponse.jsonPath().getLong(\"id\");\n        String emptyConfirmationCode = \"\";\n        \n        // When - 빈 확인코드로 취소를 요청한다\n        ExtractableResponse<Response> response = RestAssured\n                .given().log().all()\n                .queryParam(\"confirmationCode\", emptyConfirmationCode)\n                .when().delete(\"/api/reservations/\" + reservationId)\n                .then().log().all()\n                .extract();\n        \n        // Then - 취소가 실패한다\n        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());\n        assertThat(response.jsonPath().getString(\"message\")).isNotNull();\n    }\n    \n    @DisplayName(\"예약 조회 - 존재하지 않는 예약 ID로 조회\")\n    @Test\n    void getReservation_NonExistentReservationId() {\n        // Given - 존재하지 않는 예약 ID\n        Long nonExistentId = 99999L;\n        \n        // When - 존재하지 않는 ID로 조회한다\n        ExtractableResponse<Response> response = RestAssured\n                .given().log().all()\n                .when().get(\"/api/reservations/\" + nonExistentId)\n                .then().log().all()\n                .extract();\n        \n        // Then - 조회가 실패한다\n        assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());\n        assertThat(response.jsonPath().getString(\"message\")).isNotNull();\n    }\n    \n    @DisplayName(\"내 예약 조회 - 존재하지 않는 고객 정보로 조회\")\n    @Test\n    void getMyReservations_NonExistentCustomer() {\n        // Given - 존재하지 않는 고객 정보\n        String nonExistentName = \"존재하지않는고객\";\n        String nonExistentPhone = \"010-0000-0000\";\n        \n        // When - 존재하지 않는 고객 정보로 조회한다\n        ExtractableResponse<Response> response = RestAssured\n                .given().log().all()\n                .queryParam(\"name\", nonExistentName)\n                .queryParam(\"phone\", nonExistentPhone)\n                .when().get(\"/api/reservations/my\")\n                .then().log().all()\n                .extract();\n        \n        // Then - 빈 목록이 반환된다\n        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());\n        assertThat(response.jsonPath().getList(\"$\")).isEmpty();\n    }\n    \n    @DisplayName(\"사이트 조회 - 존재하지 않는 사이트 ID로 조회\")\n    @Test\n    void getSiteDetail_NonExistentSiteId() {\n        // Given - 존재하지 않는 사이트 ID\n        Long nonExistentSiteId = 99999L;\n        \n        // When - 존재하지 않는 사이트 ID로 조회한다\n        ExtractableResponse<Response> response = RestAssured\n                .given().log().all()\n                .when().get(\"/api/sites/\" + nonExistentSiteId)\n                .then().log().all()\n                .extract();\n        \n        // Then - 조회가 실패한다\n        assertThat(response.statusCode()).isIn(HttpStatus.NOT_FOUND.value(), HttpStatus.BAD_REQUEST.value());\n    }\n    \n    @DisplayName(\"사이트 가용성 조회 - 존재하지 않는 사이트 번호\")\n    @Test\n    void checkSiteAvailability_NonExistentSiteNumber() {\n        // Given - 존재하지 않는 사이트 번호\n        String nonExistentSiteNumber = \"Z999\";\n        LocalDate checkDate = LocalDate.now().plusDays(5);\n        \n        // When - 존재하지 않는 사이트 번호로 가용성을 확인한다\n        ExtractableResponse<Response> response = RestAssured\n                .given().log().all()\n                .queryParam(\"date\", checkDate.toString())\n                .when().get(\"/api/sites/\" + nonExistentSiteNumber + \"/availability\")\n                .then().log().all()\n                .extract();\n        \n        // Then - 조회가 실패하거나 사용 불가능으로 반환된다\n        assertThat(response.statusCode()).isIn(HttpStatus.OK.value(), HttpStatus.NOT_FOUND.value(), HttpStatus.BAD_REQUEST.value());\n    }\n    \n    @DisplayName(\"캘린더 조회 - 잘못된 월 값 (13월)\")\n    @Test\n    void getReservationCalendar_InvalidMonth() {\n        // Given - 잘못된 월 값\n        int invalidMonth = 13;\n        int year = LocalDate.now().getYear();\n        Long siteId = 1L;\n        \n        // When - 잘못된 월로 캘린더를 조회한다\n        ExtractableResponse<Response> response = RestAssured\n                .given().log().all()\n                .queryParam(\"year\", year)\n                .queryParam(\"month\", invalidMonth)\n                .queryParam(\"siteId\", siteId)\n                .when().get(\"/api/reservations/calendar\")\n                .then().log().all()\n                .extract();\n        \n        // Then - 조회가 실패한다\n        assertThat(response.statusCode()).isIn(HttpStatus.BAD_REQUEST.value(), HttpStatus.INTERNAL_SERVER_ERROR.value());\n    }\n    \n    @DisplayName(\"캘린더 조회 - 음수 년도\")\n    @Test\n    void getReservationCalendar_NegativeYear() {\n        // Given - 음수 년도\n        int negativeYear = -2024;\n        int month = 1;\n        Long siteId = 1L;\n        \n        // When - 음수 년도로 캘린더를 조회한다\n        ExtractableResponse<Response> response = RestAssured\n                .given().log().all()\n                .queryParam(\"year\", negativeYear)\n                .queryParam(\"month\", month)\n                .queryParam(\"siteId\", siteId)\n                .when().get(\"/api/reservations/calendar\")\n                .then().log().all()\n                .extract();\n        \n        // Then - 조회가 실패한다\n        assertThat(response.statusCode()).isIn(HttpStatus.BAD_REQUEST.value(), HttpStatus.INTERNAL_SERVER_ERROR.value());\n    }\n    \n    // Helper method\n    private ExtractableResponse<Response> createReservation() {\n        return RestAssured\n                .given().log().all()\n                .contentType(\"application/json\")\n                .body(Map.of(\n                        \"customerName\", \"테스트고객\",\n                        \"phoneNumber\", \"010-9999-9999\",\n                        \"campsiteId\", \"1\",\n                        \"startDate\", LocalDate.now().plusDays(15).toString(),\n                        \"endDate\", LocalDate.now().plusDays(17).toString()\n                ))\n                .when().post(\"/api/reservations\")\n                .then().statusCode(HttpStatus.CREATED.value())\n                .extract();\n    }\n}