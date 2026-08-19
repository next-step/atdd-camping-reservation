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


```
{"id":6,"customerName":"홍길동","startDate":"2026-09-18","endDate":"2026-09-19","siteNumber":"B-15","phoneNumber":"010-1234-5678","status":"CONFIRMED","confirmationCode":"BW1X8U","createdAt":null}
```

- 예시:
    - 30일 경계 요청 → HTTP 201
    - 경계를 초과한 요청 → HTTP 409, `예약 시작일은 오늘로부터 30일 이내여야 합니다.`

## 예약 조건

- 규칙
  - 전화번호는 필수사항이다.
  - 전화번호는 다음 두 형식 중 하나여야 한다.
    - `010-0000-0000`
    - `01000000000`
  - 형식은 정규식 `^010(?:-\d{4}-\d{4}|\d{8})$`로 정의한다.
  - 전화번호가 누락되거나 비어 있으면 HTTP 400과 `전화번호를 입력해주세요.`를 응답한다.
  - 허용된 형식이 아니면 HTTP 400과 `유효한 전화번호가 아닙니다.`를 응답한다.
  - 이 조건은 신규 예약 생성에 적용한다.
- 이유
  - 고객센터 신고에서 전화번호 없이 예약되는 문제를 확인했다.
  - 유효한 전화번호를 기재한 경우에만 예약할 수 있도록 예약 가능 범위를 제한한다.
- 경계 실측

| 경우       | 전화번호            | 실제 응답           | 기대 결과                       | 판정  |
|----------|-----------------|-----------------|-----------------------------|-----|
| 필수 조건 위반 | 빈 문자열           | HTTP 201, 예약 생성 | HTTP 400, `전화번호를 입력해주세요.`   | 불일치 |
| 필수 조건 위반 | 공백 문자열          | HTTP 201, 예약 생성 | HTTP 400, `전화번호를 입력해주세요.`   | 불일치 |
| 필수 조건 위반 | 전화번호 필드 누락      | HTTP 201, 예약 생성 | HTTP 400, `전화번호를 입력해주세요.`   | 불일치 |
| 필수 조건 위반 | `null`          | HTTP 201, 예약 생성 | HTTP 400, `전화번호를 입력해주세요.`   | 불일치 |
| 형식 조건 위반 | `12345678910`   | HTTP 201, 예약 생성 | HTTP 400, `유효한 전화번호가 아닙니다.` | 불일치 |
| 허용 형식    | `010-1234-5678` | HTTP 201, 예약 생성 | 예약 가능                       | 일치  |
| 허용 형식    | `01012345678`   | HTTP 201, 예약 생성 | 예약 가능                       | 일치  |

전화번호가 빈 문자열일 때 실제로 받은 응답:

```shell
curl -i -X POST http://localhost:8080/api/reservations \
-H 'Content-Type: application/json' \
-d '{
"customerName": "홍길동",
"startDate": "2026-09-18",
"endDate": "2026-09-19",
"siteNumber": "B-15",
"phoneNumber": ""
}'
```

```http
HTTP/1.1 201
Content-Type: application/json
Transfer-Encoding: chunked
Date: Wed, 19 Aug 2026 14:13:16 GMT

{"id":6,"customerName":"홍길동","startDate":"2026-09-18","endDate":"2026-09-19","siteNumber":"B-15","phoneNumber":"","status":"CONFIRMED","confirmationCode":"TGWGMB","createdAt":null}
```

전화번호가 공백 문자열일 때 실제로 받은 응답:

```shell
curl -i -X POST http://localhost:8080/api/reservations \
-H 'Content-Type: application/json' \
-d '{
"customerName": "홍길동",
"startDate": "2026-08-22",
"endDate": "2026-08-22",
"siteNumber": "B-15",
"phoneNumber": "     "
}'
```

```http
HTTP/1.1 201
Content-Type: application/json
Transfer-Encoding: chunked
Date: Wed, 19 Aug 2026 14:24:02 GMT

{"id":10,"customerName":"홍길동","startDate":"2026-08-22","endDate":"2026-08-22","siteNumber":"B-15","phoneNumber":"     ","status":"CONFIRMED","confirmationCode":"BNFB23","createdAt":null}
```

전화번호 필드를 누락했을 때 실제로 받은 응답:

