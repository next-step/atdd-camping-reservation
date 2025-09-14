package com.camping.legacy.test.atdd.testfixture;

import java.time.LocalDateTime;
import java.util.HashMap;

import static java.time.LocalDateTime.now;

/**
 * 예약 생성 인수테스트 test fixture
 */
public class ReservationCreationTestFixture {

    public static HashMap<String, Object> defaultRequest(String customerName, String phoneNumber, String siteNumber) {
        var request = defaultRequest();
        request.put("customerName", customerName);
        request.put("phoneNumber", phoneNumber);
        request.put("siteNumber", siteNumber);
        return request;
    }

    public static HashMap<String, Object> defaultRequest(LocalDateTime startDate, LocalDateTime endDate) {
        var request = defaultRequest();
        request.put("startDate", startDate.toString());
        request.put("endDate", endDate.toString());
        return request;
    }

    public static HashMap<String, Object> defaultRequest() {
        var request = new HashMap<String, Object>();
        request.put("customerName", "홍길동");
        request.put("startDate", now().plusDays(1).toString());
        request.put("endDate", now().plusDays(10).toString());
        request.put("siteNumber", "A-1");
        request.put("phoneNumber", "010-1234-5678");
        request.put("numberOfPeople", 4);
        request.put("carNumber", "12가3456");
        request.put("requests", "조용한 구역 부탁드립니다");
        return request;
    }
}
