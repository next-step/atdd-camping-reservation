# T-1 인수 조건

## 요구사항

1. 예약 시작일은 오늘로부터 30일 이내여야 한다.
2. 과거 날짜로 예약할 수 없고, 종료일이 시작일보다 이전일 수 없다.
3. 예약 완료 시 6자리 영숫자 확인 코드가 생성된다.

---

## 요구사항 1: 예약 시작일은 오늘로부터 30일 이내여야 한다

오늘로부터 31일 이상 남은 날짜로 예약을 시도하면 거부한다.  
생성(POST)과 수정(PUT) 모두 적용하며, 수정 시 startDate만 전달해도 동일하게 적용한다.

## 예시

기준일: 2026-08-16

### 허용 — 오늘로부터 30일 뒤 (경계 당일)

```
POST /api/reservations
{"siteNumber":"A-1","startDate":"2026-09-15","endDate":"2026-09-15","customerName":"테스터","phoneNumber":"010-1111-2222"}
```

실측 응답:
```json
{"id":6,"customerName":"테스터","startDate":"2026-09-15","endDate":"2026-09-15","siteNumber":"A-1","status":"CONFIRMED","confirmationCode":"T7MG9X"}
```
→ HTTP 201. 30일 뒤는 허용한다.

### 거부 — 오늘로부터 31일 뒤 (경계 초과, 현재 버그)

```
POST /api/reservations
{"siteNumber":"A-2","startDate":"2026-09-16","endDate":"2026-09-16","customerName":"테스터","phoneNumber":"010-1111-2222"}
```

실측 응답 (현재 동작 — 버그):
```json
{"id":7,"customerName":"테스터","startDate":"2026-09-16","endDate":"2026-09-16","siteNumber":"A-2","status":"CONFIRMED","confirmationCode":"JAIEZD"}
```
→ HTTP 201로 성공 (버그). 수정 후에는 HTTP 409와 오류 메시지를 반환해야 한다.

### 거부 — 오늘로부터 60일 뒤 (현재 버그)

```
POST /api/reservations
{"siteNumber":"A-3","startDate":"2026-10-15","endDate":"2026-10-15","customerName":"테스터","phoneNumber":"010-1111-2222"}
```

실측 응답 (현재 동작 — 버그):
```json
{"id":8,"customerName":"테스터","startDate":"2026-10-15","endDate":"2026-10-15","siteNumber":"A-3","status":"CONFIRMED","confirmationCode":"E5799E"}
```
→ HTTP 201로 성공 (버그). 수정 후에는 거부해야 한다.

### 거부 — startDate만 +31일로 수정 (endDate 없이, 현재 버그)

```
PUT /api/reservations/6?confirmationCode=70363O
{"startDate":"2026-09-16"}
```

실측 응답 (현재 동작 — 버그):
```json
{"id":6,"startDate":"2026-09-16","endDate":"2026-08-17","siteNumber":"B-1","status":"CONFIRMED","confirmationCode":"70363O"}
```
→ HTTP 200으로 성공 (버그). 수정 후에는 거부해야 한다.

### 거부 — startDate+endDate 모두 +31일로 수정 (현재 버그)

```
PUT /api/reservations/6?confirmationCode=70363O
{"startDate":"2026-09-16","endDate":"2026-09-16"}
```

실측 응답 (현재 동작 — 버그):
```json
{"id":6,"startDate":"2026-09-16","endDate":"2026-09-16","siteNumber":"B-1","status":"CONFIRMED","confirmationCode":"70363O"}
```
→ HTTP 200으로 성공 (버그). 수정 후에는 거부해야 한다.

## 이유

고객센터 신고(T-1): "오늘로부터 30일 넘게 남은 날짜인데 예약이 됩니다."  
현재 코드(`ReservationService.java:94-98`)는 체크인~체크아웃 기간이 30일을 초과하는지만 검사하며,
오늘로부터 시작일까지의 거리는 검사하지 않는다.

