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
