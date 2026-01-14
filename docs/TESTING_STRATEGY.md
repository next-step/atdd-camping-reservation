# 인수 테스트 전략 (Acceptance Test Strategy)

## 1. 기술 스택

| 구분 | 기술 | 용도 |
|------|------|------|
| 테스트 프레임워크 | JUnit 5 | 테스트 실행 및 생명주기 관리 |
| API 테스트 | RestAssured | HTTP 요청/응답 처리 |
| 검증 라이브러리 | AssertJ | 가독성 높은 assertion |
| 테스트 환경 | SpringBootTest | 통합 테스트 컨텍스트 |
| 데이터 초기화 | @Sql | 테스트 데이터 설정 |

## 2. 파일 위치 규칙

```
src/test/java/com/camping/legacy/acceptance/
└── {도메인}/
    ├── {도메인}AcceptanceTest.java           # 테스트 클래스
    └── {도메인}ApiExtractableResponse.java   # API 호출 헬퍼 (static import)
src/test/resources/
    ├── truncate.sql                          # 테이블 초기화
    └── data.sql                              # 테스트 픽스처 데이터
```

## 3. 네이밍 컨벤션

| 구분 | 형식 | 예시 |
|------|------|------|
| 테스트 클래스 | `{도메인}AcceptanceTest` | `ReservationAcceptanceTest` |
| API 헬퍼 클래스 | `{도메인}ApiExtractableResponse` | `ReservationApiExtractableResponse` |
| 테스트 메서드 | `{행위}_{결과}` 한글 스네이크 | `정상적으로_예약을_생성()`, `중복_예약은_불가()` |
| API 호출 메서드 | `{대상}을_조회/생성/수정/삭제한다()` | `예약을_생성한다()`, `사이트를_조회한다()` |
| 검증 메서드 | `{결과상태}_{되었다/이다}()` | `예약이_되었다()`, `예약상태가_확정이다()` |
| Fixture 메서드 | `{대상}_생성()` / `{대상}에서_{값}_조회()` | `예약요청_생성()`, `예약정보에서_확인코드_조회()` |
| 상수 | 한글 스네이크 케이스 | `사이트번호_A_1`, `기존예약_시작일`, `확인코드_길이_6자리` |

## 4. 테스트 클래스 내부 구조 (순서)

```java
// 1. 상수 정의 (도메인값 → 기대값 순)
private static final String 사이트번호_A_1 = "A-1";
private static final int 확인코드_길이_6자리 = 6;

// 2. 필드
@LocalServerPort int port;

// 3. @BeforeEach setUp()

// 4. @Test 메서드들 (Given-When-Then 주석 포함)

// 5. ====== Fixture 생성/추출 ====== 주석 후 private 메서드

// 6. ====== Custom Assertion ====== 주석 후 private void 메서드
```

## 5. 메서드 분류 규칙

| 구분 | 위치 | 접근제한자 | 반환타입 |
|------|------|----------|---------|
| API 호출 | ApiExtractableResponse | `public static` | `ExtractableResponse<Response>` |
| Fixture 생성 | AcceptanceTest | `private` | Request DTO |
| 데이터 추출 | AcceptanceTest | `private` | String, Long 등 |
| Custom Assertion | AcceptanceTest | `private` | `void` |

## 6. Golden Sample

```java
@DisplayName("예약 관련 기능")
@Sql({"/truncate.sql", "/data.sql"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ReservationAcceptanceTest {

    private static final String 사이트번호_A_1 = "A-1";
    private static final String 사이트_크기_대형 = "대형";
    private static final String 기존예약_시작일 = "2026-02-01";
    private static final String 기존예약_종료일 = "2026-02-03";
    private static final int 확인코드_길이_6자리 = 6;
    private static final String 예약상태_CONFIRMED = "CONFIRMED";

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    /**
     * Given: A-1 사이트가 예약 가능한 상태이다.
     * When: 홍길동이 A-1 사이트를 예약한다.
     * Then: 예약이 성공적으로 생성된다.
     * And: 6자리 확인 코드가 발급된다.
     */
    @DisplayName("정상적으로 예약을 생성한다.")
    @Test
    void 정상적으로_예약을_생성() {
        // Given
        ExtractableResponse<Response> 사이트정보 = 사이트를_조회한다(기존예약_시작일, 기존예약_종료일, 사이트_크기_대형);
        사이트가_존재한다(사이트정보, 사이트번호_A_1);

        // When
        ReservationRequest 예약요청 = 예약요청_생성("홍길동", 기존예약_시작일, 기존예약_종료일, 사이트번호_A_1);
        ExtractableResponse<Response> 예약정보 = 예약을_생성한다(예약요청);

        // Then
        예약이_되었다(예약정보);
        확인코드가_발급되었다(예약정보, 확인코드_길이_6자리);
        예약상태가_확정이다(예약정보, 예약상태_CONFIRMED);
    }

    // ====== Fixture 생성/추출 ======
    private ReservationRequest 예약요청_생성(String 예약자명, String 시작일, String 종료일, String 사이트번호) {
        return new ReservationRequest(예약자명, LocalDate.parse(시작일), LocalDate.parse(종료일), 사이트번호, ...);
    }

    // ====== Custom Assertion ======
    private void 예약이_되었다(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(201);
    }

    private void 확인코드가_발급되었다(ExtractableResponse<Response> response, int expectedLength) {
        assertThat(response.jsonPath().getString("confirmationCode")).hasSize(expectedLength);
    }

    private void 예약상태가_확정이다(ExtractableResponse<Response> response, String expectedStatus) {
        assertThat(response.jsonPath().getString("status")).isEqualTo(expectedStatus);
    }
}
```