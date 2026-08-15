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
- **이유**: T-2를 고치면서(전화번호를 필수로 바꾸면서) 기존 형식 검증 로직이 깨지지 않았는지 판정 단계에서 확인하기 위해 남긴다. 이 자릿수 기준 자체의 타당성(국제번호 등)은 T-3으로 별도 확인 필요.
