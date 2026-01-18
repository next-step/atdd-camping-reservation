package com.camping.legacy;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.repository.CampsiteRepository;
import com.camping.legacy.repository.ReservationRepository;
import com.camping.legacy.utils.DatabaseCleaner;
import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AcceptanceTest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected DatabaseCleaner databaseCleaner;

    @Autowired
    protected CampsiteRepository campsiteRepository;

    @Autowired
    protected ReservationRepository reservationRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        databaseCleaner.execute();
        campsiteRepository.save(new Campsite("A-1", "Large Site", 6));
        campsiteRepository.save(new Campsite("B-1", "Small Site", 4));
    }

    public static void 예약_성공_확인(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    }

    public static void 예약이_거부되었다(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    public static void 예약이_확정된_상태이다(ExtractableResponse<Response> response, String expected) {
        assertThat(response.jsonPath().getString("status")).isEqualTo(expected);
    }

    protected void 에러메시지가_확인된다(ExtractableResponse<Response> response, String expected) {
        assertThat(response.jsonPath().getString("message")).isEqualTo(expected);
    }

    public static void 예약_확인코드가_발급되었다(ExtractableResponse<Response> response) {
        String code = response.jsonPath().getString("confirmationCode");
        assertThat(code).isNotNull();
        assertThat(code).hasSize(6);
        assertThat(code).matches("^[A-Z0-9]*$");
    }

    public static void 검색_결과에_해당_사이트가_포함된다(
            ExtractableResponse<Response> response, String... siteNumbers) {
        List<String> resultNumbers = response.jsonPath().getList("siteNumber");
        assertThat(resultNumbers).contains(siteNumbers);
    }

    public static void 모든_사이트는_이용가능한_상태이다(ExtractableResponse<Response> response) {
        List<Boolean> availability = response.jsonPath().getList("available");
        assertThat(availability).allMatch(available -> available);
    }

    public static void 검색_결과에_특정_사이트는_포함되지_않는다(
            ExtractableResponse<Response> response, String siteNumber) {
        List<String> siteNumbers = response.jsonPath().getList("siteNumber");
        assertThat(siteNumbers).doesNotContain(siteNumber);
    }

    public static void 검색_결과에_특정_사이트만_포함된다(
            ExtractableResponse<Response> response, String siteNumber) {
        List<String> siteNumbers = response.jsonPath().getList("siteNumber");
        assertThat(siteNumbers).hasSize(1);
        assertThat(siteNumbers).containsExactly(siteNumber);
    }

    public static void 검색_요청이_거부되었다(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    public static void 검색_결과에는_최대인원과_전기가능여부가_포함되야한다(ExtractableResponse<Response> response) {
        List<Integer> maxPeopleList = response.jsonPath().getList("maxPeople");
        List<Boolean> electricityList = response.jsonPath().getList("hasElectricity");

        assertThat(maxPeopleList).allMatch(people -> people > 0);
        assertThat(electricityList).allMatch(Objects::nonNull);
    }

    public static void 예약_취소가_성공했다(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    }

    public static void 예약_취소가_거부되었다(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.jsonPath().getString("message")).isNotNull();
    }

    public static void 예약_상태_확인(ExtractableResponse<Response> response, String expectedStatus) {
        String status = response.jsonPath().getString("[0].status");
        assertThat(status).isEqualTo(expectedStatus);
    }
}