---

## 요구사항 2: 과거 날짜로 예약할 수 없고, 종료일이 시작일보다 이전일 수 없다

startDate가 오늘보다 이전이거나 endDate가 startDate보다 이전이면 거부한다.  
생성(POST)과 수정(PUT) 모두 적용하며, 수정 시 startDate만 전달해도 동일하게 적용한다.

## 예시

기준일: 2026-08-17

### 거부 — 과거 날짜로 생성 (현재 정상)

```
POST /api/reservations
{"siteNumber":"A-5","startDate":"2026-08-16","endDate":"2026-08-16","customerName":"테스터","phoneNumber":"010-1111-2222"}
```

실측 응답:
```json
{"message":"과거 날짜로 예약할 수 없습니다."}
```
→ HTTP 409. 생성 경로는 이미 거부한다.

### 거부 — endDate < startDate로 생성 (현재 정상)

```
POST /api/reservations
{"siteNumber":"A-5","startDate":"2026-08-18","endDate":"2026-08-17","customerName":"테스터","phoneNumber":"010-1111-2222"}
```

실측 응답:
```json
{"message":"종료일이 시작일보다 이전일 수 없습니다."}
```
→ HTTP 409. 생성 경로는 이미 거부한다.

### 거부 — startDate+endDate 모두 과거로 수정 (현재 정상)

```
PUT /api/reservations/6?confirmationCode=E30IF2
{"startDate":"2026-08-16","endDate":"2026-08-16"}
```

실측 응답:
```json
{"message":"과거 날짜로 예약할 수 없습니다."}
```
→ HTTP 400. 두 필드 모두 전달 시 수정 경로도 거부한다.

### 거부 — startDate만 과거로 수정 (현재 버그)

```
PUT /api/reservations/6?confirmationCode=E30IF2
{"startDate":"2026-08-16"}
```

실측 응답 (현재 동작 — 버그):
```json
{"id":6,"customerName":"테스터","startDate":"2026-08-16","endDate":"2026-08-18","siteNumber":"A-5","status":"CONFIRMED","confirmationCode":"E30IF2"}
```
→ HTTP 200으로 성공 (버그). 수정 후에는 HTTP 400을 반환해야 한다.

## 이유

`updateReservation`의 과거 날짜 체크가 `if (startDate != null && endDate != null)` 블록 안에만 있다.
startDate 단독 전달 시 블록이 실행되지 않아 과거 날짜가 그대로 저장된다.
날짜 역전 예약이 저장되면 가격·포인트 계산 루프가 0회 실행돼 0원으로 처리된다.

---

## 요구사항 3: 예약 완료 시 6자리 영숫자 확인 코드가 생성된다

예약 생성 성공 응답에 `[A-Z0-9]{6}` 형식의 확인 코드(confirmationCode)가 포함된다.

## 예시

기준일: 2026-08-17

### 허용 — 정상 예약 생성 시 코드 포함 (현재 정상)

```
POST /api/reservations
{"siteNumber":"A-8","startDate":"2026-08-18","endDate":"2026-08-18","customerName":"테스터","phoneNumber":"010-1111-2222"}
```

실측 응답:
```json
{"id":7,"customerName":"테스터","startDate":"2026-08-18","endDate":"2026-08-18","siteNumber":"A-8","status":"CONFIRMED","confirmationCode":"A7FF89"}
```
→ HTTP 201. confirmationCode는 6자리 [A-Z0-9].

5회 연속 실측: `A7FF89`, `OPK9HM`, `I0CF3R`, `UY5ORJ`, `6W4PPM` — 모두 길이 6, [A-Z0-9].

## 이유

확인 코드는 예약 수정·취소 시 본인 확인 수단이다.
코드 없이 예약이 생성되면 이후 수정·취소 API를 호출할 수 없다.
현재 구현은 정상이지만 회귀를 방지하기 위해 인수 조건으로 명시한다.
