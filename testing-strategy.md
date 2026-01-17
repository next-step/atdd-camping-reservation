# 인수 테스트 전략 (Acceptance Test Strategy)

> 캠핑 예약 시스템의 인수 테스트 작성 가이드

---

## 1. 핵심 기술 스택

| 기술 | 버전 | 용도 |
|-----|-----|-----|
| **JUnit 5** | 5.10.x | 테스트 프레임워크 |
| **RestAssured** | 5.4.x | REST API 테스트 |
| **AssertJ** | 3.24.x | Fluent Assertions |
| **Spring Boot Test** | 3.2.0 | 통합 테스트 지원 |
| **H2 Database** | - | 테스트용 인메모리 DB |

---

## 2. 테스트 환경 구성

### 2.1 의존성 추가 (build.gradle)

```groovy
dependencies {
    // RestAssured
    testImplementation 'io.rest-assured:rest-assured'

    // Spring Boot Test (RestAssured 버전 관리 포함)
    testImplementation 'org.springframework.boot:spring-boot-starter-test'

    // H2 Database
    testRuntimeOnly 'com.h2database:h2'
}
```

### 2.2 RestAssured 환경 설정

| 설정 | 위치 | 설명 |
|-----|-----|-----|
| `port` | AcceptanceTest.setUp() | 랜덤 포트를 RestAssured에 전달 |
| `baseURI` | 기본값 사용 | `http://localhost` (변경 불필요) |
| `logging` | Steps 클래스 | `.log().all()`로 요청/응답 로깅 |

```java
@BeforeEach
void setUp() {
    RestAssured.port = port;           // 랜덤 포트 설정
    // RestAssured.baseURI = "http://localhost";  // 기본값이므로 생략 가능
    databaseCleanup.execute();
}
```

### 2.3 테스트 데이터 초기화 전략

**원칙**: 각 테스트는 독립적으로 실행되어야 한다.

| 단계 | 시점 | 동작 |
|-----|-----|-----|
| 1 | `@BeforeEach` | 모든 테이블 TRUNCATE |
| 2 | 테스트 실행 | 필요한 데이터만 Steps로 생성 |
| 3 | 테스트 종료 | 별도 정리 불필요 (다음 테스트가 초기화) |

```java
// DatabaseCleanup.java - 핵심 로직
entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
for (String tableName : tableNames) {
    entityManager.createNativeQuery("TRUNCATE TABLE " + tableName).executeUpdate();
}
entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
```

---

## 3. 파일 위치 규칙

```
src/test/java/com/camping/acceptance/
├── common/                         # 공통 유틸리티
│   ├── AcceptanceTest.java         # 베이스 클래스
│   └── DatabaseCleanup.java        # DB 초기화
├── reservation/                    # 예약 도메인
│   ├── ReservationAcceptanceTest.java
│   └── ReservationSteps.java       # API 호출 헬퍼
└── site/                           # 사이트 도메인
    ├── SiteAcceptanceTest.java
    └── SiteSteps.java
```

| 유형 | 패턴 | 예시 |
|-----|-----|-----|
| 인수 테스트 클래스 | `{Domain}AcceptanceTest` | `ReservationAcceptanceTest` |
| API 호출 헬퍼 | `{Domain}Steps` | `ReservationSteps` |

---

## 4. 코딩 컨벤션

### 4.1 테스트 메소드 명명

```java
@Test
@DisplayName("한글 시나리오 설명")
void 한글_시나리오_설명() { }
```

### 4.2 Given-When-Then 구조

```java
@Test
void 정상_요청시_성공한다() {
    // given
    var request = createRequest();

    // when
    var response = requestApi(request);

    // then
    assertThat(response.statusCode()).isEqualTo(201);
}
```

### 4.3 Assertion 스타일

```java
// Good - AssertJ
assertThat(response.statusCode()).isEqualTo(201);
assertThat(list).hasSize(2).extracting("name").contains("A", "B");

// Bad - JUnit
assertEquals(201, response.statusCode());
```

### 4.4 API 호출은 Steps 클래스로 분리

```java
// Good - Steps 메소드 활용
var response = 예약_생성_요청(request);

// Bad - 테스트 내 직접 호출
RestAssured.given()...when().post()...
```

---

## 5. 골든 샘플 코드 (Golden Sample)

### 5.1 AcceptanceTest.java (베이스 클래스)

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
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

### 5.2 Steps 클래스

```java
public class SampleSteps {

    public static ExtractableResponse<Response> 생성_요청(Map<String, Object> request) {
        return RestAssured
                .given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(request)
                .when().post("/api/samples")
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 조회_요청(Long id) {
        return RestAssured
                .given().log().all()
                .when().get("/api/samples/{id}", id)
                .then().log().all()
                .extract();
    }
}
```

### 5.3 테스트 클래스

```java
@DisplayName("샘플 관리")
class SampleAcceptanceTest extends AcceptanceTest {

    @Test
    @DisplayName("생성 - 유효한 요청이면 생성된다")
    void 유효한_요청이면_생성된다() {
        // given
        var request = Map.of("name", "테스트");

        // when
        var response = 생성_요청(request);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(response.jsonPath().getLong("id")).isPositive();
    }

    @Test
    @DisplayName("조회 - 존재하는 ID로 조회하면 성공한다")
    void 존재하는_ID로_조회하면_성공한다() {
        // given
        Long id = 생성_요청(Map.of("name", "테스트")).jsonPath().getLong("id");

        // when
        var response = 조회_요청(id);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    }
}
```

---

## 6. 테스트 데이터 준비

### 6.1 선행 데이터 생성

테스트에 필요한 선행 데이터는 **다른 도메인의 Steps 클래스**를 활용합니다.

```java
@Test
@DisplayName("예약 생성 - 유효한 요청이면 성공한다")
void 유효한_예약_요청이면_성공한다() {
    // given - 선행 데이터: 사이트 생성
    Long siteId = 사이트_생성_요청(Map.of("name", "A구역")).jsonPath().getLong("id");

    var request = Map.of(
        "siteId", siteId,
        "startDate", "2024-03-01",
        "endDate", "2024-03-03"
    );

    // when
    var response = 예약_생성_요청(request);

    // then
    assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
}
```

### 6.2 Steps 클래스 재사용

```java
// ReservationAcceptanceTest.java
import static com.camping.acceptance.site.SiteSteps.*;       // 사이트 Steps
import static com.camping.acceptance.reservation.ReservationSteps.*;  // 예약 Steps
```

### 6.3 공통 픽스처 메서드

자주 사용하는 데이터 생성 패턴은 Steps 클래스에 헬퍼 메서드로 추가합니다.

```java
// SiteSteps.java
public static Long 기본_사이트_생성() {
    return 사이트_생성_요청(Map.of("name", "기본사이트"))
            .jsonPath().getLong("id");
}
```
