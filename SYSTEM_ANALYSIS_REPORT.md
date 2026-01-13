# SYSTEM_ANALYSIS_REPORT.md

## 1. API 엔드포인트 목록 및 기능 요약

### 1.1. UI 렌더링 API (`HomeController`)

서버 사이드 렌더링을 통해 사용자에게 보여줄 HTML 페이지를 반환합니다.

| Method | Path                  | 기능                         |
| :--- | :-------------------- | :--------------------------- |
| GET    | `/`                   | 메인 페이지 반환             |
| GET    | `/reservations`       | 예약 목록 페이지 반환        |
| GET    | `/reservations/new`   | 새 예약 생성 폼 페이지 반환  |
| GET    | `/reservations/search`| 예약 검색 페이지 반환        |
| GET    | `/sites`              | 캠핑장 목록 페이지 반환      |
| GET    | `/sites/{siteNumber}` | 특정 캠핑장 상세 페이지 반환 |

### 1.2. 예약 RESTful API (`ReservationController`)

예약과 관련된 핵심 기능을 제공하는 RESTful API입니다.

| Method | Path             | 기능                                    |
| :--- | :--------------- | :-------------------------------------- |
| POST   | `/api/reservations`          | 새 예약을 생성합니다.                   |
| GET    | `/api/reservations/{id}`     | 특정 ID의 예약 정보를 조회합니다.       |
| GET    | `/api/reservations`          | 전체 또는 조건별 예약 목록을 조회합니다. |
| DELETE | `/api/reservations/{id}`     | 특정 ID의 예약을 취소합니다.            |
| PUT    | `/api/reservations/{id}`     | 특정 ID의 예약 정보를 수정합니다.       |
| GET    | `/api/reservations/my`       | 이름과 전화번호로 내 예약을 조회합니다. |
| GET    | `/api/reservations/calendar` | 월별 예약 현황 캘린더를 조회합니다.     |

### 1.3. 사이트 RESTful API (`SiteController`)

캠핑장 사이트 정보 및 예약 가능 여부 조회를 위한 RESTful API입니다.

| Method | Path                               | 기능                                      |
| :--- | :--------------------------------- | :---------------------------------------- |
| GET    | `/api/sites`                       | 전체 캠핑장 목록을 조회합니다.            |
| GET    | `/api/sites/{siteId}`              | 특정 ID의 캠핑장 상세 정보를 조회합니다.  |
| GET    | `/api/sites/{siteNumber}/availability` | 특정 날짜의 사이트 예약 가능 여부를 확인합니다. |
| GET    | `/api/sites/available`             | 특정 날짜에 예약 가능한 모든 사이트를 조회합니다. |
| GET    | `/api/sites/search`                | 주어진 기간에 예약 가능한 사이트를 검색합니다. |

---

## 2. 핵심 비즈니스 규칙

프로젝트 전반, 특히 `ReservationService` 내에 하드코딩된 비즈니스 규칙은 다음과 같습니다.

### 2.1. 예약 규칙
- **예약 가능 기간**:
    - 예약은 오늘 날짜 이후로만 가능하며, 과거 날짜는 불가능합니다.
    - 종료일은 시작일보다 이전일 수 없습니다.
    - 최대 예약 기간은 30일을 초과할 수 없습니다.
- **고객 정보 검증**:
    - 예약자 이름은 2자 이상 20자 이하여야 합니다.
    - 전화번호는 10자리 또는 11자리 숫자여야 합니다.
- **중복 예약 방지**:
    - 동일 캠핑 사이트에 대해 날짜가 하루라도 겹치는 중복 예약은 불가능합니다.
- **예약 식별 및 인증**:
    - 예약 생성 시 6자리의 영문/숫자 조합 `확인 코드`가 발급됩니다.
    - 예약을 수정하거나 취소할 때 이 `확인 코드`가 반드시 일치해야 합니다.
- **예약 취소 정책**:
    - 예약 시작일 당일에 취소하면 상태가 `CANCELLED_SAME_DAY`가 됩니다.
    - 그 외의 경우 `CANCELLED` 상태가 됩니다.

### 2.2. 가격 및 포인트 정책
- **기본 가격** (1박 기준):
    - **대형 사이트 (A)**: 80,000원
    - **소형 사이트 (B)**: 50,000원
    - **기타**: 60,000원
- **할증 규칙**:
    - **주말 (토, 일)**: 30% 할증
    - **성수기 (7월, 8월)**: 50% 할증
    - **성수기 주말**: 70% 할증
- **포인트 적립 규칙**:
    - **기본**: 결제 금액의 5%
    - **주말 포함 시**: 10%
    - **성수기 (주말 제외)**: 3% (기본 적립률보다 낮아 규칙에 문제가 있을 수 있음)

