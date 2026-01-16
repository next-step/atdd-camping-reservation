# 인수 테스트 전략 (Acceptance Test Strategy)

## 1. 기술 스택

| 구분 | 기술 | 용도 |
|------|------|------|
| 테스트 프레임워크 | JUnit 5 | 테스트 실행 및 생명주기 관리 |
| API 테스트 | RestAssured | HTTP 요청/응답 처리 |
| 검증 라이브러리 | AssertJ | 가독성 높은 assertion |
| 테스트 환경 | SpringBootTest | 통합 테스트 컨텍스트 |
| 데이터 초기화 | @Sql | 테스트 데이터 설정 |

## 2. 파일 구조

```
src/test/java/com/camping/legacy/acceptance/
└── reservation/
    ├── ReservationAcceptanceTest.java              # 예약 생성/수정/취소 테스트
    ├── ReservationPricingAcceptanceTest.java       # 예약 금액 계산 테스트
    ├── ReservationTestConstants.java               # 공통 상수 관리
    ├── apiExtractableresponse/
    │   ├── ReservationApiExtractableResponse.java  # 예약 API 호출
    │   └── SiteApiExtractableResponse.java         # 사이트 API 호출
    └── builder/
        └── ReservationRequestBuilder.java          # 예약 요청 빌더

src/test/resources/
    ├── truncate.sql                                # 테이블 초기화
    └── data.sql                                    # 테스트 픽스처 데이터
```

## 3. 네이밍 컨벤션

| 구분 | 형식 | 예시 |
|------|------|------|
| 테스트 클래스 | `{도메인}AcceptanceTest` | `ReservationAcceptanceTest` |
| API 헬퍼 클래스 | `{도메인}ApiExtractableResponse` | `ReservationApiExtractableResponse` |
| 테스트 메서드 | `{행위}_{결과}` 한글 스네이크 | `정상적으로_예약을_생성()`, `중복_예약은_불가()` |
| API 호출 메서드 | `{대상}을_조회/생성/수정/삭제한다()` | `예약을_생성한다()`, `사이트를_조회한다()` |
| 검증 메서드 | `{결과상태}_{되었다/이다}()` | `예약이_되었다()`, `예약상태가_확정이다()` |
| 빌더 메서드 | `{대상}()` | `Reservation()` |
| 상수 | 한글 스네이크 케이스 | `사이트번호_A_1`, `기존예약_시작일` |

## 4. 테스트 클래스 구조

### 4.1 기본 구조

```java
@DisplayName("예약 관련 기능")
@Sql({"/truncate.sql", "/data.sql"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ReservationAcceptanceTest extends AcceptanceTestBase {

    // 1. 섹션별 주석으로 테스트 그룹화
    // =====================================================
    // 1. 예약 생성
    // =====================================================
    
    // 2. Gherkin 주석 + DisplayName
    /**
     * Given A-1 사이트가 예약 가능한 상태이고
     * When 홍길동이 A-1 사이트를 예약하면
     * Then 예약이 성공적으로 생성된다.
     */
    @DisplayName("[예약/생성] 정상적으로 예약을 생성한다.")
    @Test
    void 정상적으로_예약을_생성() {
        // Given
        // When
        // Then
    }
    
    // 3. 헬퍼 메서드 (private)
}
```

### 4.2 상수 관리 전략

**공통 상수는 별도 클래스로 분리**

```java
// ReservationTestConstants.java
public class ReservationTestConstants {
    // 사이트 정보
    public static final String 사이트번호_A_1 = "A-1";
    
    // 예약자 정보
    public static final String 홍길동 = "홍길동";
    public static final String 연락처 = "010-1234-1234";
    
    // 예약 날짜
    public static final String 기존예약_시작일 = "2026-02-01";
    public static final String 기존예약_종료일 = "2026-02-03";
    
    // 요금
    public static final int A_사이트_기본요금 = 80000;
    public static final int 주말_할증_요금 = 104000;
}
```

### 4.3 빌더 패턴 활용

```java
// ReservationRequestBuilder.java
public class ReservationRequestBuilder {
    public static ReservationRequestBuilder Reservation() {
        return new ReservationRequestBuilder();
    }
    
    public ReservationRequestBuilder reserver(String name) { ... }
    public ReservationRequestBuilder period(String start, String end) { ... }
    public ReservationRequestBuilder site(String siteNumber) { ... }
    public ReservationRequest build() { ... }
}

// 사용 예시
var 요청 = Reservation()
    .reserver(홍길동)
    .period(기존예약_시작일, 기존예약_종료일)
    .site(사이트번호_A_1)
    .build();
```

## 5. 테스트 작성 패턴

### 5.1 Given-When-Then 구조

```java
@Test
void 정상적으로_예약을_생성() {
    // Given - 테스트 전제 조건
    var 사이트정보 = 사이트를_조회한다(기존예약_시작일, 기존예약_종료일, 사이트_크기_대형);
    사이트가_존재한다(사이트정보, 사이트번호_A_1);

    // When - 테스트 대상 행위
    var 예약요청 = Reservation()
        .reserver(홍길동)
        .period(기존예약_시작일, 기존예약_종료일)
        .site(사이트번호_A_1)
        .build();
    var 예약결과 = 예약을_생성한다(예약요청);

    // Then - 검증
    예약이_되었다(예약결과);
    확인코드가_발급되었다(예약결과, 확인코드_길이_6자리);
}
```

