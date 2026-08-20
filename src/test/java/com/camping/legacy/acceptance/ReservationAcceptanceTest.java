package com.camping.legacy.acceptance;

import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.camping.legacy.acceptance.ReservationSteps.assertConfirmedCount;
import static com.camping.legacy.acceptance.ReservationSteps.assertNotReserved;
import static com.camping.legacy.acceptance.ReservationSteps.create;
import static com.camping.legacy.acceptance.ReservationSteps.createCancelled;
import static com.camping.legacy.acceptance.ReservationSteps.createWithPhoneNumber;
import static com.camping.legacy.acceptance.ReservationSteps.createWithoutPhoneNumber;
import static com.camping.legacy.acceptance.ReservationSteps.findById;
import static com.camping.legacy.acceptance.ReservationSteps.updateStartDate;
import static org.hamcrest.Matchers.equalTo;
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

            assertNotReserved("상한31");
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

            assertNotReserved("전화번호생략");
        }

        @Test
        @DisplayName("전화번호가 null이면 거절되고 예약이 생성되지 않는다")
        void rejectsNull() {
            createWithPhoneNumber("전화번호null", "A-7", today.plusDays(5), null)
                .then()
                .statusCode(409)
                .body("message", equalTo("전화번호를 입력해주세요."));

            assertNotReserved("전화번호null");
        }

        @Test
        @DisplayName("전화번호가 빈 문자열이면 거절되고 예약이 생성되지 않는다")
        void rejectsEmpty() {
            createWithPhoneNumber("전화번호빈값", "A-8", today.plusDays(5), "")
                .then()
                .statusCode(409)
                .body("message", equalTo("전화번호를 입력해주세요."));

            assertNotReserved("전화번호빈값");
        }

        @Test
        @DisplayName("전화번호가 공백만이면 거절되고 예약이 생성되지 않는다")
        void rejectsBlank() {
            createWithPhoneNumber("전화번호공백", "A-9", today.plusDays(5), "   ")
                .then()
                .statusCode(409)
                .body("message", equalTo("전화번호를 입력해주세요."));

            assertNotReserved("전화번호공백");
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

    @Nested
    @DisplayName("생성 - 취소된 예약의 자리")
    class CreateOverCancelledReservation {

        @Test
        @DisplayName("종료일이 취소된 예약의 시작일 하루 전이면 예약된다")
        void acceptsRangeEndingBeforeCancelled() {
            LocalDate baseStart = today.plusDays(5);
            LocalDate baseEnd = today.plusDays(7);
            createCancelled("B-4", baseStart, baseEnd);

            create("취소전날", "B-4", baseStart.minusDays(2), baseStart.minusDays(1))
                    .then()
                    .statusCode(201)
                    .body("confirmationCode", notNullValue());

            assertConfirmedCount(baseStart.minusDays(1), "B-4", 1);
        }

        @Test
        @DisplayName("종료일이 취소된 예약의 시작일과 같은 날이면 예약된다")
        void acceptsRangeTouchingCancelledStart() {
            LocalDate baseStart = today.plusDays(5);
            LocalDate baseEnd = today.plusDays(7);
            createCancelled("B-5", baseStart, baseEnd);

            create("취소앞경계", "B-5", baseStart.minusDays(2), baseStart)
                    .then()
                    .statusCode(201)
                    .body("confirmationCode", notNullValue());

            assertConfirmedCount(baseStart, "B-5", 1);
        }

        @Test
        @DisplayName("취소된 예약과 기간이 완전히 같아도 예약된다")
        void acceptsSameRangeAsCancelled() {
            LocalDate baseStart = today.plusDays(5);
            LocalDate baseEnd = today.plusDays(7);
            createCancelled("B-8", baseStart, baseEnd);

            create("취소동일", "B-8", baseStart, baseEnd)
                    .then()
                    .statusCode(201)
                    .body("confirmationCode", notNullValue());

            assertConfirmedCount(baseStart, "B-8", 1);
        }

        @Test
        @DisplayName("시작일이 취소된 예약의 종료일과 같은 날이면 예약된다")
        void acceptsRangeTouchingCancelledEnd() {
            LocalDate baseStart = today.plusDays(5);
            LocalDate baseEnd = today.plusDays(7);
            createCancelled("B-9", baseStart, baseEnd);

            create("취소뒤경계", "B-9", baseEnd, baseEnd.plusDays(1))
                    .then()
                    .statusCode(201)
                    .body("confirmationCode", notNullValue());

            assertConfirmedCount(baseEnd, "B-9", 1);
        }

        @Test
        @DisplayName("시작일이 취소된 예약의 종료일 다음날이면 예약된다")
        void acceptsRangeStartingAfterCancelled() {
            LocalDate baseStart = today.plusDays(5);
            LocalDate baseEnd = today.plusDays(7);
            createCancelled("B-10", baseStart, baseEnd);

            create("취소다음날", "B-10", baseEnd.plusDays(1), baseEnd.plusDays(2))
                    .then()
                    .statusCode(201)
                    .body("confirmationCode", notNullValue());

            assertConfirmedCount(baseEnd.plusDays(1), "B-10", 1);
        }

        @Test
        @DisplayName("당일 취소한 예약과 기간이 완전히 같아도 예약된다")
        void acceptsSameRangeAsSameDayCancelled() {
            LocalDate baseStart = today;
            LocalDate baseEnd = today.plusDays(1);
            createCancelled("B-11", baseStart, baseEnd);

            create("당일취소동일", "B-11", baseStart, baseEnd)
                    .then()
                    .statusCode(201)
                    .body("confirmationCode", notNullValue());

            assertConfirmedCount(baseStart, "B-11", 1);
        }
    }

    @Nested
    @DisplayName("생성 - 취소하지 않은 예약의 자리")
    class CreateOverConfirmedReservation {

        @Test
        @DisplayName("종료일이 확정 예약의 시작일 하루 전이면 예약된다")
        void acceptsRangeEndingBeforeConfirmed() {
            LocalDate baseStart = today.plusDays(5);
            LocalDate baseEnd = today.plusDays(7);
            create("B-14", baseStart, baseEnd);

            create("확정전날", "B-14", baseStart.minusDays(2), baseStart.minusDays(1))
                    .then()
                    .statusCode(201)
                    .body("confirmationCode", notNullValue());

            assertConfirmedCount(baseStart.minusDays(1), "B-14", 1);
        }

        @Test
        @DisplayName("종료일이 확정 예약의 시작일과 같은 날이면 거절되고 그 자리는 그대로 하나다")
        void rejectsRangeTouchingConfirmedStart() {
            LocalDate baseStart = today.plusDays(5);
            LocalDate baseEnd = today.plusDays(7);
            create("B-15", baseStart, baseEnd);

            create("확정앞경계", "B-15", baseStart.minusDays(2), baseStart)
                    .then()
                    .statusCode(409)
                    .body("message", equalTo("해당 기간에 이미 예약이 존재합니다."));

            assertConfirmedCount(baseStart, "B-15", 1);
            assertNotReserved("확정앞경계");
        }

        @Test
        @DisplayName("확정 예약과 기간이 완전히 같으면 거절되고 그 자리는 그대로 하나다")
        void rejectsSameRangeAsConfirmed() {
            LocalDate baseStart = today.plusDays(5);
            LocalDate baseEnd = today.plusDays(7);
            create("A-14", baseStart, baseEnd);

            create("확정동일", "A-14", baseStart, baseEnd)
                    .then()
                    .statusCode(409)
                    .body("message", equalTo("해당 기간에 이미 예약이 존재합니다."));

            assertConfirmedCount(baseStart, "A-14", 1);
            assertNotReserved("확정동일");
        }

        @Test
        @DisplayName("시작일이 확정 예약의 종료일과 같은 날이면 거절되고 그 자리는 그대로 하나다")
        void rejectsRangeTouchingConfirmedEnd() {
            LocalDate baseStart = today.plusDays(5);
            LocalDate baseEnd = today.plusDays(7);
            create("A-15", baseStart, baseEnd);

            create("확정뒤경계", "A-15", baseEnd, baseEnd.plusDays(1))
                    .then()
                    .statusCode(409)
                    .body("message", equalTo("해당 기간에 이미 예약이 존재합니다."));

            assertConfirmedCount(baseEnd, "A-15", 1);
            assertNotReserved("확정뒤경계");
        }

        @Test
        @DisplayName("시작일이 확정 예약의 종료일 다음날이면 예약된다")
        void acceptsRangeStartingAfterConfirmed() {
            LocalDate baseStart = today.plusDays(5);
            LocalDate baseEnd = today.plusDays(7);
            create("A-16", baseStart, baseEnd);

            create("확정다음날", "A-16", baseEnd.plusDays(1), baseEnd.plusDays(2))
                    .then()
                    .statusCode(201)
                    .body("confirmationCode", notNullValue());

            assertConfirmedCount(baseEnd.plusDays(1), "A-16", 1);
        }
    }

    @Nested
    @DisplayName("생성 - 전화번호 앞자리")
    class CreatePhoneNumberPrefix {

        @Test
        @DisplayName("앞자리가 010이면 예약된다")
        void acceptsMobilePrefix() {
            createWithPhoneNumber("앞자리010", "A-17", today.plusDays(5), "010-1234-5678")
                .then()
                .statusCode(201)
                .body("confirmationCode", notNullValue());
        }

        @Test
        @DisplayName("앞자리가 011이면 거절되고 예약이 생성되지 않는다")
        void rejectsOldMobilePrefix011() {
            createWithPhoneNumber("앞자리011", "A-19", today.plusDays(5), "011-1234-5678")
                .then()
                .statusCode(409)
                .body("message", equalTo("010으로 시작하는 휴대전화 번호만 입력 가능합니다."));

            assertNotReserved("앞자리011");
        }

        @Test
        @DisplayName("앞자리가 016이면 거절되고 예약이 생성되지 않는다")
        void rejectsOldMobilePrefix016() {
            createWithPhoneNumber("앞자리016", "A-20", today.plusDays(5), "016-1234-5678")
                .then()
                .statusCode(409)
                .body("message", equalTo("010으로 시작하는 휴대전화 번호만 입력 가능합니다."));

            assertNotReserved("앞자리016");
        }

        @Test
        @DisplayName("앞자리가 019이면 거절되고 예약이 생성되지 않는다")
        void rejectsOldMobilePrefix019() {
            createWithPhoneNumber("앞자리019", "A-5", today.plusDays(5), "019-1234-5678")
                .then()
                .statusCode(409)
                .body("message", equalTo("010으로 시작하는 휴대전화 번호만 입력 가능합니다."));

            assertNotReserved("앞자리019");
        }

        @Test
        @DisplayName("앞자리가 01로 시작하지 않으면 거절되고 예약이 생성되지 않는다")
        void rejectsNonMobilePrefix() {
            createWithPhoneNumber("앞자리020", "A-13", today.plusDays(5), "020-1234-5678")
                .then()
                .statusCode(409)
                .body("message", equalTo("010으로 시작하는 휴대전화 번호만 입력 가능합니다."));

            assertNotReserved("앞자리020");
        }
    }

    @Nested
    @DisplayName("생성 - 전화번호 자릿수")
    class CreatePhoneNumberLength {

        @Test
        @DisplayName("하이픈이 없어도 11자리면 예약된다")
        void acceptsElevenDigitsWithoutHyphen() {
            createWithPhoneNumber("자릿수하이픈없음", "A-18", today.plusDays(5), "01012345678")
                .then()
                .statusCode(201)
                .body("confirmationCode", notNullValue());
        }

        @Test
        @DisplayName("앞자리가 010인데 10자리면 형식 오류로 거절되고 예약이 생성되지 않는다")
        void rejectsTenDigits() {
            createWithPhoneNumber("자릿수10", "A-7", today.plusDays(5), "010-123-4567")
                .then()
                .statusCode(409)
                .body("message", equalTo("전화번호 형식이 올바르지 않습니다."));

            assertNotReserved("자릿수10");
        }

        @Test
        @DisplayName("앞자리가 010인데 12자리면 형식 오류로 거절되고 예약이 생성되지 않는다")
        void rejectsTwelveDigits() {
            createWithPhoneNumber("자릿수12", "A-11", today.plusDays(5), "010-12345-6789")
                .then()
                .statusCode(409)
                .body("message", equalTo("전화번호 형식이 올바르지 않습니다."));

            assertNotReserved("자릿수12");
        }
    }

    @Nested
    @DisplayName("생성 - 앞자리와 자릿수를 함께 어길 때")
    class CreatePhoneNumberPrefixBeforeLength {

        @Test
        @DisplayName("10자리 유선 서울 번호는 앞자리 사유로 거절되고 예약이 생성되지 않는다")
        void rejectsSeoulLandline() {
            createWithPhoneNumber("유선서울", "A-8", today.plusDays(5), "02-1234-5678")
                .then()
                .statusCode(409)
                .body("message", equalTo("010으로 시작하는 휴대전화 번호만 입력 가능합니다."));

            assertNotReserved("유선서울");
        }

        @Test
        @DisplayName("10자리 유선 지역 번호는 앞자리 사유로 거절되고 예약이 생성되지 않는다")
        void rejectsLocalLandline() {
            createWithPhoneNumber("유선지역", "A-9", today.plusDays(5), "031-123-4567")
                .then()
                .statusCode(409)
                .body("message", equalTo("010으로 시작하는 휴대전화 번호만 입력 가능합니다."));

            assertNotReserved("유선지역");
        }
    }
}
