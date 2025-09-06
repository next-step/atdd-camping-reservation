package com.camping.legacy.acceptance;

import com.camping.legacy.dto.ReservationRequest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.camping.legacy.acceptance.AcceptanceTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("동시성 제어")
class ConcurrencyControlAcceptanceTest extends AcceptanceCommon {

    @Test
    @DisplayName("여러 회원이 동일 사이트를 동일 날짜에 동시에 예약을 할 시 1명만 성공한다")
    void 동시_예약_1명만_성공() {
        // Given 특정 날짜에 예약되지 않은 사이트가 존재한다.
        ReservationRequest request = createReservationRequest();
        ReservationRequest sameRequest = createSameReservationRequest();

        // When 동시에 동일 사이트 동일 날짜 예약을 수행한다.
        ExtractableResponse<Response> response = getCreateReservationApiResponse(request);
        ExtractableResponse<Response> sameResponse = getCreateReservationApiResponse(sameRequest);

        // Then 1명은 성공하고, 나머지는 "해당 사이트는 이미 예약되었습니다" 오류 메시지가 반환된다.
        assertThat(response.statusCode()).isEqualTo(201);
        JsonPath jsonPath = response.jsonPath();
        assertThat(jsonPath.getLong("id")).isPositive();
        assertThat(jsonPath.getString("status")).isEqualTo("CONFIRMED");
        assertThat(jsonPath.getString("confirmationCode")).hasSize(6);
        assertThat(jsonPath.getString("confirmationCode")).matches("[A-Z0-9]+");

        assertThat(sameResponse.statusCode()).isEqualTo(409);
        assertThat(sameResponse.body().asString()).contains("해당 기간에 이미 예약이 존재합니다.");
    }

    @Test
    @DisplayName("여러 회원이 동일 사이트를 취소된 예약이 존재하는 동일 날짜에 동시에 예약을 할 시 1명만 성공한다")
    void 동시_예약_취소된_예약_존재_시_1명만_성공() {
        // Given 특정 날짜의 취소된 예약이 존재하는 사이트가 존재한다.
        ReservationRequest request = createCancelledReservationRequest();
        ReservationRequest sameRequest = createCancelledSameReservationRequest();

        // When 동시에 동일 사이트 동일 날짜 예약을 수행한다.
        ExtractableResponse<Response> response = getCreateReservationApiResponse(request);
        ExtractableResponse<Response> sameResponse = getCreateReservationApiResponse(sameRequest);

        // Then 1명은 성공하고, 나머지는 "해당 사이트는 이미 예약되었습니다" 오류 메시지가 반환된다.
        assertThat(response.statusCode()).isEqualTo(201);
        JsonPath jsonPath = response.jsonPath();
        assertThat(jsonPath.getLong("id")).isPositive();
        assertThat(jsonPath.getString("status")).isEqualTo("CONFIRMED");
        assertThat(jsonPath.getString("confirmationCode")).hasSize(6);
        assertThat(jsonPath.getString("confirmationCode")).matches("[A-Z0-9]+");

        assertThat(sameResponse.statusCode()).isEqualTo(409);
        assertThat(sameResponse.body().asString()).contains("해당 기간에 이미 예약이 존재합니다.");
    }

