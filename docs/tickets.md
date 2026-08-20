## T-1 30일 넘게 남은 날짜인데 예약이 됨 (처리완료)

내용: 고객센터 신고 — "오늘로부터 30일 넘게 남은 날짜인데 예약이 됩니다."

요구사항
- 예약은 오늘로부터 30일 이내의 날짜만 받는다. 30일째는 받고, 31일째부터 받지 않는다.
- 30일을 세는 기준은 시작일(체크인일)이다. 종료일에는 이 30일 상한을 적용하지 않는다.
  종료일에 상한을 둘지는 정해지지 않았다(T-5). 정해지기 전까지 지금 동작인 "허용"을 유지한다.
- 이 티켓은 구간의 위쪽 끝만 다룬다. 아래쪽 끝(당일 시작)은 바꾸지 않는다.
  당일 예약을 받을지는 정해지지 않았다(T-7). 정해지기 전까지 지금 동작인 "허용"을 유지한다.
- 예약 변경으로도 이 범위를 벗어날 수 없다.

API 스펙
- `POST /api/reservations` — 시작일이 범위를 벗어나면 `409` + `{"message":"예약은 오늘로부터 30일 이내만 가능합니다."}`
- `PUT /api/reservations/{id}` — 같은 조건으로 거절하고, 예약의 날짜는 바뀌지 않는다.
  단 이 경로는 거절을 `400` + `{"message": ...}` 로 응답한다(실측: 과거 날짜로 변경 → `400`). POST의 `409`와 다르다.
  이 티켓은 상태 코드를 통일하지 않고 각 경로의 지금 형태를 따른다.
- 기존 응답(성공 `201`/`200`, 과거 날짜 `409`)은 그대로 둔다.

인수 조건: `acceptance-criteria.md`의 T-1

실측 기록

1. 생성 — 시작일 상한 (실측 2026-08-20)
```
# D+29
curl -X POST 'http://localhost:8080/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-09-18","endDate":"2026-09-18","siteNumber":"B-1","phoneNumber":"010-1111-2222","numberOfPeople":2}'

201
{"id":8,"customerName":"실측","startDate":"2026-09-18","endDate":"2026-09-18","siteNumber":"B-1","phoneNumber":"010-1111-2222","status":"CONFIRMED","confirmationCode":"3XSWY7","createdAt":null}

# D+30
curl -X POST 'http://localhost:8080/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-09-19","endDate":"2026-09-19","siteNumber":"B-2","phoneNumber":"010-1111-2222","numberOfPeople":2}'

201
{"id":9,"customerName":"실측","startDate":"2026-09-19","endDate":"2026-09-19","siteNumber":"B-2","phoneNumber":"010-1111-2222","status":"CONFIRMED","confirmationCode":"8ZEUOY","createdAt":null}

# D+31 — 거절되어야 하는데 생성된다
curl -X POST 'http://localhost:8080/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-09-20","endDate":"2026-09-20","siteNumber":"B-3","phoneNumber":"010-1111-2222","numberOfPeople":2}'

201
{"id":10,"customerName":"실측","startDate":"2026-09-20","endDate":"2026-09-20","siteNumber":"B-3","phoneNumber":"010-1111-2222","status":"CONFIRMED","confirmationCode":"5MIZS7","createdAt":null}
```

2. 생성 — 시작일 하한 (실측 2026-08-20)
```
# D-1
curl -X POST 'http://localhost:8080/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-08-19","endDate":"2026-08-19","siteNumber":"B-6","phoneNumber":"010-1111-2222","numberOfPeople":2}'

409
{"message":"과거 날짜로 예약할 수 없습니다."}

# D
curl -X POST 'http://localhost:8080/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-08-20","endDate":"2026-08-20","siteNumber":"B-7","phoneNumber":"010-1111-2222","numberOfPeople":2}'

201
{"id":13,"customerName":"실측","startDate":"2026-08-20","endDate":"2026-08-20","siteNumber":"B-7","phoneNumber":"010-1111-2222","status":"CONFIRMED","confirmationCode":"C0AXXP","createdAt":null}
```

3. 변경 — 시작일 상한 (실측 2026-08-20)
```
# D+29
curl -X PUT 'http://localhost:8080/api/reservations/16?confirmationCode=LF62PD' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-09-18","endDate":"2026-09-18","siteNumber":"B-11","phoneNumber":"010-1111-2222","numberOfPeople":2}'

200
{"id":16,"customerName":"실측","startDate":"2026-09-18","endDate":"2026-09-18","siteNumber":"B-11","phoneNumber":"010-1111-2222","status":"CONFIRMED","confirmationCode":"LF62PD","createdAt":null}

# D+30
curl -X PUT 'http://localhost:8080/api/reservations/17?confirmationCode=WAOZ86' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-09-19","endDate":"2026-09-19","siteNumber":"B-12","phoneNumber":"010-1111-2222","numberOfPeople":2}'

200
{"id":17,"customerName":"실측","startDate":"2026-09-19","endDate":"2026-09-19","siteNumber":"B-12","phoneNumber":"010-1111-2222","status":"CONFIRMED","confirmationCode":"WAOZ86","createdAt":null}

# D+31 — 거절되어야 하는데 반영된다
curl -X PUT 'http://localhost:8080/api/reservations/18?confirmationCode=ZPN6ST' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-09-20","endDate":"2026-09-20","siteNumber":"B-13","phoneNumber":"010-1111-2222","numberOfPeople":2}'

200
{"id":18,"customerName":"실측","startDate":"2026-09-20","endDate":"2026-09-20","siteNumber":"B-13","phoneNumber":"010-1111-2222","status":"CONFIRMED","confirmationCode":"ZPN6ST","createdAt":null}
```

