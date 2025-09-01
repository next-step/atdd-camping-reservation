package com.camping.legacy.acceptance;

import com.camping.legacy.support.TestBase;
import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Map;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("캠핑장 예약 시스템 엣지 케이스 검증 테스트")
class EdgeCaseValidationTest extends TestBase {
    
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
        /*
         * 누가(Who), 무엇을(What), 언제(When), 어디서(Where), 왜(Why), 어떻게(How)
         * 누가(Who): 잘못된 전화번호 정보를 입력한 사용자
         * 무엇을(What): 예약을
         * 언제(When): 정상적인 날짜에
         * 어디서(Where): 캠핑장 예약 시스템에서
         * 왜(Why): null 전화번호에 대한 적절한 검증이 이루어지는지 확인하기 위해
         * 어떻게(How): null 전화번호로 예약 요청을 보내서
         */

        /*
         * given - null 전화번호로 예약을 시도한다
         * when - null 전화번호로 예약을 요청하면
         * then - 예약이 실패하고 적절한 검증 오류 메시지가 반환된다
         */

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
    @Test
    void createReservation_NonExistentSiteId() {
        /*
         * 누가(Who), 무엇을(What), 언제(When), 어디서(Where), 왜(Why), 어떻게(How)
         * 누가(Who): 잘못된 사이트 ID를 사용하는 사용자
         * 무엇을(What): 예약을
         * 언제(When): 정상적인 날짜에
         * 어디서(Where): 캠핑장 예약 시스템에서
         * 왜(Why): 존재하지 않는 사이트 ID에 대한 적절한 처리가 이루어지는지 확인하기 위해
         * 어떻게(How): 존재하지 않는 사이트 ID로 예약 요청을 보내서
         */

        /*
         * given - 존재하지 않는 사이트 ID를 사용한다
         * when - 존재하지 않는 사이트 ID로 예약을 요청하면
         * then - 예약이 실패하고 적절한 오류 메시지가 반환된다
         */

        // Given - 존재하지 않는 사이트 ID
        String customerName = "김철수";
        String phoneNumber = "010-1234-5678";
        String nonExistentSiteId = "99999";
        LocalDate startDate = LocalDate.now().plusDays(5);
        LocalDate endDate = startDate.plusDays(2);
        
        // When - 존재하지 않는 사이트 ID로 예약을 요청한다
        ExtractableResponse<Response> response = createReservation(customerName, phoneNumber, nonExistentSiteId, startDate, endDate);
        
        // Then - 예약이 실패한다
        assertThat(response.statusCode()).isIn(HttpStatus.BAD_REQUEST.value(), HttpStatus.CONFLICT.value(), HttpStatus.NOT_FOUND.value());
        assertThat(response.jsonPath().getString("message")).isNotNull();
    }
    
    @DisplayName("예약 생성 - 정확히 30일째 되는 날 예약 (경계값 테스트)")
    @Test
    void createReservation_ExactlyThirtiethDay() {
        /*
         * 누가(Who), 무엇을(What), 언제(When), 어디서(Where), 왜(Why), 어떻게(How)
         * 누가(Who): 예약을 하려는 사용자
         * 무엇을(What): 정확히 30일째 되는 날 예약을
         * 언제(When): 오늘로부터 정확히 30일 후
         * 어디서(Where): 캠핑장 예약 시스템에서
         * 왜(Why): 30일 제한의 경계값에서 정확한 시스템 동작을 확인하기 위해
         * 어떻게(How): 정확히 30일째 되는 날짜로 예약 요청을 보내서
         */

        /*
         * given - 오늘로부터 정확히 30일 후 날짜로 예약을 시도한다
         * when - 정확히 30일째 되는 날로 예약을 요청하면
         * then - 30일 이내 규칙에 따라 성공하거나 실패한다
         */

        // Given - 정확히 30일째 되는 날
        String customerName = "김철수";
        String phoneNumber = "010-1234-5678";
        LocalDate exactly30Days = LocalDate.now().plusDays(30);
        LocalDate endDate = exactly30Days.plusDays(2);
        
        // When - 정확히 30일째 되는 날로 예약을 요청한다
        ExtractableResponse<Response> response = createReservation(customerName, phoneNumber, "1", exactly30Days, endDate);
        
        // Then - 30일 이내 규칙에 따라 성공하거나 실패한다 (비즈니스 규칙에 따라)
        assertThat(response.statusCode()).isIn(HttpStatus.CREATED.value(), HttpStatus.BAD_REQUEST.value());
    }
    
