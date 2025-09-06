# 시스템 분석 문서

## 개요
초록 캠핑장 예약 시스템 - Spring Boot 기반 캠핑장 예약 관리 웹 애플리케이션

## REST API 명세

### 예약 관리 API

| Method | Endpoint | 설명 | 주요 파라미터 |
|--------|----------|------|--------------|
| POST | `/api/reservations` | 예약 생성 | ReservationRequest |
| GET | `/api/reservations/{id}` | 예약 조회 | id |
| GET | `/api/reservations` | 예약 목록/검색 | date, customerName |
| DELETE | `/api/reservations/{id}` | 예약 취소 | id, confirmationCode |
| PUT | `/api/reservations/{id}` | 예약 수정 | id, confirmationCode |
| GET | `/api/reservations/my` | 내 예약 조회 | name, phone |
| GET | `/api/reservations/calendar` | 월별 예약 캘린더 | year, month, siteId |

### 사이트(캠핑구역) 관리 API

| Method | Endpoint | 설명 | 주요 파라미터 |
|--------|----------|------|--------------|
| GET | `/api/sites` | 전체 사이트 목록 | - |
| GET | `/api/sites/{siteId}` | 사이트 상세 정보 | siteId |
| GET | `/api/sites/{siteNumber}/availability` | 특정 사이트 가용성 확인 | siteNumber, date |
| GET | `/api/sites/available` | 날짜별 가용 사이트 목록 | date |
| GET | `/api/sites/search` | 기간별 사이트 검색 | startDate, endDate, size |

## 비즈니스 규칙

| 규칙 | 설명 |
|------|------|
| 30일 예약 제한 | 예약은 오늘로부터 30일 이내에만 가능 |
| 동시성 제어 | 동일 사이트/기간 중복 예약 방지 (현재 버그 존재) |
| 취소 정책 | 당일 취소: 환불 불가 / 사전 취소: 전액 환불 |
| 연박 예약 | 전체 기간 가용성 확인 필수 |
| 본인 확인 | 수정/취소 시 confirmationCode 필요 |
| 필수 입력 | 예약자 이름, 전화번호 필수 |
| 날짜 제한 | 과거 날짜 예약 불가, 종료일 > 시작일 |
| 확인 코드 | 6자리 영숫자 자동 생성 |

## 주요 데이터 모델

### Campsite (캠핑 사이트)
```java
- id: Long
- siteNumber: String (A-1~A-20: 대형, B-1~B-15: 소형)
- description: String
- maxPeople: Integer
```

### Reservation (예약)
```java
- id: Long
- customerName: String
- startDate: LocalDate
- endDate: LocalDate
- campsite: Campsite
- phoneNumber: String
- status: String (CONFIRMED, CANCELLED, CANCELLED_SAME_DAY)
- confirmationCode: String (6자리)
- createdAt: LocalDateTime
```

## 아키텍처 개요

### 기술 스택
- Spring Boot 3.2.0
- Java 17
- H2 Database (인메모리)
- Spring Data JPA
- Thymeleaf

### 패키지 구조
- Controller: REST API 엔드포인트
- Service: 비즈니스 로직
- Repository: 데이터 접근
- Domain: JPA 엔티티

## 알려진 이슈

| 우선순위 | 이슈 | 설명 |
|---------|------|------|
| 🚨 치명적 | 동시성 문제 | ReservationService 56라인 Thread.sleep(100) |
| ⚠️ 높음 | 테스트 부재 | 자동화된 테스트 코드 완전 부재 |
| 📋 중간 | H2 DB 제약 | 프로덕션 환경 부적합 |
| 📋 중간 | 예외 처리 | RuntimeException만 사용 |

## 개발 명령어

### 실행
```bash
./gradlew bootRun
```

### 데이터베이스 접속
- URL: http://localhost:8080/h2-console
- JDBC: `jdbc:h2:mem:campingdb`
- Username: `sa`
- Password: (공백)