---
## T-2 전화번호 없이 예약이 완료됨 (처리완료)

내용: 고객센터 신고 — "전화번호를 안 넣었는데 예약이 완료됐습니다."

요구사항
- 예약에는 전화번호가 반드시 있어야 한다. 없으면 예약을 만들지 않는다.
- "없음"은 값이 오지 않은 것과 비어 있는 것을 모두 포함한다. 필드 생략, null, 빈 문자열,
  공백만 있는 문자열이 모두 없음이다.
- 전화번호가 있을 때의 형식 판정은 바꾸지 않는다. 지금 거절하는 것은 계속 거절하고,
  지금 받는 것은 계속 받는다.
- 통신사 앞자리를 제한할지는 정해지지 않았다(T-9). 정해지기 전까지 지금 동작인 "허용"을 유지한다.
  → T-9 에서 "010 만 허용"으로 정해졌다.
- 이 티켓은 생성만 다룬다. 변경으로 전화번호를 비울 수 있는 것은 따로 본다(T-8).

API 스펙
- `POST /api/reservations` — 전화번호가 없으면 `409` + `{"message":"전화번호를 입력해주세요."}`
  다른 필수값(사이트 번호, 예약자 이름)의 거절 문구와 같은 형태다.
  형식이 틀린 것과는 사유가 다르므로 기존 형식 오류 문구와 나눈다.
- 기존 응답(성공 `201`, 형식 오류 `409`)은 그대로 둔다.
- `PUT /api/reservations/{id}` — 이 티켓에서 바꾸지 않는다.

인수 조건: `acceptance-criteria.md`의 T-2

실측 기록

1. 생성 — 전화번호 필수 (실측 2026-08-20)
```
# 필드 생략 — 거절되어야 하는데 생성된다
curl -X POST 'http://localhost:8081/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-26","siteNumber":"B-1","numberOfPeople":2}'

201
{"id":6,"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-26","siteNumber":"B-1","phoneNumber":null,"status":"CONFIRMED","confirmationCode":"CK0RJ8","createdAt":null}

# 빈 문자열 — 거절되어야 하는데 생성된다
curl -X POST 'http://localhost:8081/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-26","siteNumber":"B-2","phoneNumber":"","numberOfPeople":2}'

201
{"id":7,"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-26","siteNumber":"B-2","phoneNumber":"","status":"CONFIRMED","confirmationCode":"53MU0F","createdAt":null}

# 공백만 — 거절되어야 하는데 생성된다
curl -X POST 'http://localhost:8081/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-26","siteNumber":"B-3","phoneNumber":"   ","numberOfPeople":2}'

201
{"id":8,"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-26","siteNumber":"B-3","phoneNumber":"   ","status":"CONFIRMED","confirmationCode":"BCVP0W","createdAt":null}

# null 명시 — 거절되어야 하는데 생성된다
curl -X POST 'http://localhost:8081/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-26","siteNumber":"B-4","phoneNumber":null,"numberOfPeople":2}'

201
{"id":9,"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-26","siteNumber":"B-4","phoneNumber":null,"status":"CONFIRMED","confirmationCode":"I12I96","createdAt":null}

# 정상 전화번호
curl -X POST 'http://localhost:8081/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-26","siteNumber":"B-5","phoneNumber":"010-1111-2222","numberOfPeople":2}'

201
{"id":10,"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-26","siteNumber":"B-5","phoneNumber":"010-1111-2222","status":"CONFIRMED","confirmationCode":"MS60PL","createdAt":null}
```

2. 생성 — 값이 있을 때의 형식 검증 (실측 2026-08-20)
```
# 자릿수 부족
curl -X POST 'http://localhost:8081/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-26","siteNumber":"B-6","phoneNumber":"010-111-222","numberOfPeople":2}'

409
{"message":"전화번호 형식이 올바르지 않습니다."}

# 숫자가 아닌 문자
curl -X POST 'http://localhost:8081/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-26","siteNumber":"B-7","phoneNumber":"010-abcd-5678","numberOfPeople":2}'

409
{"message":"전화번호는 숫자만 입력 가능합니다."}

# 01x 가 아닌 번호 — 통과한다
curl -X POST 'http://localhost:8081/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-26","siteNumber":"B-8","phoneNumber":"020-1234-5678","numberOfPeople":2}'

201
{"id":11,"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-26","siteNumber":"B-8","phoneNumber":"020-1234-5678","status":"CONFIRMED","confirmationCode":"B344EL","createdAt":null}
```

