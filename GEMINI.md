# Gemini 프로젝트 분석: ATDD 캠핑 예약

## 프로젝트 개요

이 프로젝트는 "초록 캠핑장 예약 시스템"이라는 이름의 캠핑 예약 시스템입니다. Java와 Spring Boot 프레임워크를 사용하여 제작된 웹 애플리케이션입니다. 프로젝트의 주요 목표는 캠핑장 예약을 관리하는 것이며, ATDD(인수 테스트 주도 개발) 접근 방식을 따르고 레거시 코드베이스를 리팩토링하고 개선하는 것을 기반으로 하는 것으로 보입니다.

이 애플리케이션은 Thymeleaf 템플릿을 사용하는 사용자용 웹 인터페이스와 백엔드 운영을 위한 RESTful API를 모두 제공합니다.

## 기술 스택

- **언어:** Java 17
- **프레임워크:** Spring Boot 3.2.0
- **빌드 도구:** Gradle
- **데이터베이스:** H2 (인메모리 데이터베이스)
- **템플릿 엔진:** Thymeleaf (서버 사이드 렌더링 웹 페이지용)
- **핵심 의존성:** Spring Web, Spring Data JPA, Lombok, Rest-Assured (테스트용)

## 프로젝트 구조

이 프로젝트는 표준 Spring Boot 구조를 따릅니다:

- `src/main/java/com/camping/legacy`: 핵심 Java 소스 코드를 포함합니다.
  - `controller`: 들어오는 HTTP 요청을 처리합니다.
    - `HomeController.java`: 메인 웹 페이지(예: 홈, 예약 목록, 사이트 상세)의 탐색 및 렌더링을 관리합니다.
    - `ReservationController.java`: 예약 생성, 조회, 업데이트, 삭제를 위한 REST 엔드포인트(`/api/reservations`)를 제공합니다.
    - `SiteController.java`: 캠핑장 정보 및 이용 가능 여부 조회를 위한 REST 엔드포인트(`/api/sites`)를 제공합니다.
  - `service`: 예약, 사이트 및 캘린더 기능에 대한 핵심 비즈니스 로직을 구현합니다.
  - `repository`: H2 데이터베이스와 상호 작용하기 위해 Spring Data JPA를 사용하여 데이터 액세스 계층을 정의합니다.
  - `domain`: JPA 엔티티 클래스(`Reservation`, `Campsite`)를 포함합니다.
  - `dto`: (Data Transfer Objects) API 계층과 서비스 계층 간에 데이터를 전달하는 객체를 정의합니다.
- `src/main/resources`: 애플리케이션 설정 및 정적 리소스를 포함합니다.
  - `application.yml`: 메인 Spring 애플리케이션 설정 파일입니다.
  - `data.sql`: 시작 시 인메모리 H2 데이터베이스를 초기 데이터로 채우는 스크립트입니다.
  - `templates`: 웹 인터페이스를 위한 Thymeleaf HTML 템플릿을 포함합니다.

## 주요 명령어

이 프로젝트는 Gradle 래퍼(`gradlew`)를 사용하므로 Gradle을 별도로 설치할 필요가 없습니다.

- **애플리케이션 실행:**
  ```bash
  ./gradlew bootRun
  ```
  애플리케이션은 `http://localhost:8080`에서 사용할 수 있습니다.

- **테스트 실행:**
  ```bash
  ./gradlew test
  ```
  이 명령어는 프로젝트의 모든 JUnit 테스트를 실행합니다.

- **프로젝트 빌드:**
  ```bash
  ./gradlew build
  ```
  이 명령어는 코드를 컴파일하고 테스트를 실행한 다음, 애플리케이션을 `build/libs/` 디렉토리에 실행 가능한 JAR 파일로 패키징합니다.