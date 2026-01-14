# API 명세서

---

## 1. API 목록

### 1.1 REST API - ReservationController

| 번호 | Endpoint | Method | 목적 | 주요 파라미터 | 비고 |
|:----:|----------|--------|------|--------------|------|
| 1.1.1 | `/api/reservations` | POST | 예약 생성 | Body: `ReservationRequest` | 성공 시 201 Created |
| 1.1.2 | `/api/reservations/{id}` | GET | 예약 상세 조회 | Path: `id` | 실패 시 404 Not Found |
| 1.1.3 | `/api/reservations` | GET | 예약 목록 조회 | Query: `date`, `customerName` (선택) | 필터링 가능 |
| 1.1.4 | `/api/reservations/{id}` | DELETE | 예약 취소 | Path: `id`, Query: `confirmationCode` | 확인코드 필수 |
| 1.1.5 | `/api/reservations/{id}` | PUT | 예약 수정 | Path: `id`, Query: `confirmationCode`, Body: `ReservationRequest` | 확인코드 필수 |
| 1.1.6 | `/api/reservations/my` | GET | 내 예약 조회 | Query: `name`, `phone` | 이름+전화번호로 조회 |
| 1.1.7 | `/api/reservations/calendar` | GET | 월별 캘린더 조회 | Query: `year`, `month`, `siteId` | 사이트별 예약 현황 |

#### 1.1.1 POST /api/reservations - 예약 생성

**Request Body:**
```json
{
  "customerName": "홍길동",
  "startDate": "2026-02-01",
  "endDate": "2026-02-03",
  "siteNumber": "A-1",
  "phoneNumber": "010-1234-5678",
  "numberOfPeople": 4,
  "carNumber": "12가3456",
  "requests": "조용한 자리 부탁드립니다"
}
```

**성공 응답 (201 Created):**
```json
{
  "id": 1,
  "customerName": "홍길동",
  "startDate": "2026-02-01",
  "endDate": "2026-02-03",
  "siteNumber": "A1",
  "phoneNumber": "010-1234-5678",
  "status": "CONFIRMED",
  "confirmationCode": "ABC123",
  "createdAt": "2026-01-13T10:30:00"
}
```

**실패 응답 (409 Conflict):**

| 케이스 | 에러 메시지 |
|--------|-------------|
| 사이트 번호 미입력 | 사이트 번호를 입력해주세요. |
| 존재하지 않는 사이트 | 존재하지 않는 캠핑장입니다. |
| 예약 기간 미선택 | 예약 기간을 선택해주세요. |
| 종료일이 시작일보다 이전 | 종료일이 시작일보다 이전일 수 없습니다. |
| 과거 날짜 예약 | 과거 날짜로 예약할 수 없습니다. |
| 30일 초과 예약 | 예약 기간은 최대 30일입니다. |
| 예약자 이름 미입력 | 예약자 이름을 입력해주세요. |
| 이름 2자 미만 | 예약자 이름은 최소 2자 이상이어야 합니다. |
| 이름 20자 초과 | 예약자 이름은 최대 20자까지 가능합니다. |
| 전화번호 형식 오류 | 전화번호 형식이 올바르지 않습니다. |
| 전화번호 숫자 아님 | 전화번호는 숫자만 입력 가능합니다. |
| 기간 중복 예약 존재 | 해당 기간에 이미 예약이 존재합니다. |

> 응답 형식: `{"message": "에러 메시지"}`

---

#### 1.1.2 GET /api/reservations/{id} - 예약 상세 조회

**성공 응답 (200 OK):**
```json
{
  "id": 1,
  "customerName": "홍길동",
  "startDate": "2026-02-01",
  "endDate": "2026-02-03",
  "siteNumber": "A1",
  "phoneNumber": "010-1234-5678",
  "status": "CONFIRMED",
  "confirmationCode": "ABC123",
  "createdAt": "2026-01-13T10:30:00"
}
```

**실패 응답 (404 Not Found):**
```json
{
  "message": "예약을 찾을 수 없습니다."
}
```

---

#### 1.1.3 GET /api/reservations - 예약 목록 조회

**Query Parameters (모두 선택):**
- `date`: 특정 날짜 예약 필터 (예: `2026-02-01`)
- `customerName`: 고객명 필터