---

## T-3 취소한 예약의 자리에 다시 예약되지 않음 (처리완료)

경위: 고객센터 신고 — "예약을 취소했는데 같은 날짜에 다시 예약하려니 이미 예약이 있다고 나옵니다."

지금 어떤가
- 취소는 예약을 지우지 않고 상태만 바꾼다. 그 기록이 겹침 검사에서 점유로 잡힌다.
  실측: 취소된 08-25~08-27 에 대해 같은 기간 재예약 → `409 {"message":"해당 기간에 이미 예약이 존재합니다."}`
- 점유 구간의 양 끝에 하루라도 닿으면 막힌다.
  실측: 취소된 08-25~08-27 에 대해 08-23~08-25 → `409` / 08-27~08-28 → `409`
- 그 바깥 하루부터는 지금도 예약된다.
  실측: 취소된 08-25~08-27 에 대해 08-23~08-24 → `201` / 08-28~08-29 → `201`

요구사항
- 취소된 예약은 그 사이트·기간을 점유하지 않는다. 새 예약의 겹침 검사에서 세지 않는다.
- 취소 상태는 둘이다. 시작일이 오늘인 예약을 취소하면 "당일 취소", 그 밖은 일반 취소로 남는다.
  둘 다 취소로 보고 검사에서 뺀다.
- 취소되지 않은 예약은 지금처럼 그대로 막는다. 겹침 판정의 날짜 경계도 바꾸지 않는다.

API 스펙
- `POST /api/reservations` — 겹치는 예약이 모두 취소된 것이면 `201` + 확인 코드 발급.
  취소되지 않은 예약과 겹치면 지금대로 `409` + `{"message":"해당 기간에 이미 예약이 존재합니다."}`

인수 조건: `acceptance-criteria.md`의 T-3

실측 기록

1. 생성 — 취소된 예약의 자리 (실측 2026-08-20)
```
# 준비 — B-1 에 08-25~08-27 생성 (B-2~B-5 도 같게 준비한다)
curl -X POST 'http://localhost:8081/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"준비","startDate":"2026-08-25","endDate":"2026-08-27","siteNumber":"B-1","phoneNumber":"010-1111-2222","numberOfPeople":2}'

201
{"id":6,"customerName":"준비","startDate":"2026-08-25","endDate":"2026-08-27","siteNumber":"B-1","phoneNumber":"010-1111-2222","status":"CONFIRMED","confirmationCode":"SBSSZF","createdAt":null}

# 준비 — 취소
curl -X DELETE 'http://localhost:8081/api/reservations/6?confirmationCode=SBSSZF'

200
{"message":"예약이 취소되었습니다."}

# 종료일이 08-25 하루 전 — 안 겹침
curl -X POST 'http://localhost:8081/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-08-23","endDate":"2026-08-24","siteNumber":"B-1","phoneNumber":"010-1111-2222","numberOfPeople":2}'

201
{"id":11,"customerName":"실측","startDate":"2026-08-23","endDate":"2026-08-24","siteNumber":"B-1","phoneNumber":"010-1111-2222","status":"CONFIRMED","confirmationCode":"JYO4L3","createdAt":null}

# 종료일이 08-25 와 같은 날 — 앞 경계. 통과해야 하는데 거절된다
curl -X POST 'http://localhost:8081/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-08-23","endDate":"2026-08-25","siteNumber":"B-2","phoneNumber":"010-1111-2222","numberOfPeople":2}'

409
{"message":"해당 기간에 이미 예약이 존재합니다."}

# 완전 동일 — 통과해야 하는데 거절된다
curl -X POST 'http://localhost:8081/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-27","siteNumber":"B-3","phoneNumber":"010-1111-2222","numberOfPeople":2}'

409
{"message":"해당 기간에 이미 예약이 존재합니다."}

# 시작일이 08-27 과 같은 날 — 뒤 경계. 통과해야 하는데 거절된다
curl -X POST 'http://localhost:8081/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-08-27","endDate":"2026-08-28","siteNumber":"B-4","phoneNumber":"010-1111-2222","numberOfPeople":2}'

409
{"message":"해당 기간에 이미 예약이 존재합니다."}

# 시작일이 08-27 다음날 — 안 겹침
curl -X POST 'http://localhost:8081/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-08-28","endDate":"2026-08-29","siteNumber":"B-5","phoneNumber":"010-1111-2222","numberOfPeople":2}'

201
{"id":12,"customerName":"실측","startDate":"2026-08-28","endDate":"2026-08-29","siteNumber":"B-5","phoneNumber":"010-1111-2222","status":"CONFIRMED","confirmationCode":"H8M5DK","createdAt":null}
```