### 5.2 DisplayName 작성 규칙

**형식**: `[카테고리] 테스트 설명`

```java
@DisplayName("[예약/생성] 정상적으로 예약을 생성한다.")
@DisplayName("[예약/중복] 이미 예약된 사이트는 예약되지 않는다.")
@DisplayName("[예약/수정] 틀린 확인코드로는 예약이 수정되지 않는다.")
@DisplayName("[기본요금] A 사이트(대형)의 기본 요금은 80,000원이다.")
@DisplayName("[성수기/경계] 성수기 시작일(7월 1일)에는 성수기 요금이 적용된다.")
```

### 5.3 Gherkin 주석 작성

```java
/**
 * Given A-1 사이트가 2026년 2월 1일부터 2월 3일까지 예약 가능한 상태이고
 * When 홍길동이 A-1 사이트를 2026년 2월 1일부터 2월 3일까지 예약하면
 * Then 예약이 성공적으로 생성되고
 * And 6자리 확인 코드가 발급되고
 * And 예약 상태가 "CONFIRMED"로 설정된다.
 */
```

## 6. 테스트 분류 전략

### 6.1 ReservationAcceptanceTest (21개 테스트)

**1. 예약 생성 (8개)**
- 정상 케이스
- 기간 검증 (30일 초과, 정확히 30일)
- 인원 검증 (0명, 최대 인원 초과/동일)
- 날짜 검증 (과거 날짜, 종료일이 시작일보다 이전)

**2. 예약 중복/경계 (6개)**
- 중복 예약 거부
- 경계값 테스트 (종료일=시작일, 종료일+1일)
- 기간 포함 관계 테스트

**3. 동시성 (1개)**
- 동시 예약 요청 시 한 건만 성공

**4. 예약 수정 (3개)**
- 정상 수정
- 틀린 확인코드
- 이미 예약된 날짜로 수정 불가

**5. 예약 취소 (3개)**
- 정상 취소 (사전 취소)
- 당일 취소
- 틀린 확인코드

### 6.2 ReservationPricingAcceptanceTest (15개 테스트)

**1. 기본 요금 (1개)**
- A 사이트 기본 요금 80,000원

**2. 단일 조건 할증 (4개)**
- 비수기 평일: 할증 없음
- 비수기 주말: 30% 할증
- 성수기 평일: 50% 할증
- 성수기 주말: 70% 할증

**3. 복합 요금 계산 (3개)**
- 평일 2박: 일별 합계
- 평일+주말 혼합
- 비수기+성수기 혼합

**4. 성수기 경계값 (4개)**
- 성수기 전날 (6월 30일): 비수기 요금
- 성수기 시작일 (7월 1일): 성수기 요금
- 성수기 종료일 (8월 31일): 성수기 요금
- 성수기 다음날 (9월 1일): 비수기 요금

**5. 포인트 적립 (3개)**
- 평일: 5%
- 주말: 10%
- 성수기: 3%

## 7. 헬퍼 메서드 분류

### 7.1 API 호출 (ApiExtractableResponse)

```java
public static ExtractableResponse<Response> 예약을_생성한다(ReservationRequest request)
public static ExtractableResponse<Response> 예약을_수정한다(Long id, String code, ReservationRequest request)
public static ExtractableResponse<Response> 예약을_취소한다(Long id, String code)
public static ExtractableResponse<Response> 예약을_조회한다(Long id)
```

### 7.2 데이터 추출 (AcceptanceTest)

```java
private Long 예약정보에서_예약ID_조회(ExtractableResponse<Response> response)
private String 예약정보에서_확인코드_조회(ExtractableResponse<Response> response)
```

### 7.3 검증 (AcceptanceTest)

```java
private void 예약이_되었다(ExtractableResponse<Response> response)
private void 예약이_되지않았다(ExtractableResponse<Response> response)
private void 확인코드가_발급되었다(ExtractableResponse<Response> response, int expectedLength)
private void 예약_금액이_일치한다(ExtractableResponse<Response> response, int expectedPrice)
```

## 8. 테스트 데이터 관리

### 8.1 데이터 초기화 전략

```java
@Sql({"/truncate.sql", "/data.sql"})
```

- `truncate.sql`: 모든 테이블 초기화
- `data.sql`: 기본 사이트 데이터 삽입

### 8.2 테스트 독립성 보장

- 각 테스트는 `@Sql`로 동일한 초기 상태에서 시작
- 테스트 간 데이터 의존성 없음
- 테스트 실행 순서 무관

## 9. 체크리스트

### 테스트 작성 시

- [ ] Gherkin 주석 작성 (Given-When-Then)
- [ ] DisplayName에 카테고리 포함
- [ ] 테스트 메서드 내부에 Given-When-Then 주석
- [ ] 상수는 ReservationTestConstants 사용
- [ ] 빌더 패턴으로 요청 객체 생성
- [ ] 검증 메서드로 assertion 추상화

### 코드 리뷰 시

- [ ] 테스트 이름이 의도를 명확히 표현하는가?
- [ ] Given-When-Then 구조가 명확한가?
- [ ] 중복 코드가 헬퍼 메서드로 추출되었는가?
- [ ] 매직 넘버/문자열이 상수로 정의되었는가?
- [ ] 테스트가 독립적으로 실행 가능한가?