**성공 응답 (200 OK):**
```json
[
  {
    "id": 1,
    "customerName": "홍길동",
    "startDate": "2026-02-01",
    "endDate": "2026-02-03",
    "siteNumber": "A1",
    "phoneNumber": "010-1234-5678",
    "status": "CONFIRMED",
    "confirmationCode": "ABC123",
    "createdAt": "2026-01-13T10:30:00"
  },
  {
    "id": 2,
    "customerName": "김철수",
    "startDate": "2026-02-05",
    "endDate": "2026-02-07",
    "siteNumber": "B3",
    "phoneNumber": "010-9876-5432",
    "status": "CONFIRMED",
    "confirmationCode": "XYZ789",
    "createdAt": "2026-01-13T11:00:00"
  }
]
```

---

#### 1.1.4 DELETE /api/reservations/{id} - 예약 취소

**Query Parameters (필수):**
- `confirmationCode`: 예약 확인 코드

**성공 응답 (200 OK):**
```json
{
  "message": "예약이 취소되었습니다."
}
```

**실패 응답 (400 Bad Request):**

| 케이스 | 에러 메시지 |
|--------|-------------|
| 예약 없음 | 예약을 찾을 수 없습니다. |
| 확인코드 불일치 | 확인 코드가 일치하지 않습니다. |

> 응답 형식: `{"message": "에러 메시지"}`

**상태 변경 규칙:**
- 당일 취소: `status` → `"CANCELLED_SAME_DAY"`
- 사전 취소: `status` → `"CANCELLED"`

---

#### 1.1.5 PUT /api/reservations/{id} - 예약 수정

**Query Parameters (필수):**
- `confirmationCode`: 예약 확인 코드

**Request Body (수정할 필드만):**
```json
{
  "customerName": "홍길동(수정)",
  "startDate": "2026-02-02",
  "endDate": "2026-02-04",
  "siteNumber": "A2",
  "phoneNumber": "010-1111-2222"
}
```

**성공 응답 (200 OK):**
```json
{
  "id": 1,
  "customerName": "홍길동(수정)",
  "startDate": "2026-02-02",
  "endDate": "2026-02-04",
  "siteNumber": "A2",
  "phoneNumber": "010-1111-2222",
  "status": "CONFIRMED",
  "confirmationCode": "ABC123",
  "createdAt": "2026-01-13T10:30:00"
}
```

**실패 응답 (400 Bad Request):**

| 케이스 | 에러 메시지 |
|--------|-------------|
| 예약 없음 | 예약을 찾을 수 없습니다. |
| 확인코드 미입력 | 확인 코드를 입력해주세요. |
| 확인코드 불일치 | 확인 코드가 일치하지 않습니다. |
| 종료일이 시작일보다 이전 | 종료일이 시작일보다 이전일 수 없습니다. |
| 과거 날짜 예약 | 과거 날짜로 예약할 수 없습니다. |
| 예약자 이름 빈값 | 예약자 이름을 입력해주세요. |
| 존재하지 않는 사이트 | 존재하지 않는 캠핑장입니다. |

> 응답 형식: `{"message": "에러 메시지"}`

---

#### 1.1.6 GET /api/reservations/my - 내 예약 조회

**Query Parameters (필수):**
- `name`: 예약자 이름
- `phone`: 전화번호

**성공 응답 (200 OK):**
```json
[
  {
    "id": 1,
    "customerName": "홍길동",
    "startDate": "2026-02-01",
    "endDate": "2026-02-03",
    "siteNumber": "A1",
    "phoneNumber": "010-1234-5678",
    "status": "CONFIRMED",
    "confirmationCode": "ABC123",
    "createdAt": "2026-01-13T10:30:00"
  }
]
```

**실패 응답 (400 Bad Request):**

| 케이스 | 에러 메시지 |
|--------|-------------|
| 이름 미입력 | 이름을 입력해주세요. |
| 전화번호 미입력 | 전화번호를 입력해주세요. |
| 전화번호 형식 오류 | 전화번호 형식이 올바르지 않습니다. |

> 응답 형식: `{"message": "에러 메시지"}`

---

#### 1.1.7 GET /api/reservations/calendar - 월별 캘린더 조회

**Query Parameters (필수):**
- `year`: 연도 (예: 2026)
- `month`: 월 (예: 2)
- `siteId`: 사이트 ID