2. 생성 — 취소하지 않은 예약의 자리 (실측 2026-08-20)
```
# 준비 — B-6 에 08-25~08-27 생성. 취소하지 않는다 (B-7~B-10 도 같게 준비한다)
curl -X POST 'http://localhost:8081/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"준비","startDate":"2026-08-25","endDate":"2026-08-27","siteNumber":"B-6","phoneNumber":"010-1111-2222","numberOfPeople":2}'

201
{"id":13,"customerName":"준비","startDate":"2026-08-25","endDate":"2026-08-27","siteNumber":"B-6","phoneNumber":"010-1111-2222","status":"CONFIRMED","confirmationCode":"DR76XV","createdAt":null}

# 종료일이 08-25 하루 전 — 안 겹침
curl -X POST 'http://localhost:8081/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-08-23","endDate":"2026-08-24","siteNumber":"B-6","phoneNumber":"010-1111-2222","numberOfPeople":2}'

201
{"id":18,"customerName":"실측","startDate":"2026-08-23","endDate":"2026-08-24","siteNumber":"B-6","phoneNumber":"010-1111-2222","status":"CONFIRMED","confirmationCode":"3PBSHB","createdAt":null}

# 종료일이 08-25 와 같은 날 — 앞 경계
curl -X POST 'http://localhost:8081/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-08-23","endDate":"2026-08-25","siteNumber":"B-7","phoneNumber":"010-1111-2222","numberOfPeople":2}'

409
{"message":"해당 기간에 이미 예약이 존재합니다."}

# 완전 동일
curl -X POST 'http://localhost:8081/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-27","siteNumber":"B-8","phoneNumber":"010-1111-2222","numberOfPeople":2}'

409
{"message":"해당 기간에 이미 예약이 존재합니다."}

# 시작일이 08-27 과 같은 날 — 뒤 경계
curl -X POST 'http://localhost:8081/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-08-27","endDate":"2026-08-28","siteNumber":"B-9","phoneNumber":"010-1111-2222","numberOfPeople":2}'

409
{"message":"해당 기간에 이미 예약이 존재합니다."}

# 시작일이 08-27 다음날 — 안 겹침
curl -X POST 'http://localhost:8081/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-08-28","endDate":"2026-08-29","siteNumber":"B-10","phoneNumber":"010-1111-2222","numberOfPeople":2}'

201
{"id":19,"customerName":"실측","startDate":"2026-08-28","endDate":"2026-08-29","siteNumber":"B-10","phoneNumber":"010-1111-2222","status":"CONFIRMED","confirmationCode":"E0N1RA","createdAt":null}
```

3. 생성 — 당일 취소한 예약의 자리 (실측 2026-08-20)
```
# 준비 — B-13 에 오늘 시작 예약 생성
curl -X POST 'http://localhost:8080/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"회귀당일","startDate":"2026-08-20","endDate":"2026-08-21","siteNumber":"B-13","phoneNumber":"010-1111-2222","numberOfPeople":2}'

201
{"id":24,...,"status":"CONFIRMED","confirmationCode":"YN58DV"}

# 준비 — 취소. 시작일이 오늘이므로 당일 취소로 남는다
curl -X DELETE 'http://localhost:8080/api/reservations/24?confirmationCode=YN58DV'
200
{"message":"예약이 취소되었습니다."}

curl 'http://localhost:8080/api/reservations/24'
{"id":24,...,"status":"CANCELLED_SAME_DAY","confirmationCode":"YN58DV"}

# 같은 사이트·같은 기간 재예약
curl -X POST 'http://localhost:8080/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"당일취소자리","startDate":"2026-08-20","endDate":"2026-08-21","siteNumber":"B-13","phoneNumber":"010-1111-2222","numberOfPeople":2}'

201
{"id":26,...,"status":"CONFIRMED","confirmationCode":"985IJI"}
```

---

## T-5 요구사항에 없는 기간 상한이 코드에 있고, 종료일 상한은 정해지지 않았다

발견 경위: T-1의 30일 경계를 실측하다가, 코드의 30일이 신고의 30일과 다른 것을 재는 규칙임을 확인했다.

무엇이 있는가
- 코드는 시작일과 종료일의 간격이 30일을 넘으면 거절한다. 요구사항으로 받은 적이 없다.
  실측: 간격 31일 → `409 {"message":"예약 기간은 최대 30일입니다."}` / 간격 30일 → `201`
- 예약 가능 시점은 시작일만 본다. 종료일이 오늘+30일을 넘어도 예약된다.
  실측: 시작일 D+30, 종료일 D+33 → `201`
- T-1은 둘 다 바꾸지 않는다. 정해질 때까지 지금대로 둔다.

