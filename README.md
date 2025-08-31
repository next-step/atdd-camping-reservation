# 초록 캠핑장 예약 시스템

캠핑장 예약 관리를 위한 Spring Boot 기반 웹 애플리케이션입니다.

## 빠른 시작

### 사전 요구사항
- Java 17 이상
- Gradle 7.x 이상

### 실행 방법

1. 프로젝트 클론
```bash
git clone [repository-url]
cd camping-reservation-legacy
```

2. 애플리케이션 실행
```bash
./gradlew bootRun
```

3. 접속
- API: http://localhost:8080
- H2 Console: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:campingdb`
  - Username: `sa`
  - Password: (비워두기)

