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
