package camping.legacy.site;

import camping.legacy.utils.AcceptanceTest;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class SiteAcceptanceTest extends AcceptanceTest {

    public static ExtractableResponse<Response> 전체_사이트_목록_조회_요청() {
        return given().log().all()
                .when().get("/sites")
                .then().log().all().extract();
    }

    @DisplayName("전체 사이트 목록을 조회한다.")
    @Test
    void 전체_사이트_목록_조회() {
        // when
        var response = 전체_사이트_목록_조회_요청();

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    }
}
