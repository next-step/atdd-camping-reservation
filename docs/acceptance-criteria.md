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

---

# T-2 인수 조건

## 요구사항

1. 전화번호는 필수다. 없으면 예약할 수 없다.

---

## 요구사항 1: 전화번호는 필수다. 없으면 예약할 수 없다

예약 생성(POST) 시 phoneNumber가 없거나(필드 누락 또는 null), 빈 문자열이거나, 공백만 있으면 거부한다.
생성(POST)과 수정(PUT) 모두 적용한다 — 수정 시 phoneNumber를 빈 문자열이나 공백만으로 바꾸려는 시도도
동일하게 거부한다. 단, 수정 요청에서 phoneNumber 필드를 아예 보내지 않으면(다른 필드만 수정) 기존 값을
그대로 둔다 — "건드리지 않음"과 "빈 값으로 지움"은 다른 요청이다.

## 예시

기준일: 2026-08-17

### 거부 — phoneNumber 필드 자체 없음 (현재 버그)

```
POST /api/reservations
{"siteNumber":"A-11","startDate":"2026-08-20","endDate":"2026-08-20","customerName":"테스터"}
```

실측 응답 (현재 동작 — 버그):
```json
{"id":6,"customerName":"테스터","startDate":"2026-08-20","endDate":"2026-08-20","siteNumber":"A-11","phoneNumber":null,"status":"CONFIRMED","confirmationCode":"YSA2C8","createdAt":null}
```
→ HTTP 201로 성공 (버그). 수정 후에는 HTTP 409와 오류 메시지를 반환해야 한다.

### 거부 — phoneNumber 빈 문자열 (현재 버그)

```
POST /api/reservations
{"siteNumber":"A-12","startDate":"2026-08-20","endDate":"2026-08-20","customerName":"테스터","phoneNumber":""}
```

실측 응답 (현재 동작 — 버그):
```json
{"id":7,"customerName":"테스터","startDate":"2026-08-20","endDate":"2026-08-20","siteNumber":"A-12","phoneNumber":"","status":"CONFIRMED","confirmationCode":"P9X92L","createdAt":null}
```
→ HTTP 201로 성공 (버그). 수정 후에는 거부해야 한다.

### 거부 — phoneNumber 공백만 (현재 버그)

```
POST /api/reservations
{"siteNumber":"A-14","startDate":"2026-08-20","endDate":"2026-08-20","customerName":"테스터","phoneNumber":"   "}
```

실측 응답 (현재 동작 — 버그):
```json
{"id":9,"customerName":"테스터","startDate":"2026-08-20","endDate":"2026-08-20","siteNumber":"A-14","phoneNumber":"   ","status":"CONFIRMED","confirmationCode":"S9ORUL","createdAt":null}
```
→ HTTP 201로 성공 (버그). 수정 후에는 거부해야 한다.

### 허용 — 정상 형식 전화번호로 예약 (현재 정상, 회귀 방지)

```
POST /api/reservations
{"siteNumber":"A-16","startDate":"2026-08-20","endDate":"2026-08-20","customerName":"테스터","phoneNumber":"010-1111-2222"}
```

실측 응답:
```json
{"id":10,"customerName":"테스터","startDate":"2026-08-20","endDate":"2026-08-20","siteNumber":"A-16","phoneNumber":"010-1111-2222","status":"CONFIRMED","confirmationCode":"NG5HWM","createdAt":null}
```
→ HTTP 201. phoneNumber가 있으면 기존대로 통과한다.

### 거부 — phoneNumber 형식이 올바르지 않음 (현재 정상, 참고용)

```
POST /api/reservations
{"siteNumber":"A-15","startDate":"2026-08-20","endDate":"2026-08-20","customerName":"테스터","phoneNumber":"123"}
```

실측 응답:
```json
{"message":"전화번호 형식이 올바르지 않습니다."}
```
→ HTTP 409. 형식 검증은 이미 있고 정상 동작한다. "필수" 검증은 이 형식 검증보다 먼저 실행되어야 한다.

### 거부 — PUT으로 기존 phoneNumber를 빈 문자열로 수정 (현재 버그)

