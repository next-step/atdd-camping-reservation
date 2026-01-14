# 시스템 분석 결과

## 1. API 엔드포인트 분석 (총 12개)

### 1.1 예약 도메인 (ReservationController)
| Method | Endpoint | 설명 | 주요 파라미터 |
|--------|--------|------|--------------|
| POST | `/api/reservations` | 예약 생성 | `ReservationRequest` (이름, 기간, 사이트번호, 폰번호 등) |
| GET | `/api/reservations/{id}` | 예약 단건 조회 | `id` (Path) |
| GET | `/api/reservations` | 예약 목록 조회 | `date` (Optional), `customerName` (Optional) |
| DELETE | `/api/reservations/{id}` | 예약 취소 | `id` (Path), `confirmationCode` (Query) |
| PUT | `/api/reservations/{id}` | 예약 수정 | `id` (Path), `confirmationCode` (Query), Body |
| GET | `/api/reservations/my` | 내 예약 조회 | `name`, `phone` |
| GET | `/api/reservations/calendar` | 월별 예약 캘린더 | `year`, `month`, `siteId` |

### 1.2 사이트 도메인 (SiteController) 
| Method | Endpoint | 설명 | 주요 파라미터 |
|--------|----------|------|--------------|
| GET | `/api/sites` | 전체 사이트 목록 | 없음 |
| GET | `/api/sites/{siteId}` | 사이트 상세 조회 | `siteId` (DB PK) |
| GET | `/api/sites/{siteNumber}/availability` | 특정 사이트 가용성 확인 | `siteNumber` (String ID), `date` |
| GET | `/api/sites/available` | 날짜별 가용 사이트 조회 | `date` |
| GET | `/api/sites/search` | 기간별 가용 사이트 검색 | `startDate`, `endDate`, `size` |

---

## 2. 숨겨진 비즈니스 규칙

### 2-1. 가격 정책 (Pricing)
| 구분 | 내용 | 비고 |
|------|------|------|
| **기본 요금** | A구역(대형): 80,000원<br>B구역(소형): 50,000원<br>기타: 60,000원 | 1박 기준 |
| **주말 할증** | 기본 요금의 **30%** 가산 | 토요일, 일요일 |
| **성수기 할증** | 기본 요금의 **50%** 가산 | 7월, 8월 |
| **복합 할증** | 성수기 주말: 기본 요금의 **70%** 가산 | 성수기 + 주말 |

### 2-2. 예약 규칙 (Reservation Rules)
| 구분 | 규칙 내용 | 비고 |
|------|-----------|------|
| **예약 가능 기간** | 신청일(오늘)로부터 최대 **30일** 이내의 날짜까지만 예약 가능 | 과거 날짜 예약 불가 |
| **고객 정보 검증** | - **이름**: 2자 이상 20자 이하, 공백만 입력 불가<br>- **전화번호**: 10~11자리 숫자, 하이픈 제거 후 검증<br>- **사이트 번호**: 필수 입력 (유효한 번호여야 함)<br>- **날짜**: 시작일/종료일 필수, 종료일이 시작일 이후여야 함 | 유효성 검사 실패 시 400 Bad Request |
| **중복 예약 방지** | 예약 요청 기간이 기존 예약 기간과 **단 하루라도 겹치면 예약 불가** | DB Lock 미사용으로 인한 Race Condition 위험 존재 |
| **예약 식별 인증** | 예약 완료 시 **6자리의 영문 대문자와 숫자 조합**으로 구성된 `confirmationCode` 자동 생성 | 수정/취소 시 본인 확인용으로 필수 |
| **예약 취소 정책** | - **사전 취소**: 예약 시작일 이전 취소 시 상태 `CANCELLED`<br>- **당일 취소**: 예약 시작일 당일 취소 시 상태 `CANCELLED_SAME_DAY` | 상태값으로만 구분되며 환불 로직 미구현 |

### 2-3. 포인트 정책 (Point) - *비일관성 주의*
- **기본**: 결제 금액의 5% 적립.
- **주말**: 주말 포함 시 10% 적립 (일부 로직).
- **결제 수단별**: 카드(10%), 모바일(8%), 계좌이체(5%), 현금(3%) (Deprecated 메서드 기준).

---

## 3. 잠재적 버그 및 코드 스멜

