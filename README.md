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

## 주요 기능

### 1. 예약 관리
- **예약 생성**: 캠핑 사이트 예약 신청
- **예약 조회**: 예약 번호 또는 전화번호로 조회
- **예약 수정**: 예약 정보 변경
- **예약 취소**: 예약 취소 처리
- **예약 검색**: 키워드 기반 예약 검색

### 2. 캠핑 사이트 관리
- **사이트 목록 조회**: 전체 캠핑 사이트 정보
- **가용성 확인**: 특정 날짜의 예약 가능 사이트 조회
- **사이트 검색**: 날짜 범위 및 사이트 타입별 검색

### 3. 캘린더 기능
- **월별 예약 현황**: 월 단위 예약 상태 조회
- **예약 가능일 확인**: 사이트별 예약 가능 날짜 확인
