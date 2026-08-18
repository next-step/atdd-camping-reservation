## 예약 가능 날짜 범위

- 규칙
  - 예약 시작일은 오늘로부터 30일 이내여야 한다.
  - 오늘로부터 31일 이상 남은 날짜에는 예약이 생성되지 않아야 한다.
- 이유
  - 고객센터 신고에서 오늘로부터 30일을 초과해 남은 날짜가 예약되는 문제를 확인했다.
  - 30일 이내의 날짜만 예약할 수 있도록 예약 가능 범위를 제한한다.
- 경계 실측

| 경우    | 요청 기간                   | 실제 응답           | 기대 결과 | 판정  |
|-------|-------------------------|-----------------|-------|-----|
| 허용 경계 | 2026-09-17 ~ 2026-09-18 | HTTP 201, 예약 생성 | 예약 가능 | 일치  |
| 제한 경계 | 2026-09-18 ~ 2026-09-19 | HTTP 201, 예약 생성 | 예약 불가 | 불일치 |

허용 경계에서 실제로 받은 응답:

curl -i -X POST http://localhost:8080/api/reservations \
-H 'Content-Type: application/json' \
-d '{
"customerName": "홍길동",
"startDate": "2026-09-17",
"endDate": "2026-09-18",
"siteNumber": "B-15",
"phoneNumber": "010-1234-5678"
}'
HTTP/1.1 201
Content-Type: application/json
Transfer-Encoding: chunked
Date: Tue, 18 Aug 2026 12:09:14 GMT

```
{"id":6,"customerName":"홍길동","startDate":"2026-09-17","endDate":"2026-09-18","siteNumber":"B-15","phoneNumber":"010-1234-5678","status":"CONFIRMED","confirmationCode":"S5K363","createdAt":null}
```

제한 경계에서 실제로 받은 응답:

curl -i -X POST http://localhost:8080/api/reservations \
-H 'Content-Type: application/json' \
-d '{
"customerName": "홍길동",
"startDate": "2026-09-18",
"endDate": "2026-09-19",
"siteNumber": "B-15",
"phoneNumber": "010-1234-5678"
}'
HTTP/1.1 201
Content-Type: application/json
Transfer-Encoding: chunked
Date: Tue, 18 Aug 2026 12:17:55 GMT


```
{"id":6,"customerName":"홍길동","startDate":"2026-09-18","endDate":"2026-09-19","siteNumber":"B-15","phoneNumber":"010-1234-5678","status":"CONFIRMED","confirmationCode":"BW1X8U","createdAt":null}
```

- 예시:
    - 30일 경계 요청 → HTTP 201
    - 경계를 초과한 요청 → HTTP 409, `예약 시작일은 오늘로부터 30일 이내여야 합니다.`
