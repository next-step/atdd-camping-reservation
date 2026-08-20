# CLAUDE.md

## 프로젝트 요약

> 아래 요약은 코드를 읽고 정리한 것으로, 아직 실제 호출로 검증하지 않은 내용이다.

- 초록 캠핑장 예약 시스템: Spring Boot 3 + JPA + H2 인메모리 DB 기반 캠핑장 예약 관리 서비스다.
- 도메인 객체는 둘이다. Campsite(사이트 번호, 설명, 최대 인원)와 Reservation(예약자명, 기간, 상태, 6자리 확인코드)이 1:N 관계를 이룬다.
- REST API는 `/api/sites`(사이트 조회·가용성 검색)와 `/api/reservations`(예약 생성·조회·수정·취소·캘린더)에 있고, Thymeleaf 화면도 함께 제공한다.
- 예약 수정·취소에는 confirmationCode가 필요하며, 시드 데이터(사이트 35개, 예약 5건)는 `src/main/resources/data.sql`에 있다.
- 패키지명이 `legacy`이고 과거 날짜 예약 등 의도된 버그가 있는 레거시 코드를 다루는 실습 저장소다.

## 작업 방식

- 이 저장소의 작업은 `docs/principles.md`(개발 원칙)와 `docs/plan.md`(티켓 처리 순서)를 따른다.
- 작업 문서는 `docs/`의 여섯 파일로 관리한다: principles/plan(받아서 시작), acceptance-criteria/tickets(저장소에 쌓기), test-guide/retrospective(자기 것으로 쌓기).
- 서버 기동: `./gradlew bootRun` (8080 포트, 사용 중이면 `SERVER_PORT=8081`). 테스트: `./gradlew test` (캐시로 결과가 안 보이면 `--rerun-tasks`).