```shell
curl -i -X POST http://localhost:8080/api/reservations \
-H 'Content-Type: application/json' \
-d '{
"customerName": "홍길동",
"startDate": "2026-08-23",
"endDate": "2026-08-23",
"siteNumber": "B-15"
}'
```

```http
HTTP/1.1 201
Content-Type: application/json
Transfer-Encoding: chunked
Date: Wed, 19 Aug 2026 14:24:24 GMT

{"id":11,"customerName":"홍길동","startDate":"2026-08-23","endDate":"2026-08-23","siteNumber":"B-15","phoneNumber":null,"status":"CONFIRMED","confirmationCode":"FF1XLN","createdAt":null}
```

전화번호를 명시적으로 `null`로 보냈을 때 실제로 받은 응답:

```shell
curl -i -X POST http://localhost:8080/api/reservations \
-H 'Content-Type: application/json' \
-d '{
"customerName": "홍길동",
"startDate": "2026-08-24",
"endDate": "2026-08-24",
"siteNumber": "B-15",
"phoneNumber": null
}'
```

```http
HTTP/1.1 201
Content-Type: application/json
Transfer-Encoding: chunked
Date: Wed, 19 Aug 2026 14:27:25 GMT

{"id":12,"customerName":"홍길동","startDate":"2026-08-24","endDate":"2026-08-24","siteNumber":"B-15","phoneNumber":null,"status":"CONFIRMED","confirmationCode":"IN28QM","createdAt":null}
```

허용되지 않은 전화번호 형식으로 실제로 받은 응답:

```shell
curl -i -X POST http://localhost:8080/api/reservations \
-H 'Content-Type: application/json' \
-d '{
"customerName": "홍길동",
"startDate": "2026-08-19",
"endDate": "2026-08-19",
"siteNumber": "B-15",
"phoneNumber": "12345678910"
}'
```

```http
HTTP/1.1 201
Content-Type: application/json
Transfer-Encoding: chunked
Date: Wed, 19 Aug 2026 14:18:43 GMT

{"id":7,"customerName":"홍길동","startDate":"2026-08-19","endDate":"2026-08-19","siteNumber":"B-15","phoneNumber":"12345678910","status":"CONFIRMED","confirmationCode":"8ILXLN","createdAt":null}
```

허용된 전화번호 형식으로 실제로 받은 응답:

```shell
curl -i -X POST http://localhost:8080/api/reservations \
-H 'Content-Type: application/json' \
-d '{
"customerName": "홍길동",
"startDate": "2026-08-20",
"endDate": "2026-08-20",
"siteNumber": "B-15",
"phoneNumber": "010-1234-5678"
}'
```

```http
HTTP/1.1 201
Content-Type: application/json
Transfer-Encoding: chunked
Date: Wed, 19 Aug 2026 14:19:08 GMT

{"id":8,"customerName":"홍길동","startDate":"2026-08-20","endDate":"2026-08-20","siteNumber":"B-15","phoneNumber":"010-1234-5678","status":"CONFIRMED","confirmationCode":"14QMIA","createdAt":null}
```

하이픈 없는 허용 전화번호 형식으로 실제로 받은 응답:

```shell
curl -i -X POST http://localhost:8080/api/reservations \
-H 'Content-Type: application/json' \
-d '{
"customerName": "홍길동",
"startDate": "2026-08-21",
"endDate": "2026-08-21",
"siteNumber": "B-15",
"phoneNumber": "01012345678"
}'
```

```http
HTTP/1.1 201
Content-Type: application/json
Transfer-Encoding: chunked
Date: Wed, 19 Aug 2026 14:23:05 GMT

{"id":9,"customerName":"홍길동","startDate":"2026-08-21","endDate":"2026-08-21","siteNumber":"B-15","phoneNumber":"01012345678","status":"CONFIRMED","confirmationCode":"E6NBRJ","createdAt":null}
```

- 예시:
    - 전화번호 누락 또는 빈 값 요청 → HTTP 400, `전화번호를 입력해주세요.`
    - 허용되지 않은 전화번호 형식 요청 → HTTP 400, `유효한 전화번호가 아닙니다.`
    - `010-1234-5678` 형식 요청 → HTTP 201
    - `01012345678` 형식 요청 → HTTP 201
