package com.camping.legacy.test.atdd.testfixture;

import java.time.LocalDateTime;
import java.util.HashMap;

import static java.time.LocalDateTime.now;

/**
 * 예약 수정 인수테스트 test fixture
 */
public class ReservationModificationTestFixture {

    public static HashMap<String, Object> modificationRequest(String siteNumber,
                                                              LocalDateTime startDate,
                                                              LocalDateTime endDate,
                                                              String customerName,
                                                              String phoneNumber) {
        var request = new HashMap<String, Object>();
        request.put("siteNumber", siteNumber);
        request.put("startDate", startDate.toLocalDate().toString());
        request.put("endDate", endDate.toLocalDate().toString());
        request.put("customerName", customerName);
        request.put("phoneNumber", phoneNumber);
        return request;
    }

    public static HashMap<String, Object> modificationRequest(String siteNumber,
                                                              LocalDateTime startDate,
                                                              LocalDateTime endDate) {
        var request = new HashMap<String, Object>();
        request.put("siteNumber", siteNumber);
        request.put("startDate", startDate.toLocalDate().toString());
        request.put("endDate", endDate.toLocalDate().toString());
        return request;
    }

    public static HashMap<String, Object> modificationRequest(LocalDateTime startDate,
                                                              LocalDateTime endDate) {
        var request = new HashMap<String, Object>();
        request.put("startDate", startDate.toLocalDate().toString());
        request.put("endDate", endDate.toLocalDate().toString());
        return request;
    }


    public static HashMap<String, Object> modificationRequest(String siteNumber) {
        var request = new HashMap<String, Object>();
        request.put("siteNumber", siteNumber);
        return request;
    }

    public static HashMap<String, Object> defaultModificationRequest() {
        var request = new HashMap<String, Object>();
        request.put("siteNumber", "A-2");
        request.put("startDate", now().plusDays(10).toLocalDate().toString());
        request.put("endDate", now().plusDays(12).toLocalDate().toString());
        return request;
    }

}
