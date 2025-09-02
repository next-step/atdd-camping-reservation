Feature: 예약 수정
  사용자가 기존 예약을 수정할 때, 본인 확인 후 유효성 검사를 거쳐 수정되어야 한다

  Background:
    Given 오늘이 "2025-01-01"이다
    And 사이트가 존재한다:
      | siteNumber | description | maxPeople |
      | A1         | 대형          | 6         |
      | B1         | 소형          | 4         |
    And 다음 예약이 이미 존재한다:
      | id | customerName | phoneNumber   | siteNumber | startDate  | endDate    | status    | confirmationCode |
      | 1  | 기존고객         | 010-1111-1111 | A1         | 2025-01-10 | 2025-01-12 | CONFIRMED | ABC123           |

  Scenario: 사이트, 시작일, 종료일, 예약자 이름, 전화번호 수정이 가능하다
    When 사용자가 예약 ID "1"을 다음 정보로 수정한다:
      | confirmationCode | customerName | phoneNumber   | siteNumber | startDate  | endDate    |
      | ABC123           | 새로운이름        | 010-8888-8888 | B1         | 2025-01-20 | 2025-01-22 |
    Then 예약 수정이 성공한다
    And 예약 정보가 수정된다

  Scenario: 예약 수정 시 예약 시 발급받은 확인 코드가 일치해야 한다
    When 사용자가 예약 ID "1"을 다음 정보로 수정한다:
      | confirmationCode | customerName | phoneNumber   | siteNumber | startDate  | endDate    |
      | WRONG1           | 수정된고객        | 010-9999-9999 | B1         | 2025-01-05 | 2025-01-07 |
    Then 예약 수정이 실패한다
    And "확인 코드가 일치하지 않습니다"라는 메시지가 표시된다

  Scenario: 수정된 예약이 기존 다른 예약과 중복되지 않도록 검증된다
    Given 다음 예약이 추가로 존재한다:
      | id | customerName | phoneNumber   | siteNumber | startDate  | endDate    | status    | confirmationCode |
      | 2  | 다른고객         | 010-2222-2222 | B1         | 2025-01-05 | 2025-01-05 | CONFIRMED | DEF456           |
    When 사용자가 예약 ID "1"을 다음 정보로 수정한다:
      | confirmationCode | customerName | phoneNumber   | siteNumber | startDate  | endDate    |
      | ABC123           | 기존고객         | 010-1111-1111 | B1         | 2025-01-05 | 2025-01-05 |
    Then 예약 수정이 실패한다
    And "해당 기간에 이미 다른 예약이 존재합니다"라는 메시지가 표시된다

  Scenario: 수정하는 날짜는 오늘로부터 30일 이내에만 가능하다
    When 사용자가 예약 ID "1"을 다음 정보로 수정한다:
      | confirmationCode | customerName | phoneNumber   | siteNumber | startDate  | endDate    |
      | ABC123           | 기존고객         | 010-1111-1111 | A1         | 2025-02-01 | 2025-02-02 |
    Then 예약 수정이 실패한다
    And "예약은 오늘로부터 30일 이내에만 가능합니다"라는 메시지가 표시된다

  Scenario: 과거 날짜로 수정이 불가능하다
    When 사용자가 예약 ID "1"을 다음 정보로 수정한다:
      | confirmationCode | customerName | phoneNumber   | siteNumber | startDate  | endDate    |
      | ABC123           | 기존고객         | 010-1111-1111 | A1         | 2024-12-31 | 2024-12-31 |
    Then 예약 수정이 실패한다
    And "과거 날짜로는 수정할 수 없습니다"라는 메시지가 표시된다

  Scenario: 종료일이 시작일보다 이전으로 수정될 수 없다
    When 사용자가 예약 ID "1"을 다음 정보로 수정한다:
      | confirmationCode | customerName | phoneNumber   | siteNumber | startDate  | endDate    |
      | ABC123           | 기존고객         | 010-1111-1111 | A1         | 2025-01-15 | 2025-01-10 |
    Then 예약 수정이 실패한다
    And "종료일은 시작일보다 이전일 수 없습니다"라는 메시지가 표시된다

  Scenario: 본인의 예약이 아닌 경우 수정할 수 없다
    Given 다음 예약이 추가로 존재한다:
      | id | customerName | phoneNumber   | siteNumber | startDate  | endDate    | status    | confirmationCode |
      | 2  | 다른고객         | 010-2222-2222 | B1         | 2025-01-15 | 2025-01-16 | CONFIRMED | DEF456           |
    When 사용자가 예약 ID "2"를 다음 정보로 수정한다:
      | confirmationCode | customerName | phoneNumber   | siteNumber | startDate  | endDate    |
      | ABC123           | 기존고객         | 010-1111-1111 | A1         | 2025-01-05 | 2025-01-07 |
    Then 예약 수정이 실패한다
    And "확인 코드가 일치하지 않습니다"라는 메시지가 표시된다

  Scenario: 수정 후에도 연박 예약이 가능하다
    When 사용자가 예약 ID "1"을 다음 정보로 수정한다:
      | confirmationCode | customerName | phoneNumber   | siteNumber | startDate  | endDate    |
      | ABC123           | 기존고객         | 010-1111-1111 | B1         | 2025-01-20 | 2025-01-25 |
    Then 예약 수정이 성공한다
    And 예약 정보가 수정된다
