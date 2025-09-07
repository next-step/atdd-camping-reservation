package com.camping.legacy.domain;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import java.util.Map;

public class SiteRequestSender {

    static ExtractableResponse<Response> send(String path, Map<String, String> params) {
        return RequestSender.get(path, params);
    }
}
