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

---

## 7. 테스트 데이터 관리

### 7.1 테스트 독립성 보장

#### 독립성 보장 메커니즘

```
┌─────────────────────────────────────────────────────────────┐
│                    각 테스트 실행 흐름                        │
├─────────────────────────────────────────────────────────────┤
│  @BeforeEach (AcceptanceTest)                               │
│  ├── RestAssured.port = port                                │
│  └── databaseCleanup.execute()  ◀── 모든 테이블 TRUNCATE    │
│                                                             │
│  @BeforeEach (각 테스트 클래스의 setUpFixture)               │
│  └── Fixture를 통해 필요한 데이터 생성                        │
│                                                             │
│  @Test 실행                                                  │
│  └── 독립적인 데이터 환경에서 테스트                          │
└─────────────────────────────────────────────────────────────┘
```

#### 독립성 검증 체크리스트

| 항목 | 보장 여부 | 설명 |
|------|----------|------|
| 테스트 순서 변경 | ✅ 보장 | 어떤 순서로 실행해도 동일한 결과 |
| 테스트 단독 실행 | ✅ 보장 | 개별 테스트만 실행해도 통과 |
| 테스트 병렬 실행 | ⚠️ 주의 | 기본 설정(순차 실행)에서만 보장 |
| 동시성 테스트 | ⚠️ 주의 | `@DirtiesContext` 권장 |

#### 병렬 실행 시 주의사항

```java
// 동시성 테스트에는 @DirtiesContext 추가 권장
@Test
@DirtiesContext
@DisplayName("두 고객이 동시에 같은 사이트를 예약하면 한 명만 성공한다")
void 동시_예약_테스트() {
    // ExecutorService를 사용한 동시 요청 테스트
}
```

### 7.2 Fixture 클래스

#### 파일 구조

```
src/test/java/com/camping/acceptance/common/
├── AcceptanceTest.java      # 베이스 클래스
├── AcceptanceTestConfig.java # 테스트 설정
├── DatabaseCleanup.java     # DB 초기화
├── SiteFixture.java         # 사이트 데이터 생성
└── ReservationFixture.java  # 예약 데이터 생성
```

#### SiteFixture - 사이트 데이터 생성

| 메서드 | 설명 | 사용 예시 |
|--------|------|----------|
| `대형_사이트_생성(siteNumber)` | 대형 사이트 1개 생성 | `대형_사이트_생성("A-1")` |
| `소형_사이트_생성(siteNumber)` | 소형 사이트 1개 생성 | `소형_사이트_생성("B-1")` |
| `대형_사이트_여러개_생성(siteNumbers...)` | 대형 사이트 여러개 생성 | `대형_사이트_여러개_생성("A-1", "A-2")` |
| `소형_사이트_여러개_생성(siteNumbers...)` | 소형 사이트 여러개 생성 | `소형_사이트_여러개_생성("B-1", "B-2")` |
| `기본_사이트_설정()` | A-1, A-2, B-1, B-2 생성 | `기본_사이트_설정()` |


#### ReservationFixture - 예약 데이터 생성

| 메서드 | 설명 | 사용 예시 |
|--------|------|----------|
| `예약_생성(campsite, customerName, phone, startDate, endDate, confirmationCode)` | 모든 정보 지정 | 확인코드 검증 테스트 |
| `예약_생성(campsite, startDate, endDate)` | 기본 고객 정보 사용 | 간단한 예약 생성 |
| `예약_생성(campsite, customerName, startDate, endDate)` | 고객명만 지정 | 고객별 테스트 |
| `취소된_예약_생성(campsite, customerName, phone, startDate, endDate)` | 취소 상태 예약 생성 | 취소 관련 테스트 |
| `취소된_예약_생성(campsite, startDate, endDate)` | 기본 정보로 취소 예약 | 간단한 취소 예약 |


### 7.3 테스트 순서 의존성

#### 원칙: 테스트 간 순서 의존성 없음

```java
// ❌ Bad - 다른 테스트의 데이터에 의존
@Test
void 예약_조회_테스트() {
    // 다른 테스트에서 생성한 예약이 있다고 가정 (위험!)
    var response = 예약_조회_요청(1L);
    assertThat(response.statusCode()).isEqualTo(200);
}

// ✅ Good - 테스트 내에서 필요한 데이터 직접 생성
@Test
void 예약_조회_테스트() {
    // given - 테스트에 필요한 데이터 직접 생성
    Campsite 사이트 = siteFixture.대형_사이트_생성("A-1");
    Reservation 예약 = reservationFixture.예약_생성(사이트, 시작일, 종료일);

    // when
    var response = 예약_조회_요청(예약.getId());

    // then
    assertThat(response.statusCode()).isEqualTo(200);
}
```

#### 테스트 클래스 구조 권장 패턴

```java
@DisplayName("예약 생성")
class ReservationCreateAcceptanceTest extends AcceptanceTest {

    @Autowired
    private SiteFixture siteFixture;

    @Autowired
    private ReservationFixture reservationFixture;

    // 테스트에서 공통으로 사용할 데이터
    private Campsite 대형사이트;
    private LocalDate 시작일;
    private LocalDate 종료일;

    @BeforeEach
    void setUpFixture() {
        // 각 테스트 전에 필요한 기본 데이터 생성
        대형사이트 = siteFixture.대형_사이트_생성("A-1");
        시작일 = LocalDate.now().plusDays(1);
        종료일 = LocalDate.now().plusDays(3);
    }

    @Test
    @DisplayName("빈 사이트를 예약하면 확인 코드를 받는다")
    void 빈_사이트를_예약하면_확인_코드를_받는다() {
        // given - setUpFixture에서 생성된 데이터 활용

        // when
        var response = 예약_생성_요청("A-1", "홍길동", "010-1234-5678", 시작일, 종료일);

        // then
        assertThat(response.statusCode()).isEqualTo(201);
    }
}
```
