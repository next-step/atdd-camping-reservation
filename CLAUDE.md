# CLAUDE.md

이 파일은 이 저장소에서 코드를 작업할 때 Claude Code (claude.ai/code)에게 가이드를 제공합니다.

## 프로젝트 개요

초록 캠핑장 예약 시스템은 Spring Boot 3.2.0 기반의 캠핑장 예약 관리 시스템입니다. 예약, 취소, 사이트 가용성 확인, 캘린더 뷰 기능을 제공합니다.

## 주요 개발 명령어

### 빌드 & 실행
- **애플리케이션 실행**: `./gradlew bootRun`
- **프로젝트 빌드**: `./gradlew build`
- **클린 빌드**: `./gradlew clean build`
- **테스트 실행**: `./gradlew test`

### 데이터베이스 접속
- **H2 콘솔**: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:testdb`
  - 사용자명: `sa`
  - 비밀번호: (비어있음)

## 아키텍처 개요

### 패키지 구조
```
com.camping.legacy/
├── controller/          # REST API 엔드포인트
│   ├── HomeController      # 홈페이지 및 내비게이션
│   ├── ReservationController # 예약 CRUD 작업
│   └── SiteController      # 캠핑 사이트 및 사이트 검색
├── domain/             # 핵심 비즈니스 엔티티
│   ├── Campsite           # 캠핑 사이트 엔티티
│   └── Reservation        # 예약 엔티티
├── dto/                # 데이터 전송 객체
├── repository/         # JPA 레포지토리
├── service/            # 비즈니스 로직 계층
│   ├── ReservationService # 예약 비즈니스 로직
│   ├── CampsiteService   # 캠핑 사이트 관리
│   ├── SiteService       # 사이트 가용성 확인
│   └── CalendarService   # 캘린더 뷰 기능
```

### 기술 스택
- **프레임워크**: Spring Boot 3.2.0 with Java 17
- **데이터베이스**: H2 (개발용 인메모리)
- **ORM**: Spring Data JPA with Hibernate
- **템플릿 엔진**: Thymeleaf
- **테스팅**: JUnit Platform, REST Assured 통합 테스트
- **빌드 도구**: Gradle 7.x

## 주요 비즈니스 규칙 및 기능

### 예약 시스템
- **30일 제한**: 오늘부터 30일 이내에만 예약 가능
- **동시성 제어**: 동일 사이트/날짜에 대한 여러 동시 요청 시 하나만 성공
- **확인 코드**: 각 예약에 대해 6자리 영숫자 확인 코드 생성
- **취소 정책**: 당일 취소는 환불 불가
- **연박 예약**: 연속된 밤 예약의 경우 전체 기간 가용성 검증 필요

### 사이트 관리
- **사이트 유형**: A-접두사 (대형), B-접두사 (소형)
- **가용성 확인**: 단일 날짜 및 날짜 범위에 대한 실시간 가용성 확인
- **캘린더 뷰**: 특정 사이트의 월별 예약 상태 표시

## 설정 참고사항

- **애플리케이션명**: camping-reservation-legacy
- **기본 포트**: 8080
- **데이터베이스**: H2 자동 스키마 생성, SQL 로깅 활성화
- **로깅**: com.camping 패키지 DEBUG 레벨
- **데이터 초기화**: data.sql을 사용한 초기 데이터 설정

## 테스팅 방법

프로젝트는 API 테스팅을 위해 REST Assured를 사용합니다. 별도의 테스트 실행 스크립트는 설정되어 있지 않으므로 표준 Gradle 테스트 명령어를 사용하세요.

## 개발 참고사항

- Spring Boot 관례를 따르는 명확한 관심사 분리
- 보일러플레이트 코드 줄이기 위한 Lombok 사용
- 개발 중 데이터베이스 검사를 위한 H2 콘솔 활성화
- 디버깅을 위한 SQL 포맷팅 및 로깅 활성화
- 애플리케이션 재시작 시마다 데이터베이스 스키마 재생성 (create-drop)