**성공 응답 (200 OK):**
```json
{
  "year": 2026,
  "month": 2,
  "siteId": 1,
  "siteNumber": "A1",
  "days": [
    {
      "date": "2026-02-01",
      "available": false,
      "customerName": "홍길동",
      "reservationId": 1
    },
    {
      "date": "2026-02-02",
      "available": false,
      "customerName": "홍길동",
      "reservationId": 1
    },
    {
      "date": "2026-02-03",
      "available": true,
      "customerName": null,
      "reservationId": null
    }
  ],
  "summary": {
    "totalDays": 28,
    "reservedDays": 5,
    "availableDays": 23
  }
}
```

**실패 케이스 (RuntimeException):**
```json
{
  "message": "사이트를 찾을 수 없습니다."
}
```

---

### 1.2 REST API - SiteController

| 번호 | Endpoint | Method | 목적 | 주요 파라미터 | 비고 |
|:----:|----------|--------|------|--------------|------|
| 1.2.1 | `/api/sites` | GET | 전체 사이트 목록 | - | 모든 캠핑장 사이트 반환 |
| 1.2.2 | `/api/sites/{siteId}` | GET | 사이트 상세 조회 | Path: `siteId` | ID로 조회 |
| 1.2.3 | `/api/sites/{siteNumber}/availability` | GET | 사이트 가용성 확인 | Path: `siteNumber`, Query: `date` | 특정 날짜 예약 가능 여부 |
| 1.2.4 | `/api/sites/available` | GET | 예약 가능 사이트 목록 | Query: `date` | 해당 날짜 가용 사이트만 |
| 1.2.5 | `/api/sites/search` | GET | 사이트 검색 | Query: `startDate`, `endDate`, `size` (선택) | 기간+크기 필터링 |

#### 1.2.1 GET /api/sites - 전체 사이트 목록

**성공 응답 (200 OK):**
```json
[
  {
    "id": 1,
    "siteNumber": "A1",
    "description": "대형 캠핑카 사이트",
    "maxPeople": 6,
    "size": "대형",
    "hasElectricity": true,
    "toiletDistance": 10,
    "facilities": "화장실, 샤워장, 개수대",
    "rules": "22시 이후 소음 금지, 직화 금지"
  },
  {
    "id": 2,
    "siteNumber": "B1",
    "description": "소형 텐트 사이트",
    "maxPeople": 4,
    "size": "소형",
    "hasElectricity": true,
    "toiletDistance": 10,
    "facilities": "화장실, 샤워장, 개수대",
    "rules": "22시 이후 소음 금지, 직화 금지"
  }
]
```

---

#### 1.2.2 GET /api/sites/{siteId} - 사이트 상세 조회

**성공 응답 (200 OK):**
```json
{
  "id": 1,
  "siteNumber": "A1",
  "description": "대형 캠핑카 사이트",
  "maxPeople": 6,
  "size": "대형",
  "hasElectricity": true,
  "toiletDistance": 10,
  "facilities": "화장실, 샤워장, 개수대",
  "rules": "22시 이후 소음 금지, 직화 금지"
}
```

**실패 케이스 (RuntimeException):**
```json
{
  "message": "사이트를 찾을 수 없습니다."
}
```

---

#### 1.2.3 GET /api/sites/{siteNumber}/availability - 사이트 가용성 확인

**Query Parameters (필수):**
- `date`: 조회할 날짜 (예: `2026-02-01`)

**성공 응답 (200 OK):**
```json
{
  "siteNumber": "A1",
  "date": "2026-02-01",
  "available": true
}
```

**실패 응답 (400 Bad Request):**

| 케이스 | 에러 메시지 |
|--------|-------------|
| 사이트 번호 미입력 | 사이트 번호를 입력해주세요. |
| 날짜 미선택 | 날짜를 선택해주세요. |
| 과거 날짜 조회 | 과거 날짜는 조회할 수 없습니다. |
| 존재하지 않는 사이트 | 사이트를 찾을 수 없습니다: A99 |

> 응답 형식: `{"message": "에러 메시지"}`

---

#### 1.2.4 GET /api/sites/available - 예약 가능 사이트 목록

**Query Parameters (필수):**
- `date`: 조회할 날짜 (예: `2026-02-01`)

**성공 응답 (200 OK):**
```json
[
  {
    "siteId": 1,
    "siteNumber": "A1",
    "size": "대형",
    "hasElectricity": true,
    "date": "2026-02-01",
    "available": true,
    "maxPeople": 6,
    "description": "대형 캠핑카 사이트"
  },
  {
    "siteId": 3,
    "siteNumber": "B1",
    "size": "소형",
    "hasElectricity": false,
    "date": "2026-02-01",
    "available": true,
    "maxPeople": 4,
    "description": "소형 텐트 사이트"
  }
]
```

