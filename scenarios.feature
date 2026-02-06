Feature: 예약 생성
  예약을 생성할 수 있어야 한다.

  Scenario: 정상 - 유효한 예약 생성
    Given today is 2026-02-05
    And campsite "A-1" exists
    And no reservation exists for site "A-1" between "2026-02-10" and "2026-02-12"
    When I POST "/api/reservations" with:
      | customerName | phoneNumber   | siteNumber | startDate   | endDate     |
      | 홍길동       | 010-1234-5678 | A-1        | 2026-02-10  | 2026-02-12  |
    Then response status should be 201
    And response body contains "confirmationCode" with 6 alphanumeric chars

  Scenario: 실패 - 기간 중복
    Given today is 2026-02-05
    And campsite "A-1" has an existing reservation from "2026-02-10" to "2026-02-12"
    When I POST "/api/reservations" with:
      | customerName | phoneNumber   | siteNumber | startDate   | endDate     |
      | 김철수       | 010-2222-3333 | A-1        | 2026-02-11  | 2026-02-13  |
    Then response status should be 409

  Scenario: 실패 - 종료일이 시작일보다 이전
    Given today is 2026-02-05
    And campsite "A-1" exists
    When I POST "/api/reservations" with:
      | customerName | phoneNumber   | siteNumber | startDate   | endDate     |
      | 이영희       | 010-4444-5555 | A-1        | 2026-02-12  | 2026-02-10  |
    Then response status should be 409

Feature: 연박 예약
  연박 예약 시 전체 기간이 하나의 예약으로 처리되어야 한다.

  Scenario: 정상 - 3박 예약
    Given today is 2026-02-05
    And campsite "B-1" exists
    And no reservation exists for site "B-1" between "2026-02-14" and "2026-02-16"
    When I POST "/api/reservations" with:
      | customerName | phoneNumber   | siteNumber | startDate   | endDate     |
      | 박민수       | 010-6666-7777 | B-1        | 2026-02-14  | 2026-02-16  |
    Then response status should be 201

  Scenario: 실패 - 기간 중 일부 날짜에 예약 존재
    Given today is 2026-02-05
    And campsite "B-1" has an existing reservation from "2026-02-15" to "2026-02-15"
    When I POST "/api/reservations" with:
      | customerName | phoneNumber   | siteNumber | startDate   | endDate     |
      | 정수진       | 010-8888-9999 | B-1        | 2026-02-14  | 2026-02-16  |
    Then response status should be 409

Feature: 예약 조회(단건)
  예약 ID로 상세를 조회할 수 있어야 한다.

  Scenario: 정상 - 예약 ID로 조회
    Given a reservation exists with id 1
    When I GET "/api/reservations/1"
    Then response status should be 200
    And response body contains reservation id 1

  Scenario: 실패 - 존재하지 않는 예약 ID
    When I GET "/api/reservations/99999"
    Then response status should be 404

Feature: 예약 조회(목록)
  날짜/이름/내 예약 조회를 지원해야 한다.

  Scenario: 정상 - 날짜로 예약 조회
    Given a reservation exists for site "A-1" from "2026-02-10" to "2026-02-12"
    When I GET "/api/reservations?date=2026-02-11"
    Then response status should be 200
    And response list contains that reservation

  Scenario: 정상 - 이름으로 예약 조회
    Given a reservation exists for customer "홍길동"
    When I GET "/api/reservations?customerName=홍길동"
    Then response status should be 200
    And response list contains reservations for customer "홍길동"

  Scenario: 정상 - 이름+전화번호로 내 예약 조회
    Given a reservation exists for customer "홍길동" with phone "010-1234-5678"
    When I GET "/api/reservations/my?name=홍길동&phone=010-1234-5678"
    Then response status should be 200
    And response list contains reservations for customer "홍길동"

  Scenario: 실패 - 잘못된 날짜 포맷
    When I GET "/api/reservations?date=2026-02-30"
    Then response status should be 400