정해야 하는 것
- 체류 기간에 상한을 두는가. 둔다면 며칠이고, 박 수인가 일 수인가. 지금은 간격 30이 통과하므로 31일을 점유할 수 있다.
- 종료일에도 예약 가능 시점의 상한을 거는가. 걸면 30일째 체크인은 사실상 1박이 된다.

무엇이 확인되면 정리되나: 최대 연박 제한이 있는지

---

## T-6 예약 변경이 생성과 다른 규칙을 적용한다

발견 경위: T-1의 변경 경로를 실측하다 확인했다.

무엇이 있는가
- 예약 생성에는 있는 검증(체류 기간 상한, 예약자 이름 길이, 전화번호 형식)이 예약 변경에는 없다. 변경 경로는 과거 날짜와 확인 코드만 본다.
- 같은 예약이 어느 경로로 만들어졌는지에 따라 지킨 규칙이 달라진다.

정해야 하는 것
- 변경은 생성과 같은 규칙을 지켜야 하는가, 아니면 변경에만 허용되는 예외가 있는가.
- 이미 저장된 예약이 새 규칙을 어기는 상태일 때 변경 요청을 어떻게 다룰 것인가.

무엇이 확인되면 정리되나: 변경 가능한 항목의 목록과 각 항목의 제약이 확정되면.

---

## T-7 당일(오늘) 시작 예약을 허용할지 정해지지 않았다

발견 경위: T-1의 30일 범위를 실측하다가, 범위의 아래쪽 끝을 확인하며 나왔다. 신고는 "30일 넘게 남은 날짜"만 문제 삼았고, 오늘 당일을 받을지는 어디에도 없다.

무엇이 있는가
- 지금은 시작일이 오늘이면 예약된다. 하루 전이면 "과거 날짜"로 거절한다.
- 실측: 시작일 D → `201`, 시작일 D-1 → `409 {"message":"과거 날짜로 예약할 수 없습니다."}`
- 요청은 날짜만 받는다. 시각이 없으므로 당일 몇 시에 요청했는지는 판정에 들어가지 않는다.

정해야 하는 것
- 당일 시작 예약을 받을 것인가. 받지 않는다면 가장 이른 시작일은 내일인가.
- 받는다면 접수 마감 시각이 있는가. 체크인 시각이 지난 뒤 들어온 당일 예약을 어떻게 다룰 것인가.
- "오늘"을 누구의 시각으로 재는가. 요청자와 캠핑장의 시간대가 다르면 하루가 어긋난다.
- 받지 않기로 하면 거절 사유가 "과거 날짜"와 달라진다. 메시지를 나눌 것인가.

무엇이 확인되면 정리되나: 캠핑장의 체크인 운영 시각과 당일 접수 마감 정책이 확정되면.

---

## T-8 전화번호 필수가 생성에만 걸린다

발견 경위: T-2의 전화번호 없음을 실측하다가, 변경 경로로 같은 상태를 만들 수 있는 것을 확인했다.

무엇이 있는가
- 변경 요청에 전화번호를 빈 문자열로 보내면 기존 전화번호가 지워진다.
  실측: 전화번호가 있던 예약에 `PUT` 으로 `""` → `200`, 전화번호가 비워진 채 저장된다
- 전화번호를 보내지 않으면 기존 값이 유지된다. 실측: 필드 생략 → `200`, 값 그대로

정해야 하는 것
- 변경으로 전화번호를 비우는 것을 막는가. 막는다면 거절 사유는 생성과 같은 문구인가.
- 이미 전화번호 없이 저장된 예약을 변경할 때, 전화번호를 채우도록 요구할 것인가.

무엇이 확인되면 정리되나: 변경 요청에서 "값을 보내지 않음"과 "빈 값으로 지움"을 구분할지 정해지면

---

## T-9 010이 아닌 번호로도 예약이 된다

경위: T-2 의 전화번호 형식 경계를 실측하다 나왔다. 확인 연락 수단을 휴대전화로 정하면서 요구사항이 확정됐다.

지금 어떤가
- 앞자리를 전혀 보지 않는다. 011·016·019 같은 옛 휴대전화 번호가 그대로 예약된다.
  실측: `011-1234-5678` → `201` / `016-1234-5678` → `201` / `019-1234-5678` → `201`
- 휴대전화가 아닌 번호도 예약된다.
  실측: `020-1234-5678` → `201` / `02-1234-5678` → `201` / `031-123-4567` → `201`
- 하이픈을 뺀 자릿수가 10~11이면 통과한다. 그래서 존재하지 않는 10자리 010 번호가 들어온다.
  실측: `010-123-4567` → `201` / `010-111-222`(9자리) → `409` / `010-12345-6789`(12자리) → `409`