---

#### 1.2.5 GET /api/sites/search - 사이트 검색

**Query Parameters:**
- `startDate` (필수): 시작일 (예: `2026-02-01`)
- `endDate` (필수): 종료일 (예: `2026-02-03`)
- `size` (선택): 사이트 크기 (`대형`, `소형`, `일반`)

**성공 응답 (200 OK):**
```json
[
  {
    "siteId": 1,
    "siteNumber": "A1",
    "size": "대형",
    "hasElectricity": true,
    "date": "2026-02-01",
    "available": true,
    "maxPeople": 6,
    "description": "대형 캠핑카 사이트"
  }
]
```

**실패 응답 (400 Bad Request):**

| 케이스 | 에러 메시지 |
|--------|-------------|
| 검색 기간 미선택 | 검색 기간을 선택해주세요. |
| 종료일이 시작일보다 이전 | 종료일이 시작일보다 이전일 수 없습니다. |
| 과거 날짜 검색 | 과거 날짜는 검색할 수 없습니다. |

> 응답 형식: `{"message": "에러 메시지"}`

---

### 1.3 MVC Controller - HomeController

| Endpoint | Method | 목적 | 렌더링 뷰 | 비고 |
|----------|--------|------|-----------|------|
| `/` | GET | 메인 페이지 | `index` | 홈 화면 |
| `/reservations` | GET | 예약 목록 페이지 | `reservation/list` | 날짜 필터링 가능 |
| `/reservations/new` | GET | 예약 폼 페이지 | `reservation/form` | 캠핑장 목록 포함 |
| `/reservations/search` | GET | 예약 검색 페이지 | `reservation/search` | 검색 화면 |
| `/sites` | GET | 사이트 목록 페이지 | `sites/list` | 전체 사이트 목록 |
| `/sites/{siteNumber}` | GET | 사이트 상세 페이지 | `sites/detail` | 사이트 번호로 조회 |

---

## 2. 핵심 비즈니스 규칙

### 2.1 예약 규칙

| 규칙명 | 상세 내용 | 적용 위치 |
|--------|----------|-----------|
| 예약 기간 제한 | 최대 30일까지만 예약 가능 | `ReservationService:96` |
| 과거 날짜 예약 불가 | 시작일이 오늘보다 이전일 수 없음 | `ReservationService:91` |
| 날짜 순서 검증 | 종료일이 시작일보다 이전일 수 없음 | `ReservationService:86` |
| 중복 예약 방지 | 동일 사이트의 동일 기간 중복 예약 불가 | `ReservationService:137` |
| 당일 취소 구분 | 예약 당일 취소 시 `CANCELLED_SAME_DAY` 상태로 변경 | `ReservationService:311` |

### 2.2 고객 정보 유효성

| 규칙명 | 상세 내용 | 적용 위치 |
|--------|----------|-----------|
| 이름 필수 | 예약자 이름 필수 입력 | `ReservationService:106` |
| 이름 길이 | 최소 2자, 최대 20자 | `ReservationService:110-114` |
| 전화번호 형식 | 10~11자리 숫자 (하이픈 허용) | `ReservationService:118-131` |
| 확인코드 형식 | 6자리 영문대문자+숫자 | `ValidationUtils:120-142` |

### 2.3 가격 정책

| 규칙명 | 상세 내용 | 금액/비율 |
|--------|----------|-----------|
| 대형 사이트 기본가 | A로 시작하는 사이트 | 80,000원/1박 |
| 소형 사이트 기본가 | B로 시작하는 사이트 | 50,000원/1박 |
| 일반 사이트 기본가 | 기타 사이트 | 60,000원/1박 |
| 주말 할증 | 토요일, 일요일 | +30% |
| 성수기 할증 | 7월~8월 | +50% |
| 성수기 주말 할증 | 7월~8월 + 주말 | +70% |

### 2.4 포인트 적립 정책

| 규칙명 | 상세 내용 | 적립률 |
|--------|----------|--------|
| 기본 적립 | 평일 예약 | 5% |
| 주말 적립 | 주말 포함 예약 | 10% |
| 성수기 적립 | 성수기 예약 (할인) | 3% |

### 2.5 사이트 분류 규칙

