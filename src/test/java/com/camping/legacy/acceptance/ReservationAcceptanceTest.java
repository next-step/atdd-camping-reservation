package com.camping.legacy.acceptance;

import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.camping.legacy.acceptance.ReservationSteps.create;
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