    @Test
    @DisplayName("실제 멀티스레드 동시성 테스트 - 10개 스레드가 동일 사이트 예약 시도")
    void 실제_멀티스레드_동시성_테스트_10개_스레드() throws InterruptedException {
        // Given 10개의 스레드가 동시에 같은 사이트 예약을 시도한다
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<ExtractableResponse<Response>> responses = Collections.synchronizedList(new ArrayList<>());

        // When 모든 스레드가 동시에 예약 요청을 보낸다
        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i;
            executorService.submit(() -> {
                try {
                    startLatch.await(); // 모든 스레드가 동시에 시작되도록 대기
                    
                    ReservationRequest request = ReservationRequestBuilder.builder()
                            .name("동시고객" + threadNum)
                            .phoneNumber("010-" + String.format("%04d", threadNum) + "-" + String.format("%04d", threadNum))
                            .build();
                    
                    ExtractableResponse<Response> response = getCreateReservationApiResponse(request);
                    responses.add(response);
                    
                    if (response.statusCode() == 201) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                    
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // 모든 스레드를 동시에 시작
        endLatch.await(30, TimeUnit.SECONDS); // 모든 스레드 완료 대기
        executorService.shutdown();

        // Then 정확히 1명만 성공하고 나머지는 실패해야 한다
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(threadCount - 1);

        // 성공한 응답 검증
        ExtractableResponse<Response> successResponse = responses.stream()
                .filter(response -> response.statusCode() == 201)
                .findFirst()
                .orElseThrow();
        
        JsonPath jsonPath = successResponse.jsonPath();
        assertThat(jsonPath.getLong("id")).isPositive();
        assertThat(jsonPath.getString("status")).isEqualTo("CONFIRMED");
        assertThat(jsonPath.getString("confirmationCode")).hasSize(6);

        // 실패한 응답들 검증
        long conflictCount = responses.stream()
                .filter(response -> response.statusCode() == 409)
                .count();
        assertThat(conflictCount).isEqualTo(threadCount - 1);
    }

    @RepeatedTest(3)
    @DisplayName("반복 동시성 테스트 - 경쟁 조건 재현 확률 높이기")
    void 반복_동시성_테스트_경쟁_조건_재현() throws InterruptedException {
        // Given 많은 스레드가 동시에 예약을 시도한다
        int threadCount = 20;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger otherErrorCount = new AtomicInteger(0);

        // When 모든 스레드가 동시에 예약 요청을 보낸다
        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    
                    ReservationRequest request = ReservationRequestBuilder.builder()
                            .name("반복고객" + threadNum)
                            .phoneNumber("010-" + String.format("%04d", threadNum + 1000) + "-" + String.format("%04d", threadNum))
                            .build();
                    
                    ExtractableResponse<Response> response = getCreateReservationApiResponse(request);
                    
                    if (response.statusCode() == 201) {
                        successCount.incrementAndGet();
                    } else if (response.statusCode() == 409 && 
                              response.body().asString().contains("해당 기간에 이미 예약이 존재합니다")) {
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

        // Then 정확히 1명만 성공하고 나머지는 충돌 오류여야 한다
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(threadCount - 1);
        assertThat(otherErrorCount.get()).isEqualTo(0);
    }

    @Test
    @DisplayName("다른 사이트 동시 예약 테스트 - 서로 다른 사이트는 모두 성공해야 함")
    void 다른_사이트_동시_예약_모두_성공() throws InterruptedException {
        // Given 5개 스레드가 서로 다른 사이트에 예약을 시도한다
        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // When 각각 다른 사이트에 동시 예약 요청
        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    
                    ReservationRequest request = ReservationRequestBuilder.builder()
                            .name("다른사이트고객" + threadNum)
                            .siteName("B-" + (threadNum + 1)) // B-1, B-2, B-3, B-4, B-5
                            .phoneNumber("010-" + String.format("%04d", threadNum + 2000) + "-" + String.format("%04d", threadNum))
                            .build();
                    
                    ExtractableResponse<Response> response = getCreateReservationApiResponse(request);
                    
                    if (response.statusCode() == 201) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                    
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // Then 서로 다른 사이트이므로 모두 성공해야 한다
        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(failureCount.get()).isEqualTo(0);
    }

//    @Test
//    @DisplayName("대용량 동시성 스트레스 테스트 - 50개 스레드로 5개 사이트 경쟁")
//    void 대용량_동시성_스트레스_테스트() throws InterruptedException {
//        // Given 50개 스레드가 5개 사이트에 분산되어 예약을 시도한다
//        int threadCount = 50;
//        int siteCount = 5;
//        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
//        CountDownLatch startLatch = new CountDownLatch(1);
//        CountDownLatch endLatch = new CountDownLatch(threadCount);
//
//        AtomicInteger successCount = new AtomicInteger(0);
//        AtomicInteger conflictCount = new AtomicInteger(0);
//        AtomicInteger otherErrorCount = new AtomicInteger(0);
//
//        // When 스레드들이 사이트별로 분산되어 동시 요청
//        for (int i = 0; i < threadCount; i++) {
//            final int threadNum = i;
//            executorService.submit(() -> {
//                try {
//                    startLatch.await();
//
//                    // 스레드를 사이트별로 분산 (각 사이트마다 10개 스레드가 경쟁)
//                    int siteNum = (threadNum % siteCount) + 1;
//
//                    ReservationRequest request = ReservationRequestBuilder.builder()
//                            .name("스트레스고객" + threadNum)
//                            .siteName("C-" + siteNum)
//                            .phoneNumber("010-" + String.format("%04d", threadNum + 3000) + "-" + String.format("%04d", threadNum))
//                            .build();
//
//                    ExtractableResponse<Response> response = getCreateReservationApiResponse(request);
//
//                    if (response.statusCode() == 201) {
//                        successCount.incrementAndGet();
//                    } else if (response.statusCode() == 409) {
//                        conflictCount.incrementAndGet();
//                    } else {
//                        otherErrorCount.incrementAndGet();
//                    }
//
//                } catch (Exception e) {
//                    otherErrorCount.incrementAndGet();
//                } finally {
//                    endLatch.countDown();
//                }
//            });
//        }
//
//        startLatch.countDown();
//        endLatch.await(60, TimeUnit.SECONDS);
//        executorService.shutdown();
//
//        // Then 사이트 개수만큼만 성공하고 나머지는 충돌 오류여야 한다
//        assertThat(successCount.get()).isEqualTo(siteCount);
//        assertThat(conflictCount.get()).isEqualTo(threadCount - siteCount);
//        assertThat(otherErrorCount.get()).isEqualTo(0);
//    }

    @Test
    @DisplayName("예약과 취소 동시 실행 테스트")
    void 예약과_취소_동시_실행_테스트() throws InterruptedException {
        // Given 기존 예약이 하나 존재한다
        ExtractableResponse<Response> existingReservation = 예약_생성_성공();
        String confirmationCode = existingReservation.jsonPath().getString("confirmationCode");
        Long reservationId = existingReservation.jsonPath().getLong("id");
        
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(2);
        
        AtomicInteger cancelSuccessCount = new AtomicInteger(0);
        AtomicInteger reservationSuccessCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // When 취소와 새 예약이 동시에 실행된다
        // 취소 스레드
        executorService.submit(() -> {
            try {
                startLatch.await();
                
                ExtractableResponse<Response> cancelResponse = RestAssured
                        .given().log().all()
                        .param("confirmationCode", confirmationCode)
                        .when()
                        .delete("/api/reservations/" + reservationId)
                        .then().log().all()
                        .extract();
                
                if (cancelResponse.statusCode() == 200) {
                    cancelSuccessCount.incrementAndGet();
                } else {
                    failureCount.incrementAndGet();
                }
                
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                endLatch.countDown();
            }
        });
        
        // 새 예약 스레드 (동일한 사이트, 동일한 날짜)
        executorService.submit(() -> {
            try {
                startLatch.await();
                Thread.sleep(100); // 취소가 먼저 실행되도록 약간의 지연
                
                ReservationRequest newRequest = ReservationRequestBuilder.builder()
                        .name("새고객")
                        .phoneNumber("010-9999-9999")
                        .build();
                
                ExtractableResponse<Response> newReservationResponse = getCreateReservationApiResponse(newRequest);
                
                if (newReservationResponse.statusCode() == 201) {
                    reservationSuccessCount.incrementAndGet();
                } else {
                    failureCount.incrementAndGet();
                }
                
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                endLatch.countDown();
            }
        });

        startLatch.countDown();
        endLatch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // Then 취소와 새 예약 모두 성공해야 한다
        assertThat(cancelSuccessCount.get()).isEqualTo(1);
        assertThat(reservationSuccessCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(0);
    }
}