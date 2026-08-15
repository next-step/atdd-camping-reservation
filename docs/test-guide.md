# 테스트 작성 제약

## 인수 테스트 클래스는 티켓 단위가 아니라 도메인 단위로 묶는다

- 규칙: 인수 테스트 클래스(예: `ReservationAcceptanceTest`)는 티켓 번호가 아니라 도메인으로 이름 짓고, 여러 티켓의 테스트를 `@Nested`로 함께 담는다. 클래스 레벨 주석에는 파일 전체에 공통되는 규칙(상대 날짜 사용, 시드 데이터 확인 등)만 남기고, 티켓 번호·왜 레드인지 같은 티켓별 세부 내용은 각 `@Nested` 클래스 바로 위에 적는다.
- 이유: 클래스 레벨 주석에 티켓별 세부 내용을 적으면, 다음 티켓의 테스트가 같은 클래스에 `@Nested`로 추가될 때 그 주석이 새 티켓 내용과 맞지 않게 된다.
- 적용: `@DisplayName("T-1-1: ...")`처럼 `@Nested` 클래스에 티켓 번호를 달고, 그 위 Javadoc에 "왜 이 케이스가 지금 레드/그린인지"를 적는다.

## 격리는 먼 날짜나 다른 사이트로 피하지 않고 @Sql로 보장한다

- 규칙: `@SpringBootTest` + RestAssured로 실제 서버에 HTTP 요청을 보내는 인수 테스트는 `@Transactional` 테스트처럼 자동 롤백되지 않는다 — 커밋된 예약이 DB에 그대로 남는다. 테스트 간 격리를 "먼 미래 날짜를 쓴다", "테스트마다 다른 사이트 번호를 쓴다"는 식으로 우회하지 않고, 클래스에 `@Sql(scripts = "...", executionPhase = BEFORE_TEST_METHOD)`로 매 테스트 전 관련 테이블을 비워 보장한다.
- 이유: 먼 날짜·다른 사이트로 피하는 방식은 테스트가 늘어날수록 어떤 사이트/날짜가 비어 있는지 계속 추적해야 해서 깨지기 쉽고, 재실행 시 이전 실행이 남긴 데이터와도 충돌할 수 있다. `@Sql` 초기화는 실행 순서·횟수와 무관하게 항상 같은 시작 상태를 보장한다.
- 적용: `src/test/resources/sql/cleanup-reservations.sql`에 `DELETE FROM reservations;`를 두고, 클래스 레벨에 `@Sql(scripts = "/sql/cleanup-reservations.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)`를 단다. 이후 테스트들은 같은 사이트 번호를 재사용해도 된다.
