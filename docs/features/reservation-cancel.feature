# language: ko

Feature: 예약 취소
  고객이 예약을 취소할 수 있다

  Background:
    Given 고객 "홍길동"이 "A1" 사이트를 예약하고 확인코드 "ABC123"을 받았다

  Scenario: 올바른 확인 코드로 예약을 취소한다
    When 확인코드 "ABC123"으로 예약 취소를 요청한다
    Then 예약이 취소된다

  Scenario: 체크인 당일 취소 시 당일 취소로 처리된다
    Given 예약 시작일이 오늘이다
    When 확인코드 "ABC123"으로 예약 취소를 요청한다
    Then 예약 상태가 "CANCELLED_SAME_DAY"로 변경된다

  Scenario: 잘못된 확인 코드로 취소하면 실패한다
    When 확인코드 "WRONG1"으로 예약 취소를 요청한다
    Then 취소가 실패한다
