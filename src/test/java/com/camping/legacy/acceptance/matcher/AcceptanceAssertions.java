package com.camping.legacy.acceptance.matcher;

import io.restassured.path.json.JsonPath;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("NonAsciiCharacters")
public final class AcceptanceAssertions {

    private AcceptanceAssertions() {
    }

    public static ResponseAssert assertThatResponse(ExtractableResponse<Response> response) {
        return new ResponseAssert(response);
    }

    @SuppressWarnings("NonAsciiCharacters")
    public static final class ResponseAssert {
        private final ExtractableResponse<Response> response;
        private final JsonPath json;

        private ResponseAssert(ExtractableResponse<Response> response) {
            this.response = response;
            this.json = response.jsonPath();
        }

        public ResponseAssert status(int expected) {
            assertThat(response.statusCode()).isEqualTo(expected);
            return this;
        }

        public ResponseAssert response(Consumer<JsonPath> assertion) {
            assertion.accept(json);
            return this;
        }
    }
}
