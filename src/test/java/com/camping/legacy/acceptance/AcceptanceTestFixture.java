package com.camping.legacy.acceptance;

import com.camping.legacy.dto.ReservationRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

public class AcceptanceTestFixture {
    private static final LocalDate now = LocalDate.now();
    private static final LocalDate startDate = now.plusDays(20);

    public static ReservationRequest createReservationRequest() {
        return ReservationRequestBuilder.builder()
                .build();
    }

    public static ReservationRequest createReservationRequest(int day) {
        return ReservationRequestBuilder.builder()
                .startDate(now.plusDays(day))
                .endDate(now.plusDays(day))
                .build();
    }

    public static ReservationRequest createSameReservationRequest() {
        return ReservationRequestBuilder.builder()
                .name("박철수")
                .phoneNumber("010-9876-5432")
                .build();
    }

    public static ReservationRequest createCancelledReservationRequest() {
        예약_취소_성공();
        return ReservationRequestBuilder.builder()
                .build();
    }

    public static ReservationRequest createCancelledSameReservationRequest() {
        예약_취소_성공();
        return ReservationRequestBuilder.builder()
                .name("박철수")
                .phoneNumber("010-9876-5432")
                .build();
    }

    public static ReservationRequest createWrongReservationRequest() {
        return ReservationRequestBuilder.builder()
                .startDate(now.plusDays(31))
                .endDate(now.plusDays(31))
                .build();
    }

    public static ReservationRequest createBookedReservationRequest() {
        return ReservationRequestBuilder.builder()
                .build();
    }

    public static ExtractableResponse<Response> 예약_생성_성공() {
        ReservationRequest request = createReservationRequest();

        ExtractableResponse<Response> response = RestAssured
                .given().log().all()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/reservations")
                .then().log().all()
                .extract();

        assertThat(response.statusCode()).isEqualTo(201);
        return response;
    }

    public static ExtractableResponse<Response> 예약_취소_성공() {
        ExtractableResponse<Response> successResponse = 예약_생성_성공();

        ExtractableResponse<Response> response = RestAssured
                .given().log().all()
                .contentType(ContentType.JSON)
                .param("confirmationCode", successResponse.jsonPath().getString("confirmationCode"))
                .when()
                .delete("/api/reservations/" + successResponse.jsonPath().getLong("id"))
                .then().log().all()
                .extract();

        assertThat(response.statusCode()).isEqualTo(200);
        return response;
    }

    public static ReservationRequest createConsecutiveReservationRequest() {
        예약_취소_성공();
        return ReservationRequestBuilder.builder()
                .name("김연박")
                .endDate(startDate.plusDays(2))
                .siteName("B-2")
                .phoneNumber("010-1111-2222")
                .numberOfPeople(4)
                .build();
    }

    public static ReservationRequest createConsecutiveWithCancelledReservationRequest() {
        예약_취소_성공();
        return ReservationRequestBuilder.builder()
                .name("박연박")
                .endDate(startDate.plusDays(2))
                .siteName("B-2")
                .phoneNumber("010-3333-4444")
                .numberOfPeople(3)
                .build();
    }

    public static ReservationRequest createBlockedConsecutiveReservationRequest() {
        return ReservationRequestBuilder.builder()
                .name("이연박")
                .startDate(now.plusDays(5))
                .endDate(now.plusDays(7))
                .phoneNumber("010-5555-6666")
                .build();
    }

    public static ReservationRequest createExistingReservationInConsecutivePeriod() {
        return ReservationRequestBuilder.builder()
                .name("최기존")
                .startDate(now.plusDays(6))
                .endDate(now.plusDays(6))
                .phoneNumber("010-7777-8888")
                .build();
    }

    public static ExtractableResponse<Response> getCreateReservationApiResponse(ReservationRequest request) {
        return RestAssured
                .given().log().all()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/reservations")
                .then().log().all()
                .extract();
    }

    public record ConcurrentTestResult(
            int successCount,
            int conflictCount,
            int otherErrorCount,
            List<ExtractableResponse<Response>> responses
    ) {}

    public static ConcurrentTestResult executeConcurrentTest(
            int threadCount,
            Function<Integer, ReservationRequest> requestBuilder
    ) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger otherErrorCount = new AtomicInteger(0);
        List<ExtractableResponse<Response>> responses = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    ReservationRequest request = requestBuilder.apply(threadNum);
                    ExtractableResponse<Response> response = getCreateReservationApiResponse(request);
                    responses.add(response);

                    if (response.statusCode() == 201) {
                        successCount.incrementAndGet();
                    } else if (response.statusCode() == 409) {
                        conflictCount.incrementAndGet();
                    } else {
                        otherErrorCount.incrementAndGet();
                    }

                } catch (Exception e) {
                    otherErrorCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        return new ConcurrentTestResult(
                successCount.get(),
                conflictCount.get(),
                otherErrorCount.get(),
                responses
        );
    }
}
