Feature: 예약 생성
  사용자가 캠핑장 예약을 생성할 때, 유효성 검사를 한 뒤 예약이 생성된다

  Background:
    Given 오늘이 "2025-01-01"이다
    And 사이트가 존재한다:
      | siteNumber | description | maxPeople |
      | A1         | 대형          | 6         |
      | B1         | 소형          | 4         |

  Scenario: 유효한 예약 정보로 예약을 생성한다
    When 사용자가 다음 정보로 예약을 생성한다:
      | customerName | phoneNumber   | siteNumber | startDate  | endDate    |
      | 테스터          | 010-1111-1111 | A1         | 2025-01-02 | 2025-01-02 |
    Then 예약이 성공한다
    And 6자리 영숫자 확인 코드가 생성된다

  Scenario: 30일 초과 날짜로 예약을 생성할 수 없다
    When 사용자가 다음 정보로 예약을 생성한다:
      | customerName | phoneNumber   | siteNumber | startDate  | endDate    |
      | 테스터          | 010-1111-1111 | A1         | 2025-02-01 | 2025-02-01 |
    Then 예약이 실패한다
    And "예약은 오늘로부터 30일 이내에만 가능합니다"라는 메시지가 표시된다

  Scenario: 과거 날짜로 예약을 생성할 수 없다
    When 사용자가 다음 정보로 예약을 생성한다:
      | customerName | phoneNumber   | siteNumber | startDate  | endDate    |
      | 테스터          | 010-1111-1111 | A1         | 2024-12-31 | 2024-12-31 |
    Then 예약이 실패한다
    And "과거 날짜로는 예약할 수 없습니다"라는 메시지가 표시된다

  Scenario: 종료일이 시작일보다 이전일 수 없다
    When 사용자가 다음 정보로 예약을 생성한다:
      | customerName | phoneNumber   | siteNumber | startDate  | endDate    |
      | 테스터          | 010-1111-1111 | A1         | 2025-01-05 | 2025-01-03 |
    Then 예약이 실패한다
    And "종료일은 시작일보다 이전일 수 없습니다"라는 메시지가 표시된다

  Scenario: 예약자 이름이 필수 입력값이다
    When 사용자가 다음 정보로 예약을 생성한다:
      | customerName | phoneNumber   | siteNumber | startDate  | endDate    |
      |              | 010-1111-1111 | A1         | 2025-01-02 | 2025-01-02 |
    Then 예약이 실패한다
    And "예약자 이름은 필수 입력값입니다"라는 메시지가 표시된다

  Scenario: 전화번호가 필수 입력값이다
    When 사용자가 다음 정보로 예약을 생성한다:
      | customerName | phoneNumber | siteNumber | startDate  | endDate    |
      | 테스터          |             | A1         | 2025-01-02 | 2025-01-02 |
    Then 예약이 실패한다
    And "전화번호는 필수 입력값입니다"라는 메시지가 표시된다

  Scenario: 동일 사이트, 동일 기간에 중복 예약이 불가능하다
    Given 다음 예약이 이미 존재한다:
      | customerName | phoneNumber   | siteNumber | startDate  | endDate    | status    |
      | 기존예약자        | 010-2222-2222 | A1         | 2025-01-03 | 2025-01-04 | CONFIRMED |
    When 사용자가 다음 정보로 예약을 생성한다:
      | customerName | phoneNumber   | siteNumber | startDate  | endDate    |
      | 새예약자         | 010-1111-1111 | A1         | 2025-01-03 | 2025-01-04 |
    Then 예약이 실패한다
    And "해당 기간에 이미 예약이 존재합니다"라는 메시지가 표시된다

  Scenario: 예약이 취소된 경우 동일 사이트, 동일 기간에 다시 예약할 수 있다
    Given 다음 예약이 이미 존재한다:
      | customerName | phoneNumber   | siteNumber | startDate  | endDate    | status    |
      | 취소된예약자       | 010-2222-2222 | A1         | 2025-01-03 | 2025-01-04 | CANCELLED |
    When 사용자가 다음 정보로 예약을 생성한다:
      | customerName | phoneNumber   | siteNumber | startDate  | endDate    |
      | 새예약자         | 010-1111-1111 | A1         | 2025-01-03 | 2025-01-04 |
    Then 예약이 성공한다
    And 6자리 영숫자 확인 코드가 생성된다

  Scenario: 연박 예약 시 시작일부터 종료일까지 모든 날짜가 예약 가능하다
    When 사용자가 다음 정보로 예약을 생성한다:
      | customerName | phoneNumber   | siteNumber | startDate  | endDate    |
      | 테스터          | 010-1111-1111 | A1         | 2025-01-03 | 2025-01-06 |
    Then 예약이 성공한다
    And 6자리 영숫자 확인 코드가 생성된다

  Scenario: 연박 예약 시 중간 날짜에 기존 예약이 있으면 실패한다
    Given 다음 예약이 이미 존재한다:
      | customerName | phoneNumber   | siteNumber | startDate  | endDate    | status    |
      | 기존예약자        | 010-2222-2222 | A1         | 2025-01-05 | 2025-01-05 | CONFIRMED |
    When 사용자가 다음 정보로 예약을 생성한다:
      | customerName | phoneNumber   | siteNumber | startDate  | endDate    |
      | 새예약자         | 010-1111-1111 | A1         | 2025-01-03 | 2025-01-06 |
    Then 예약이 실패한다
    And "예약 기간 중 일부 날짜에 이미 예약이 존재합니다"라는 메시지가 표시된다
