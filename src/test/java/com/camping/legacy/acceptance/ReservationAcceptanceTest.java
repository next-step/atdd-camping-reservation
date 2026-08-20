package com.camping.legacy.acceptance;

import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.camping.legacy.acceptance.ReservationSteps.create;
import static com.camping.legacy.acceptance.ReservationSteps.createWithPhoneNumber;
import static com.camping.legacy.acceptance.ReservationSteps.createWithoutPhoneNumber;
import static com.camping.legacy.acceptance.ReservationSteps.findByCustomerName;
import static com.camping.legacy.acceptance.ReservationSteps.findById;
import static com.camping.legacy.acceptance.ReservationSteps.updateStartDate;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

class ReservationAcceptanceTest extends AcceptanceTest {

    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
    }

    @Nested
    @DisplayName("생성 - 시작일 상한")
    class CreateUpperBound {

        @Test
        @DisplayName("시작일이 29일 뒤면 예약된다")
        void acceptsDayBeforeLimit() {
            create("상한29", "B-1", today.plusDays(29))
                    .then()
                    .statusCode(201)
                    .body("confirmationCode", notNullValue());
        }

        @Test
        @DisplayName("시작일이 30일 뒤면 예약된다")
        void acceptsLimitDay() {
            create("상한30", "B-2", today.plusDays(30))
                    .then()
                    .statusCode(201)
                    .body("confirmationCode", notNullValue());
        }

        @Test
        @DisplayName("시작일이 31일 뒤면 거절되고 예약이 생성되지 않는다")
        void rejectsBeyondLimit() {
            create("상한31", "B-3", today.plusDays(31))
                    .then()
                    .statusCode(409)
                    .body("message", equalTo("예약은 오늘로부터 30일 이내만 가능합니다."));

            findByCustomerName("상한31")
                    .then()
                    .statusCode(200)
                    .body("$", hasSize(0));
        }
    }

    @Nested
    @DisplayName("생성 - 시작일 하한")
    class CreateLowerBound {

        @Test
        @DisplayName("시작일이 어제면 과거 날짜로 거절된다")
        void rejectsYesterday() {
            create("하한어제", "B-6", today.minusDays(1))
                    .then()
                    .statusCode(409)
                    .body("message", equalTo("과거 날짜로 예약할 수 없습니다."));
        }

        @Test
        @DisplayName("시작일이 오늘이면 예약된다")
        void acceptsToday() {
            create("하한오늘", "B-7", today)
                    .then()
                    .statusCode(201)
                    .body("confirmationCode", notNullValue());
        }
    }

    @Nested
    @DisplayName("생성 - 전화번호 필수")
    class CreatePhoneNumberRequired {

        @Test
        @DisplayName("전화번호 필드를 생략하면 거절되고 예약이 생성되지 않는다")
        void rejectsMissingField() {
            createWithoutPhoneNumber("전화번호생략", "A-5", today.plusDays(5))
                .then()
                .statusCode(409)
                .body("message", equalTo("전화번호를 입력해주세요."));

            findByCustomerName("전화번호생략")
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
        }

        @Test
        @DisplayName("전화번호가 null이면 거절되고 예약이 생성되지 않는다")
        void rejectsNull() {
            createWithPhoneNumber("전화번호null", "A-7", today.plusDays(5), null)
                .then()
                .statusCode(409)
                .body("message", equalTo("전화번호를 입력해주세요."));

            findByCustomerName("전화번호null")
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
        }

        @Test
        @DisplayName("전화번호가 빈 문자열이면 거절되고 예약이 생성되지 않는다")
        void rejectsEmpty() {
            createWithPhoneNumber("전화번호빈값", "A-8", today.plusDays(5), "")
                .then()
                .statusCode(409)
                .body("message", equalTo("전화번호를 입력해주세요."));

            findByCustomerName("전화번호빈값")
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
        }

        @Test
        @DisplayName("전화번호가 공백만이면 거절되고 예약이 생성되지 않는다")
        void rejectsBlank() {
            createWithPhoneNumber("전화번호공백", "A-9", today.plusDays(5), "   ")
                .then()
                .statusCode(409)
                .body("message", equalTo("전화번호를 입력해주세요."));

            findByCustomerName("전화번호공백")
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
        }

        @Test
        @DisplayName("전화번호가 있으면 예약된다")
        void acceptsPhoneNumber() {
            createWithPhoneNumber("전화번호정상", "A-10", today.plusDays(5), "010-1111-2222")
                .then()
                .statusCode(201)
                .body("confirmationCode", notNullValue());
        }
    }

    @Nested
    @DisplayName("생성 - 전화번호 형식")
    class CreatePhoneNumberFormat {

        @Test
        @DisplayName("자릿수가 모자라면 형식 오류로 거절된다")
        void rejectsTooShort() {
            createWithPhoneNumber("형식자릿수", "A-11", today.plusDays(5), "010-111-222")
                .then()
                .statusCode(409)
                .body("message", equalTo("전화번호 형식이 올바르지 않습니다."));
        }

        @Test
        @DisplayName("숫자가 아닌 문자가 있으면 숫자 오류로 거절된다")
        void rejectsNonDigit() {
            createWithPhoneNumber("형식문자", "A-12", today.plusDays(5), "010-abcd-5678")
                .then()
                .statusCode(409)
                .body("message", equalTo("전화번호는 숫자만 입력 가능합니다."));
        }

        @Test
        @DisplayName("01x 로 시작하지 않아도 예약된다")
        void acceptsNonMobilePrefix() {
            createWithPhoneNumber("형식앞자리", "A-13", today.plusDays(5), "020-1234-5678")
                .then()
                .statusCode(201)
                .body("confirmationCode", notNullValue());
        }
    }

    @Nested
    @DisplayName("변경 - 시작일 상한")
    class UpdateUpperBound {

        @Test
        @DisplayName("시작일을 29일 뒤로 변경하면 반영된다")
        void acceptsDayBeforeLimit() {
            JsonPath reserved = create("변경29", "B-11", today.plusDays(5)).jsonPath();

            updateStartDate(reserved, today.plusDays(29))
                    .then()
                    .statusCode(200)
                    .body("startDate", equalTo(today.plusDays(29).toString()));
        }

        @Test
        @DisplayName("시작일을 30일 뒤로 변경하면 반영된다")
        void acceptsLimitDay() {
            JsonPath reserved = create("변경30", "B-12", today.plusDays(5)).jsonPath();

            updateStartDate(reserved, today.plusDays(30))
                    .then()
                    .statusCode(200)
                    .body("startDate", equalTo(today.plusDays(30).toString()));
        }

        @Test
        @DisplayName("시작일을 31일 뒤로 변경하면 거절되고 날짜가 바뀌지 않는다")
        void rejectsBeyondLimit() {
            JsonPath reserved = create("변경31", "B-13", today.plusDays(5)).jsonPath();

            updateStartDate(reserved, today.plusDays(31))
                    .then()
                    .statusCode(400)
                    .body("message", equalTo("예약은 오늘로부터 30일 이내만 가능합니다."));

            findById(reserved.getLong("id"))
                    .then()
                    .statusCode(200)
                    .body("startDate", equalTo(today.plusDays(5).toString()));
        }
    }
}
