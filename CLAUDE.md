# CLAUDE.md

## 프로젝트 개요
초록 캠핑장 예약 시스템 — 캠핑 사이트 예약을 관리하는 Spring Boot 웹 애플리케이션이다.
핵심 도메인은 `Campsite`(사이트 번호·설명·최대 인원)와 `Reservation`(이용 기간·예약자·연락처·확인 코드·상태)이며, `Campsite` 하나가 여러 `Reservation`을 가진다.
사이트 검색/가용성 조회, 예약 생성·조회·수정·취소(확인 코드로 본인 확인), 이름+전화번호로 내 예약 조회, 월별 예약 캘린더 조회를 `/api/sites`, `/api/reservations` 아래에서 제공한다.
레거시 코드를 인수 테스트 주도 개발(ATDD)로 리팩터링하는 학습용 저장소다.

## 작업 방식
**이 저장소에서 작업할 때는 반드시 `docs/principles.md`와 `docs/plan.md`를 따른다.**

이 저장소는 인수 테스트 주도 개발(ATDD)로 작업한다. 진행 절차는 `docs/`의 여섯 파일로 관리하며 세 묶음으로 나뉜다.

| 구분 | 파일 | 내용 |
| --- | --- | --- |
| 받아서 시작 (뼈대 있음, 다르면 고쳐 씀) | `principles.md` | 저장소가 바뀌어도 통하는 개발 원칙 |
| | `plan.md` | 티켓 하나를 처리하는 4단계 + 이 저장소의 실행 방법 |
| 이 저장소의 것을 쌓음 (비어 있음) | `acceptance-criteria.md` | 확정한 규칙·예시·이유 |
| | `tickets.md` | 받은 티켓과 처리하며 발견한 티켓 |
| 작업자 개인 것을 쌓음 (비어 있고 저장소가 바뀌어도 유지) | `test-guide.md` | 테스트 작성 시 지킬 규칙 한 줄씩 |
| | `retrospective.md` | 겪은 것에 대한 회고 |

무엇을 언제, 어디까지 적는지는 미션 문서를 따른다.

## 실행
- 서버 기동: `./gradlew bootRun` (8080 포트, 충돌 시 `SERVER_PORT=8081 ./gradlew bootRun`)
- 호출 가능한 API는 `/api` 아래에 있다
- 테스트: `./gradlew test` (결과 줄이 안 보이면 캐시다 — `./gradlew test --rerun-tasks`로 재실행)
- 데이터를 다루기 전에 `src/main/resources/data.sql`을 읽는다
