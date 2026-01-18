# 인수 테스트 전략 (Acceptance Test Strategy)

이 문서는 캠핑 예약 시스템의 인수 테스트 전략과 코딩 규칙을 정의합니다.

---

## 1. 핵심 기술 스택

| 구분 | 기술 | 버전 | 용도 |
|------|------|------|------|
| **테스트 프레임워크** | JUnit 5 | 5.10.x | 테스트 실행 및 라이프사이클 관리 |
| **API 테스트** | RestAssured | 5.4.x | REST API 인수 테스트 |
| **Assertions** | AssertJ | 3.24.x | 가독성 높은 검증 구문 |
| **Spring 테스트** | Spring Boot Test | 3.2.x | 통합 테스트 환경 |
| **데이터베이스** | H2 (in-memory) | 2.x | 테스트용 인메모리 DB |

### build.gradle 의존성

```groovy
dependencies {
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'io.rest-assured:rest-assured'
}
```

---

## 2. 파일 위치 규칙

```
src/
├── main/java/com/camping/legacy/
│   ├── controller/
│   ├── service/
│   ├── domain/
│   └── dto/
│
└── test/java/com/camping/
    ├── acceptance/                    # 인수 테스트 (API 레벨)
    │   ├── ReservationAcceptanceTest.java
    │   ├── CalendarAcceptanceTest.java
    │   └── common/
    │       ├── AcceptanceTest.java    # 공통 설정 상속 클래스
    │       ├── DatabaseCleanup.java   # DB 초기화 유틸
    │       └── TestFixture.java       # 테스트 데이터 헬퍼
    │
    ├── integration/                   # 통합 테스트 (서비스 레벨)
    └── unit/                          # 단위 테스트
```

---

## 3. 코딩 컨벤션

### 3.1 테스트 클래스 명명 규칙

| 테스트 유형 | 클래스명 패턴 | 예시 |
|------------|--------------|------|
| 인수 테스트 | `{기능}AcceptanceTest` | `ReservationAcceptanceTest` |
| 통합 테스트 | `{대상}IntegrationTest` | `ReservationServiceIntegrationTest` |
| 단위 테스트 | `{대상}Test` | `PriceCalculatorTest` |

### 3.2 테스트 메서드 명명 규칙

**패턴**: 한글로 `행위_조건_결과` 형식 사용

```java
// Good
@Test
void 예약_생성_성공() { }

@Test
void 중복_예약_시도시_예외_발생() { }

// Bad
@Test
void test1() { }

@Test
void createReservationTest() { }
```

### 3.3 테스트 메서드 구조 (Given-When-Then)

```java
@Test
void 예약_생성_성공() {
    // given - 테스트 사전 조건
    사이트_등록("A-1", 6);

    // when - 테스트 대상 행위
    ExtractableResponse<Response> response = 예약_생성_요청(
        "A-1", "2024-08-15", "2024-08-17", "홍길동", 4
    );

    // then - 결과 검증
    assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
}
```

### 3.4 주석 스타일

```java
/**
 * Feature: 중복 예약 방지
 *
 * @see docs/4.scenarios.feature - "중복 예약 방지" 섹션 참조
 */
class ReservationAcceptanceTest extends AcceptanceTest {

    /**
     * Scenario: [정상] 예약이 없는 기간에 정상적으로 예약 생성
     */
    @Test
    @DisplayName("[정상] 예약이 없는 기간에 정상적으로 예약 생성")
    void 예약_생성_성공() {
        // given - 사이트 "A-1"이 등록되어 있다

        // when - "김철수"가 예약을 요청한다

        // then - 예약이 성공적으로 생성된다
    }
}
```

### 3.5 헬퍼 메서드 규칙

- **한글 메서드명 사용**: `예약_생성_요청()`, `사이트_등록()`
- **static import 활용**: RestAssured의 given(), when(), then()

---

## 4. 골든 샘플 코드 (Golden Sample)

### 4.1 공통 설정 클래스

```java
package com.camping.acceptance.common;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AcceptanceTest {

    @LocalServerPort
    int port;

    @Autowired
    private DatabaseCleanup databaseCleanup;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        databaseCleanup.execute();
    }
}
```

### 4.2 테스트 픽스처