| 사이트 번호 | 크기 | 전기 사용 | 최대 인원 |
|-------------|------|-----------|-----------|
| A1 ~ A10 | 대형 | 가능 | 설정값 |
| B1 ~ B10 | 소형 | 불가 | 설정값 |

---

## 3. 잠재적 버그 및 코드 스멜

### 3.1 심각도 높음 (Critical)

#### 3.1.1 동시성 문제 (Race Condition)

| 위치 | 문제 | 영향 |
|------|------|------|
| `ReservationService.java:137-141, 210-214` | 예약 충돌 검사 후 저장까지 시간 간격 존재 (`Thread.sleep(100)`) | 동시 요청 시 중복 예약 발생 가능 |

**문제 코드:**
```java
// 예약 충돌 검사
boolean hasConflict = reservationRepository.existsByCampsiteAnd...();
if (hasConflict) throw new RuntimeException(...);

// 동시성 문제 재현을 위한 지연 (주석에 명시됨)
Thread.sleep(100);  // ← 위험: 이 사이에 다른 요청이 저장할 수 있음

// 예약 저장
Reservation saved = reservationRepository.save(reservation);
```

**권장 해결 방안:**
- 비관적 잠금 (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) 적용
- 또는 DB 유니크 제약조건 추가

---

#### 3.1.2 N+1 쿼리 및 성능 문제

| 위치 | 문제 | 영향 |
|------|------|------|
| `ReservationService.java:285-288` | `findAll()` 후 메모리에서 필터링 | 데이터 증가 시 성능 급격히 저하 |
| `ReservationService.java:674-691` | 캘린더 조회 시 모든 예약 조회 | 대용량 데이터에서 OOM 가능 |
| `ReservationService.java:734-747` | 일별 통계 - 전체 조회 후 필터링 | 불필요한 데이터 로드 |

**문제 코드 예시:**
```java
// getReservationsByDate()
List<Reservation> reservations = reservationRepository.findAll().stream()
    .filter(r -> r.getStartDate() != null && r.getEndDate() != null)
    .filter(r -> !date.isBefore(r.getStartDate()) && !date.isAfter(r.getEndDate()))
    .collect(Collectors.toList());
```

**권장 해결 방안:**
- Repository에 적절한 쿼리 메서드 추가
- JPQL 또는 QueryDSL 활용

---

### 3.2 심각도 중간 (Medium)

#### 3.2.1 코드 중복 (DRY 원칙 위반)

| 중복 유형 | 위치 | 설명 |
|-----------|------|------|
| 날짜 검증 로직 | `ReservationService:82-100`, `SiteService:63-79`, `ValidationUtils:90-114` | 동일한 날짜 검증이 3곳에 중복 |
| DTO 변환 로직 | `ReservationService:262-271`, `341-352`, `420-428`, `452-463` | `ReservationResponse.from()` 대신 직접 변환 |
| 사이트 크기 결정 | `SiteService:48`, `SiteService:88-94`, `SiteService:109-115` | 사이트 번호 → 크기 매핑이 여러 곳에 하드코딩 |
| 가격 계산 로직 | `ReservationService:146-180`, `ReservationService:513-541` | 동일한 가격 계산이 두 번 구현됨 |

**권장 해결 방안:**
- `ValidationUtils` 클래스 활용 (현재 미사용)
- `ReservationResponse.from()` 일관되게 사용
- 사이트 타입 Enum 도입

---

#### 3.2.2 Deprecated 코드 미정리

| 위치 | 문제 | 설명 |
|------|------|------|
| `CalendarService.java` | 전체가 Deprecated | ReservationService로 통합됨, 아직 코드 존재 |
| `ReservationService.java:476` | `processReservationWithPayment()` | Deprecated 표시됨, 200줄 이상의 거대 메서드 |
| `ReservationController.java:26-29` | 주석 처리된 CalendarService 의존성 | 주석만 남아있음 |

---

#### 3.2.3 깊은 중첩 (Deep Nesting)

| 위치 | 중첩 레벨 | 설명 |
|------|-----------|------|
| `ReservationService.createReservation()` | 4~5 단계 | if-else 체인이 깊게 중첩됨 |

**문제 코드 패턴:**
```java
if (siteNumber == null) {
    throw ...
} else {
    if (startDate == null) {
        throw ...
    } else {
        if (endDate.isBefore(startDate)) {
            throw ...
        } else {
            if (startDate.isBefore(today)) {
                throw ...
            } else {
                // 실제 로직
            }
        }
    }
}
```

