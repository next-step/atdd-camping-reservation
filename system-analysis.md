# 시스템 분석 결과

## API 목록

| Endpoint                             | Method | 목적                   | 주요 파라미터                                              | 비고                     |
|--------------------------------------|--------|----------------------|------------------------------------------------------|------------------------|
| /api/reservations                    | POST   | 예약 생성                | siteNumber, startDate, endDate, customerName         | @PrePersist 로 예약 상태 변경 |
| /api/reservations/{id}               | GET    | 예약 조회                | id(reservation)                                      |                        |
| /api/reservations                    | GET    | 예약 목록 조회             | date, customerName                                   |                        |
| /api/reservations/{id}               | DELETE | 예약 취소                | id, confirmationCode                                 |                        |
| /api/reservations/{id}               | PUT    | 예약 변경                | id, confirmationCode, siteNumber, startDate, endDate | 요구사항에 없어서 추가           |
| /api/reservations/my                 | GET    | 내 예약 목록 조회           | name, phone                                          |                        |
| /api/reservations/calendar           | GET    | 날짜별 예약 현황 조회         | year, month, siteId                                  |                        |
| /api/sites                           | GET    | 전체 사이트 목록 조회         |                                                      |                        |
| /api/sites/{sitedId}                 | GET    | 사이트 정보 조회            | siteId                                               |                        |
| /api/sites/{siteNumber}/availability | GET    | 단일 사이트 가용성 확인        | siteNumber, date                                     | 요구사항에 없어서 추가           |
| /api/sites/available                 | GET    | 특정 날짜의 예약 가능한 사이트 조회 | date                                                 |                        |
| /api/sites/search                    | GET    | 특정 기간의 예약 가능한 사이트 조회 | startDate, endDate, size                             |                        |

## 발견한 비즈니스 규칙

- 예약 상태(당일취소와 사전취소를 상태값으로 구분한다.)
    - CONFIRMED
    - CANCELLED
    - CANCELLED_SAME_DAY
- 확인코드는 랜덤한 6자리의 영문(대문자) 숫자 조합이다.
- 사이트 번호는 고유 식별자이다.
- 대형사이트(A로 시작)만 전력을 제공한다.
- 사이트 사이즈 표현 값: `"대형", "소형"`
