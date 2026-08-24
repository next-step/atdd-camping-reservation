규칙 예약 시작일은 오늘로부터 30일 이내여야한다.
이유 한 달 너머는 운영 일정이 확정되지 않아 받지 않는다.

Given 시드가 쓰지 않는 사이트
When 시작일이 오늘 + 29일 Then 201 Created, 확인 코드 발급
When 시작일이 오늘 + 31일 Then 409 "예약 시작일은 오늘로부터 30일 이내여야합니다."

질문 정확히 30일째은 포함인가?
---
규칙 과거 날짜로 예약할 수 없다.
이유 이미 지난 날짜에는 캠프를 사용할 수 없다.

Given 예약이 없는 사이트
When 시작일 > 현재 Then 201 Created, 확인 코드 발급
When 시작일 < 현재 Then 409 "과거 날짜로 예약할 수 없습니다."

질문 시작일이 현재인 경우는 예약이 가능한가?
---
규칙 종료일이 시작일보다 이전일 수 없다.
이유 유효하지 못한 이용기간 이다.

Given 예약이 없는 사이트
When 시작일 < 종료일 Then 201 Created, 확인 코드 발급
When 시작일 > 종료일 Then 409  "종료일이 시작일보다 이전일 수 없습니다."

질문 시작일과 종료일이 같은 수 있는가?
---
규칙 예약시 전화번호는 필수이다.
이유 전화번호가 없으면 유사시에 고객에게 연락할 수 없다.

Given 예약이 없는 사이트
When 전화번호 == null Then 409 "전화번호를 입력해주세요"
When 전화번호 == "" or 공백 Then 409 "전화번호를 입력해주세요"
When 전화번호 10 ~ 11자리 숫자 Then 201 Created, 확인 코드 발급

질문 전화번호 형식이 유효한지 판단 기준은 없는가? 
    ex) 0101234-1234
---
규칙 사이트를 예약할 때는 상태를 확인해야한다.
이유 사이트의 상태에 따라 예약 가능 여부가 달라진다.
1단계: 기존 예약 확인

GET /api/reservations/1 → 200
A-1 사이트, 2026-08-31 ~ 2026-09-02, status: "CONFIRMED"

2단계: 예약 취소

DELETE /api/reservations/1?confirmationCode=ABC123 → 200
{"message":"예약이 취소되었습니다."}

3단계: 취소 후 상태 확인

GET /api/reservations/1 → 200
status: "CANCELLED" (취소 반영됨)

4단계: 같은 기간에 재예약 시도

POST /api/reservations
{"siteNumber":"A-1","startDate":"2026-08-31","endDate":"2026-09-02",...}
→ 409 {"message":"해당 기간에 이미 예약이 존재합니다."}

Given 상태가 존재하는 사이트
when STATUS == CANCELLED Then 201 Created, 확인 코드 발급
when STATUS == CONFIRMED Then 409 "해당 기간에 이미 예약이 존재합니다."
when STATUS == CANCELLED_SAME_DAY Then 201 Created, 확인 코드 발급
when STATUS == null Then 409 "해당 기간에 이미 예약이 존재합니다."
---