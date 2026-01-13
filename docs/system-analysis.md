# 시스템 분석 결과

## API 목록

### 예약 API (`/api/reservations`)

#### `POST /api/reservations`

| 항목 | 내용 |
|------|------|
| 목적 | 새 예약 생성 |
| 파라미터 | Body |
| 동작 | 입력값 검증 → 사이트 존재 확인 → 날짜 검증(과거/30일 제한) → 중복 체크 → 가격 계산 → 확인코드 생성 → 저장 |

**Request Body:**
```json
{
    "customerName": "테스트",
    "startDate": "2026-02-10",
    "endDate": "2026-02-11",
    "siteNumber": "A-1",
    "phoneNumber": "010-9999-8888"
}
```

**Success Response (201 Created):**
```json
{
    "id": 6,
    "customerName": "테스트",
    "startDate": "2026-02-10",
    "endDate": "2026-02-11",
    "siteNumber": "A-1",
    "phoneNumber": "010-9999-8888",
    "status": "CONFIRMED",
    "confirmationCode": "XH1UJM",
    "createdAt": null
}
```

**Error Response - 중복 예약 (409 Conflict):**
```json
{
    "message": "해당 기간에 이미 예약이 존재합니다."
}
```

**Error Response - 예약자명 누락 (400 Bad Request):**
```json
{
    "message": "예약자 이름을 입력해주세요."
}
```

**Error Response - 과거 날짜 (400 Bad Request):**
```json
{
    "message": "과거 날짜로 예약할 수 없습니다."
}
```

**Error Response - 존재하지 않는 사이트 (400 Bad Request):**
```json
{
    "message": "존재하지 않는 캠핑장입니다."
}
```

**Error Response - 종료일이 시작일보다 이전 (400 Bad Request):**
```json
{
    "message": "종료일이 시작일보다 이전일 수 없습니다."
}
```

---

#### `GET /api/reservations/{id}`

| 항목 | 내용 |
|------|------|
| 목적 | 예약 단건 조회 |
| 파라미터 | Path - id |
| 동작 | ID로 예약 조회 |

**Success Response (200 OK):**
```json
{
    "id": 1,
    "customerName": "홍길동",
    "startDate": "2026-01-20",
    "endDate": "2026-01-22",
    "siteNumber": "A-1",
    "phoneNumber": "010-1234-5678",
    "status": "CONFIRMED",
    "confirmationCode": "ABC123",
    "createdAt": "2026-01-13T23:02:37.877679"
}
```

**Error Response (404 Not Found):**
```json
{
    "message": "예약을 찾을 수 없습니다."
}
```

---

#### `GET /api/reservations`

| 항목 | 내용 |
|------|------|
| 목적 | 예약 목록 조회 |
| 파라미터 | Query - date, customerName (선택) |
| 동작 | date → 날짜 필터 / customerName → 이름 필터 / 없으면 전체 조회 |

**Success Response - 전체 조회 (200 OK):**
```json
[
    {
        "id": 1,
        "customerName": "홍길동",
        "startDate": "2026-01-20",
        "endDate": "2026-01-22",
        "siteNumber": "A-1",
        "phoneNumber": "010-1234-5678",
        "status": "CONFIRMED",
        "confirmationCode": "ABC123",
        "createdAt": "2026-01-13T23:02:37.877679"
    },
    ...
]
```

**Success Response - 날짜 필터 (200 OK):**
`GET /api/reservations?date=2026-01-20`
```json
[
    {
        "id": 1,
        "customerName": "홍길동",
        "startDate": "2026-01-20",
        "endDate": "2026-01-22",
        "siteNumber": "A-1",
        "phoneNumber": "010-1234-5678",
        "status": "CONFIRMED",
        "confirmationCode": "ABC123",
        "createdAt": "2026-01-13T23:02:37.877679"
    }
]
```

**Success Response - 고객명 필터 (200 OK):**
`GET /api/reservations?customerName=홍길동`
```json
[
    {
        "id": 1,
        "customerName": "홍길동",
        "startDate": "2026-01-20",
        "endDate": "2026-01-22",
        "siteNumber": "A-1",
        "phoneNumber": "010-1234-5678",
        "status": "CONFIRMED",
        "confirmationCode": "ABC123",
        "createdAt": "2026-01-13T23:02:37.877679"
    }
]
```

