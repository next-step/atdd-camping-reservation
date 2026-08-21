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

## 예약 변경 전화번호 조건

- 규칙
  - 예약 변경에서도 전화번호는 필수사항이다.
  - 전화번호는 정규식 `^010(?:-\d{4}-\d{4}|\d{8})$`에 맞는 다음 두 형식 중 하나여야 한다.
    - `010-0000-0000`
    - `01000000000`
  - 전화번호 필드가 누락되거나 `null`, 빈 문자열, 공백 문자열이면 HTTP 400과 `전화번호를 입력해주세요.`를 응답한다.
  - 허용된 형식이 아니면 HTTP 400과 `유효한 전화번호가 아닙니다.`를 응답한다.
  - 거부된 변경 요청은 기존 전화번호를 포함한 예약 정보를 변경하지 않는다.
  - 허용된 전화번호이면 HTTP 200을 응답하고 해당 전화번호를 예약에 저장한다.
- 적용 범위
  - `/api/reservations/{id}` 예약 변경 API에 적용한다.
  - 신규 예약 생성의 전화번호 조건은 기존 「예약 조건」에서 다룬다.
- 이유
  - 앞서 전화번호 필수 여부와 허용 형식을 신규 예약 생성과 예약 변경에 모두 적용하기로 정했다.
  - 예약 변경을 통해 필수 전화번호가 제거되거나 허용하지 않은 형식으로 바뀌는 경로를 막는다.
- 변경 전 경계 실측
  - 실측 기준 시각: 2026-08-21 (Asia/Seoul)
  - 각 경우는 2026-08-22의 서로 다른 사이트 B-1~B-11에 유효한 전화번호 `010-1111-2222`로 기준 예약을 만든 뒤 측정했다.

| 경우 | 변경 요청의 전화번호 | 실제 결과 | 기대 결과 | 변경 후 저장 상태 | 판정 |
|---|---|---|---|---|---|
| 필수 조건 위반 | 필드 누락 | HTTP 200 | HTTP 400, `전화번호를 입력해주세요.` | 기존 번호 유지 | 불일치 |
| 필수 조건 위반 | `null` | HTTP 200 | HTTP 400, `전화번호를 입력해주세요.` | 기존 번호 유지 | 불일치 |
| 필수 조건 위반 | 빈 문자열 | HTTP 200 | HTTP 400, `전화번호를 입력해주세요.` | 빈 문자열 저장 | 불일치 |
| 필수 조건 위반 | 공백 문자열 | HTTP 200 | HTTP 400, `전화번호를 입력해주세요.` | 공백 문자열 저장 | 불일치 |
| 형식 조건 위반 | `12345678910` | HTTP 200 | HTTP 400, `유효한 전화번호가 아닙니다.` | 잘못된 형식 저장 | 불일치 |
| 형식 조건 위반 | `011-1234-5678` | HTTP 200 | HTTP 400, `유효한 전화번호가 아닙니다.` | 잘못된 형식 저장 | 불일치 |
| 형식 조건 위반 | `010-12345678` | HTTP 200 | HTTP 400, `유효한 전화번호가 아닙니다.` | 잘못된 형식 저장 | 불일치 |
| 형식 조건 위반 | `010-123-5678` | HTTP 200 | HTTP 400, `유효한 전화번호가 아닙니다.` | 잘못된 형식 저장 | 불일치 |
| 형식 조건 위반 | `010-1234-567A` | HTTP 200 | HTTP 400, `유효한 전화번호가 아닙니다.` | 잘못된 형식 저장 | 불일치 |
| 허용 형식 | `010-2222-3333` | HTTP 200 | HTTP 200 | 요청한 번호 저장 | 일치 |
| 허용 형식 | `01022223333` | HTTP 200 | HTTP 200 | 요청한 번호 저장 | 일치 |

전화번호 필드를 누락했을 때 받은 변경 및 저장 조회 응답:

```shell
curl -i -X PUT 'http://localhost:8080/api/reservations/11?confirmationCode=G42OI6' \
-H 'Content-Type: application/json' \
-d '{}'

curl -i http://localhost:8080/api/reservations/11
```

```http
HTTP/1.1 200
Content-Type: application/json

{"id":11,"customerName":"T3 기준 예약","startDate":"2026-08-22","endDate":"2026-08-22","siteNumber":"B-1","phoneNumber":"010-1111-2222","status":"CONFIRMED","confirmationCode":"G42OI6","createdAt":null}

HTTP/1.1 200
Content-Type: application/json

{"id":11,"customerName":"T3 기준 예약","startDate":"2026-08-22","endDate":"2026-08-22","siteNumber":"B-1","phoneNumber":"010-1111-2222","status":"CONFIRMED","confirmationCode":"G42OI6","createdAt":"2026-08-21T15:05:41.629362"}
```

