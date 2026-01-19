# 인수 테스트 전략

이 문서는 캠핑장 예약 시스템의 인수 테스트(Acceptance Test) 전략을 정의합니다. 모든 인수 테스트는 이 문서에 정의된 규칙과 표준을 따라야 합니다.

## 1. 핵심 기술 스택

인수 테스트는 아래의 기술 스택을 기반으로 작성합니다.

- **JUnit 5**: 테스트 실행 프레임워크의 핵심입니다. `@Test`, `@DisplayName`, `@BeforeEach` 등 다양한 어노테이션을 활용하여 테스트의 구조를 명확하게 정의합니다.
- **RestAssured**: API 엔드포인트를 테스트하기 위한 라이브러리입니다. HTTP 요청을 보내고 응답을 검증하는 과정을 단순하고 직관적인 DSL(Domain-Specific Language)로 작성할 수 있습니다.
- **AssertJ**: 테스트의 결과를 검증하기 위한 라이브러리입니다. 풍부한 API와 명료한 문법을 통해 가독성 높은 검증 코드를 작성할 수 있습니다.

## 2. 코딩 컨벤션

일관성 있는 테스트 코드를 유지하기 위해 다음 코딩 컨벤션을 준수합니다.

### 테스트 클래스 이름
- 테스트 대상이 되는 기능(Feature)이나 도메인 이름 뒤에 `AcceptanceTest` 접미사를 붙입니다.
- **예시**: `ReservationAcceptanceTest`, `SiteAcceptanceTest`

### 테스트 메소드 이름
- 테스트 시나리오를 한글로 명확하게 설명하는 방식을 사용합니다.
- "어떤_상황에서_어떤_동작을하면_어떤_결과가_나온다" 형식의 패턴을 권장합니다.
- **예시**: `예약_가능한_날짜에_캠핑장을_예약하면_예약에_성공한다()`

### 내부 주석 스타일
- 복잡한 테스트 시나리오의 경우, `given`, `when`, `then` 구조를 주석으로 명시하여 테스트의 흐름을 쉽게 파악할 수 있도록 합니다.
- 각 단계가 무엇을 준비하고, 무엇을 실행하며, 무엇을 검증하는지 명확히 표현합니다.

## 3. 파일 위치 규칙

- 모든 인수 테스트 코드는 다음 패키지 경로 하위에 위치해야 합니다.
  ```
  src/test/java/com/camping/acceptance
  ```
- 기능별로 하위 패키지를 만들어 관리할 수 있습니다.
  - **예시**: `src/test/java/com/camping/acceptance/reservation/`, `src/test/java/com/camping/acceptance/site/`

## 4. 표준 샘플 코드 (Golden Sample)

아래는 위에서 정의한 모든 규칙을 준수하는 표준 샘플 코드입니다. 새로운 인수 테스트를 작성할 때 이 코드를 참고하십시오.

```java
package com.camping.acceptance;

import com.camping.legacy.CampingApplication;
import com.camping.legacy.domain.Campsite;
import com.camping.legacy.repository.CampsiteRepository;
import com.camping.support.DatabaseCleaner;
import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 인수 테스트 표준 샘플 클래스
 * 기능: 캠핑장 예약
 */
@SuppressWarnings("NonAsciiCharacters")
@ActiveProfiles("test")
@SpringBootTest(classes = CampingApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(DatabaseCleaner.class)
@DisplayName("캠핑장 예약 인수 테스트")
public class ReservationAcceptanceTest {

    public static final String API_RESERVATIONS = "/api/reservations";

    @LocalServerPort
    private int port;

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @Autowired
    private CampsiteRepository campsiteRepository;

    private Campsite testSite;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        databaseCleaner.execute();

        // 테스트용 사이트 데이터 미리 저장
        testSite = campsiteRepository.save(Campsite.builder()
                .siteNumber("A-01")
                .description("테스트 사이트")
                .maxPeople(4)
                .build());
    }

    /**
     * 시나리오: 사용자는 원하는 날짜에 캠핑장을 예약할 수 있다.
     * given: 예약 가능한 캠핑장이 등록되어 있고,
     * when: 사용자가 예약 기간과 개인 정보를 담아 예약을 요청하면,
     * then: 예약이 성공적으로 생성되고, 생성된 예약의 위치(Location)와 함께 상태 코드 201(Created)을 반환한다.
     */
    @Test
    @DisplayName("예약_가능한_날짜에_캠핑장을_예약하면_예약에_성공한다")
    void 예약_성공() {
        // given - 예약 요청에 필요한 데이터를 생성합니다.
        var request = 예약_요청_생성(
                testSite.getSiteNumber(),
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(12),
                "홍길동",
                "010-1234-5678"
        );

        // when - 캠핑장 예약을 요청하는 API를 호출합니다.
        var response = 예약을_요청한다(request);

        // then - 예약이 성공적으로 생성되었는지 검증합니다.
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(response.header("Location")).matches("^/api/reservations/\\d+$");
    }

    // --- Helper Methods ---

    private ExtractableResponse<Response> 예약을_요청한다(Map<String, Object> request) {
        return RestAssured
                .given().log().all()
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(request)
                .when()
                    .post(API_RESERVATIONS)
                .then().log().all()
                    .extract();
    }

    private Map<String, Object> 예약_요청_생성(String siteNumber, LocalDate startDate, LocalDate endDate, String customerName, String phoneNumber) {
        Map<String, Object> request = new HashMap<>();
        request.put("siteNumber", siteNumber);
        request.put("startDate", startDate.toString());
        request.put("endDate", endDate.toString());
        request.put("customerName", customerName);
        request.put("phoneNumber", phoneNumber);
        return request;
    }
}
```