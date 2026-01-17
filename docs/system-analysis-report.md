# SYSTEM_ANALYSIS_REPORT

작성일: 2026-01-13  
대상 프로젝트: `atdd-camping-reservation` (`com.camping.legacy`)

> 이 문서는 현재 코드베이스(레거시 구현)의 구조를 빠르게 파악하고, API/비즈니스 규칙/리스크를 한 곳에 모아 팀 내 공유가 가능하도록 정리한 보고서입니다.

---

## 1. API 엔드포인트 목록과 기능 요약

### 1.1 JSON API (`/api/**`)

#### Reservations

| Endpoint | Method | 목적 | 주요 파라미터 | 비고 |
|---|---:|---|---|---|
| `/api/reservations` | POST | 예약 생성 | Body: `ReservationRequest` (customerName, startDate, endDate, siteNumber, phoneNumber, ...) | 성공: 201 + `ReservationResponse` / 실패: 409 + `{message}` |
| `/api/reservations/{id}` | GET | 예약 단건 조회 | Path: `id` | 성공: 200 / 실패: 404 + `{message}` |
| `/api/reservations` | GET | 예약 목록 조회(필터 포함) | Query(옵션): `date`(YYYY-MM-DD), `customerName` | `date` 우선, 다음 `customerName`, 둘 다 없으면 전체 조회 |
| `/api/reservations/{id}` | DELETE | 예약 취소 | Path: `id`, Query: `confirmationCode` | 성공: 200 + `{message}` / 실패: 400 + `{message}` |
| `/api/reservations/{id}` | PUT | 예약 수정(부분 업데이트) | Path: `id`, Query: `confirmationCode`, Body: `ReservationRequest` | 성공: 200 + `ReservationResponse` / 실패: 400 + `{message}` |
| `/api/reservations/my` | GET | 내 예약 조회(이름+전화번호) | Query: `name`, `phone` | 컨트롤러에서 예외를 잡지 않아 입력 오류 시 500 가능 |
| `/api/reservations/calendar` | GET | 월별 예약 캘린더(사이트별) | Query: `year`, `month`, `siteId` | 성공: 200 + `CalendarResponse` |

#### Sites

| Endpoint | Method | 목적 | 주요 파라미터 | 비고 |
|---|---:|---|---|---|
| `/api/sites` | GET | 전체 사이트 목록 | - | 성공: 200 + `List<SiteResponse>` |
| `/api/sites/{siteId}` | GET | 사이트 상세 조회(ID 기준) | Path: `siteId` | 성공: 200 + `SiteResponse` |
| `/api/sites/{siteNumber}/availability` | GET | 특정 사이트번호/일자 가용성 조회 | Path: `siteNumber`, Query: `date`(YYYY-MM-DD) | 성공: 200 + `{siteNumber, date, available}` |
| `/api/sites/available` | GET | 특정 날짜에 가능한 사이트 목록 | Query: `date`(YYYY-MM-DD) | `reservationDate` 기반 체크를 사용(기간 체크와 혼재) |
| `/api/sites/search` | GET | 기간 + (옵션) 크기 필터로 가능한 사이트 검색 | Query: `startDate`, `endDate`, (옵션) `size` | 현재 구현은 시작/종료일만 체크(중간 날짜 누락 가능) |

> 참고: `SiteService#isAvailable`은 start/end 기간 겹침으로 체크하지만,
> `getAvailableSites`/`searchAvailableSites`는 `reservationDate` 기반 체크가 섞여 있어 일관성이 떨어집니다(아래 리스크 참고).

---

### 1.2 화면(Thymeleaf) 라우트

| Endpoint | Method | 목적 | 주요 파라미터 | 비고 |
|---|---:|---|---|---|
| `/` | GET | 홈 화면 | - | `index.html` |
| `/reservations` | GET | 예약 목록 화면 | Query(옵션): `date`(YYYY-MM-DD) | `reservation/list.html` |
| `/reservations/new` | GET | 예약 생성 폼 | - | `reservation/form.html` |
| `/reservations/search` | GET | 예약 검색 화면 | - | `reservation/search.html` |
| `/sites` | GET | 사이트 목록 화면 | - | `sites/list.html` |
| `/sites/{siteNumber}` | GET | 사이트 상세 화면 | Path: `siteNumber` | `sites/detail.html` |

---

## 2. 핵심 비즈니스 규칙(검증/제약 조건) 정리

