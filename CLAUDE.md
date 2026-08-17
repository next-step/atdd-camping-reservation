# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

캠핑장 예약 관리 Spring Boot 애플리케이션. `src/main/java/com/camping/legacy` 패키지명이 말해주듯,
의도적으로 레거시 스타일(긴 메서드, 깊은 중첩, 절차적 로직, 중복 코드, 하드코딩)로 작성된 코드베이스다.

## Commands

- 서버 기동: `./gradlew bootRun` (기본 8080 포트, 충돌 시 `SERVER_PORT=8081 ./gradlew bootRun`)
- 전체 테스트: `./gradlew test`
- 단일 테스트: `./gradlew test --tests "com.camping.legacy.전체클래스명"`
- 결과 줄이 안 보이면 캐시다: `./gradlew test --rerun-tasks`
- 자바 17 필요. 그레이들 래퍼가 포함되어 있어 별도 설치 불필요.
- H2 인메모리 DB (`jdbc:h2:mem:testdb`), 콘솔: `/h2-console`. 스키마는 `ddl-auto: create-drop` +
  `src/main/resources/data.sql`(`defer-datasource-initialization`)로 매 기동/테스트마다 재생성된다.
  데이터를 다루는 작업 전에 이 파일을 먼저 읽을 것.
- 모든 API는 `/api` 아래에 있다 (`/api/reservations`, `/api/sites`).

## `docs/` 파일별 역할

| 파일 | 내용 |
| --- | --- |
| `principles.md` | 일하는 방식과 개발 원칙 |
| `plan.md` | 작업 절차와 이 저장소의 실행 방법 |
| `acceptance-criteria.md` | 확정된 규칙·예시·이유가 티켓별로 누적 |
| `tickets.md` | 받은 티켓과 작업 중 발견한 질문/후속 티켓 |
| `test-guide.md` | 테스트 작성 규칙(클린업, 네이밍, 그룹화 등) 한 줄씩 누적 |
| `retrospective.md` | 회고 |

## Architecture

Controller → Service → Repository의 단순한 3계층 구조지만, 서비스 계층이 여러 차례 "통합"되며
책임이 한 클래스에 쌓여 있다는 점이 이 코드를 읽을 때 가장 먼저 알아야 할 사실이다.

- **`ReservationService`가 사실상 God Object다.** 클래스 주석(작성자/수정자 이력)에 나오듯 예약 CRUD,
  캘린더 관리(구 `CalendarService` 통합, 2020-06-15), 통계, 가격 계산, 포인트 적립, 알림 발송(시뮬레이션),
  가용성 체크까지 한 클래스에 있다. `CalendarService`는 여전히 존재하지만 컨트롤러에서는 더 이상 쓰이지
  않는다 (`ReservationController`의 주석 처리된 필드 참고) — 실제 로직은 모두 `ReservationService`로 옮겨졌다.
- **`createReservation`/`updateReservation`은 검증·가격·포인트·저장·알림·응답변환을 한 메서드 안에서
  순차적으로 처리**하며, 같은 검증/DTO 변환 로직이 여러 메서드에 중복돼 있다 (주석에 "중복 코드 N"으로
  표시돼 있음). 이런 중복은 의도된 것이므로, 버그를 고칠 때 한 곳만 고치고 나머지 중복 지점을 놓치지
  않았는지 확인해야 한다.
- **날짜 관련 정책이 두 군데(생성/수정)에 따로 구현**되어 있다: `MAX_RESERVATION_DAYS = 30`이 상수로
  선언돼 있지만 실제 "오늘로부터 30일 이내" 체크는 `ChronoUnit.DAYS.between(today, startDate) > 30`으로
  `createReservation`과 `updateReservation` 양쪽에 각각 하드코딩돼 있다. 이 종류의 정책을 고칠 때는
  두 경로 모두 확인할 것.
- **가격/포인트 계산 로직도 중복**되어 있다: `calculateReservationPrice`/`calculatePoints`(정식 경로,
  `DateUtils` 사용)와 `createReservation`/`processReservationWithPayment` 내부의 인라인 계산(로직 재구현,
  `DateUtils` 미사용)이 별도로 존재한다.
- **예외 처리는 전부 `RuntimeException` + 메시지 문자열**이며, 컨트롤러가 이를 잡아 HTTP 상태 코드로
  변환한다. 상태 코드가 액션마다 다르다 (생성 실패→409, 조회 실패→404, 수정/취소 실패→400) — 새 검증을
  추가할 때 해당 엔드포인트의 기존 컨벤션을 따를 것.
- **도메인**: `Campsite`(1) — `Reservation`(N), Lombok `@Getter/@Setter`로 빈약한(anemic) 엔티티. 사이트
  종류는 `siteNumber`의 접두문자(`A-`=대형/`B-`=소형/그 외=기타)로 판별하며 별도 타입 필드는 없다.
  프론트엔드는 Thymeleaf 템플릿(`src/main/resources/templates/`)이 서버사이드 렌더링한다.
