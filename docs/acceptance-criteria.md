# 인수 조건

## T-1 30일 넘게 남은 날짜인데 예약이 됨

### 1. 예약 시작일은 오늘로부터 30일 이내여야 한다

- **현재 상태**: 미구현 (버그). 코드의 "30일" 검증(`ReservationService.createReservation`)은 `startDate`~`endDate` 사이 숙박 일수만 검사하고, `오늘`부터 `startDate`까지 남은 일수는 검사하지 않는다.
- **예시(직접 호출로 확인, 오늘=2026-08-14, 현재는 아래 둘 다 잘못 통과함)**:

  - 오늘로부터 40일 뒤 시작
    ```
    POST /api/reservations
    {"siteNumber":"A-1","startDate":"2026-09-23","endDate":"2026-09-24","customerName":"tester"}
    ```
    → `201 Created`
    ```
    {"id":7,"customerName":"tester","startDate":"2026-09-23","endDate":"2026-09-24","siteNumber":"A-1","phoneNumber":null,"status":"CONFIRMED","confirmationCode":"0O55WF","createdAt":null}
    ```

  - 오늘로부터 정확히 30일 뒤 시작
    ```
    POST /api/reservations
    {"siteNumber":"A-6","startDate":"2026-09-13","endDate":"2026-09-14","customerName":"boundary30"}
    ```
    → `201 Created`
    ```
    {"id":8,"customerName":"boundary30","startDate":"2026-09-13","endDate":"2026-09-14","siteNumber":"A-6","phoneNumber":null,"status":"CONFIRMED","confirmationCode":"JONM78","createdAt":null}
    ```
- **이유**: 고객센터 신고 "오늘로부터 30일 넘게 남은 날짜인데 예약이 됩니다"를 막기 위한 규칙. 시작일이 오늘로부터 30일을 넘게 남았으면 예약을 거부해야 한다.
- **경계**: 오늘로부터 30일 "이내"까지 허용한다. 즉 `startDate - today <= 30`이면 허용, `> 30`이면 거부한다. 위 예시의 정확히 30일 뒤 시작(`2026-09-13`) 케이스는 허용되어야 하고, 31일 뒤부터 거부되어야 한다.

### 2. 과거 날짜로 예약할 수 없고, 종료일이 시작일보다 이전일 수 없다

- **현재 상태**: 구현되어 있고 정상 동작함(이 티켓의 버그와는 무관, 회귀 확인용으로 기록).
- **예시(직접 호출로 확인)**:

  - 과거 날짜
    ```
    POST /api/reservations
    {"siteNumber":"A-7","startDate":"2026-08-01","endDate":"2026-08-03","customerName":"pastdate"}
    ```
    → `409 Conflict`
    ```
    {"message":"과거 날짜로 예약할 수 없습니다."}
    ```

  - 날짜 역전 (종료일이 시작일보다 이전)
    ```
    POST /api/reservations
    {"siteNumber":"A-8","startDate":"2026-09-10","endDate":"2026-09-05","customerName":"reversed"}
    ```
    → `409 Conflict`
    ```
    {"message":"종료일이 시작일보다 이전일 수 없습니다."}
    ```
- **이유**: T-1을 고치면서 이미 되던 검증(과거 날짜, 날짜 역전)이 깨지지 않았는지 판정 단계에서 다시 확인하기 위해 남긴다.

### 3. 예약 완료 시 6자리 영숫자 확인 코드가 생성된다

- **현재 상태**: 구현되어 있고 정상 동작함(이 티켓의 버그와는 무관, 회귀 확인용으로 기록).
- **예시(직접 호출로 확인)**:

  ```
  POST /api/reservations
  {"siteNumber":"A-1","startDate":"2026-09-23","endDate":"2026-09-24","customerName":"tester"}
  ```
  → `201 Created`, 응답의 `confirmationCode`: `"0O55WF"` (영문 대문자+숫자 6자리)
- **이유**: 확인 코드는 예약 취소(`DELETE /api/reservations/{id}`)·수정(`PUT`)에 필요한 값이므로, T-1 수정 후에도 형식이 유지되는지 판정 단계에서 확인하기 위해 남긴다.

## T-2 전화번호 형식 제한 / 미입력 시 예약 완료

### 1. 전화번호는 필수다. 없으면 예약할 수 없다

- **현재 상태**: 미구현 (버그). `ReservationService.createReservation`의 전화번호 검증(`phoneNumber != null && !phoneNumber.trim().isEmpty()`)이 전화번호가 없을 때는 검증 자체를 건너뛰어, 전화번호 없이도 예약이 그대로 완료된다.
- **예시(직접 호출로 확인, 현재는 아래처럼 잘못 통과함)**:

  - 전화번호 필드 자체를 생략
    ```
    POST /api/reservations
    {"siteNumber":"A-2","startDate":"2026-08-20","endDate":"2026-08-21","customerName":"nophone"}
    ```
    → `201 Created`
    ```
    {"id":6,"customerName":"nophone","startDate":"2026-08-20","endDate":"2026-08-21","siteNumber":"A-2","phoneNumber":null,"status":"CONFIRMED","confirmationCode":"KF8DFW","createdAt":null}
    ```
