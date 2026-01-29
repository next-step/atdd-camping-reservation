# 시스템 분석 결과

## API 목록 (총 12개)

| No | Endpoint | Method | 목적 |
|----|----------|--------|------|
| 1 | `/api/reservations` | POST | 예약 생성 |
| 2 | `/api/reservations/{id}` | GET | 예약 단건 조회 |
| 3 | `/api/reservations` | GET | 예약 목록 조회 |
| 4 | `/api/reservations/{id}` | DELETE | 예약 취소 |
| 5 | `/api/reservations/{id}` | PUT | 예약 수정 |
| 6 | `/api/reservations/my` | GET | 내 예약 조회 |
| 7 | `/api/reservations/calendar` | GET | 월별 캘린더 조회 |
| 8 | `/api/sites` | GET | 전체 사이트 목록 |
| 9 | `/api/sites/{siteId}` | GET | 사이트 상세 조회 |
| 10 | `/api/sites/{siteNumber}/availability` | GET | 특정 사이트 가용성 확인 |
| 11 | `/api/sites/available` | GET | 가용 사이트 목록 |
| 12 | `/api/sites/search` | GET | 기간별 사이트 검색 |

---

## 발견한 비즈니스 규칙

### 1. 명시된 비즈니스 규칙 (requirements.md 기준)

| 규칙 | 구현 위치 | 구현 상태 |
|------|----------|----------|
| 예약은 30일 이내만 가능 | `ReservationService:96` | 구현됨 (단, 오늘로부터 30일이 아닌 예약 기간이 30일) |
| 과거 날짜 예약 불가 | `ReservationService:91` | 구현됨 |
| 종료일이 시작일보다 이전 불가 | `ReservationService:86` | 구현됨 |
| 예약자 이름 필수 (2~20자) | `ReservationService:106-114` | 구현됨 |
| 전화번호 검증 | `ReservationService:118-131` | 구현됨 (10~11자리 숫자) |
| 중복 예약 방지 | `ReservationService:137-141` | **부분 구현** (취소된 예약 제외 안됨) |
| 확인 코드 6자리 생성 | `ReservationService:228-238` | 구현됨 |
| 당일 취소 시 환불 불가 | `ReservationService:311-315` | 상태만 구분 (CANCELLED_SAME_DAY) |

### 2. 숨겨진 비즈니스 규칙 (코드에서 발견)

#### 가격 정책
- **기본 가격** (`ReservationService:152-158`)
  - A로 시작하는 사이트 (대형): 80,000원/박
  - B로 시작하는 사이트 (소형): 50,000원/박
  - 기타: 60,000원/박

- **할증 정책** (`ReservationService:170-176`)
  - 주말(토/일): 30% 할증
  - 성수기(7~8월): 50% 할증
  - 성수기 주말: 70% 할증

#### 포인트 적립 정책
- **기본 적립률** (`ReservationService:187-204`)
  - 평일: 5%
  - 주말 포함 예약: 10%

- **결제수단별 적립률** (`ReservationService:591-601`)
  - 카드: 10%
  - 모바일: 8%
  - 계좌이체: 5%
  - 현금: 3%

#### 사이트 분류 규칙
- **사이트 번호 체계** (`SiteService:48-49, 88-94`)
  - A로 시작: 대형, 전기 사용 가능
  - B로 시작: 소형, 전기 사용 불가

#### 예약 상태 관리
- **기본 상태** (`Reservation:49-51`)
  - 생성 시 자동으로 `CONFIRMED` 상태
- **취소 상태**
  - 당일 취소: `CANCELLED_SAME_DAY`
  - 사전 취소: `CANCELLED`

---

## 발견된 버그 및 문제점

### 1. 연박 예약 시 중간 날짜 미검증 (Critical)
- **위치**: `SiteService:101-106` (searchAvailableSites)
- **문제**: 시작일과 종료일만 검사하고 중간 날짜는 검사하지 않음
- **영향**: 연박 예약 시 중간에 이미 예약이 있어도 예약 가능으로 표시됨
```java
// 현재 코드: 시작일과 종료일만 검사
boolean startAvailable = !reservationRepository.existsByCampsiteAndReservationDate(site, request.getStartDate());
boolean endAvailable = !reservationRepository.existsByCampsiteAndReservationDate(site, request.getEndDate());
```

