## 프로젝트

Spring Boot / JPA / Thymeleaf. DB는 H2 인메모리라 기동마다 초기화된다.

## 이 서비스 (아직 검증하지 않은 내용)

코드를 읽고 적었을 뿐 직접 호출해 확인하지 않았다. 가설로 받는다.

캠핑장 사이트 예약 시스템. 예약 생성·조회·변경·취소, 날짜별 빈 사이트 검색, 사이트별 월간 캘린더를 제공한다.
도메인은 `Campsite`(사이트)와 `Reservation`(기간 예약) 둘뿐이다. 사이트 하나가 예약 여럿을 갖는다.
예약은 사이트 하나를 연속된 기간 동안 점유한다. 같은 사이트의 겹치는 기간은 둘이 점유할 수 없다.

## 작업 방식

인수 테스트 주도 개발로 티켓 하나씩 처리한다. 
**티켓을 시작할 때와 단계를 넘어갈 때마다 `docs/principles.md`와 `docs/plan.md`를 읽는다.**

**`docs/`**

- `principles.md` 일하는 방식 / `plan.md` 작업 순서와 실행 방법 — 뼈대가 채워져 있고, 맞지 않으면 고쳐 쓴다
- `acceptance-criteria.md` 확정한 규칙·예시·이유 / `tickets.md` 받은 티켓과 발견한 티켓 — 이 저장소에 쌓는다
- `test-guide.md` 테스트 초안에 줄 제약 / `retrospective.md` 무엇을 겪었는가 — 저장소가 바뀌어도 딸려 간다

## 실행

- 서버 `./gradlew bootRun` (8080, 충돌 시 `SERVER_PORT=8081`). API는 `/api` 아래
- 테스트 `./gradlew test`. 결과 줄이 안 보이면 캐시이므로 `--rerun-tasks`. 단건은 `--tests '*ReservationTest*'`
- 데이터를 다루기 전에 `src/main/resources/data.sql`을 읽는다
