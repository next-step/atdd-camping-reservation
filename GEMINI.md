# GEMINI.md

## 프로젝트 개요

이 프로젝트는 "초록 캠핑장 예약 시스템"이라는 이름의 캠핑장 예약 관리를 위한 Spring Boot 웹 애플리케이션입니다. Java 17과 Gradle로 빌드되었습니다. 핵심 기능은 예약 생성, 조회, 수정 및 취소입니다. 이 애플리케이션은 RESTful API를 위해 Spring Web을, 데이터베이스 상호작용을 위해 Spring Data JPA를, 그리고 인메모리 H2 데이터베이스를 사용합니다. Thymeleaf가 포함되어 있어 서버 사이드 렌더링된 프론트엔드가 있을 수 있지만, REST API에 중점을 둔 것으로 보입니다. Lombok은 상용구 코드를 줄이기 위해 사용됩니다.

## 빌드 및 실행

- **빌드:** `./gradlew build`
- **실행:** `./gradlew bootRun`
- **테스트:** `./gradlew test`

애플리케이션은 `http://localhost:8080`에서 접근할 수 있습니다. H2 데이터베이스 콘솔은 `http://localhost:8080/h2-console`에서 JDBC URL `jdbc:h2:mem:testdb`와 사용자 이름 `sa`로 사용할 수 있습니다.

## 개발 컨벤션

- **아키텍처:** 프로젝트는 표준 계층형 아키텍처를 따릅니다:
    - `controller`: HTTP 요청 및 응답을 처리합니다.
    - `service`: 비즈니스 로직을 구현합니다.
    - `domain`: JPA 엔티티를 포함합니다.
    - `repository`: Spring Data JPA를 사용하여 데이터 접근을 관리합니다.
    - `dto`: 계층 간 통신에 데이터 전송 객체(DTO)가 사용됩니다.
- **API 스타일:** API는 RESTful이며, 요청 및 응답에 JSON을 사용합니다. 엔드포인트는 `/api` 아래에 구성됩니다.
- **오류 처리:** 컨트롤러 계층에서 예외를 포착하고 응답 본문에 오류 메시지와 함께 적절한 HTTP 상태 코드를 반환합니다.
- **데이터베이스:** 애플리케이션은 `ddl-auto: create-drop`으로 인메모리 H2 데이터베이스를 사용하므로 애플리케이션이 시작될 때마다 데이터베이스가 다시 생성됩니다. 초기 데이터는 `src/main/resources/data.sql`에서 채워집니다.
- **테스팅:** REST API 테스트를 위해 `rest-assured`가 포함되어 있습니다.