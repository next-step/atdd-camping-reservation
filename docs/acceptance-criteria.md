# T-1 인수 조건

## 규칙

예약 시작일(startDate)은 오늘로부터 30일 이내여야 한다.  
오늘로부터 31일 이상 남은 날짜로 예약을 시도하면 거부한다.

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

## 이유

고객센터 신고(T-1): "오늘로부터 30일 넘게 남은 날짜인데 예약이 됩니다."  
현재 코드(`ReservationService.java:94-98`)는 체크인~체크아웃 기간이 30일을 초과하는지만 검사하며,
오늘로부터 시작일까지의 거리는 검사하지 않는다.

## 수정 경로 (PUT /api/reservations/{id})

수정 시에도 같은 규칙이 적용된다. 현재는 두 가지 경우 모두 버그다.

### startDate만 +31일로 수정 (endDate 없이)

```
PUT /api/reservations/6?confirmationCode=70363O
{"startDate":"2026-09-16"}
```

실측 응답 (현재 동작 — 버그):
```json
{"id":6,"startDate":"2026-09-16","endDate":"2026-08-17","siteNumber":"B-1","status":"CONFIRMED","confirmationCode":"70363O"}
```
→ HTTP 200으로 성공 (버그). 수정 후에는 거부해야 한다.

### startDate+endDate 모두 +31일로 수정

```
PUT /api/reservations/6?confirmationCode=70363O
{"startDate":"2026-09-16","endDate":"2026-09-16"}
```

실측 응답 (현재 동작 — 버그):
```json
{"id":6,"startDate":"2026-09-16","endDate":"2026-09-16","siteNumber":"B-1","status":"CONFIRMED","confirmationCode":"70363O"}
```
→ HTTP 200으로 성공 (버그). 수정 후에는 거부해야 한다.