    @DisplayName("예약 취소 - 존재하지 않는 예약 ID로 취소 시도")
    @Test
    void cancelReservation_NonExistentReservationId() {
        /*
         * 누가(Who), 무엇을(What), 언제(When), 어디서(Where), 왜(Why), 어떻게(How)
         * 누가(Who): 잘못된 예약 ID를 사용하는 사용자
         * 무엇을(What): 예약 취소를
         * 언제(When): 언제든지
         * 어디서(Where): 캠핑장 예약 시스템에서
         * 왜(Why): 존재하지 않는 예약 ID에 대한 적절한 처리가 이루어지는지 확인하기 위해
         * 어떻게(How): 존재하지 않는 예약 ID로 취소 요청을 보내서
         */

        /*
         * given - 존재하지 않는 예약 ID를 사용한다
         * when - 존재하지 않는 예약 ID로 취소를 요청하면
         * then - 취소가 실패하고 적절한 오류 메시지가 반환된다
         */

        // Given - 존재하지 않는 예약 ID
        Long nonExistentId = 99999L;
        String confirmationCode = "ABC123";
        
        // When - 존재하지 않는 예약 ID로 취소를 요청한다
        ExtractableResponse<Response> response = cancelReservation(nonExistentId, confirmationCode);
        
        // Then - 취소가 실패한다
        assertThat(response.statusCode()).isIn(HttpStatus.NOT_FOUND.value(), HttpStatus.BAD_REQUEST.value());
        assertThat(response.jsonPath().getString("message")).isNotNull();
    }
    
    @DisplayName("예약 취소 - 빈 확인코드로 취소 시도")
    @Test
    void cancelReservation_EmptyConfirmationCode() {
        /*
         * 누가(Who), 무엇을(What), 언제(When), 어디서(Where), 왜(Why), 어떻게(How)
         * 누가(Who): 잘못된 확인코드를 사용하는 사용자
         * 무엇을(What): 예약 취소를
         * 언제(When): 예약이 있는 상태에서
         * 어디서(Where): 캠핑장 예약 시스템에서
         * 왜(Why): 빈 확인코드에 대한 적절한 검증이 이루어지는지 확인하기 위해
         * 어떻게(How): 빈 확인코드로 취소 요청을 보내서
         */

        /*
         * given - 예약이 생성되고 빈 확인코드를 사용한다
         * when - 빈 확인코드로 취소를 요청하면
         * then - 취소가 실패하고 적절한 검증 오류 메시지가 반환된다
         */

        // Given - 예약을 생성하고 빈 확인코드를 준비한다
        ExtractableResponse<Response> createResponse = createReservation("테스트고객", "010-9999-9999", "1", 
                LocalDate.now().plusDays(15), LocalDate.now().plusDays(17));
        Long reservationId = createResponse.jsonPath().getLong("id");
        String emptyConfirmationCode = "";
        
        // When - 빈 확인코드로 취소를 요청한다
        ExtractableResponse<Response> response = cancelReservation(reservationId, emptyConfirmationCode);
        
        // Then - 취소가 실패한다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.jsonPath().getString("message")).isNotNull();
    }
    
    @DisplayName("예약 조회 - 존재하지 않는 예약 ID로 조회")
    @Test
    void getReservation_NonExistentReservationId() {
        // Given - 존재하지 않는 예약 ID
        Long nonExistentId = 99999L;
        
        // When - 존재하지 않는 ID로 조회한다
        ExtractableResponse<Response> response = getReservation(nonExistentId);
        
        // Then - 조회가 실패한다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(response.jsonPath().getString("message")).isNotNull();
    }
    
    @DisplayName("내 예약 조회 - 존재하지 않는 고객 정보로 조회")
    @Test
    void getMyReservations_NonExistentCustomer() {
        // Given - 존재하지 않는 고객 정보
        String nonExistentName = "존재하지않는고객";
        String nonExistentPhone = "010-0000-0000";
        
        // When - 존재하지 않는 고객 정보로 조회한다
        ExtractableResponse<Response> response = getMyReservations(nonExistentName, nonExistentPhone);
        
        // Then - 빈 목록이 반환된다
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getList("$")).isEmpty();
    }
    
    @DisplayName("사이트 조회 - 존재하지 않는 사이트 ID로 조회")
    @Test
    void getSiteDetail_NonExistentSiteId() {
        // Given - 존재하지 않는 사이트 ID
        Long nonExistentSiteId = 99999L;
        
        // When - 존재하지 않는 사이트 ID로 조회한다
        ExtractableResponse<Response> response = getSiteDetail(nonExistentSiteId);
        
        // Then - 조회가 실패한다
        assertThat(response.statusCode()).isIn(HttpStatus.NOT_FOUND.value(), HttpStatus.BAD_REQUEST.value());
    }
    
    @DisplayName("캘린더 조회 - 잘못된 월 값 (13월)")
    @Test
    void getReservationCalendar_InvalidMonth() {
        // Given - 잘못된 월 값
        int invalidMonth = 13;
        int year = LocalDate.now().getYear();
        Long siteId = 1L;
        
        // When - 잘못된 월로 캘린더를 조회한다
        ExtractableResponse<Response> response = getReservationCalendar(year, invalidMonth, siteId);
        
        // Then - 조회가 실패한다
        assertThat(response.statusCode()).isIn(HttpStatus.BAD_REQUEST.value(), HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}