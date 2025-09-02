# 시스템 분석 결과

## API 목록

| Endpoint                             | Method | 목적              | 주요 파라미터                                  | 비고 |
|--------------------------------------|--------|-----------------|------------------------------------------|----|
| /api/sites                           | GET    | 사이트 전체 목록 조회    |                                          |    |
| /api/sites/{siteId}                  | GET    | 사이트 상세 정보 조회    | siteId                                   |    |
| /api/sites/search                    | POST   | 사이트 조건 검색       | startDate, endDate, size                 |    |
| /api/sites/{siteNumber}/availability | GET    | 사이트 예약 가능 여부 조회 | siteNumber, date                         |    |
| /api/sites/available                 | GET    | 예약 가능한 사이트 조회   | date                                     |    |
| /api/reservations                    | POST   | 예약 생성           | ReservationRequest                       |    |
| /api/reservations                    | GET    | 예약 목록 조회        | page, customerName                       |    |
| /api/reservations/{id}               | GET    | 예약 상세 조회        | id                                       |    |
| /api/reservations/{id}               | DELETE | 예약 취소           | id, confirmationCode                     |    |
| /api/reservations/{id}               | PUT    | 예약 수정           | id, confirmationCode, ReservationRequest |    |
| /api/reservations/my                 | POST   | 내 예약 목록 조회      | name, phone                              |    |
| /api/reservations/calendar           | GET    | 달력별 예약 현황 조회    | year, month, siteId                      |    |


## 발견한 비즈니스 규칙
- 예약시 필수 정보
  - 예약자는 이름, 전화번호, 캠핑 사이트 번호를 필수로 입력해야 한다.
  - 예약자 이름은 빈 문자열일 수 없으며, 전화번호는 null일 수 없다.
- 예약 기간
  - 예약 기간은 오늘로부터 30일 이내여야 한다.
- 예약 날짜 정보
  - 과거 날짜는 예약할 수 없다.
  - 종료일이 시작일보다 이전일 수 없다.
- 중복 예약 방지
  - 동일 사이트, 동일 기간에 중복 예약은 불가하다.
  - 동시에 여러 요청이 들어와도 단 하나의 예약만 성공해야 한다. (동시성 제어 필요)
  - 취소된 예약(CANCELLED 상태)은 중복 체크에서 제외되어야 한다.
- 확인 코드
  - 예약 완료 시 6자리 영숫자 확인 코드가 자동 생성되어야 한다
- 연박 예약
  - 일과 종료일뿐만 아니라 중간 날짜들도 모두 예약 가능해야 한다.