### 2. 취소된 예약이 중복 체크에서 제외되지 않음 (Critical)
- **위치**: `ReservationService:137-141`
- **문제**: CANCELLED 상태의 예약도 중복 체크에 포함됨
- **영향**: 취소된 예약이 있으면 해당 기간에 새 예약 불가

### 3. 예약 수정 시 중복 검사 누락 (High)
- **위치**: `ReservationService:358-431` (updateReservation)
- **문제**: 날짜나 사이트 변경 시 중복 예약 검사가 없음
- **영향**: 수정으로 인한 중복 예약 발생 가능

### 4. 동시성 제어 미흡 (High)
- **위치**: `ReservationService:210-214`
- **문제**: Thread.sleep(100)으로 동시성 제어 시도
- **영향**: 동시 예약 요청 시 중복 예약 발생 가능
```java
// 동시성 문제 재현을 위한 지연 - 실제 동시성 제어 아님
try {
    Thread.sleep(100);
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
}
```

### 5. 30일 제한 규칙 불일치 (Medium)
- **위치**: `ReservationService:95-98`
- **문제**: "오늘로부터 30일 이내"가 아닌 "예약 기간이 30일 이내"로 구현
- **요구사항**: 예약은 오늘로부터 30일 이내에만 가능

### 6. 가용성 확인 API 불일치 (Medium)
- **위치**: `SiteService:43` vs `SiteService:159`
- **문제**: `getAvailableSites`와 `isAvailable`이 서로 다른 쿼리 사용
  - `existsByCampsiteAndReservationDate` (단일 날짜)
  - `existsByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual` (기간)

---

## 데이터 모델

### Reservation (예약)
| 필드 | 타입 | 설명 | 제약조건 |
|------|------|------|---------|
| id | Long | PK | 자동생성 |
| customerName | String | 예약자명 | NOT NULL, 2~20자 |
| startDate | LocalDate | 시작일 | NOT NULL |
| endDate | LocalDate | 종료일 | NOT NULL |
| reservationDate | LocalDate | 예약 기준일 | - |
| campsite | Campsite | 캠핑사이트 | FK, NOT NULL |
| phoneNumber | String | 전화번호 | 10~11자리 |
| status | String | 상태 | CONFIRMED/CANCELLED/CANCELLED_SAME_DAY |
| confirmationCode | String | 확인코드 | 6자리 영숫자 |
| createdAt | LocalDateTime | 생성일시 | 자동생성 |

### Campsite (캠핑사이트)
| 필드 | 타입 | 설명 | 제약조건 |
|------|------|------|---------|
| id | Long | PK | 자동생성 |
| siteNumber | String | 사이트번호 | UNIQUE, NOT NULL |
| description | String | 설명 | - |
| maxPeople | Integer | 최대인원 | - |

---

## 검증 규칙 요약

| 항목 | 규칙 | 에러 메시지 |
|------|------|------------|
| 사이트번호 | 필수, 존재해야 함 | "사이트 번호를 입력해주세요." / "존재하지 않는 캠핑장입니다." |
| 시작일/종료일 | 필수 | "예약 기간을 선택해주세요." |
| 날짜 순서 | 종료일 >= 시작일 | "종료일이 시작일보다 이전일 수 없습니다." |
| 과거 날짜 | 시작일 >= 오늘 | "과거 날짜로 예약할 수 없습니다." |
| 예약 기간 | <= 30일 | "예약 기간은 최대 30일입니다." |
| 예약자명 | 필수, 2~20자 | "예약자 이름을 입력해주세요." / "예약자 이름은 최소 2자 이상이어야 합니다." |
| 전화번호 | 10~11자리 숫자 | "전화번호 형식이 올바르지 않습니다." |
| 중복 예약 | 불가 | "해당 기간에 이미 예약이 존재합니다." |
| 확인코드 | 취소/수정 시 필수 | "확인 코드가 일치하지 않습니다." |