요구사항
- 앞자리가 010 인 번호만 받는다. 011·016·017·018·019 도 받지 않는다. 유선번호도 받지 않는다.
- 앞자리가 010 이면 하이픈을 뺀 자릿수가 11자리여야 한다. 10자리는 받지 않는다.
- 앞자리를 자릿수보다 먼저 본다. 유선번호처럼 둘을 함께 어기는 값은 앞자리 사유로 거절한다.
- 전화번호 없음과 숫자 아님의 판정은 바꾸지 않는다.
- 이 티켓은 생성만 다룬다. 변경 경로가 형식을 보지 않는 것은 따로 본다(T-6·T-8).

API 스펙
- `POST /api/reservations` — 앞자리가 010 이 아니면 `409` + `{"message":"010으로 시작하는 휴대전화 번호만 입력 가능합니다."}`
  없음(`전화번호를 입력해주세요.`)·숫자 아님(`전화번호는 숫자만 입력 가능합니다.`)과 사유가 다르므로 문구를 나눈다.
- `POST /api/reservations` — 앞자리가 010 인데 자릿수가 11이 아니면 `409` + `{"message":"전화번호 형식이 올바르지 않습니다."}`
  기존 자릿수 오류와 같은 문구다.
- 기존 응답(성공 `201`, 없음 `409`, 숫자 아님 `409`)은 그대로 둔다.
- `PUT /api/reservations/{id}` — 이 티켓에서 바꾸지 않는다.

인수 조건: `acceptance-criteria.md`의 T-9

실측 기록

1. 생성 — 앞자리는 010만 (실측 2026-08-20, 8083 포트)
```
# 010 — 통과한다
curl -X POST 'http://localhost:8083/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-26","siteNumber":"B-1","phoneNumber":"010-1234-5678","numberOfPeople":2}'

201
{"id":6,"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-26","siteNumber":"B-1","phoneNumber":"010-1234-5678","status":"CONFIRMED","confirmationCode":"CYBYVE","createdAt":null}

# 011 — 거절되어야 하는데 생성된다
curl -X POST 'http://localhost:8083/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-26","siteNumber":"B-2","phoneNumber":"011-1234-5678","numberOfPeople":2}'

201
{"id":7,"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-26","siteNumber":"B-2","phoneNumber":"011-1234-5678","status":"CONFIRMED","confirmationCode":"2K786U","createdAt":null}

# 019 — 거절되어야 하는데 생성된다
curl -X POST 'http://localhost:8083/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-26","siteNumber":"B-3","phoneNumber":"019-1234-5678","numberOfPeople":2}'

201
{"id":8,"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-26","siteNumber":"B-3","phoneNumber":"019-1234-5678","status":"CONFIRMED","confirmationCode":"8C6C55","createdAt":null}

# 016 — 거절되어야 하는데 생성된다
curl -X POST 'http://localhost:8083/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-26","siteNumber":"B-4","phoneNumber":"016-1234-5678","numberOfPeople":2}'

201
{"id":9,"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-26","siteNumber":"B-4","phoneNumber":"016-1234-5678","status":"CONFIRMED","confirmationCode":"FDMU6F","createdAt":null}

# 020 — 거절되어야 하는데 생성된다
curl -X POST 'http://localhost:8083/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-26","siteNumber":"B-5","phoneNumber":"020-1234-5678","numberOfPeople":2}'

201
{"id":10,"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-26","siteNumber":"B-5","phoneNumber":"020-1234-5678","status":"CONFIRMED","confirmationCode":"SCTNLM","createdAt":null}
```

2. 생성 — 010일 때의 자릿수 (실측 2026-08-20, 8083 포트)
```
# 하이픈 없는 11자리 — 통과한다
curl -X POST 'http://localhost:8083/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-26","siteNumber":"B-8","phoneNumber":"01012345678","numberOfPeople":2}'

201
{"id":13,"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-26","siteNumber":"B-8","phoneNumber":"01012345678","status":"CONFIRMED","confirmationCode":"5IGI6D","createdAt":null}

# 010 인데 10자리 — 거절되어야 하는데 생성된다
curl -X POST 'http://localhost:8083/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-26","siteNumber":"B-9","phoneNumber":"010-123-4567","numberOfPeople":2}'

201
{"id":14,"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-26","siteNumber":"B-9","phoneNumber":"010-123-4567","status":"CONFIRMED","confirmationCode":"KJK45I","createdAt":null}

# 9자리
curl -X POST 'http://localhost:8083/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-26","siteNumber":"B-15","phoneNumber":"010-111-222","numberOfPeople":2}'

409
{"message":"전화번호 형식이 올바르지 않습니다."}

# 12자리
curl -X POST 'http://localhost:8083/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-26","siteNumber":"A-8","phoneNumber":"010-12345-6789","numberOfPeople":2}'

409
{"message":"전화번호 형식이 올바르지 않습니다."}
```