| 우선순위 | 구분 | 위치 | 이슈 요약 |
|:---:|:---:|:---:|:---|
| **1** | **Critical** | `ReservationService` | **동시성 제어 미비 (Race Condition)** |
| **2** | **Critical** | `ReservationService` | **전체 데이터 조회 후 메모리 필터링 (OOM 위험)** |
| **3** | **Critical** | `SiteService` | **기간 검색 로직 오류 (중복 예약 허용 버그)** |
| **4** | **Major** | `SiteService` | **데이터(DB)와 비즈니스 로직 불일치** |
| **5** | **Major** | `ReservationService` | **God Class (SRP 위반)** |
| **6** | **Minor** | `ReservationController` | **Layer Violation (직접적 예외 처리)** |

### 3-1. 동시성 제어 미비 (Race Condition)
- **설명**: 
  - `createReservation` 메서드에서 `existsBy...`로 중복을 확인하고 `save`하는 사이에 별도의 DB 락(Lock)이 없습니다. 
  - 특히 코드 내에 `Thread.sleep(100)`이 포함되어 있어, 동시에 여러 요청이 들어올 경우 동일한 사이트와 날짜에 대해 중복 예약이 생성될 가능성이 매우 높습니다.
- **해결 방안**:
  - `Pessimistic Lock`(비관적 락) 또는 `Optimistic Lock`(낙관적 락)을 도입하여 데이터 정합성 보장.
  - DB의 Unique Constraint(복합 인덱스 등) 활용.

### 3-2. 전체 데이터 조회 후 메모리 필터링 (OOM 위험)
- **설명**: 
  - `getDailyReservationCount`, `getReservationsByDate` 등의 메서드에서 `reservationRepository.findAll()`을 호출하여 **모든** 예약 데이터를 DB에서 가져옵니다. 
  - 이후 Java Stream으로 필터링하는데, 데이터가 수만 건 이상 쌓이면 메모리 부족(OOM)으로 서버가 중단될 수 있습니다.
- **해결 방안**:
  - Repository 레벨에서 `WHERE` 절을 사용해 필요한 데이터만 조회하도록 쿼리 메서드(`findByStartDateBetween` 등) 또는 JPQL로 변경.

### 3-3. 기간 검색 로직 오류 (Logic Bug)
- **설명**: 
  - `SiteService.searchAvailableSites`에서 시작일과 종료일의 가용성만 각각 체크합니다. 
  - 예를 들어 1일 ~ 5일 검색 시, 1일과 5일만 비어있으면 3일에 예약이 있어도 '예약 가능'으로 판단하여 결과에 포함시키는 치명적 버그가 있습니다.
- **해결 방안**:
  - 검색 기간 내에 예약이 하나라도 존재하는지 확인하는 로직(`countBy... > 0`)으로 수정.

### 3-4. 데이터와 로직 불일치
- **설명**: 
  - `data.sql`에는 B구역 설명에 "전기 있음"이 명시되어 있으나, 비즈니스 로직(`SiteService`)에서는 사이트 번호가 'A'로 시작해야만 `hasElectricity = true`를 반환합니다.
- **해결 방안**:
  - `Campsite` 엔티티에 `hasElectricity` 필드를 추가하여 DB 데이터에 기반하도록 수정하거나, 로직을 데이터에 맞게 통일.

### 3-5. God Class (SRP 위반)
- **설명**: 
  - `ReservationService`가 예약 CRUD 외에도 가격 계산, 포인트 적립, 통계, 알림, 캘린더 생성 등 너무 많은 책임을 지고 있습니다. 
  - 코드 라인 수가 길어지고 유지보수가 어렵습니다.
- **해결 방안**:
  - `PriceCalculator`, `PointService`, `NotificationService`, `CalendarService` 등으로 책임 분리 및 리팩토링.

### 3-6. Layer Violation
- **설명**: 
  - 컨트롤러에서 `try-catch`로 예외를 잡은 뒤 `Map`을 생성하여 `ResponseEntity`를 반환합니다. 
  - 이는 코드 중복을 야기하고 예외 처리 정책을 파편화시킵니다.
- **해결 방안**:
  - `@RestControllerAdvice`를 사용하여 글로벌 예외 처리 로직으로 통합 및 표준화.