---

#### `GET /api/reservations/my`

| 항목 | 내용 |
|------|------|
| 목적 | 내 예약 조회 |
| 파라미터 | Query - name, phone (필수) |
| 동작 | 이름+전화번호로 본인 예약 검색 |

**Success Response (200 OK):**
`GET /api/reservations/my?name=홍길동&phone=010-1234-5678`
```json
[
    {
        "id": 1,
        "customerName": "홍길동",
        "startDate": "2026-01-20",
        "endDate": "2026-01-22",
        "siteNumber": "A-1",
        "phoneNumber": "010-1234-5678",
        "status": "CONFIRMED",
        "confirmationCode": "ABC123",
        "createdAt": null
    }
]
```

---

#### `GET /api/reservations/calendar`

| 항목 | 내용 |
|------|------|
| 목적 | 월별 캘린더 조회 |
| 파라미터 | Query - year, month, siteId |
| 동작 | 사이트 조회 → 월별 예약 조회 → 날짜별 가용 상태 반환 |

**Success Response (200 OK):**
`GET /api/reservations/calendar?year=2026&month=1&siteId=1`
```json
{
    "year": 2026,
    "month": 1,
    "siteId": 1,
    "siteNumber": "A-1",
    "days": [
        {"date": "2026-01-01", "available": true, "customerName": null, "reservationId": null},
        {"date": "2026-01-02", "available": true, "customerName": null, "reservationId": null},
        ...
        {"date": "2026-01-20", "available": false, "customerName": "홍길동", "reservationId": 1},
        {"date": "2026-01-21", "available": false, "customerName": "홍길동", "reservationId": 1},
        {"date": "2026-01-22", "available": false, "customerName": "홍길동", "reservationId": 1},
        ...
        {"date": "2026-01-31", "available": true, "customerName": null, "reservationId": null}
    ],
    "summary": {
        "totalDays": 31,
        "reservedDays": 3,
        "availableDays": 28
    }
}
```

---

#### `PUT /api/reservations/{id}`

| 항목 | 내용 |
|------|------|
| 목적 | 예약 수정 |
| 파라미터 | Path - id, Query - confirmationCode, Body |
| 동작 | 확인코드 검증 → 날짜/고객정보 검증 → 필드 업데이트 |

**Request Body:**
```json
{
    "customerName": "테스트수정",
    "startDate": "2026-02-10",
    "endDate": "2026-02-12",
    "siteNumber": "A-1",
    "phoneNumber": "010-9999-8888"
}
```

**Success Response (200 OK):**
`PUT /api/reservations/6?confirmationCode=XH1UJM`
```json
{
    "id": 6,
    "customerName": "테스트수정",
    "startDate": "2026-02-10",
    "endDate": "2026-02-12",
    "siteNumber": "A-1",
    "phoneNumber": "010-9999-8888",
    "status": "CONFIRMED",
    "confirmationCode": "XH1UJM",
    "createdAt": null
}
```

**Error Response (400 Bad Request):**
```json
{
    "message": "확인 코드가 일치하지 않습니다."
}
```

---

#### `DELETE /api/reservations/{id}`

| 항목 | 내용 |
|------|------|
| 목적 | 예약 취소 |
| 파라미터 | Path - id, Query - confirmationCode |
| 동작 | 확인코드 검증 → 당일이면 CANCELLED_SAME_DAY / 사전이면 CANCELLED |

**Success Response (200 OK):**
`DELETE /api/reservations/6?confirmationCode=XH1UJM`
```json
{
    "message": "예약이 취소되었습니다."
}
```

**Error Response (400 Bad Request):**
```json
{
    "message": "확인 코드가 일치하지 않습니다."
}
```

---

### 사이트 API (`/api/sites`)

#### `GET /api/sites`

| 항목 | 내용 |
|------|------|
| 목적 | 전체 사이트 목록 조회 |
| 파라미터 | 없음 |
| 동작 | 모든 캠핑 사이트 조회 |