---

## 3. 잠재적 버그 및 코드 스멜

### 3.1. 심각한 동시성 문제 (Critical Concurrency Bug)
- **위치**: `ReservationService.createReservation()`
- **문제점**: 예약 가능 여부를 확인한 후, 실제 DB에 저장하기 전에 `Thread.sleep(100)`으로 지연시키는 로직이 존재합니다. 이는 **TOCTTOU (Time-of-check to time-of-use)** 라는 전형적인 Race Condition(경쟁 상태) 버그를 유발합니다.
- **위험성**: 두 명 이상의 사용자가 거의 동시에 동일한 사이트와 날짜로 예약을 시도할 경우, 두 요청 모두 예약이 가능하다고 판단하여 **중복 예약이 발생**하게 됩니다. 이는 시스템의 데이터 정합성을 파괴하는 치명적인 결함입니다.

### 3.2. 비효율적인 데이터 조회 (Major Performance Bottleneck)
- **위치**: `ReservationService.getMonthlyCalendar()`, `getReservationsByDate()`, `getDailyReservationCount()` 등 다수
- **문제점**: `reservationRepository.findAll()`을 호출하여 모든 예약 데이터를 메모리에 로드한 뒤, 애플리케이션 레벨에서 `stream().filter()`를 이용해 필터링합니다.
- **위험성**: 예약 데이터가 증가함에 따라 애플리케이션의 응답 시간이 급격히 느려지고, 심각한 경우 메모리 부족(Out of Memory) 오류를 유발하여 서버가 다운될 수 있습니다.

### 3.3. 거대하고 복잡한 메서드 (God Method)
- **위치**: `ReservationService.createReservation()`
- **문제점**: 하나의 메서드가 100줄이 넘으며, **입력값 검증, 가격 계산, 포인트 계산, 예약 가능 여부 확인, 객체 생성, DB 저장, 알림 전송** 등 너무 많은 책임을 한 번에 처리하고 있습니다. 또한 `if-else`가 여러 단계로 중첩되어 코드의 복잡도가 매우 높습니다.
- **위험성**: 코드를 이해하고 수정하기가 극히 어려워 사소한 변경에도 새로운 버그가 발생할 가능성이 높으며, 유지보수 비용을 크게 증가시킵니다.

### 3.4. 광범위한 코드 중복 (Widespread Code Duplication)
- **위치**: 프로젝트 전반
- **문제점**: 유사하거나 동일한 코드가 여러 곳에 반복해서 나타납니다.
    - **가격 계산 로직**: `createReservation`, `processReservationWithPayment`, `calculateReservationPrice` 등 여러 곳에 중복됩니다.
    - **유효성 검증 로직**: `ValidationUtils` 클래스가 존재함에도 불구하고, `createReservation`, `updateReservation` 등 각 서비스 메서드 내에서 직접 검증 로직을 반복적으로 구현하고 있습니다.
    - **DTO 변환 로직**: `ReservationResponse.from()`이라는 변환 메서드가 있지만, 많은 곳에서 수동으로 객체의 필드를 하나씩 복사하고 있습니다.
- **위험성**: 비즈니스 로직 변경 시 모든 중복 코드를 찾아 함께 수정해야 하므로 실수가 발생하기 쉽고, 애플리케이션의 일관성을 해칠 수 있습니다.

### 3.5. 일반적인 예외 처리 (Generic Exception Handling)
- **위치**: `ReservationController`, `SiteController` 등 컨트롤러 계층
- **문제점**: `catch (RuntimeException e)`와 같이 최상위 예외를 포괄적으로 잡아, 모든 종류의 비즈니스 오류에 대해 동일한 HTTP 상태 코드(예: 409, 400)와 단순한 오류 메시지만을 반환합니다.
- **위험성**: API를 사용하는 클라이언트(프론트엔드)가 오류의 구체적인 원인(`예약 불가`, `잘못된 입력값` 등)을 파악할 수 없어, 사용자에게 적절한 피드백을 제공하기 어렵습니다.

### 3.6. 하드코딩된 값 (Hardcoded Magic Numbers)
- **위치**: `ReservationService` 등 서비스 계층
- **문제점**: 예약 상태(`"CANCELLED"`), 사이트 종류를 구분하는 문자열(`"A"`, `"B"`), 가격, 포인트 적립률과 같은 비즈니스 핵심 값들이 코드 전반에 상수가 아닌 문자열 리터럴이나 숫자로 하드코딩되어 있습니다.
- **위험성**: 가격, 포인트 정책 등 비즈니스 규칙이 변경될 때마다 관련된 모든 코드를 일일이 찾아 수정해야 하며, 이 과정에서 실수가 발생하거나 누락될 가능성이 매우 높습니다.