- **이유**: 고객센터 신고 "전화번호를 안 넣었는데 예약이 완료됐습니다"를 막기 위한 규칙. 전화번호는 필수 항목이며, 없으면(`null` 또는 공백) 예약을 거부해야 한다.
- **거부 시 응답 형식**: 다른 검증들과 동일하게 `409 Conflict` + `{"message": "전화번호를 입력해주세요."}`.

### 2. 전화번호 형식 검증(하이픈 제거 후 10~11자리 숫자)은 유지한다

- **현재 상태**: 구현되어 있고 정상 동작함(이 티켓의 버그와는 무관, 회귀 확인용으로 기록). `-`를 제거한 뒤 10~11자리 숫자가 아니면 "전화번호 형식이 올바르지 않습니다."로 거부한다.
- **이유**: T-2를 고치면서(전화번호를 필수로 바꾸면서) 기존 형식 검증 로직이 깨지지 않았는지 판정 단계에서 확인하기 위해 남긴다. 이 자릿수 기준 자체의 타당성(국제번호 등)은 별도 확인 필요(자릿수 기준 근거 자체는 T-3과 무관하며, 아직 티켓화되지 않았다 — 발견 시점 기록 오류로 보인다).

## T-3 취소한 예약의 자리에 다시 예약되지 않음

### 1. 취소된 예약은 같은 사이트·같은 기간의 새 예약을 막지 않는다

- **현재 상태**: 미구현 (버그). `ReservationService.createReservation`의 중복 체크(`reservationRepository.existsByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual`)가 `status`를 전혀 보지 않는다. `cancelReservation`이 예약을 지우지 않고 `status`를 `CANCELLED`/`CANCELLED_SAME_DAY`로만 바꾸므로(레코드는 남아있음), 취소된 예약도 여전히 "겹치는 예약"으로 잡혀 같은 자리의 새 예약을 막는다.
- **예시(직접 호출로 확인, 오늘=2026-08-16)**:

  - 1) A-5 사이트, 2026-08-18~19 예약 생성
    ```
    POST /api/reservations
    {"siteNumber":"A-5","startDate":"2026-08-18","endDate":"2026-08-19","customerName":"TestUser","phoneNumber":"01099998888"}
    ```
    → `201 Created`
    ```
    {"id":6,"customerName":"TestUser","startDate":"2026-08-18","endDate":"2026-08-19","siteNumber":"A-5","phoneNumber":"01099998888","status":"CONFIRMED","confirmationCode":"60JBBD","createdAt":null}
    ```

  - 2) 위 예약 취소
    ```
    DELETE /api/reservations/6?confirmationCode=60JBBD
    ```
    → `200 OK`
    ```
    {"message":"예약이 취소되었습니다."}
    ```
    취소 후 조회(`GET /api/reservations/6`) 시 `status`는 `"CANCELLED"`로 바뀌어 있고 레코드는 그대로 남아있음.

  - 3) 같은 사이트(A-5)·같은 기간(2026-08-18~19)으로 새 예약 시도 (현재는 아래처럼 잘못 거부됨)
    ```
    POST /api/reservations
    {"siteNumber":"A-5","startDate":"2026-08-18","endDate":"2026-08-19","customerName":"TestUser2","phoneNumber":"01088887777"}
    ```
    → `409 Conflict`
    ```
    {"message":"해당 기간에 이미 예약이 존재합니다."}
    ```
- **이유**: 고객센터 신고 "예약을 취소했는데 같은 날짜에 다시 예약하려니 이미 예약이 있다고 나옵니다"를 막기 위한 규칙. 취소(`CANCELLED`, `CANCELLED_SAME_DAY`)된 예약은 자리를 비운 것으로 취급해야 하고, `CONFIRMED` 상태의 예약만 겹침(중복) 판정에 포함해야 한다.

### 관련이지만 이번 티켓 범위 밖으로 남기는 것

- `ReservationService.checkAvailability`/`checkPeriodAvailability`(`SiteController`의 예약 가능 여부 조회 API가 사용)도 같은 방식(`existsByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual`)으로 `status`를 보지 않는다. 취소된 예약이 있는 날짜를 "예약 불가"로 잘못 보여줄 수 있다.
- `ReservationService.getMonthlyCalendar`도 `status`와 무관하게 모든 예약을 캘린더에 표시한다. 취소된 예약이 있던 날짜가 계속 "예약됨"으로 보일 수 있다.
- 두 가지 모두 T-3과 같은 근본 원인(취소된 예약을 유효한 예약처럼 취급)을 공유하지만, 신고된 증상(재예약 실패)의 직접 원인은 `createReservation`의 중복 체크이므로 이번 티켓은 거기에 한정한다. 나머지는 `tickets.md`에 별도 항목으로 남긴다.