이 섹션은 `requirements.md`의 규칙을 기준으로, **현재 코드에 실제로 구현된 제약**과 **미구현/불일치 항목**을 함께 정리합니다.

### 2.1 예약 생성 규칙 (`POST /api/reservations`)

#### 2.1.1 입력값 필수/형식 규칙
- 사이트번호(`siteNumber`)
  - 필수: null/blank 불가
  - 존재 검증: `CampsiteRepository.findBySiteNumber(siteNumber)`로 존재해야 함
  - DB 제약: `Campsite.siteNumber`는 `nullable=false`, `unique=true`
  - 참고: `ValidationUtils.isValidSiteNumber()`(A1~A10/B1~B10 형식)은 존재하지만, 예약 생성 로직에서는 사용하지 않음 → 형식이 A/B로 시작하지 않더라도 DB에 존재하면 통과

- 날짜(`startDate`, `endDate`)
  - 필수: 둘 다 null이면 실패
  - 논리: `endDate < startDate` 불가
  - 과거: `startDate < today` 불가
  - 기간 제한: `ChronoUnit.DAYS.between(startDate, endDate) > 30`이면 실패
    - 주의: 코드에 `MAX_RESERVATION_DAYS = 30` 상수가 있지만 실제 조건문은 리터럴 `30`을 사용

- 예약자 이름(`customerName`)
  - 필수: null/blank 불가
  - 길이: 2~20자
  - 참고: 동일한 규칙이 `ValidationUtils.isValidCustomerName()`에도 존재하지만 사용하지 않음

- 전화번호(`phoneNumber`)
  - 요구사항: “필수, null 불가”
  - 현재 구현(예약 생성): **필수로 강제하지 않음**
    - 값이 있을 때만 검증 수행
    - 하이픈 제거 후 길이 10~11, 숫자만 가능
  - 현재 구현(내 예약 조회): name/phone 둘 다 필수 + 길이 10~11만 허용
  - 참고: `ValidationUtils.isValidPhoneNumber()`가 존재하나 서비스 로직 전반에서 일관되게 사용하지 않음

- 기타 필드(`numberOfPeople`, `carNumber`, `requests`)
  - `ReservationRequest`에는 존재하지만, 현재 `Reservation` 엔티티에 저장되지 않고 비즈니스 규칙도 적용되지 않음

#### 2.1.2 중복 예약(기간 겹침) 규칙
- 동일 사이트에서 기간이 겹치면 생성 실패
  - 구현: `existsByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual(campsite, endDate, startDate)`
  - 의미: 요청 구간 `[startDate, endDate]` 와 기존 예약 구간이 하나라도 겹치면 충돌로 판단

- 요구사항: “CANCELLED 상태는 중복 체크에서 제외”
  - 현재 구현: **status 조건을 전혀 고려하지 않음** → CANCELLED도 충돌로 취급될 가능성 큼

#### 2.1.3 예약 상태/메타데이터 규칙
- 상태(`Reservation.status`)
  - 저장 시 기본값: `@PrePersist`에서 `CONFIRMED`
  - 취소 시: `CANCELLED` 또는 `CANCELLED_SAME_DAY`
  - Enum이 아닌 String으로 관리됨

- 생성일시(`Reservation.createdAt`)
  - `@PrePersist`에서 자동 세팅

- 확인코드(`confirmationCode`)
  - 예약 생성 시 6자리 영숫자(0-9/A-Z) 생성
  - DB 제약: `@Column(length = 6)`
  - 참고: `ValidationUtils.isValidConfirmationCode()`가 있으나 검증에 사용하진 않음(생성 로직만 존재)

---

### 2.2 연박/기간 가용성(Availability) 규칙

요구사항: 연박 예약 시 **시작~종료까지 모든 날짜가 예약 가능**해야 함.

- 예약 생성(ReservationService.createReservation)
  - 기간 겹침 쿼리로 “전체 기간 중복”을 차단하려는 의도는 충족(단, 동시성 제어는 별도 문제)

- 사이트 단일일자 가용성
  - `GET /api/sites/{siteNumber}/availability`
  - 구현: `existsByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual(campsite, date, date)` (기간 기준)

- 사이트 목록 가용성(특정 날짜)
  - `GET /api/sites/available?date=...`
  - 구현: `existsByCampsiteAndReservationDate(site, date)` (reservationDate 기준)

- 사이트 기간 검색
  - `GET /api/sites/search?startDate=...&endDate=...&size=...`
  - 구현: 시작일/종료일만 `reservationDate`로 체크 → **중간 날짜가 예약되어 있어도 통과할 수 있음**