**Success Response (200 OK):**
```json
[
    {
        "id": 1,
        "siteNumber": "A-1",
        "description": "대형 사이트 - 전기 있음, 화장실 인근",
        "maxPeople": 6,
        "size": "대형",
        "hasElectricity": true,
        "toiletDistance": 10,
        "facilities": "화장실, 샤워장, 개수대",
        "rules": "22시 이후 소음 금지, 직화 금지"
    },
    {
        "id": 2,
        "siteNumber": "A-2",
        "description": "대형 사이트 - 전기 있음, 화장실 인근",
        "maxPeople": 6,
        "size": "대형",
        "hasElectricity": true,
        "toiletDistance": 20,
        ...
    },
    ...
]
```

---

#### `GET /api/sites/{siteId}`

| 항목 | 내용 |
|------|------|
| 목적 | 사이트 상세 조회 |
| 파라미터 | Path - siteId |
| 동작 | ID로 사이트 조회 |

**Success Response (200 OK):**
`GET /api/sites/1`
```json
{
    "id": 1,
    "siteNumber": "A-1",
    "description": "대형 사이트 - 전기 있음, 화장실 인근",
    "maxPeople": 6,
    "size": "대형",
    "hasElectricity": true,
    "toiletDistance": 10,
    "facilities": "화장실, 샤워장, 개수대",
    "rules": "22시 이후 소음 금지, 직화 금지"
}
```

**Error Response (500 Internal Server Error):**
> ⚠️ 존재하지 않는 사이트 조회 시 404가 아닌 500 에러 발생
```json
{
    "timestamp": "2026-01-13T14:57:15.968+00:00",
    "status": 500,
    "error": "Internal Server Error",
    "path": "/api/sites/999"
}
```

---

#### `GET /api/sites/{siteNumber}/availability`

| 항목 | 내용 |
|------|------|
| 목적 | 사이트 가용성 확인 |
| 파라미터 | Path - siteNumber, Query - date |
| 동작 | 사이트 번호/날짜 검증 → 과거 날짜 체크 → 해당 날짜 예약 존재 여부 확인 |

**Success Response (200 OK):**
`GET /api/sites/A-1/availability?date=2026-01-25`
```json
{
    "date": "2026-01-25",
    "available": true,
    "siteNumber": "A-1"
}
```

---

#### `GET /api/sites/available`

| 항목 | 내용 |
|------|------|
| 목적 | 가용 사이트 목록 조회 |
| 파라미터 | Query - date |
| 동작 | 전체 사이트 조회 → 해당 날짜 예약 없는 사이트만 필터링 |

**Success Response (200 OK):**
`GET /api/sites/available?date=2025-01-25`
```json
[
    {
        "siteId": 1,
        "siteNumber": "A-1",
        "size": "대형",
        "hasElectricity": true,
        "date": "2025-01-25",
        "available": true,
        "maxPeople": 6,
        "description": "대형 사이트 - 전기 있음, 화장실 인근"
    },
    {
        "siteId": 2,
        "siteNumber": "A-2",
        "size": "대형",
        "hasElectricity": true,
        "date": "2025-01-25",
        "available": true,
        ...
    },
    ...
]
```

---

#### `GET /api/sites/search`

| 항목 | 내용 |
|------|------|
| 목적 | 기간별 사이트 검색 |
| 파라미터 | Query - startDate, endDate, size (선택) |
| 동작 | 날짜 검증 → 크기 필터링 → 시작일/종료일만 가용성 확인 (중간 날짜 미확인) |

**Success Response (200 OK):**
`GET /api/sites/search?startDate=2026-01-25&endDate=2026-01-27`
```json
[
    {
        "siteId": 1,
        "siteNumber": "A-1",
        "size": "대형",
        "hasElectricity": true,
        "date": "2026-01-25",
        "available": true,
        "maxPeople": 6,
        "description": "대형 사이트 - 전기 있음, 화장실 인근"
    },
    {
        "siteId": 2,
        "siteNumber": "A-2",
        ...
    },
    ...
]
```

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

### 사이트 속성 규칙

| 규칙 | 발견 위치 |
|------|----------|
| 사이트 번호 유효 범위: A/B + 1~10 | ValidationUtils:163-186 |

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
