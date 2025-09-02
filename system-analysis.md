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


## 