3. 생성 — 앞자리와 자릿수를 함께 어길 때 (실측 2026-08-20, 8083 포트)
```
# 유선 서울, 10자리 — 거절되어야 하는데 생성된다
curl -X POST 'http://localhost:8083/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-26","siteNumber":"B-6","phoneNumber":"02-1234-5678","numberOfPeople":2}'

201
{"id":11,"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-26","siteNumber":"B-6","phoneNumber":"02-1234-5678","status":"CONFIRMED","confirmationCode":"9Z13WW","createdAt":null}

# 유선 지역, 10자리 — 거절되어야 하는데 생성된다
curl -X POST 'http://localhost:8083/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-26","siteNumber":"B-7","phoneNumber":"031-123-4567","numberOfPeople":2}'

201
{"id":12,"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-26","siteNumber":"B-7","phoneNumber":"031-123-4567","status":"CONFIRMED","confirmationCode":"TLFF7C","createdAt":null}
```

4. 생성 — 바뀌지 않는 판정 (실측 2026-08-20, 8083 포트)
```
# 필드 생략
curl -X POST 'http://localhost:8083/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-26","siteNumber":"B-11","numberOfPeople":2}'

409
{"message":"전화번호를 입력해주세요."}

# null 명시
curl -X POST 'http://localhost:8083/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-26","siteNumber":"B-12","phoneNumber":null,"numberOfPeople":2}'

409
{"message":"전화번호를 입력해주세요."}

# 빈 문자열
curl -X POST 'http://localhost:8083/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-26","siteNumber":"B-13","phoneNumber":"","numberOfPeople":2}'

409
{"message":"전화번호를 입력해주세요."}

# 공백만
curl -X POST 'http://localhost:8083/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-26","siteNumber":"B-14","phoneNumber":"   ","numberOfPeople":2}'

409
{"message":"전화번호를 입력해주세요."}

# 숫자가 아닌 문자 — 앞자리는 010 이다
curl -X POST 'http://localhost:8083/api/reservations' -H 'Content-Type: application/json' \
  -d '{"customerName":"실측","startDate":"2026-08-25","endDate":"2026-08-26","siteNumber":"A-7","phoneNumber":"010-abcd-5678","numberOfPeople":2}'

409
{"message":"전화번호는 숫자만 입력 가능합니다."}
```

---

## T-10 내 예약 조회에 빈 전화번호를 주면 500이 난다

발견 경위: T-2에서 전화번호 없는 예약이 조회되는지 확인하다 나왔다.

무엇이 있는가
- 실측: `GET /api/reservations/my?name=실측&phone=` → `500 Internal Server Error`
- 형식이 맞는 값을 주면 응답이 나간다. 실측: `phone=010-1111-2222` → `200 []`
- 잘못된 입력인데 4xx가 아니라 5xx로 나간다. 다른 경로의 거절은 `409` 또는 `400`이다.
- 이 경로는 이름과 전화번호를 함께 요구한다. 전화번호 없이 만들어진 예약은 여기로 찾을 수 없다.

정해야 하는 것
- 빈 전화번호로 조회했을 때의 응답은 무엇인가. 거절인가, 빈 목록인가.
- 거절이라면 상태 코드와 문구를 다른 경로와 맞출 것인가.

무엇이 확인되면 정리되나: 조회 경로의 입력 검증 실패를 어떤 응답으로 낼지 정해지면

---

## T-11 취소된 예약이 빈자리를 보여주는 경로 세 곳에서 여전히 자리를 막는다

경위: T-3 의 재예약 거절을 실측하다, 같은 원인이 조회 경로에도 나타나는 것을 확인했다.
T-3 의 범위를 재예약 하나로 좁히기로 정하면서 나머지를 이 티켓으로 넘겼다.

지금 어떤가
- 취소된 예약(B-1, 08-25~08-27)이 사이트 가용 여부에서 점유로 잡힌다.
  실측: `GET /api/sites/B-1/availability?date=2026-08-26` → `200 {"available":false}`
- 기간 검색에서 그 사이트가 목록에서 빠진다.
  실측: `GET /api/sites/search?startDate=2026-08-25&endDate=2026-08-27` → 목록에 B-1 없음
- 캘린더가 그 날짜를 막힌 것으로 보내고, 취소한 사람의 이름과 예약 id 까지 싣는다.
  실측: `GET /api/reservations/calendar?year=2026&month=8&siteId=21` → 25~27일 `available:false`
- 확정 예약이 있는 사이트와 응답이 구분되지 않는다.
  실측: 취소된 예약만 있는 B-2 와 확정 예약이 있는 B-3 의 캘린더 응답이 같은 형태다

요구사항
- 정해야 하는 것: 취소된 예약을 빈자리 판정에서 빼는가. 빼면 세 경로 모두에서 빼는가.
- 정해야 하는 것: 캘린더에 취소한 사람의 이름과 예약 id 를 실어 보내는 것이 맞는가.

API 스펙
- 정해지지 않았다.

인수 조건: 아직 없다.

---

## T-12 날짜별 빈 사이트 검색이 예약을 전혀 거르지 않는다