- 내부적으로 “전체 기간을 하루씩” 체크하는 메서드가 존재
  - `ReservationService.checkPeriodAvailability(siteNumber, startDate, endDate)`
  - 참고: 외부 API로 노출되진 않으며, 내부에서 `checkAvailability`를 하루씩 호출하는 구조

---

### 2.3 예약 조회 규칙

- ID로 개별 조회
  - `GET /api/reservations/{id}`
  - 존재하지 않으면 실패

- 날짜별 예약 현황 조회
  - `GET /api/reservations?date=...`
  - 구현: `reservationRepository.findAll()` 후 메모리에서 필터링
  - 포함 기준: `startDate <= date <= endDate`

- 고객명으로 조회
  - `GET /api/reservations?customerName=...`
  - 구현: `ReservationRepository.findByCustomerName(customerName)` (부분일치/키워드가 아니라 정확 매칭)

- 이름+전화번호로 내 예약 조회
  - `GET /api/reservations/my?name=...&phone=...`
  - name/phone 둘 다 필수
  - 전화번호 형식은 하이픈 제거 후 길이 10~11만 체크(숫자 여부/01로 시작 여부는 체크하지 않음)

- 키워드 검색(이름 또는 전화번호 포함)
  - 서비스 메서드 `ReservationService.searchReservations(keyword)`는 존재하나, 현재 컨트롤러/API에 연결되어 있지 않음

---

### 2.4 예약 수정 규칙 (`PUT /api/reservations/{id}`)

- 본인 확인
  - `confirmationCode` 필수
  - DB에 저장된 `Reservation.confirmationCode`와 일치해야 함

- 부분 업데이트 허용

- 날짜 검증(부분 구현)
  - `startDate`와 `endDate`가 “둘 다 존재할 때만” 검증 수행
  - 검증 내용: end<start 불가, 과거 start 불가
  - **중요 누락**: 업데이트 시에는 “30일 이내” 제한과 “기간 겹침(중복 예약)” 체크가 없음

---

### 2.5 예약 취소 규칙 (`DELETE /api/reservations/{id}`)

- 본인 확인
  - 요청 파라미터 `confirmationCode`가 `Reservation.confirmationCode`와 일치해야 함

- 취소 정책
  - 예약 시작일이 오늘이면: `CANCELLED_SAME_DAY`
  - 그 외: `CANCELLED`

- 환불 정책/환불 금액
  - 요구사항에는 존재하지만, 현재 구현은 상태 변경만 수행(환불 금액 계산/저장 없음)

---

## 3. 잠재적 버그 / 동시성 이슈 / 코드 스멜 후보

아래 항목은 “실제 장애 또는 요구사항 불일치로 이어질 수 있는” 포인트를 우선순위로 정리했습니다.

### 3.1 동시성 / 중복 예약 방지(가장 중요)
- TOCTOU(Time-of-check to time-of-use) 레이스
  - 근거: `ReservationService#createReservation`
    - (1) `existsBy...`로 충돌 체크
    - (2) `Thread.sleep(100)`로 지연(동시성 재현용)
    - (3) `reservationRepository.save`
  - 위험: 두 요청이 동시에 들어오면 둘 다 (1) 통과 후 (3)을 수행할 수 있어 “중복 예약 저장” 가능
  - 추가로, 취소 상태(CANCELLED)는 중복 체크에서 제외되어야 한다는 요구사항이 있으나,
    현재 EXISTS 쿼리는 `status` 조건이 없어 CANCELLED도 충돌로 취급될 가능성이 큼.

### 3.2 예약 기간 계산(오프바이원/정책 불명확)
- 기간 제한(30일) 계산이 ‘end-start’ 기준
  - `ChronoUnit.DAYS.between(start, end)`는 start==end이면 0
  - 한편 가격 계산은 `while (!current.isAfter(endDate))`로 “종료일 포함” 방식
  - 결과: “30일”의 의미가 정책적으로 불명확(박수/일수 계산 기준이 불일치할 수 있음)

### 3.3 전화번호 필수 요구사항 불일치
- 요구사항: 전화번호 필수(null 불가)
- 현 구현:
  - `createReservation`에서 phone은 null/blank이어도 통과(검증도 ‘있으면 한다’ 수준)
  - Entity에서도 `phoneNumber`는 nullable 제한 없음