```java
package com.camping.acceptance.common;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.http.MediaType;
import java.util.HashMap;
import java.util.Map;

public class TestFixture {

    public static ExtractableResponse<Response> 사이트_등록_요청(String siteNumber, int maxPeople) {
        Map<String, Object> params = new HashMap<>();
        params.put("siteNumber", siteNumber);
        params.put("description", siteNumber + " 사이트");
        params.put("maxPeople", maxPeople);

        return RestAssured
                .given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(params)
                .when().post("/api/sites")
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 예약_생성_요청(
            String siteNumber, String startDate, String endDate,
            String customerName, int numberOfPeople) {

        Map<String, Object> params = new HashMap<>();
        params.put("siteNumber", siteNumber);
        params.put("startDate", startDate);
        params.put("endDate", endDate);
        params.put("customerName", customerName);
        params.put("numberOfPeople", numberOfPeople);
        params.put("phoneNumber", "010-1234-5678");

        return RestAssured
                .given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(params)
                .when().post("/api/reservations")
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 예약_취소_요청(Long id, String confirmationCode) {
        return RestAssured
                .given().log().all()
                .queryParam("confirmationCode", confirmationCode)
                .when().delete("/api/reservations/{id}", id)
                .then().log().all()
                .extract();
    }
}
```

### 4.3 예약 인수 테스트 (완전한 예시)

```java
package com.camping.acceptance;

import com.camping.acceptance.common.AcceptanceTest;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static com.camping.acceptance.common.TestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: 중복 예약 방지
 *
 * @see docs/4.scenarios.feature
 */
@DisplayName("예약 생성 인수 테스트")
class ReservationAcceptanceTest extends AcceptanceTest {

    @BeforeEach
    void setUpFixture() {
        // Background: 사이트 "A-1"이 등록되어 있다
        사이트_등록_요청("A-1", 6);
    }

    @Nested
    @DisplayName("정상 케이스")
    class HappyPath {

        @Test
        @DisplayName("[정상] 예약이 없는 기간에 정상적으로 예약 생성")
        void 예약_생성_성공() {
            // given - "A-1" 사이트의 해당 기간에 예약이 없다

            // when - "김철수"가 예약을 요청한다
            ExtractableResponse<Response> response = 예약_생성_요청(
                    "A-1", "2024-08-15", "2024-08-17", "김철수", 4
            );

            // then - 예약이 성공적으로 생성된다
            assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
            assertThat(response.jsonPath().getString("confirmationCode")).hasSize(8);
        }
    }

    @Nested
    @DisplayName("예외 케이스")
    class ExceptionCase {

        @Test
        @DisplayName("[예외] 동일 기간 동일 사이트 중복 예약 거부")
        void 중복_예약_시도시_예외_발생() {
            // given - "A-1" 사이트에 "홍길동"의 예약이 있다
            예약_생성_요청("A-1", "2024-08-15", "2024-08-17", "홍길동", 4);

            // when - "김철수"가 같은 기간에 예약 시도한다
            ExtractableResponse<Response> response = 예약_생성_요청(
                    "A-1", "2024-08-15", "2024-08-17", "김철수", 4
            );

            // then - 예약이 거부된다
            assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(response.jsonPath().getString("message"))
                    .isEqualTo("해당 기간에 이미 예약이 존재합니다");
        }

        @Test
        @DisplayName("[예외] 일부 기간이 겹치는 예약 거부")
        void 기간_겹침_예약_시도시_예외_발생() {
            // given - "A-1" 사이트에 예약이 있다 (8/15 ~ 8/17)
            예약_생성_요청("A-1", "2024-08-15", "2024-08-17", "홍길동", 4);

            // when - 겹치는 기간에 예약 시도한다 (8/16 ~ 8/18)
            ExtractableResponse<Response> response = 예약_생성_요청(
                    "A-1", "2024-08-16", "2024-08-18", "김철수", 4
            );

            // then - 예약이 거부된다
            assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        }
    }
}
```

---

## 5. 체크리스트

테스트 코드 작성 시 확인:

- [ ] `AcceptanceTest` 상속 여부
- [ ] 한글 메서드명 + `행위_조건_결과` 패턴
- [ ] `@DisplayName`에 `[정상]`/`[예외]` 명시
- [ ] Given-When-Then 주석 작성
- [ ] 헬퍼 메서드로 중복 제거
- [ ] AssertJ 사용
- [ ] `.feature` 시나리오와 1:1 대응