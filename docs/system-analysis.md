# 시스템 분석 결과

## API 목록

| Endpoint | Method | 목적 | 주요 파라미터 | 비고 |
|----------|--------|------|--------------|------|
| `/api/reservations` | POST | 예약 생성 | customerName, startDate, endDate, siteNumber, phoneNumber | 중복 예약 검증 |
| `/api/reservations/{id}` | GET | 예약 단건 조회 | id | - |
| `/api/reservations` | GET | 예약 목록 조회 | date, customerName (optional) | 필터 조회 |
| `/api/reservations/{id}` | DELETE | 예약 취소 | id, confirmationCode | 당일/사전 구분 |
| `/api/reservations/{id}` | PUT | 예약 수정 | id, confirmationCode, body | 확인코드 필수 |
| `/api/reservations/my` | GET | 내 예약 조회 | name, phone | - |
| `/api/reservations/calendar` | GET | 월별 캘린더 | year, month, siteId | - |
| `/api/sites` | GET | 전체 사이트 목록 | - | - |
| `/api/sites/{siteId}` | GET | 사이트 상세 | siteId | - |
| `/api/sites/{siteNumber}/availability` | GET | 가용성 확인 | siteNumber, date | - |

## 발견한 비즈니스 규칙

### 예약 검증
- 과거 날짜 예약 불가 (startDate >= today)
- 종료일이 시작일보다 이전이면 안 됨
- 최대 예약 기간: 30일
- 예약자 이름: 2~20자
- 전화번호: 10~11자리 숫자만

### 중복 예약 방지
- 동일 사이트, 겹치는 기간 예약 불가
- 중복 체크: 기존예약.startDate <= 신규.endDate AND 기존예약.endDate >= 신규.startDate

### 가격 계산
| 사이트 | 기본가 | 주말(금,토) | 성수기(7~8월) | 주말+성수기 |
|--------|--------|-------------|--------------|------------|
| A (대형) | 80,000 | +30% | +50% | +70% |
| B (소형) | 50,000 | +30% | +50% | +70% |

### 포인트 적립
| 조건 | 적립률 |
|------|--------|
| 기본 | 5% |
| 주말 포함 | 10% |
| 성수기 | 3% |

### 예약 상태
- `CONFIRMED`: 예약 확정 (기본값)
- `CANCELLED`: 사전 취소 (startDate > today)
- `CANCELLED_SAME_DAY`: 당일 취소 (startDate == today)

### 확인 코드
- 6자리 영숫자 (0-9, A-Z)
- 예약 생성 시 랜덤 발급
- 취소/수정 시 검증 필수

---

## 주요 문제점 (리팩토링 대상)

### 1. 동시성 문제
- `Thread.sleep(100)` 의도적 지연으로 Race Condition 발생
- 낙관적/비관적 잠금 없음 → 중복 예약 가능
- 동시 요청 시 두 예약 모두 성공할 수 있음

### 2. 코드 복잡도
- `createReservation()` 213줄, 중첩 깊이 5단계
- 단일 책임 위반: 검증, 가격계산, 포인트, 알림까지 한 메서드에서 처리
- `ReservationService`가 7가지 책임 담당 (CRUD, 캘린더, 통계, 가격, 포인트, 알림, 가용성)

### 3. 코드 중복
- **가격 계산**: 3곳에서 동일 로직 반복 (createReservation, processReservationWithPayment, calculateReservationPrice)
- **날짜 검증**: 5곳에서 중복
- **고객명/전화번호 검증**: 3~4곳에서 중복
- **DTO 변환**: 4곳에서 수동 변환 (ReservationResponse.from() 미활용)

### 4. 버그 가능성
- **연박 검증 버그**: SiteService.searchAvailableSites()가 시작일/종료일만 체크, 중간 날짜 미확인
- **포인트 우선순위 모호**: 주말+성수기 시 주말만 적용 (성수기 할인 누락?)
- 상태값 문자열 관리 (ENUM 미사용) → 오타 가능

### 5. 테스트 어려움
- 테스트 코드 전무
- `LocalDate.now()` 직접 호출 → 시간 고정 불가
- 모든 예외가 `RuntimeException` → 예외 타입 구분 불가
- 쿼리 비효율: `findAll()` 후 메모리 필터링

### 6. 유지보수 문제
- Deprecated 코드 방치 (CalendarService, processReservationWithPayment)
- 상수 중복 정의 (MAX_RESERVATION_DAYS가 3곳에)
- 하드코딩된 가격/할증율
