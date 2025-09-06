package com.camping.legacy;


import com.camping.legacy.dto.ReservationRequest;
import com.camping.legacy.repository.ReservationRepository;
import com.camping.legacy.stub.ReservationRequestTestDataBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

//@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD) -> 현재 단계에서는 필요없고, 속도를 느리게 하기 때문에 주석 처리
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class TestBase {

    @LocalServerPort
    protected int port;

    @Autowired
    ReservationRepository reservationRepository;

    protected void clearDB() {
        reservationRepository.deleteAll();
    }

    protected Long createReservationAndGetId(String customerName, String siteNumber, LocalDate startDate, LocalDate endDate) {
        ReservationRequest req = ReservationRequestTestDataBuilder.get(
                customerName, siteNumber, startDate, endDate
        );
        Response res = given().log().all()
                .contentType(ContentType.JSON)
                .body(req)
                .post("/api/reservations");
        res.then().log().all()
                .statusCode(201)
                .body("id", notNullValue());
        ShareContext.CONFIRMMATION_CODE = res.jsonPath().getString("confirmationCode");
        return res.jsonPath().getLong("id");
    }
}
