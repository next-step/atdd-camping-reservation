# 캠핑장 예약 시스템 API 명세서

## 목차
1. [예약 관리 (Reservation)](#1-예약-관리-reservation)
    - [예약 생성](#11-예약-생성)
    - [예약 단건 조회](#12-예약-단건-조회)
    - [예약 목록 조회](#13-예약-목록-조회)
    - [내 예약 조회](#14-내-예약-조회)
    - [월별 예약 캘린더 조회](#15-월별-예약-캘린더-조회)
    - [예약 수정](#16-예약-수정)
    - [예약 취소](#17-예약-취소)
2. [사이트 관리 (Site)](#2-사이트-관리-site)
    - [전체 사이트 목록 조회](#21-전체-사이트-목록-조회)
    - [사이트 상세 조회](#22-사이트-상세-조회)
    - [특정 사이트 가용성 확인](#23-특정-사이트-가용성-확인)
    - [날짜별 가용 사이트 조회](#24-날짜별-가용-사이트-조회)
    - [기간별 가용 사이트 검색](#25-기간별-가용-사이트-검색)

---

## 1. 예약 관리 (Reservation)

### 1.1 예약 생성
새로운 예약을 생성합니다.

- **URL**: `/api/reservations`
- **Method**: `POST`
- **Content-Type**: `application/json`

#### 요청 (Request)
```json
{
  "customerName": "홍길동",
  "startDate": "2026-02-01",
  "endDate": "2026-02-03",
  "siteNumber": "A-1",
  "phoneNumber": "010-1234-5678",
  "numberOfPeople": 4
}
```

#### 응답 (Response)
**Status: 201 Created**
```json
{
  "id": 6,
  "customerName": "홍길동",
  "startDate": "2026-02-01",
  "endDate": "2026-02-03",
  "siteNumber": "A-1",
  "phoneNumber": "010-1234-5678",
  "status": "CONFIRMED",
  "confirmationCode": "RU0J39",
  "createdAt": null
}
```

#### 예외 (Exception)
- **409 Conflict**: 해당 기간에 이미 예약이 존재할 경우
- **400 Bad Request**: 입력 데이터가 유효하지 않은 경우 (과거 날짜, 전화번호 형식 등)

---

### 1.2 예약 단건 조회
예약 ID를 사용하여 상세 정보를 조회합니다.

- **URL**: `/api/reservations/{id}`
- **Method**: `GET`

#### 응답 (Response)
**Status: 200 OK**
```json
{
  "id": 6,
  "customerName": "홍길동",
  "startDate": "2026-02-01",
  "endDate": "2026-02-03",
  "siteNumber": "A-1",
  "phoneNumber": "010-1234-5678",
  "status": "CONFIRMED",
  "confirmationCode": "RU0J39",
  "createdAt": null
}
```

#### 예외 (Exception)
- **404 Not Found**: 존재하지 않는 예약 ID일 경우

---

### 1.3 예약 목록 조회
날짜별 또는 고객 이름별 예약을 조회합니다. 파라미터가 없으면 전체 목록을 반환합니다.

- **URL**: `/api/reservations`
- **Method**: `GET`
- **Parameters**:
    - `date` (Optional): 조회할 날짜 (예: 2026-02-01)
    - `customerName` (Optional): 조회할 고객 이름

#### 응답 (Response)
**Status: 200 OK**
```json
[
  {
    "id": 6,
    "customerName": "홍길동",
    "startDate": "2026-02-01",
    "endDate": "2026-02-03",
    "siteNumber": "A-1",
    "phoneNumber": "010-1234-5678",
    "status": "CONFIRMED",
    "confirmationCode": "RU0J39",
    "createdAt": null
  }
]
```

---

### 1.4 내 예약 조회
이름과 전화번호를 사용하여 본인의 예약을 조회합니다.

- **URL**: `/api/reservations/my`
- **Method**: `GET`
- **Parameters**:
    - `name` (Required): 예약자 이름
    - `phone` (Required): 예약자 전화번호

#### 응답 (Response)
**Status: 200 OK**
```json
[
  {
    "id": 6,
    "customerName": "홍길동",
    "startDate": "2026-02-01",
    "endDate": "2026-02-03",
    "siteNumber": "A-1",
    "phoneNumber": "010-1234-5678",
    "status": "CONFIRMED",
    "confirmationCode": "RU0J39",
    "createdAt": null
  }
]
```

---

### 1.5 월별 예약 캘린더 조회
특정 사이트의 월별 예약 가능 여부를 조회합니다.

- **URL**: `/api/reservations/calendar`
- **Method**: `GET`
- **Parameters**:
    - `year` (Required): 조회할 연도
    - `month` (Required): 조회할 월
    - `siteId` (Required): 조회할 사이트 ID

#### 응답 (Response)
**Status: 200 OK**
```json
{
  "year": 2026,
  "month": 2,
  "siteId": 1,
  "siteNumber": "A-1",
  "days": [
    {
      "date": "2026-02-01",
      "available": false,
      "customerName": "홍길동",
      "reservationId": 6
    },
    {
      "date": "2026-02-04",
      "available": true,
      "customerName": null,
      "reservationId": null
    }
    // ... 나머지 일자 생략
  ],
  "summary": {
    "totalDays": 28,
    "reservedDays": 3,
    "availableDays": 25
  }
}
```

---

### 1.6 예약 수정
확인 코드를 사용하여 예약 정보를 수정합니다.

- **URL**: `/api/reservations/{id}`
- **Method**: `PUT`
- **Parameters**: `confirmationCode` (Query Parameter)
- **Content-Type**: `application/json`

#### 요청 (Request)
```json
{
  "customerName": "홍길동(수정)",
  "startDate": "2026-02-05",
  "endDate": "2026-02-06",
  "siteNumber": "A-1",
  "phoneNumber": "010-1234-5678"
}
```

#### 응답 (Response)
**Status: 200 OK**
```json
{
  "id": 6,
  "customerName": "홍길동(수정)",
  "startDate": "2026-02-05",
  "endDate": "2026-02-06",
  "siteNumber": "A-1",
  "phoneNumber": "010-1234-5678",
  "status": "CONFIRMED",
  "confirmationCode": "RU0J39",
  "createdAt": null
}
```

#### 예외 (Exception)
- **400 Bad Request**: 확인 코드가 일치하지 않거나 입력 데이터가 잘못된 경우

---

### 1.7 예약 취소
확인 코드를 사용하여 예약을 취소합니다.

- **URL**: `/api/reservations/{id}`
- **Method**: `DELETE`
- **Parameters**: `confirmationCode` (Query Parameter)

#### 응답 (Response)
**Status: 200 OK**
```json
{
  "message": "예약이 취소되었습니다."
}
```

#### 예외 (Exception)
- **400 Bad Request**: 확인 코드가 일치하지 않을 경우

---

## 2. 사이트 관리 (Site)

### 2.1 전체 사이트 목록 조회
모든 캠핑장 사이트의 기본 정보를 조회합니다.

- **URL**: `/api/sites`
- **Method**: `GET`

#### 응답 (Response)
**Status: 200 OK**
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
  }
  // ...
]
```

---

### 2.2 사이트 상세 조회
특정 사이트의 상세 정보를 조회합니다.

- **URL**: `/api/sites/{siteId}`
- **Method**: `GET`

#### 응답 (Response)
**Status: 200 OK**
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

---

### 2.3 특정 사이트 가용성 확인
특정 날짜에 해당 사이트가 예약 가능한지 확인합니다.

- **URL**: `/api/sites/{siteNumber}/availability`
- **Method**: `GET`
- **Parameters**: `date` (Required, ISO format)

#### 응답 (Response)
**Status: 200 OK**
```json
{
  "siteNumber": "A-1",
  "date": "2026-02-01",
  "available": false
}
```

---

### 2.4 날짜별 가용 사이트 조회
특정 날짜에 예약 가능한 모든 사이트 목록을 조회합니다.

- **URL**: `/api/sites/available`
- **Method**: `GET`
- **Parameters**: `date` (Required, ISO format)

#### 응답 (Response)
**Status: 200 OK**
```json
[
  {
    "siteId": 2,
    "siteNumber": "A-2",
    "size": "대형",
    "hasElectricity": true,
    "date": "2026-02-01",
    "available": true,
    "maxPeople": 6,
    "description": "대형 사이트 - 전기 있음, 화장실 인근"
  }
  // ...
]
```

---

### 2.5 기간별 가용 사이트 검색
특정 기간 동안 예약 가능한 사이트를 검색합니다.

> **주의**: 현재 버전에서 시작일과 종료일 사이의 날짜에 대한 가용성 체크가 누락되는 버그가 있습니다.

- **URL**: `/api/sites/search`
- **Method**: `GET`
- **Parameters**:
    - `startDate` (Required)
    - `endDate` (Required)
    - `size` (Optional): "대형" 또는 "소형"

#### 응답 (Response)
**Status: 200 OK**
```json
[
  {
    "siteId": 2,
    "siteNumber": "A-2",
    "size": "대형",
    "hasElectricity": true,
    "date": "2026-02-01",
    "available": true,
    "maxPeople": 6,
    "description": "대형 사이트 - 전기 있음, 화장실 인근"
  }
  // ...
]
```