Feature: 예약 수정
  예약을 수정할 수 있어야 한다.

  Scenario: 정상 - 확인 코드로 예약 수정
    Given a reservation exists with id 1 and confirmation code "ABC123"
    When I PUT "/api/reservations/1?confirmationCode=ABC123" with:
      | customerName | startDate   | endDate     |
      | 홍길동       | 2026-02-20  | 2026-02-22  |
    Then response status should be 200
    And response body reflects updated dates

  Scenario: 실패 - 확인 코드 불일치
    Given a reservation exists with id 1 and confirmation code "ABC123"
    When I PUT "/api/reservations/1?confirmationCode=WRONG" with:
      | customerName |
      | 홍길동       |
    Then response status should be 400

  Scenario: 실패 - 종료일이 시작일보다 이전
    Given a reservation exists with id 1 and confirmation code "ABC123"
    When I PUT "/api/reservations/1?confirmationCode=ABC123" with:
      | startDate   | endDate     |
      | 2026-02-12  | 2026-02-10  |
    Then response status should be 400

Feature: 예약 취소
  확인 코드로 예약을 취소할 수 있어야 한다.

  Scenario: 정상 - 확인 코드로 취소
    Given a reservation exists with id 1 and confirmation code "ABC123"
    When I DELETE "/api/reservations/1?confirmationCode=ABC123"
    Then response status should be 200
    And response body contains "예약이 취소되었습니다."

  Scenario: 실패 - 확인 코드 불일치
    Given a reservation exists with id 1 and confirmation code "ABC123"
    When I DELETE "/api/reservations/1?confirmationCode=WRONG"
    Then response status should be 400

Feature: 사이트 조회
  전체 사이트 목록을 조회할 수 있어야 한다.

  Scenario: 정상 - 전체 사이트 조회
    Given campsites exist
    When I GET "/api/sites"
    Then response status should be 200
    And response list contains sites

  Scenario: 예외 - 사이트가 없으면 빈 목록
    Given no campsites exist
    When I GET "/api/sites"
    Then response status should be 200
    And response list is empty

Feature: 사이트 상세
  사이트 ID로 상세 정보를 조회할 수 있어야 한다.

  Scenario: 정상 - 사이트 상세 조회
    Given a campsite exists with id 1
    When I GET "/api/sites/1"
    Then response status should be 200
    And response body contains campsite id 1

  Scenario: 실패 - 존재하지 않는 사이트 ID
    When I GET "/api/sites/99999"
    Then response status should be 404

Feature: 사이트 가용성 조회(단건)
  사이트 번호와 날짜로 가용 여부를 조회한다.

  Scenario: 정상 - 사이트 가용 여부 반환
    Given campsite "A-1" exists
    When I GET "/api/sites/A-1/availability?date=2026-02-10"
    Then response status should be 200
    And response body contains "available"

  Scenario: 실패 - 과거 날짜 조회
    Given today is 2026-02-05
    And campsite "A-1" exists
    When I GET "/api/sites/A-1/availability?date=2026-02-01"
    Then error response is returned

Feature: 가용성 조회(단일 날짜)
  특정 날짜에 예약 가능한 사이트 목록을 조회한다.

  Scenario: 정상 - 특정 날짜 가용 사이트 목록
    Given a reservation exists for site "A-1" with reservationDate "2026-02-10"
    When I GET "/api/sites/available?date=2026-02-10"
    Then response status should be 200
    And response list does not include site "A-1"

  Scenario: 실패 - date 파라미터 누락
    When I GET "/api/sites/available"
    Then response status should be 400

Feature: 가용성 조회(기간)
  시작일/종료일 기간 가용 사이트를 조회한다.

  Scenario: 정상 - 기간 가용 사이트 + size 필터
    Given campsites exist
    And no reservation exists for site "A-1" on "2026-02-20" and "2026-02-22"
    When I GET "/api/sites/search?startDate=2026-02-20&endDate=2026-02-22&size=대형"
    Then response status should be 200
    And response list only contains size "대형"

  Scenario: 실패 - 종료일이 시작일보다 이전
    When I GET "/api/sites/search?startDate=2026-02-22&endDate=2026-02-20"
    Then error response is returned

Feature: 월별 예약 현황
  특정 사이트의 월별 예약 상태를 조회한다.

  Scenario: 정상 - 월별 캘린더 조회
    Given a campsite exists with id 1
    When I GET "/api/reservations/calendar?year=2026&month=2&siteId=1"
    Then response status should be 200
    And response body contains daily statuses
    And reserved days are marked with "available=false"

  Scenario: 실패 - 존재하지 않는 사이트 ID
    When I GET "/api/reservations/calendar?year=2026&month=2&siteId=99999"
    Then error response is returned
