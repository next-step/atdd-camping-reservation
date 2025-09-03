Feature: 예약 생성
  Scenario: 사용자가 특정 사이트에 예약한다.
    Given 정상적인 예약 생성 Request 생성
    When 예약 생성 요청
    Then 예약 생성 성공

  Scenario: 사용자가 30일 이후 날짜로 예약한다.
    Given 예약 날짜가 30일 이후인 예약 생성 Request 생성
    When 예약 생성 요청
    Then 예약 생성 실패
    And 에러 메시지 확인 - "30일 이후 날짜로 예약은 불가능합니다."

  Scenario: 사용자가 과거 날짜로 예약한다.
    Given 예약 날짜가 과거인 예약 생성 Request 생성
    When 예약 생성 요청
    Then 예약 생성 실패
    And 에러 메시지 확인 - "과거 날짜로 예약은 불가능합니다."

  Scenario: 사용자가 종료일이 시작일 이전인 예약을 한다..
    Given 종료일이 시작일 이전인 예약 생성 Request 생성
    When 예약 생성 요청
    Then 예약 생성 실패
    And 에러 메시지 확인 - "종료일이 시작일 이전인 예약은 불가능합니다."

  Scenario: 사용자가 중복된 예약한다.
    Given 정상적인 예약 생성 Request 생성
    And 예약 생성 요청
    When 예약 생성 요청
    Then 예약 생성 실패
    And 에러 메시지 확인 - "이미 예약되어 있습니다."

  Scenario: 취소된 예약은 다시 예약할 수 있다.
    Given 정상적인 예약 생성 Request 생성
    And 예약 생성 요청
    And 예약 취소 요청
    When 예약 생성 요청
    Then 예약 생성 성공
