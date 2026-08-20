## T-1 예약 가능 시점은 오늘로부터 30일 이내

### 1. 생성 — 시작일 상한

```
규칙   예약 시작일은 오늘로부터 30일 이내여야 한다. 30일째는 받고 31일째부터 받지 않는다
이유   고객센터 신고 "30일 넘게 남은 날짜인데 예약이 됩니다". 세는 기준은 시작일이고 종료일에는 걸지 않는다

Given  오늘이 D일, 시드가 쓰지 않는 사이트
When   시작일이 D+29    Then  201 Created, 확인 코드 발급                              (회귀)
When   시작일이 D+30    Then  201 Created, 확인 코드 발급                              (회귀)
When   시작일이 D+31    Then  409 "예약은 오늘로부터 30일 이내만 가능합니다." 예약은 생성되지 않는다
                        지금  201 Created — 신고된 증상
```

30일째는 상한 안이므로 D+31을 막아도 D+30의 결과는 바뀌지 않는다.

예시 (실측 2026-08-20)
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

### 2. 생성 — 시작일 하한

```
규칙   과거 시작일은 거절한다. 오늘 시작은 허용한다
이유   이 티켓은 구간의 위쪽 끝만 다룬다. 아래쪽 끝은 지금 동작을 유지해 깨지지 않게 지킨다

Given  오늘이 D일, 시드가 쓰지 않는 사이트
When   시작일이 D-1     Then  409 "과거 날짜로 예약할 수 없습니다."                     (회귀)
When   시작일이 D       Then  201 Created, 확인 코드 발급                              (회귀)

질문   당일 시작 예약을 받는가 — T-7. 정해지기 전까지 지금 동작인 "허용"을 유지한다
```

예시 (실측 2026-08-20)
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

### 3. 변경 — 시작일 상한

```
규칙   변경으로도 30일 범위를 벗어날 수 없다
이유   범위가 생성에만 걸리면 만든 뒤 옮겨서 빠져나갈 수 있다
       거절 상태 코드는 이 경로의 지금 형태를 따라 400이다(생성은 409). 

Given  시작일이 D+5 인 예약이 하나 있다
When   시작일을 D+29 로 변경   Then  200 OK, 변경이 반영된다                            (회귀)
When   시작일을 D+30 으로 변경  Then  200 OK, 변경이 반영된다                            (회귀)
When   시작일을 D+31 로 변경   Then  400 "예약은 오늘로부터 30일 이내만 가능합니다." 예약의 날짜는 바뀌지 않는다
                          지금  200 OK — 그대로 반영된다
```

30일째는 상한 안이므로 D+31을 막아도 D+30의 결과는 바뀌지 않는다.

예시 (실측 2026-08-20)
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

## T-2 전화번호는 예약에 반드시 있어야 한다

### 1. 생성 — 전화번호 필수

```
규칙   전화번호가 없으면 예약을 만들지 않는다.
       필드 생략, null, 빈 문자열, 공백만 문자열을 모두 "없음"으로 본다
이유   고객센터 신고 "전화번호를 안 넣었는데 예약이 완료됐습니다"
       내 예약 조회는 이름과 전화번호로 찾는다. 전화번호 없는 예약은 그 경로로 영영 찾을 수 없다

Given  오늘이 D일, 시드가 쓰지 않는 사이트
When   phoneNumber 필드 생략  Then  409 "전화번호를 입력해주세요." 예약은 생성되지 않는다
                        지금  201 Created — 신고된 증상
When   phoneNumber 이 null   Then  409 "전화번호를 입력해주세요." 예약은 생성되지 않는다
                        지금  201 Created — 신고된 증상
When   phoneNumber 이 ""     Then  409 "전화번호를 입력해주세요." 예약은 생성되지 않는다
                        지금  201 Created — 신고된 증상
When   phoneNumber 이 "   "  Then  409 "전화번호를 입력해주세요." 예약은 생성되지 않는다
                        지금  201 Created — 신고된 증상
When   phoneNumber 이 "010-1111-2222"  Then  201 Created, 확인 코드 발급             (회귀)
```

예시 (실측 2026-08-20, 8081 포트)
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

### 2. 생성 — 값이 있을 때의 형식 검증

```
규칙   전화번호가 있을 때의 형식 판정은 이번 티켓에서 바꾸지 않는다
이유   신고는 "없는데 통과한다"만 문제 삼았다. 없음을 막아도 있는 값의 판정은 달라지지 않아야 한다

Given  오늘이 D일, 시드가 쓰지 않는 사이트
When   "010-111-222"     Then  409 "전화번호 형식이 올바르지 않습니다."               (회귀)
When   "010-abcd-5678"   Then  409 "전화번호는 숫자만 입력 가능합니다."                (회귀)
When   "020-1234-5678"   Then  201 Created — 01x 가 아니어도 통과한다                (회귀)

질문   통신사 앞자리를 제한하는가 — T-9. 정해지기 전까지 지금 동작인 "허용"을 유지한다
```

예시 (실측 2026-08-20, 8081 포트)
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

## T-3 취소한 예약은 그 자리를 점유하지 않는다

### 1. 생성 — 취소된 예약의 자리

```
규칙   취소된 예약은 새 예약의 겹침 검사에서 세지 않는다.
       취소된 예약과 겹치는 기간은 같은 사이트라도 다시 예약할 수 있다
이유   고객센터 신고 "예약을 취소했는데 같은 날짜에 다시 예약하려니 이미 예약이 있다고 나옵니다"
       취소는 자리를 내놓는 행위다. 내놓은 자리가 계속 막혀 있으면 그 사이트·기간은 영영 팔리지 않는다

Given  시드가 쓰지 않는 사이트에 08-25~08-27 예약이 있고, 그것을 취소했다

When   08-23~08-24 (종료일이 08-25 하루 전)   Then  201 Created, 확인 코드 발급            (회귀)
When   08-23~08-25 (종료일이 08-25 와 같은 날) Then  201 Created, 확인 코드 발급
When   08-25~08-27 (완전 동일)               Then  201 Created, 확인 코드 발급
When   08-27~08-28 (시작일이 08-27 과 같은 날) Then  201 Created, 확인 코드 발급
When   08-28~08-29 (시작일이 08-27 다음날)    Then  201 Created, 확인 코드 발급            (회귀)
```

예시 (실측 2026-08-20, 8081 포트)
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

### 2. 생성 — 취소하지 않은 예약의 자리

```
규칙   취소되지 않은 예약과 겹치는 기간은 거절한다.
       점유 구간은 시작일과 종료일을 모두 포함하고, 그 바깥 하루부터 겹치지 않는다
이유   겹침 검사에서 취소를 빼는 변경은, 잘못 손대면 확정 예약까지 함께 빠져 이중 예약이 된다.

Given  시드가 쓰지 않는 사이트에 08-25~08-27 예약이 있고, 취소하지 않았다

When   08-23~08-24 (종료일이 08-25 하루 전)   Then  201 Created, 확인 코드 발급               (회귀)
When   08-23~08-25 (종료일이 08-25 와 같은 날) Then  409 "해당 기간에 이미 예약이 존재합니다."  (회귀)
When   08-25~08-27 (완전 동일)               Then  409 "해당 기간에 이미 예약이 존재합니다."  (회귀)
When   08-27~08-28 (시작일이 08-27 과 같은 날) Then  409 "해당 기간에 이미 예약이 존재합니다."  (회귀)
When   08-28~08-29 (시작일이 08-27 다음날)    Then  201 Created, 확인 코드 발급               (회귀)
```

예시 (실측 2026-08-20, 8081 포트)
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
