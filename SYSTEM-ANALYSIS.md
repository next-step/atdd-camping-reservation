# 캠핑장 예약 시스템 분석

## API 엔드포인트 (10개)

### 사이트 API
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/sites` | 전체 목록 |
| GET | `/api/sites/{siteId}` | 상세 조회 |
| GET | `/api/sites/{siteNumber}/availability?date=` | 날짜별 가용성 |
| GET | `/api/sites/available?date=` | 가용 사이트 목록 |
| GET | `/api/sites/search?startDate=&endDate=&size=` | 기간 검색 |

### 예약 API
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/reservations` | 목록 (?date= 또는 ?customerName=) |
| GET | `/api/reservations/{id}` | 상세 조회 |
| POST | `/api/reservations` | 생성 |
| PUT | `/api/reservations/{id}?confirmationCode=` | 수정 |
| DELETE | `/api/reservations/{id}?confirmationCode=` | 취소 |

### 추가 API
- `GET /api/reservations/my?name=&phone=` - 내 예약
- `GET /api/reservations/calendar?year=&month=&siteId=` - 캘린더

---

## 비즈니스 규칙

### 예약 검증
- 과거 날짜 예약 불가
- 종료일 >= 시작일
- 예약 기간 최대 30일
- 이름 2~20자
- 전화번호 10~11자리
- 동일 사이트/기간 중복 불가
- 확인코드 6자리 자동생성

### 취소 규칙
- confirmationCode 일치 필수
- 당일취소: `CANCELLED_SAME_DAY`
- 사전취소: `CANCELLED`

---

## 숨겨진 규칙 (명세서에 없음)

### 가격
| 타입 | 가격 | 주말 | 성수기(7-8월) | 성수기+주말 |
|------|------|------|---------------|-------------|
| A(대형) | 8만 | +30% | +50% | +70% |
| B(소형) | 5만 | +30% | +50% | +70% |

### 포인트 적립률
- 기본 5%, 주말포함 10%, 성수기 3%
- 결제수단: CARD 10%, MOBILE 8%, TRANSFER 5%, CASH 3%

### 사이트 구분
- A: 대형, B: 소형
- 화장실 거리 = 사이트번호 * 10m

---

## 버그 목록

| # | 버그 | 위치 |
|---|------|------|
| 1 | 연박 예약 중간 날짜 미검증 | `SiteService:101-106` |
| 2 | 취소된 예약도 중복 체크됨 | `ReservationService:137-140` |
| 3 | "오늘로부터 30일 이내" 검증 누락 | `ReservationService:95-98` |
| 4 | 전화번호 null이면 검증 스킵 | `ReservationService:118` |
| 5 | 동시성 제어 없음 (100ms 지연) | `ReservationService:210-214` |

---

## 코드 이슈

- **God Class**: ReservationService (예약/캘린더/통계/가격/포인트/알림 모두 담당)
- **중복**: 날짜검증, DTO변환, 사이트크기 결정 로직
- **4단계 if-else 중첩**: createReservation()

