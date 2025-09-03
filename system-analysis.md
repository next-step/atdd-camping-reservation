# 시스템 분석 결과

## Reservation API 목록
### 프론트에서 사용중인 API
| Endpoint               | Method | 목적                            | 주요 파라미터                                      | 비고                                                                                                                                                               |
|------------------------|--------|-------------------------------|----------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| /api/reservations      | GET    | Reservation 목록을 조회하기 위함       | date, customerName                           | 1. date 있으면 date 기준 조회<br/>2. date 없고 customerName 있으면 customerName 기준 조회<br/>3. 둘다 없으면 전체 조회                                                                    |
| /api/reservations      | POST   | Reservation 을 생성하기 위함         | siteNumber, startDate, endDate, customerName | 1. siteNumber 를 가지는 site 가 존재해야함<br/>2. startDate 는 endDate 보다 이전일 수 없음<br/>3. startDate 와 endDate 사이에 siteNumber 가 예약되어 있으면 안됨<br/>4. customerName 은 입력해야됨입력해야됨 |
| /api/reservations/my   | GET    | 사용자가 본인 Reservation 을 조회하기 위함 | name, phone                                  | 1. name 또는 phone 이 null 일 경우 에러 발생                                                                                                                               |
| /api/reservations/{id} | DELETE | 사용자가 본인 Reservation 을 취소하기 위함 | id, confirmationCode                         | 1. id 를 가지는 Reservation 이 존재해야함<br/>2. confirmationCode 가 동일 해야함                                                                                                 |

### 프론트에서 사용하지 않는 API
| Endpoint                        | Method | 목적                       | 주요 파라미터              | 비고                                                                  |
|---------------------------------|--------|--------------------------|----------------------|---------------------------------------------------------------------|
| /api/reservations/{id}          | GET    | Reservation 을 상세 조회하기 위함 | id                   | 1. id 를 가지는 Reservation 이 존재해야함                                     |
| /api/reservations/{id}          | PUT    | Reservation 을 수정하기 위함    | id, confirmationCode | 1. id 를 가지는 Reservation 이 존재해야함<br/>2. confirmationCode 가 동일 해야함해야함 |
| /api/reservations/calendar      | GET    | 잘 모르겟음                   | year, month, siteId  |                                                                     |