전화번호를 명시적으로 `null`로 보냈을 때 받은 변경 및 저장 조회 응답:

```shell
curl -i -X PUT 'http://localhost:8080/api/reservations/6?confirmationCode=VDHEEF' \
-H 'Content-Type: application/json' \
-d '{"phoneNumber":null}'

curl -i http://localhost:8080/api/reservations/6
```

```http
HTTP/1.1 200
Content-Type: application/json

{"id":6,"customerName":"T3 기준 예약","startDate":"2026-08-22","endDate":"2026-08-22","siteNumber":"B-2","phoneNumber":"010-1111-2222","status":"CONFIRMED","confirmationCode":"VDHEEF","createdAt":null}

HTTP/1.1 200
Content-Type: application/json

{"id":6,"customerName":"T3 기준 예약","startDate":"2026-08-22","endDate":"2026-08-22","siteNumber":"B-2","phoneNumber":"010-1111-2222","status":"CONFIRMED","confirmationCode":"VDHEEF","createdAt":"2026-08-21T15:05:40.340415"}
```

빈 문자열, 공백 문자열, 허용되지 않은 형식으로 변경했을 때 받은 응답:

```shell
curl -i -X PUT 'http://localhost:8080/api/reservations/7?confirmationCode=0CZ42R' \
-H 'Content-Type: application/json' \
-d '{"phoneNumber":""}'

curl -i -X PUT 'http://localhost:8080/api/reservations/9?confirmationCode=FBSICK' \
-H 'Content-Type: application/json' \
-d '{"phoneNumber":"     "}'

curl -i -X PUT 'http://localhost:8080/api/reservations/10?confirmationCode=O7XWPC' \
-H 'Content-Type: application/json' \
-d '{"phoneNumber":"12345678910"}'
```

```http
HTTP/1.1 200
Content-Type: application/json

{"id":7,"customerName":"T3 기준 예약","startDate":"2026-08-22","endDate":"2026-08-22","siteNumber":"B-3","phoneNumber":"","status":"CONFIRMED","confirmationCode":"0CZ42R","createdAt":null}

HTTP/1.1 200
Content-Type: application/json

{"id":9,"customerName":"T3 기준 예약","startDate":"2026-08-22","endDate":"2026-08-22","siteNumber":"B-4","phoneNumber":"     ","status":"CONFIRMED","confirmationCode":"FBSICK","createdAt":null}

HTTP/1.1 200
Content-Type: application/json

{"id":10,"customerName":"T3 기준 예약","startDate":"2026-08-22","endDate":"2026-08-22","siteNumber":"B-5","phoneNumber":"12345678910","status":"CONFIRMED","confirmationCode":"O7XWPC","createdAt":null}
```

조회 결과에도 각각 빈 문자열, 공백 문자열, `12345678910`이 그대로 저장되어 있음을 확인했다.

두 허용 형식으로 변경했을 때 받은 응답:

```shell
curl -i -X PUT 'http://localhost:8080/api/reservations/8?confirmationCode=FGG2HB' \
-H 'Content-Type: application/json' \
-d '{"phoneNumber":"010-2222-3333"}'

curl -i -X PUT 'http://localhost:8080/api/reservations/12?confirmationCode=BWN6UG' \
-H 'Content-Type: application/json' \
-d '{"phoneNumber":"01022223333"}'
```

```http
HTTP/1.1 200
Content-Type: application/json

{"id":8,"customerName":"T3 기준 예약","startDate":"2026-08-22","endDate":"2026-08-22","siteNumber":"B-6","phoneNumber":"010-2222-3333","status":"CONFIRMED","confirmationCode":"FGG2HB","createdAt":null}

HTTP/1.1 200
Content-Type: application/json

{"id":12,"customerName":"T3 기준 예약","startDate":"2026-08-22","endDate":"2026-08-22","siteNumber":"B-7","phoneNumber":"01022223333","status":"CONFIRMED","confirmationCode":"BWN6UG","createdAt":null}
```

추가 형식 오류 경계에서 받은 응답:

```shell
curl -i -X PUT 'http://localhost:8080/api/reservations/9?confirmationCode=14LSU2' \
-H 'Content-Type: application/json' \
-d '{"phoneNumber":"011-1234-5678"}'

curl -i -X PUT 'http://localhost:8080/api/reservations/6?confirmationCode=NCAHTZ' \
-H 'Content-Type: application/json' \
-d '{"phoneNumber":"010-12345678"}'

curl -i -X PUT 'http://localhost:8080/api/reservations/7?confirmationCode=A2S8NA' \
-H 'Content-Type: application/json' \
-d '{"phoneNumber":"010-123-5678"}'

curl -i -X PUT 'http://localhost:8080/api/reservations/8?confirmationCode=AD39IB' \
-H 'Content-Type: application/json' \
-d '{"phoneNumber":"010-1234-567A"}'
```

