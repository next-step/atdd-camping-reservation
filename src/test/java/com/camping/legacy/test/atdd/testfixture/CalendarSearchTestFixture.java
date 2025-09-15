package com.camping.legacy.test.atdd.testfixture;

import java.time.LocalDate;
import java.util.HashMap;

/**
 * 캘린더 조회 인수테스트 test fixture
 */
public class CalendarSearchTestFixture {

    public static HashMap<String, Object> createReservationRequest(String customerName, String phoneNumber, 
                                                                   String siteNumber, LocalDate startDate, LocalDate endDate) {
        var request = new HashMap<String, Object>();
        request.put("customerName", customerName);
        request.put("phoneNumber", phoneNumber);
        request.put("siteNumber", siteNumber);
        request.put("startDate", startDate.toString());
        request.put("endDate", endDate.toString());
        request.put("numberOfPeople", 4);
        request.put("carNumber", "12가3456");
        request.put("requests", "조용한 구역 부탁드립니다");
        return request;
    }

    public static HashMap<String, Object> createReservationRequest(String customerName, String phoneNumber, 
                                                                   String siteNumber, LocalDate startDate, LocalDate endDate,
                                                                   String carNumber, String requests) {
        var request = new HashMap<String, Object>();
        request.put("customerName", customerName);
        request.put("phoneNumber", phoneNumber);
        request.put("siteNumber", siteNumber);
        request.put("startDate", startDate.toString());
        request.put("endDate", endDate.toString());
        request.put("numberOfPeople", 4);
        request.put("carNumber", carNumber);
        request.put("requests", requests);
        return request;
    }
}