### 3.4 사이트 가용성 로직의 일관성 문제(reservationDate vs 기간)
- `Reservation`에는 `startDate/endDate`와 별개로 `reservationDate` 필드가 존재(널 가능)
- `SiteService#getAvailableSites`는 `existsByCampsiteAndReservationDate`로 가용성 체크
  - 하지만 `createReservation`은 `reservationDate = startDate`만 세팅
  - 연박이면 중간 날짜들은 `reservationDate`로는 막히지 않음
- 동시에 `SiteService#isAvailable`은 기간 겹침(정상적인 방식)으로 체크
- 결과: API별로 “가용성 결과가 달라지는” 버그 가능

### 3.5 취소/상태 모델의 불명확성과 요구사항 불일치
- 상태가 문자열로 관리됨 (`Reservation.status`)
  - `CONFIRMED`, `CANCELLED`, `CANCELLED_SAME_DAY` 등
- 요구사항: CANCELLED는 중복 체크에서 제외
  - 현재 중복 체크/조회/캘린더 표시는 status 고려 없음
- 환불 정책/환불 금액 계산 로직 부재

### 3.6 예외/에러 모델 및 HTTP 상태코드 일관성 부족
- 컨트롤러에서 `RuntimeException`을 잡고 메시지를 그대로 반환
  - `POST /api/reservations`: 무조건 409
  - `DELETE/PUT`: 무조건 400
  - `GET /api/reservations/my`: 예외 핸들링이 없어 500으로 터질 수 있음
- 입력 검증 실패(400), 리소스 없음(404), 충돌(409) 등이 뒤섞일 가능성

### 3.7 성능/확장성: findAll 후 메모리 필터링
- `getReservationsByDate`, `getMonthlyCalendar`, 통계 메서드 다수가 `reservationRepository.findAll()` 후 stream/loop 처리
  - 데이터가 커지면 API 응답이 느려지고, 메모리 사용 증가

### 3.8 코드 스멜: 거대 서비스/중복 로직/매직 넘버
- `ReservationService`가 1000+ 라인, 너무 많은 책임(예약/결제/통계/가격/알림/캘린더/가용성)
- 검증 로직이 `ValidationUtils`에 있음에도 서비스에서 하드코딩 중복
- 가격/포인트/할증률(1.3/1.5/1.7), 금액(80000/50000/60000), 30일 제한 등이 곳곳에 하드코딩

---

## 4. 요구사항 대비 구현 현황(요약)

- 예약 30일 이내만 가능: **부분 충족**
  - createReservation에서 체크는 있으나 종료일 포함/일수 정의가 불명확
- 과거 날짜 예약 불가: **대체로 충족**
- 종료일 < 시작일 금지: **충족**
- 전화번호 필수: **미충족**
- 동일 사이트/기간 중복 예약 방지: **단일 스레드 기준은 충족**, 동시성은 **미흡**
- 동시 요청 시 하나만 성공(동시성 제어): **미충족**(sleep으로 오히려 취약성 재현)
- CANCELLED는 중복 체크에서 제외: **미충족 가능성 큼**(status 조건 없음)
- 확인 코드 6자리 영숫자 자동 생성: **충족**
- 연박 예약 시 전체 기간 가용성 확인: 
  - 예약 생성은 기간 겹침 쿼리로 사실상 커버
  - 사이트 검색은 **미충족**(중간 날짜 확인 없음)
- 월별 예약 현황: **충족(비효율적 구현)**
- 예약 취소 + 본인 확인: **충족**
- 환불 정책/환불금액 계산: **미구현**

---

## 5. 개선 제안(우선순위)

1) **동시성 해결(가장 먼저)**
   - DB 레벨에서 기간 중복을 원자적으로 막는 장치가 필요(예: 비관적 락/격리수준 상향/유니크 제약 모델링 등).
   - 최소한 `Thread.sleep(100)` 제거.

2) **가용성 로직 통일**
   - `reservationDate` 기반 체크 제거 또는 의미를 명확히 재정의.
   - 모든 availability 판단을 `start/end 겹침`으로 통일.

3) **상태를 Enum으로 정리 + CANCELLED 제외 규칙 반영**
   - 중복 체크/캘린더/조회에서 상태를 고려.

4) **ValidationUtils/DateUtils 사용으로 검증 표준화**
   - 서비스의 중복 검증 제거 및 공통 에러 응답 모델 정의.

5) **Repository 쿼리 최적화**
   - 날짜별 조회/캘린더/통계는 DB 쿼리로 필터링해서 `findAll()` 제거.