```http
HTTP/1.1 200
Content-Type: application/json

{"id":9,"customerName":"T3 추가 경계","startDate":"2026-08-22","endDate":"2026-08-22","siteNumber":"B-8","phoneNumber":"011-1234-5678","status":"CONFIRMED","confirmationCode":"14LSU2","createdAt":null}

HTTP/1.1 200
Content-Type: application/json

{"id":6,"customerName":"T3 추가 경계","startDate":"2026-08-22","endDate":"2026-08-22","siteNumber":"B-9","phoneNumber":"010-12345678","status":"CONFIRMED","confirmationCode":"NCAHTZ","createdAt":null}

HTTP/1.1 200
Content-Type: application/json

{"id":7,"customerName":"T3 추가 경계","startDate":"2026-08-22","endDate":"2026-08-22","siteNumber":"B-10","phoneNumber":"010-123-5678","status":"CONFIRMED","confirmationCode":"A2S8NA","createdAt":null}

HTTP/1.1 200
Content-Type: application/json

{"id":8,"customerName":"T3 추가 경계","startDate":"2026-08-22","endDate":"2026-08-22","siteNumber":"B-11","phoneNumber":"010-1234-567A","status":"CONFIRMED","confirmationCode":"AD39IB","createdAt":null}
```

각 변경 후 저장 상태를 조회한 응답:

```http
HTTP/1.1 200
Content-Type: application/json

{"id":9,"customerName":"T3 추가 경계","startDate":"2026-08-22","endDate":"2026-08-22","siteNumber":"B-8","phoneNumber":"011-1234-5678","status":"CONFIRMED","confirmationCode":"14LSU2","createdAt":"2026-08-21T15:10:19.484704"}

HTTP/1.1 200
Content-Type: application/json

{"id":6,"customerName":"T3 추가 경계","startDate":"2026-08-22","endDate":"2026-08-22","siteNumber":"B-9","phoneNumber":"010-12345678","status":"CONFIRMED","confirmationCode":"NCAHTZ","createdAt":"2026-08-21T15:10:16.590461"}

HTTP/1.1 200
Content-Type: application/json

{"id":7,"customerName":"T3 추가 경계","startDate":"2026-08-22","endDate":"2026-08-22","siteNumber":"B-10","phoneNumber":"010-123-5678","status":"CONFIRMED","confirmationCode":"A2S8NA","createdAt":"2026-08-21T15:10:17.804286"}

HTTP/1.1 200
Content-Type: application/json

{"id":8,"customerName":"T3 추가 경계","startDate":"2026-08-22","endDate":"2026-08-22","siteNumber":"B-11","phoneNumber":"010-1234-567A","status":"CONFIRMED","confirmationCode":"AD39IB","createdAt":"2026-08-21T15:10:18.218986"}
```

## 취소한 예약 자리의 재예약

- 규칙
  - 신규 예약 생성 시 같은 사이트와 겹치는 기간에 `CONFIRMED` 예약이 있으면 HTTP 409와 `해당 기간에 이미 예약이 존재합니다.`를 응답하며 새 예약을 저장하지 않는다.
  - `CANCELLED` 또는 `CANCELLED_SAME_DAY` 상태의 예약은 신규 예약의 기간 충돌에서 제외한다.
  - 따라서 취소가 완료된 예약과 같은 사이트·같은 날짜로 다시 예약하면 HTTP 201로 새 `CONFIRMED` 예약을 생성한다.
  - 기존 취소 예약은 이력으로 남기며, 재예약 성공 시 별도의 새 예약을 저장한다.
- 적용 범위
  - `/api/reservations` 신규 예약 생성의 사이트·기간 중복 판단에 적용한다.
  - 예약 가능 여부 조회, 캘린더 조회, 예약 변경의 중복 판단은 T-4의 범위에 포함하지 않는다.
- 이유
  - 고객센터 신고에서 예약을 취소했는데도 같은 날짜의 같은 자리를 다시 예약할 때 기존 예약으로 판정되는 문제를 확인했다.
  - 취소된 예약은 사용 가능한 자리를 점유하지 않으므로 신규 예약을 막지 않아야 한다.
- 변경 전 경계 실측
  - 실측 기준 시각: 2026-08-21 (Asia/Seoul)

