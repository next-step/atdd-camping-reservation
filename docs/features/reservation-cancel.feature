@reservation @cancel
Feature: 예약 취소 - DELETE /api/reservations/{id}
  고객이 기존에 생성한 예약을 취소하는 기능

  @positive @state
  Scenario: 유효한 예약 ID와 확인 코드로 예약 취소
    Given 고객 "김철수"의 예약 ID "123"과 확인 코드 "ABCDE1"을 가진 예약이 존재한다
    When 고객 "김철수"가 예약 ID "123"을 확인 코드 "ABCDE1"로 취소 요청하면
    Then 예약이 성공적으로 취소되고 "200" 상태 코드와 함께 "예약이 취소되었습니다." 메시지를 반환한다
    And 예약 ID "123"의 상태는 "CANCELLED"가 된다

  @positive @state
  Scenario: 예약 당일 취소 시 CANCELLED_SAME_DAY로 상태 변경
    Given 고객 "박영희"의 예약 ID "456"은 예약 시작일이 오늘이고 확인 코드 "FGHIJ2"를 가진 예약이 존재한다
    When 고객 "박영희"가 예약 ID "456"을 확인 코드 "FGHIJ2"로 취소 요청하면
    Then 예약이 성공적으로 취소되고 "200" 상태 코드와 함께 "예약이 취소되었습니다." 메시지를 반환한다
    And 예약 ID "456"의 상태는 "CANCELLED_SAME_DAY"가 된다

  @validation @negative
  Scenario Outline: 유효하지 않은 정보로 예약 취소 시도
    Given 고객 "이민준"의 예약 ID "789"와 확인 코드 "KLMNO3"을 가진 예약이 존재한다
    When 고객 "이민준"이 <id>와 <confirmation_code>로 예약을 취소하면
    Then 예약 취소가 실패하고 "<status>" 상태 코드와 함께 적절한 에러 메시지를 반환한다

    Examples:
      | id     | confirmation_code | description           | status |
      | 9999   | KLMNO3            | 존재하지 않는 예약 ID     | 404    |
      | 789    | WRONGCODE         | 잘못된 확인 코드        | 400    |
      | 789    | ""                | 빈 확인 코드          | 400    |

  @state @negative
  Scenario: 이미 취소된 예약 재취소 시도
    Given 고객 "최수빈"의 예약 ID "101"이 이미 "CANCELLED" 상태이며 확인 코드 "PQRSTU"를 가진 예약이 존재한다
    When 고객 "최수빈"이 예약 ID "101"을 확인 코드 "PQRSTU"로 취소 요청하면
    Then 예약 취소가 실패하고 "400" 상태 코드와 함께 적절한 에러 메시지를 반환한다

  @security @negative
  Scenario: 타인의 예약 ID와 임의의 확인 코드로 취소 시도
    Given 고객 "정우성"의 예약 ID "202"와 확인 코드 "UVWXYZ"을 가진 예약이 존재한다
    When 악의적인 사용자가 예약 ID "202"를 임의의 확인 코드 "000000"로 취소 요청하면
    Then 예약 취소가 실패하고 "400" 상태 코드와 함께 적절한 에러 메시지를 반환한다
