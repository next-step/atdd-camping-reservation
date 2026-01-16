# API 상세 명세서

## 1. 예약 API

### 1.1 예약 생성
`POST /api/reservations`

**Request**
```json
{
  "customerName": "홍길동",
  "startDate": "2024-07-15",
  "endDate": "2024-07-17",
  "siteNumber": "A-1",
  "phoneNumber": "010-1234-5678",
  "numberOfPeople": 4,
  "carNumber": "12가3456",
  "requests": "조용한 자리 부탁드립니다"
}
```

**Response (201)**
```json
{
  "id": 1,
  "customerName": "홍길동",
  "startDate": "2024-07-15",
  "endDate": "2024-07-17",
  "siteNumber": "A-1",
  "phoneNumber": "010-1234-5678",
  "status": "CONFIRMED",
  "confirmationCode": "ABC123",
  "createdAt": "2024-07-01T10:30:00"
}
```

---

### 1.2 예약 단건 조회
`GET /api/reservations/{id}`

**Response (200)**
```json
{
  "id": 1,
  "customerName": "홍길동",
  "startDate": "2024-07-15",
  "endDate": "2024-07-17",
  "siteNumber": "A-1",
  "phoneNumber": "010-1234-5678",
  "status": "CONFIRMED",
  "confirmationCode": "ABC123",
  "createdAt": "2024-07-01T10:30:00"
}
```

---

### 1.3 예약 목록 조회
`GET /api/reservations`
`GET /api/reservations?date=2024-07-15`
`GET /api/reservations?customerName=홍길동`

**Response (200)**
```json
[
  {
    "id": 1,
    "customerName": "홍길동",
    "startDate": "2024-07-15",
    "endDate": "2024-07-17",
    "siteNumber": "A-1",
    "phoneNumber": "010-1234-5678",
    "status": "CONFIRMED",
    "confirmationCode": "ABC123",
    "createdAt": "2024-07-01T10:30:00"
  }
]
```

---

### 1.4 예약 취소
`DELETE /api/reservations/{id}?confirmationCode=ABC123`

**Response (200)**
```json
{
  "message": "예약이 취소되었습니다."
}
```

---

### 1.5 예약 수정
`PUT /api/reservations/{id}?confirmationCode=ABC123`

**Request**: 예약 생성과 동일

**Response (200)**: 예약 단건 조회와 동일

---

### 1.6 내 예약 조회
`GET /api/reservations/my?name=홍길동&phone=010-1234-5678`

**Response (200)**: 예약 목록 조회와 동일

---

### 1.7 예약 캘린더 조회
`GET /api/reservations/calendar?year=2024&month=7&siteId=1`

**Response (200)**
```json
{
  "year": 2024,
  "month": 7,
  "siteId": 1,
  "siteNumber": "A-1",
  "days": [
    {
      "date": "2024-07-15",
      "available": false,
      "customerName": "홍길동",
      "reservationId": 1
    },
    {
      "date": "2024-07-16",
      "available": true,
      "customerName": null,
      "reservationId": null
    }
  ],
  "summary": {
    "available": 25,
    "reserved": 6
  }
}
```

---

## 2. 사이트 API

### 2.1 전체 사이트 목록
`GET /api/sites`

**Response (200)**
```json
[
  {
    "id": 1,
    "siteNumber": "A-1",
    "description": "숲 근처 대형 사이트",
    "maxPeople": 6,
    "size": "대형",
    "hasElectricity": true,
    "toiletDistance": 10,
    "facilities": "화장실, 샤워장, 개수대",
    "rules": "22시 이후 소음 금지, 직화 금지"
  }
]
```

---

### 2.2 사이트 상세 조회
`GET /api/sites/{siteId}`

**Response (200)**: 사이트 목록의 단일 객체와 동일

---

### 2.3 사이트 가용성 확인
`GET /api/sites/{siteNumber}/availability?date=2024-07-15`

**Response (200)**
```json
{
  "siteNumber": "A-1",
  "date": "2024-07-15",
  "available": true
}
```

---

### 2.4 가용 사이트 목록
`GET /api/sites/available?date=2024-07-15`

**Response (200)**
```json
[
  {
    "siteId": 1,
    "siteNumber": "A-1",
    "size": "대형",
    "hasElectricity": true,
    "date": "2024-07-15",
    "available": true,
    "maxPeople": 6,
    "description": "숲 근처 대형 사이트"
  }
]
```

---

### 2.5 사이트 검색
`GET /api/sites/search?startDate=2024-07-15&endDate=2024-07-17&size=대형`

**Response (200)**: 가용 사이트 목록과 동일

---

## 3. 공통 에러 응답

```json
{
  "message": "에러 메시지"
}
```

| Status | 예시 메시지 |
|--------|------------|
| 400 | "확인 코드가 일치하지 않습니다." |
| 404 | "예약을 찾을 수 없습니다." |
| 409 | "해당 날짜에 이미 예약이 존재합니다." |