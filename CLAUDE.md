# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

> ⚠️ 아래 개요는 코드를 직접 실행해 검증한 내용이 아니라, 코드를 읽고 정리한 것이다. 작업 전 `principles.md`의 "실측에 근거한 판정" 원칙대로 직접 호출해 확인한다.

캠핑장 예약 관리를 위한 Spring Boot 3.2 / Java 17 기반 웹 애플리케이션이다(`com.camping.legacy`, H2 인메모리 DB, Thymeleaf UI, `/api/**` REST 엔드포인트). 도메인 객체는 `Campsite`(캠핑 사이트)와 `Reservation`(예약, `Campsite`를 참조)이며, 예약 생성·조회·수정·취소, 가격/포인트 계산, 캘린더 조회, 통계 등을 다룬다. 이 저장소는 ATDD(인수 테스트 주도 개발) 연습용으로 만들어진 레거시 코드베이스로, 긴 절차적 메서드·중복 로직·책임이 뒤섞인 `ReservationService` 등 의도된 기술 부채를 담고 있다 — 지시받지 않은 정리/리팩터링을 임의로 하지 않는다.

## 실행

- 서버: `./gradlew bootRun` (기본 8080 포트, 사용 중이면 `SERVER_PORT=8081 ./gradlew bootRun`)
- 테스트: `./gradlew test` (결과 줄이 안 보이면 캐시 문제이므로 `./gradlew test --rerun-tasks`)
- 호출 가능한 API는 `/api` 아래에 있다
- 데이터를 다루기 전에 `src/main/resources/data.sql`을 읽는다

## 작업 방식 (`docs/`)

이 저장소의 작업은 인수 테스트 주도 개발(ATDD)로, `docs/`의 여섯 파일로 관리한다.

- `principles.md`, `plan.md` — 뼈대가 채워진 원칙과 티켓 처리 순서(인수 조건 → 인수 테스트 → 구현 → 판정). 맞지 않으면 고쳐 쓴다.
- `acceptance-criteria.md`, `tickets.md` — 이 저장소에서 쌓는 것(빈 상태로 시작): 확정한 규칙/예시/이유, 받은/발견한 티켓
- `test-guide.md`, `retrospective.md` — 작업자 개인이 쌓는 것(빈 상태로 시작, 저장소가 바뀌어도 유지): 테스트 작성 제약, 겪은 것

새 티켓을 시작하기 전에 `principles.md`와 `plan.md`를 먼저 읽는다.