| 경우 | 기존 예약 상태 | 요청 사이트·기간 | 실제 결과 | 기대 결과 | 저장 확인 | 판정 |
|---|---|---|---|---|---|---|
| 활성 예약과 동일한 자리 | `CONFIRMED` | B-13, 2026-08-22 | HTTP 409, `해당 기간에 이미 예약이 존재합니다.` | HTTP 409, 같은 오류 메시지 | 기존 예약 1건만 존재 | 일치 |
| 일반 취소 후 동일한 자리 | `CANCELLED` | B-13, 2026-08-22 | HTTP 409, `해당 기간에 이미 예약이 존재합니다.` | HTTP 201, 새 `CONFIRMED` 예약 생성 | 취소 예약 1건만 존재하고 새 예약 없음 | 불일치 |
| 당일 취소 후 동일한 자리 | `CANCELLED_SAME_DAY` | B-14, 2026-08-21 | HTTP 409, `해당 기간에 이미 예약이 존재합니다.` | HTTP 201, 새 `CONFIRMED` 예약 생성 | 당일 취소 예약 1건만 존재하고 새 예약 없음 | 불일치 |

활성 예약이 동일한 자리의 신규 예약을 막는 기준 응답:

```shell
curl -i -X POST http://localhost:8080/api/reservations \
-H 'Content-Type: application/json' \
-d '{
"customerName": "T4 재예약 시도",
"startDate": "2026-08-22",
"endDate": "2026-08-22",
"siteNumber": "B-13",
"phoneNumber": "01012345678"
}'
```

```http
HTTP/1.1 409
Content-Type: application/json

{"message":"해당 기간에 이미 예약이 존재합니다."}
```

일반 취소를 완료하고 저장 상태를 조회한 응답:

```shell
curl -i -X DELETE 'http://localhost:8080/api/reservations/6?confirmationCode=C13O4H'
curl -i http://localhost:8080/api/reservations/6
```

```http
HTTP/1.1 200
Content-Type: application/json

{"message":"예약이 취소되었습니다."}

HTTP/1.1 200
Content-Type: application/json

{"id":6,"customerName":"T4 일반취소 기준","startDate":"2026-08-22","endDate":"2026-08-22","siteNumber":"B-13","phoneNumber":"010-1234-5678","status":"CANCELLED","confirmationCode":"C13O4H","createdAt":"2026-08-21T14:42:39.14459"}
```

일반 취소 후 같은 자리를 다시 예약했을 때 받은 응답:

```shell
curl -i -X POST http://localhost:8080/api/reservations \
-H 'Content-Type: application/json' \
-d '{
"customerName": "T4 재예약 시도",
"startDate": "2026-08-22",
"endDate": "2026-08-22",
"siteNumber": "B-13",
"phoneNumber": "01012345678"
}'
```

```http
HTTP/1.1 409
Content-Type: application/json

{"message":"해당 기간에 이미 예약이 존재합니다."}
```

재예약 실패 후 날짜 조회로 확인한 저장 상태:

```shell
curl -i 'http://localhost:8080/api/reservations?date=2026-08-22'
```

```http
HTTP/1.1 200
Content-Type: application/json

[{"id":6,"customerName":"T4 일반취소 기준","startDate":"2026-08-22","endDate":"2026-08-22","siteNumber":"B-13","phoneNumber":"010-1234-5678","status":"CANCELLED","confirmationCode":"C13O4H","createdAt":"2026-08-21T14:42:39.14459"}]
```

당일 취소를 완료하고 저장 상태를 조회한 응답:

```shell
curl -i -X DELETE 'http://localhost:8080/api/reservations/7?confirmationCode=CGO3YC'
curl -i http://localhost:8080/api/reservations/7
```

```http
HTTP/1.1 200
Content-Type: application/json

{"message":"예약이 취소되었습니다."}

HTTP/1.1 200
Content-Type: application/json

{"id":7,"customerName":"T4 당일취소 기준","startDate":"2026-08-21","endDate":"2026-08-21","siteNumber":"B-14","phoneNumber":"010-9876-5432","status":"CANCELLED_SAME_DAY","confirmationCode":"CGO3YC","createdAt":"2026-08-21T14:43:21.973264"}
```

당일 취소 후 같은 자리를 다시 예약했을 때 받은 응답:

```shell
curl -i -X POST http://localhost:8080/api/reservations \
-H 'Content-Type: application/json' \
-d '{
"customerName": "T4 당일 재예약",
"startDate": "2026-08-21",
"endDate": "2026-08-21",
"siteNumber": "B-14",
"phoneNumber": "01098765432"
}'
```

```http
HTTP/1.1 409
Content-Type: application/json

{"message":"해당 기간에 이미 예약이 존재합니다."}
```

- 남은 질문과 후속 범위
  - T-5: 취소된 다일 예약과 일부 날짜만 겹치는 신규 예약도 허용해야 하는지는 티켓 문구만으로 확정하지 않았다. 기간 겹침 전체에 같은 규칙을 적용할지 확인한다.
  - T-6: 예약 가능 여부 조회와 캘린더가 두 취소 상태를 모두 빈자리로 표시하는지는 이번 단계에서 실측하지 않았다. 각 조회 경로를 별도로 점검한다.