준비: `POST {"siteNumber":"A-17",...,"phoneNumber":"010-1111-2222"}`로 예약 생성 → id=6, confirmationCode=Q7Z0WY

```
PUT /api/reservations/6?confirmationCode=Q7Z0WY
{"phoneNumber":""}
```

실측 응답 (현재 동작 — 버그):
```json
{"id":6,"customerName":"테스터","startDate":"2026-08-20","endDate":"2026-08-20","siteNumber":"A-17","phoneNumber":"","status":"CONFIRMED","confirmationCode":"Q7Z0WY","createdAt":null}
```
→ HTTP 200으로 성공 (버그). 수정 후에는 HTTP 400을 반환해야 한다 (수정 실패 컨벤션).

### 거부 — PUT으로 기존 phoneNumber를 공백만으로 수정 (현재 버그)

```
PUT /api/reservations/6?confirmationCode=Q7Z0WY
{"phoneNumber":"   "}
```

실측 응답 (현재 동작 — 버그):
```json
{"id":6,"customerName":"테스터","startDate":"2026-08-20","endDate":"2026-08-20","siteNumber":"A-17","phoneNumber":"   ","status":"CONFIRMED","confirmationCode":"Q7Z0WY","createdAt":null}
```
→ HTTP 200으로 성공 (버그). 수정 후에는 거부해야 한다.

### 허용 — PUT에서 phoneNumber 필드를 생략하고 다른 필드만 수정 (현재 정상, 회귀 방지)

준비: `POST {"siteNumber":"A-18",...,"phoneNumber":"010-2222-3333"}`로 예약 생성 → id=7, confirmationCode=2C6ZJ9

```
PUT /api/reservations/7?confirmationCode=2C6ZJ9
{"customerName":"테스터2"}
```

실측 응답:
```json
{"id":7,"customerName":"테스터2","startDate":"2026-08-20","endDate":"2026-08-20","siteNumber":"A-18","phoneNumber":"010-2222-3333","status":"CONFIRMED","confirmationCode":"2C6ZJ9","createdAt":null}
```
→ HTTP 200. phoneNumber를 건드리지 않으면 기존 값(010-2222-3333)이 유지된다. 이 동작은 그대로 둔다.

### 허용 — PUT으로 정상 형식의 새 phoneNumber로 수정 (현재 정상, 회귀 방지)

```
PUT /api/reservations/7?confirmationCode=2C6ZJ9
{"phoneNumber":"010-9999-8888"}
```

실측 응답:
```json
{"id":7,"customerName":"테스터2","startDate":"2026-08-20","endDate":"2026-08-20","siteNumber":"A-18","phoneNumber":"010-9999-8888","status":"CONFIRMED","confirmationCode":"2C6ZJ9","createdAt":null}
```
→ HTTP 200. 정상 형식의 새 번호로 바꾸는 것은 그대로 허용한다.

## 이유

고객센터 신고(T-2): "전화번호를 안 넣었는데 예약이 완료됐습니다."
현재 코드(`ReservationService.java:123`)는 `if (phoneNumber != null && !phoneNumber.trim().isEmpty())`로
phoneNumber가 존재할 때만 형식을 검사하며, 존재 자체를 요구하지 않는다. 즉 phoneNumber가 없어도 이 블록을
그냥 통과해 예약이 완료된다.

전화번호는 예약 확인 알림(SMS 발송 시뮬레이션, STEP 10)과 이름+전화번호로 예약을 조회하는
`getReservationsByNameAndPhone`의 유일한 조회 키다. 값이 없으면 알림 발송과 조회 모두 불가능해진다.

수정(PUT) 경로(`updateReservation`, `ReservationService.java:429-431`)는 이보다 더 심하다 —
`if (request.getPhoneNumber() != null) { reservation.setPhoneNumber(...) }`뿐이라 형식 검증조차 없고,
빈 문자열을 보내면 정상 생성된 예약의 전화번호도 지워진다. 생성 시점에만 막고 수정으로 우회할 수 있다면
"필수" 규칙은 지켜지지 않은 것과 같으므로 이번 티켓에서 함께 다룬다.
