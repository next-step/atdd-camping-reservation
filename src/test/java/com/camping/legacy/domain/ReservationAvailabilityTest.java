package com.camping.legacy.domain;

import com.camping.legacy.domain.dto.ReservationParams;
import com.camping.legacy.dto.SiteAvailabilityResponse;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.camping.legacy.domain.ReservationTest.API_RESERVATIONS;
import static org.assertj.core.api.Assertions.assertThat;

@Sql(scripts = "/cleanup-and-reinit.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ReservationAvailabilityTest {

    public static final String API_SITES_AVAILABLE = "/api/sites/available";
    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("동검색한 기간 전체에 걸쳐 예약 가능한 사이트만 조회된다")
    void 검색한_기간_전체에_걸쳐_예약_가능한_사이트만_조회된다() {
        // NOTE: 현재 로직은 검색 기간의 시작일에 예약 가능한 경우만 고려됨.
        //   따라서 (시작일, 종료일) 사이에 예약이 존재하는 경우 전체 기간 동안의 예약 가능 여부를 정확히 반영하도록 로직 개선 필요

        // given
        String siteName = "A-11";
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(4);
        final var params = ReservationParams.ofWithSiteNumber(siteName, startDate, endDate);
        Map<String, String> availableRequest = Map.of(
                "date", startDate.plusDays(1).toString()
        );


        // when
        ExtractableResponse<Response> response = ReservationRequestSender.send(API_RESERVATIONS, params);
        ExtractableResponse<Response> availabilityResponse = SiteRequestSender.send(API_SITES_AVAILABLE, availableRequest);


        List<SiteAvailabilityResponse> siteAvailabilityResponses =
                availabilityResponse.as(new TypeRef<List<SiteAvailabilityResponse>>() {});

        Boolean isExist = siteAvailabilityResponses.stream()
                .anyMatch(siteAvailabilityResponse -> siteAvailabilityResponse.getSiteNumber().equals(siteName) && siteAvailabilityResponse.getAvailable());

        // then
        Assertions.assertAll(
                () ->assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value()),
                () ->assertThat(availabilityResponse.statusCode()).isEqualTo(HttpStatus.OK.value()),
                () ->assertThat(isExist).isFalse()
        );
    }
}
