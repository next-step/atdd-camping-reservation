package com.camping.legacy.test.utils;

import io.restassured.response.Response;
import org.springframework.http.HttpStatus;

public class ResponseUtils {

    /**
     * 응답이 성공(2xx)인지 확인한다.
     */
    public static boolean isSuccessful(Response response) {
        return HttpStatus.valueOf(response.statusCode()).is2xxSuccessful();
    }
}
