# 시스템 분석 결과

## API 목록
| Endpoint                               | Method | 목적           | 주요 파라미터                                               | 비고                          |
|----------------------------------------|--------|---------------|-------------------------------------------------------------|-------------------------------|
| /api/reservations                      | POST   | 예약 생성      | customerName, phoneNumber, startDate, endDate, siteNumber   | 6자리 확인코드 자동 생성       |
| /api/reservations/{id}                 | GET    | 예약 조회      | id (path)                                                   | 개별 예약 상세 정보            |
| /api/reservations                      | GET    | 예약 목록 조회 | date, customerName (optional)                               | 날짜별/고객명별 필터링         |
| /api/reservations/{id}                 | DELETE | 예약 취소      | id (path), confirmationCode                                 | 확인코드 검증 필수             |
| /api/reservations/{id}                 | PUT    | 예약 수정      | id (path), ReservationRequest, confirmationCode             | 확인코드 검증 필수             |
| /api/reservations/my                   | GET    | 내 예약 조회   | name, phone                                                 | 이름+전화번호로 본인 예약 조회 |
| /api/reservations/calendar             | GET    | 예약 캘린더    | year, month, siteId                                         | 월별 예약 현황 조회            |
| /api/sites                             | GET    | 사이트 목록    | 없음                                                        | 전체 캠핑 사이트 조회           |
| /api/sites/{siteId}                    | GET    | 사이트 상세    | siteId (path)                                               | 개별 사이트 정보               |
| /api/sites/{siteNumber}/availability   | GET    | 가용성 확인    | siteNumber (path), date                                     | 특정 날짜 예약 가능 여부        |
| /api/sites/available                   | GET    | 가용 사이트 조회| date                                                        | 특정 날짜 예약 가능한 모든 사이트|
| /api/sites/search                      | GET    | 사이트 검색    | startDate, endDate, size (optional)                         | 기간별 가용 사이트 검색         |


## 발견한 비즈니스 규칙
- 예약 기간 제한: 예약은 오늘로부터 30일 이내에만 가능 
- 과거 날짜 예약 불가: 과거 날짜로는 예약할 수 없음 
- 필수 입력 정보: 예약자 이름(빈 문자열 불가), 전화번호(null 불가), 캠핑 사이트 번호 필수 
- 중복 예약 방지: 동일 사이트, 동일 기간에 중복 예약 불가능 
- 동시성 제어: 동시에 여러 요청이 들어와도 단 하나의 예약만 성공
- 취소된 예약 제외: 취소된 예약(CANCELLED 상태)은 중복 체크에서 제외 
- 확인 코드 생성: 예약 완료 시 6자리 영숫자 확인 코드 자동 생성 
- 연박 예약 검증: 시작일부터 종료일까지 모든 날짜에 대해 예약 가능 여부 확인 필요
- 예약 취소 본인 확인: 예약 취소 시 확인 코드 검증 필수
- 취소 정책: 당일 취소 시 환불 불가, 사전 취소 시 전액 환불 가능
- 사이트 분류: A로 시작하는 사이트는 대형, B로 시작하는 사이트는 소형
- 취소된 예약 사이트 재예약: 취소된 예약 사이트는 즉시 재예약 가능
