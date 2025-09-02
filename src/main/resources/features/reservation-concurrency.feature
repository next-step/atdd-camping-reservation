Feature: 동시성 제어
  동시에 같은 사이트에 대한 예약 요청이 들어와도 단 하나의 예약만 성공하여 데이터 일관성이 보장되어야 한다

  Background:
Given 사이트가 존재한다:
      | siteNumber | description | maxPeople |
      | A1         | 대형          | 6         |

  Scenario: 동일한 사이트와 날짜에 동시 예약 요청이 들어온다
    Given 오늘이 "2025-01-01"이다
    When 두 사용자가 동시에 같은 사이트에 예약을 요청한다:
      | customerName | phoneNumber   | siteNumber | startDate  | endDate    |
      | 테스터1         | 010-1111-1111 | A1         | 2025-01-02 | 2025-01-02 |
      | 테스터2         | 010-2222-2222 | A1         | 2025-01-02 | 2025-01-02 |
    Then 하나의 예약만 성공한다
    And 다른 하나의 예약은 실패한다
