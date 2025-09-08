package com.camping.legacy.domain;

import com.camping.legacy.domain.dto.ReservationParams;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import java.util.Map;

public class ReservationRequestSender {

    private static final ObjectMapper mapper = new ObjectMapper();

    static final ExtractableResponse<Response> send(String path, ReservationParams params) {
        Map<String, String> stringObjectMap = mapper.convertValue(params, new TypeReference<Map<String, String>>() {
        });
        return RequestSender.post(path, stringObjectMap);
    }

    static final ExtractableResponse<Response> send(String path, Map<String, String> params) {
        return RequestSender.post(path, params);
    }
}
