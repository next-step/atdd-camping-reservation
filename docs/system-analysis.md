# 시스템 분석 결과

## API 목록

### 예약 API (`/api/reservations`)

| 메서드 | 엔드포인트 | 목적 | 주요 파라미터 |
|--------|-----------|------|--------------|
| POST | `/api/reservations` | 새 예약 생성 | Body: customerName, startDate, endDate, siteNumber, phoneNumber |
| GET | `/api/reservations/{id}` | 예약 단건 조회 | Path: id |
| GET | `/api/reservations` | 예약 목록 조회 | Query: date, customerName (선택) |
| GET | `/api/reservations/my` | 내 예약 조회 | Query: name, phone (필수) |
| GET | `/api/reservations/calendar` | 월별 캘린더 조회 | Query: year, month, siteId |
| PUT | `/api/reservations/{id}` | 예약 수정 | Path: id, Query: confirmationCode, Body |
| DELETE | `/api/reservations/{id}` | 예약 취소 | Path: id, Query: confirmationCode |

### 사이트 API (`/api/sites`)

| 메서드 | 엔드포인트 | 목적 | 주요 파라미터 |
|--------|-----------|------|--------------|
| GET | `/api/sites` | 전체 사이트 목록 조회 | 없음 |
| GET | `/api/sites/{siteId}` | 사이트 상세 조회 | Path: siteId |
| GET | `/api/sites/{siteNumber}/availability` | 사이트 가용성 확인 | Path: siteNumber, Query: date |
| GET | `/api/sites/available` | 가용 사이트 목록 조회 | Query: date |
| GET | `/api/sites/search` | 기간별 사이트 검색 | Query: startDate, endDate, size (선택) |

---

## 발견한 비즈니스 규칙

### 예약 생성 규칙

| 규칙 | 발견 위치 |
|------|----------|
| 예약은 최대 30일까지만 가능 | ReservationService:96-98 |
| 과거 날짜로 예약 불가 | ReservationService:91-92 |
| 종료일이 시작일보다 이전일 수 없음 | ReservationService:86-87 |
| 동일 사이트, 동일 기간 중복 예약 불가 | ReservationService:137-141 |
| 사이트번호, 시작일, 종료일 필수 입력 | ReservationService:74-83 |
| 예약자명 필수 입력 | ReservationService:106-107 |
| 예약 완료 시 6자리 영숫자 확인 코드 자동 생성 | ReservationService:228-238 |

### 예약 취소/수정 규칙

| 규칙 | 발견 위치 |
|------|----------|
| 예약 취소 시 확인 코드 검증 필수 | ReservationService:306-308 |
| 예약 수정 시 확인 코드 검증 필수 | ReservationService:363-368 |
| 당일 취소 시 상태 = CANCELLED_SAME_DAY | ReservationService:311-312 |
| 사전 취소 시 상태 = CANCELLED | ReservationService:314 |

### 사이트 분류 규칙

| 규칙 | 발견 위치 |
|------|----------|
| 사이트 번호가 A로 시작: 대형 사이트 (80,000원/박) | ReservationService:152-153 |
| 사이트 번호가 B로 시작: 소형 사이트 (50,000원/박) | ReservationService:154-155 |
| 기타 사이트: 60,000원/박 | ReservationService:156-157 |

---

## 숨겨진 비즈니스 규칙

### 입력값 검증 규칙

| 규칙 | 발견 위치 |
|------|----------|
| 예약자 이름은 2자 이상 20자 이하 | ReservationService:110-114 |
| 전화번호는 10~11자리 숫자만 허용 | ReservationService:119-123 |
| 전화번호는 01로 시작해야 함 | ValidationUtils:70-72 |

### 가격 계산 규칙

| 규칙 | 금액/비율 | 발견 위치 |
|------|-----------|----------|
| 대형 사이트 (A-) 기본 가격 | 80,000원/박 | ReservationService:152-153 |
| 소형 사이트 (B-) 기본 가격 | 50,000원/박 | ReservationService:154-155 |
| 기타 사이트 기본 가격 | 60,000원/박 | ReservationService:156-157 |
| 주말 (토, 일) 할증 | +30% | ReservationService:174-175 |
| 성수기 (7~8월) 평일 할증 | +50% | ReservationService:172-173 |
| 성수기 주말 할증 | +70% | ReservationService:170-171 |

### 포인트 적립 규칙

| 규칙 | 적립률 | 발견 위치 |
|------|--------|----------|
| 기본 포인트 적립 | 5% | ReservationService:187, 954 |
| 주말 포함 예약 포인트 | 10% | ReservationService:200-201, 956-957 |
| 성수기 예약 포인트 | 3% | ReservationService:958-959 |

### 상태 관리 규칙

| 규칙 | 발견 위치 |
|------|----------|
| 당일 취소 시 상태 = CANCELLED_SAME_DAY | ReservationService:311-312 |
| 사전 취소 시 상태 = CANCELLED | ReservationService:314 |
| 예약 생성 시 상태 = CONFIRMED | Reservation 도메인 기본값 |

---

## 발견한 잠재적 버그

| 심각도 | 문제 | 영향 |
|--------|------|------|
| Critical | 동시성 제어 부재 (Thread.sleep 100ms) | 중복 예약 발생 가능 |
| Critical | 취소 예약 필터링 누락 | 취소된 기간 재예약 불가 |
| Critical | 연박 중간 날짜 가용성 미확인 | 중간 날짜 충돌 가능 |
| High | 잘못된 Repository 메서드 사용 | 가용성 판단 오류 |
| High | 가용성 확인 로직 불일치 | API별 다른 결과 |
| High | NPE 위험 (Campsite null) | 서버 에러 발생 |
| High | 예약 수정 시 중복 체크 누락 | 중복 예약 생성 가능 |

---

*분석일: 2025-01-13*