**권장 해결 방안:**
- Early Return 패턴 적용
- 검증 로직 별도 메서드로 추출

---

#### 3.2.4 매직 넘버 및 하드코딩

| 위치 | 하드코딩 값 | 설명 |
|------|-------------|------|
| `ReservationService:153-158` | 80000, 50000, 60000 | 사이트 종류별 가격 |
| `ReservationService:171-176` | 1.7, 1.5, 1.3 | 할증 비율 |
| `ReservationService:96` | 30 | 최대 예약 일수 |
| `SiteService:48-49` | "A", "대형" | 사이트 타입 매핑 |

**권장 해결 방안:**
- 상수 클래스 또는 Enum으로 추출
- application.yml 설정으로 외부화

---

### 3.3 심각도 낮음 (Low)

#### 3.3.1 일관성 없는 예외 처리

| 문제 | 설명 |
|------|------|
| RuntimeException만 사용 | 모든 예외가 `RuntimeException`으로 처리됨 |
| 에러 메시지 하드코딩 | 국제화(i18n) 불가능 |
| 예외 타입 미구분 | 비즈니스 예외와 시스템 예외 구분 없음 |

**권장 해결 방안:**
- 커스텀 예외 클래스 도입 (`ReservationNotFoundException`, `DuplicateReservationException` 등)
- `@ControllerAdvice`를 통한 전역 예외 처리

---

#### 3.3.2 Repository 메서드 불일치

| 메서드 | 사용처 | 문제 |
|--------|--------|------|
| `existsByCampsiteAndReservationDate()` | `SiteService.getAvailableSites()` | `reservationDate` 필드 기반 (단일 날짜) |
| `existsByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual()` | `ReservationService`, `SiteService.isAvailable()` | `startDate`~`endDate` 범위 기반 |

**문제:** 두 메서드가 다른 로직으로 예약 존재 여부를 확인하여 결과 불일치 가능

---

#### 3.3.3 미사용 유틸리티

| 클래스 | 상태 | 설명 |
|--------|------|------|
| `ValidationUtils` | 미사용 | 검증 메서드가 정의되어 있지만 서비스에서 직접 검증 |
| `StringUtils` | 미확인 | 사용 여부 불명확 |

---

#### 3.3.4 Getter/Setter 남용 (빈약한 도메인 모델)

| 엔티티 | 문제 | 설명 |
|--------|------|------|
| `Reservation` | 비즈니스 로직 부재 | 모든 로직이 Service에 집중됨 |
| `Campsite` | 단순 데이터 홀더 | 사이트 타입 판별 로직이 외부에 존재 |

**권장 해결 방안:**
- 도메인 주도 설계(DDD) 원칙 적용
- 엔티티에 비즈니스 로직 캡슐화

---

### 3.4 잠재적 Null 처리 문제

| 위치 | 필드 | 문제 |
|------|------|------|
| `ReservationService:336-337` | `phoneNumber` | null 체크 후 `.contains()` 호출 |
| `ReservationService:680-681` | `startDate`, `endDate` | null 체크하지만 이후 로직에서 NPE 가능 |
| `Reservation.java:38-39` | `phoneNumber`, `status` | nullable 필드, 조회 시 null 가능 |

**권장 해결 방안:**
- Optional 활용
- Bean Validation (`@NotNull`, `@NotBlank`) 적용

---

## 4. 요약

### 4.1 우선순위별 개선 사항

| 우선순위 | 항목 | 예상 영향 |
|----------|------|-----------|
| **1 (긴급)** | 동시성 문제 해결 | 데이터 무결성 |
| **2 (높음)** | N+1 쿼리 최적화 | 성능 개선 |
| **3 (중간)** | 코드 중복 제거 | 유지보수성 |
| **4 (낮음)** | 예외 처리 개선 | 안정성 |
| **5 (낮음)** | Deprecated 코드 정리 | 코드 품질 |

### 4.2 아키텍처 개선 제안

1. **검증 레이어 분리**: `ValidationUtils` 활용 또는 Spring Validation 적용
2. **예외 처리 표준화**: 커스텀 예외 + 전역 핸들러
3. **도메인 모델 강화**: 엔티티에 비즈니스 로직 이동
4. **쿼리 최적화**: Repository 메서드 추가, 페이징 적용
5. **설정 외부화**: 가격, 할증률 등 설정 파일로 이동

---