경위: T-3 에서 취소된 예약이 빈자리 판정에 어떻게 잡히는지 재다, 이 경로만 답이 다른 것을 확인했다.

지금 어떤가
- 사이트 35개를 전부 돌려준다. 확정 예약이 있는 사이트도, 취소된 예약이 있는 사이트도 모두 목록에 있다.
  실측: `GET /api/sites/available?date=2026-08-26` → `200`, 항목 35개 (B-1~B-5 모두 포함)

요구사항
- 정해야 하는 것: 이 경로가 예약을 걸러야 하는가, 아니면 사이트 목록을 돌려주는 것이 원래 용도인가.

API 스펙
- 정해지지 않았다.

인수 조건: 아직 없다. 

---

## T-13 예약 변경이 겹침 검사를 하지 않아 이중 예약이 만들어진다

경위: T-3 에서 변경으로 취소된 자리에 옮길 수 있는지 재다, 확정 예약이 있는 자리로도 옮겨지는 것을 확인했다.

지금 어떤가
- 다른 사람의 확정 예약과 완전히 겹치는 자리로 옮겨진다. 같은 사이트·같은 기간에 확정 예약 둘이 남는다.
  실측: B-3 에 08-25~08-27 확정 예약이 있는 상태에서 다른 예약을 같은 사이트·같은 기간으로 `PUT` → `200`
- 생성 경로는 같은 요청을 거절한다.
  실측: 같은 사이트·같은 기간 `POST` → `409 {"message":"해당 기간에 이미 예약이 존재합니다."}`

요구사항
- 정해야 하는 것: 변경에도 겹침 검사를 거는가. 걸면 거절은 이 경로의 형태를 따라 `400` 인가, 생성과 같은 `409` 인가.

API 스펙
- 정해지지 않았다.

인수 조건: 아직 없다. 

---

## T-14 취소된 예약을 다시 취소하거나 변경할 수 있다

경위: T-3 에서 취소 뒤의 상태를 재다 나왔다.

지금 어떤가
- 이미 취소된 예약에 취소를 다시 보내면 성공으로 답한다.
  실측: 취소된 예약에 `DELETE` → `200 {"message":"예약이 취소되었습니다."}`
- 취소된 예약의 날짜와 사이트를 변경할 수 있다. 상태는 CANCELLED 로 남는다.
  실측: 취소된 예약에 `PUT` 으로 다른 날짜 → `200`, 날짜가 바뀐 채 CANCELLED 로 저장된다

요구사항
- 정해야 하는 것: 취소된 예약에 대한 취소·변경 요청을 거절하는가. 거절한다면 상태 코드와 문구는 무엇인가.
- 정해야 하는 것: 취소된 예약을 되살리는 경로가 필요한가. 필요하다면 변경으로 하는가, 별도 경로인가.

API 스펙
- 정해지지 않았다.

인수 조건: 아직 없다. 

---

## T-15 날짜별 예약 목록에 취소된 예약이 섞여 나온다

경위: T-3 에서 취소된 예약이 어느 경로에 노출되는지 훑다 나왔다.

지금 어떤가
- CANCELLED 예약과 CONFIRMED 예약을 함께 돌려준다.
  실측: `GET /api/reservations?date=2026-08-26` → `200`, 5건 중 3건이 `"status":"CANCELLED"`
- 취소된 예약의 예약자 이름과 전화번호가 그대로 실린다.

요구사항
- 정해야 하는 것: 이 경로의 기본 응답에서 취소된 예약을 뺄 것인가, 상태를 지정하는 조건을 받을 것인가.

API 스펙
- 정해지지 않았다.

인수 조건: 아직 없다. 

---

## T-16 변경 경로로 T-9 의 앞자리 규칙을 빠져나갈 수 있다

경위: T-9 를 적용한 뒤 4단계에서 변경 경로를 실측하다 확인했다.

지금 어떤가
- 생성은 010 아닌 번호를 막지만, 만든 뒤 변경으로 바꾸면 그대로 저장된다.
  실측: 010 으로 만든 예약에 `PUT` 으로 `011-1234-5678` → `200`, 그 번호로 저장된다
- 같은 상황에서 T-1 은 변경 경로도 함께 막았다("범위가 생성에만 걸리면 만든 뒤 옮겨서
  빠져나갈 수 있다"). T-9 는 생성만 막아 두 규칙의 처리가 갈렸다.

요구사항
- 정해야 하는 것: 변경에도 앞자리 규칙을 거는가. T-1 처럼 막을 것인가,
  아니면 변경 경로 전체를 T-6 에서 한꺼번에 정할 때까지 두는가.
- 정해야 하는 것: 막는다면 거절은 이 경로의 지금 형태를 따라 `400` 인가, 생성과 같은 `409` 인가.

API 스펙
- 정해지지 않았다.

인수 조건: 아직 없다.

무엇이 확인되면 정리되나: T-6 에서 변경 가능한 항목의 목록과 각 항목의 제약이 확정되면

---
