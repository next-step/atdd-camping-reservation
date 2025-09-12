Feature: 캠핑장 예약 생성
  고객이 유효한 정보로 캠핑장 예약을 생성할 수 있다

  Background:
    Given '초록 캠핑장'에 다음 캠핑장들이 존재한다:
      | siteNumber | description   | maxPeople |
      | A-1        | 대형 캠핑장 1 | 6         |
      | A-2        | 대형 캠핑장 2 | 6         |
      | B-1        | 소형 캠핑장 1 | 4         |

  Scenario: 유효한 정보로 예약 성공
    When 유효한 정보로 예약을 시도한다
    Then 예약이 성공적으로 생성된다
    And 6자리 영숫자 확인 코드가 발급된다
    And 예약 상태가 "CONFIRMED"로 설정된다

  Scenario: 필수 정보 누락으로 예약 실패
    When 불충분한 정보만 가지고 예약을 시도한다.
    Then 예약이 실패한다
    And "예약자 이름을 입력해주세요" 오류 메시지가 반환된다

  Scenario: 30일 제한 규칙을 한참 넘는 위반으로 예약 실패
    Given 오늘 날짜가 "2025-01-01"이다
    And 고객이 "A-1" 캠핑장을 "2025-02-15"부터 "2025-02-17"까지 예약하려고 한다
    When 예약을 시도한다
    Then 예약이 실패한다
    And "오늘로부터 30일 이내로 예약할 수 있습니다" 오류 메시지가 반환된다

  Scenario: 30일 제한 규칙을 애매하게 넘는 위반으로 예약 실패
    Given 오늘 날짜가 "2025-01-01"이다
    And 고객이 "A-1" 캠핑장을 "2025-01-31"부터 "2025-02-05"까지 예약하려고 한다
    When 예약을 시도한다
    Then 예약이 실패한다
    And "오늘로부터 30일 이내로 예약할 수 있습니다" 오류 메시지가 반환된다

  Scenario: 종료일이 시작일보다 이전인 경우 예약 실패
    Given 오늘 날짜가 "2025-01-01"이다
    And 고객이 "A-1" 캠핑장을 "2025-01-16"부터 "2025-01-15"까지 예약하려고 한다
    When 예약을 시도한다
    Then 예약이 실패한다
    And "종료일이 시작일보다 이전일 수 없습니다" 오류 메시지가 반환된다

  Scenario: 예약 일자가 확실하게 겹치면 예약 실패
    Given 오늘 날짜가 "2025-01-01"이다
    And "A-1" 캠핑장에 "2025-01-15"부터 "2025-01-17"까지 예약이 존재한다
    And 다른 고객이 "A-1" 캠핑장을 "2025-01-16"부터 "2025-01-18"까지 예약하려고 한다
    When 예약을 시도한다
    Then 예약이 실패한다
    And "해당 기간에 이미 예약이 존재합니다" 오류 메시지가 반환된다

  Scenario: 예약 일자가 애매하게 겹치면 예약 실패
    Given 오늘 날짜가 "2025-01-01"이다
    And "A-1" 캠핑장에 "2025-01-15"부터 "2025-01-17"까지 예약이 존재한다
    And 다른 고객이 "A-1" 캠핑장을 "2025-01-17"부터 "2025-01-18"까지 예약하려고 한다
    When 예약을 시도한다
    Then 예약이 실패한다
    And "해당 기간에 이미 예약이 존재합니다" 오류 메시지가 반환된다

  Scenario: 동시 예약 요청 시 하나만 성공
    Given 오늘 날짜가 "2025-01-01"이다
    And 두 고객의 예약 정보가 주어진다:
      | customerName | phoneNumber   | siteNumber | startDate  | endDate    |
      | 김철수       | 010-1234-5678 | A-1        | 2025-01-15 | 2025-01-17 |
      | 이영희       | 010-9876-5432 | A-1        | 2025-01-15 | 2025-01-17 |
    When 두 고객이 동시에 같은 캠핑장에 예약을 요청한다
    Then 하나의 예약만 성공한다
    And 다른 예약은 "해당 기간에 이미 예약이 존재합니다" 오류로 실패한다


Feature: 캠핑장 예약 수정
  고객이 확인 코드를 통해 기존 예약 정보를 수정할 수 있다

  Scenario: 예약 날짜 수정 성공
    Given 오늘 날짜가 "2025-01-01"이다
    And 다음 예약이 존재한다:
      | id | customerName | siteNumber | startDate  | endDate    | confirmationCode |
      | 1  | 김철수       | A-1        | 2025-01-15 | 2025-01-17 | ABC123           |
    And 수정하려는 예약 일자는 다음과 같다:
      | startDate  | endDate    |
      | 2025-01-20 | 2025-01-22 |
    When 고객이 예약 "1"을 확인 코드 "ABC123"으로 예약을 수정한다
    Then 예약 수정이 성공한다
    And 예약의 시작일이 "2025-01-20"으로 변경된다
    And 예약의 종료일이 "2025-01-22"로 변경된다

  Scenario: 30일 제한 규칙에 벗어난 확실한 예약 날짜로 수정 실패
    Given 오늘 날짜가 "2025-01-01"이다
    And 다음 예약이 존재한다:
      | id | customerName | siteNumber | startDate  | endDate    | confirmationCode |
      | 1  | 김철수       | A-1        | 2025-01-15 | 2025-01-17 | ABC123           |
    And 수정하려는 예약 일자는 다음과 같다:
      | startDate  | endDate    |
      | 2025-02-15 | 2025-02-17 |
    When 고객이 예약 "1"을 확인 코드 "ABC123"으로 예약을 수정한다
    Then 예약 수정이 성공한다
    And 예약의 시작일이 "2025-01-20"으로 변경된다
    And 예약의 종료일이 "2025-01-22"로 변경된다

  Scenario: 30일 제한 규칙에 벗어난 애매한 예약 날짜로 수정 실패
    Given 오늘 날짜가 "2025-01-01"이다
    And 다음 예약이 존재한다:
      | id | customerName | siteNumber | startDate  | endDate    | confirmationCode |
      | 1  | 김철수       | A-1        | 2025-01-15 | 2025-01-17 | ABC123           |
    And 수정하려는 예약 일자는 다음과 같다:
      | startDate  | endDate    |
      | 2025-01-31 | 2025-02-05 |
    When 고객이 예약 "1"을 확인 코드 "ABC123"으로 예약을 수정한다
    Then 예약 수정이 성공한다0
    And 예약의 시작일이 "2025-01-20"으로 변경된다
    And 예약의 종료일이 "2025-01-22"로 변경된다

  Scenario: 중복 예약이 발생하는 확실한 날짜로 수정 실패
    Given 다음 예약들이 존재한다:
      | id | customerName | siteNumber | startDate  | endDate    | confirmationCode |
      | 1  | 김철수       | A-1        | 2025-01-15 | 2025-01-17 | ABC123           |
      | 2  | 이영희       | A-1        | 2025-01-20 | 2025-01-22 | DEF456           |
    When 고객이 예약 "1"을 확인 코드 "ABC123"으로 다음과 같이 수정 요청한다:
      | startDate  | endDate    |
      | 2025-01-21 | 2025-01-23 |
    Then 예약 수정이 실패한다
    And "해당 기간에 이미 예약이 존재합니다" 오류 메시지가 반환된다

  Scenario: 중복 예약이 발생하는 애매한 날짜로 수정 실패
    Given 다음 예약들이 존재한다:
      | id | customerName | siteNumber | startDate  | endDate    | confirmationCode |
      | 1  | 김철수       | A-1        | 2025-01-15 | 2025-01-17 | ABC123           |
      | 2  | 이영희       | A-1        | 2025-01-20 | 2025-01-22 | DEF456           |
    When 고객이 예약 "1"을 확인 코드 "ABC123"으로 다음과 같이 수정 요청한다:
      | startDate  | endDate    |
      | 2025-01-22 | 2025-01-24 |
    Then 예약 수정이 실패한다
    And "해당 기간에 이미 예약이 존재합니다" 오류 메시지가 반환된다

  Scenario: 캠핑장 예약 변경 성공
    Given 다음 예약이 존재한다:
      | id | customerName | siteNumber | startDate  | endDate    | confirmationCode |
      | 1  | 김철수       | A-1        | 2025-01-15 | 2025-01-17 | ABC123           |
    And "A-2" 캠핑장이 "2025-01-15"부터 "2025-01-17"까지 예약 가능하다
    When 고객이 예약 "1"을 확인 코드 "ABC123"으로 다음과 같이 수정 요청한다:
      | siteNumber |
      | A-2        |
    Then 예약 수정이 성공한다
    And 예약의 캠핑장이 "A-2"로 변경된다

  Scenario: 존재하지 않는 캠핑장이므로 수정 실패
    Given 다음 예약이 존재한다:
      | id | customerName | siteNumber | startDate  | endDate    | confirmationCode |
      | 1  | 홍길동       | A-1        | 2025-01-15 | 2025-01-17 | ABC123           |
    When 고객이 예약 "1"을 확인 코드 "ABC123"으로 다음과 같이 수정 요청한다:
      | siteNumber |
      | X-999      |
    Then 예약 수정이 실패한다
    And "존재하지 않는 캠핑장입니다" 오류 메시지가 반환된다

  Scenario: 고객 정보 수정 성공
    Given 다음 예약이 존재한다:
      | id | customerName | phoneNumber   | siteNumber | startDate  | endDate    | confirmationCode |
      | 1  | 김철수       | 010-1234-5678 | A-1        | 2025-01-15 | 2025-01-17 | ABC123           |
    When 고객이 예약 "1"을 확인 코드 "ABC123"으로 다음과 같이 수정 요청한다:
      | customerName | phoneNumber   |
      | 김철수님     | 010-1111-2222 |
    Then 예약 수정이 성공한다
    And 예약의 고객명이 "김철수님"으로 변경된다
    And 예약의 전화번호가 "010-1111-2222"로 변경된다

  Scenario: 잘못된 확인 코드로 수정 실패
    Given 다음 예약이 존재한다:
      | id | customerName | siteNumber | startDate  | endDate    | confirmationCode |
      | 1  | 김철수       | A-1        | 2025-01-15 | 2025-01-17 | ABC123           |
    When 고객이 예약 "1"을 확인 코드 "WRONG123"으로 수정 요청한다
    Then 예약 수정이 실패한다
    And "확인 코드가 일치하지 않습니다" 오류 메시지가 반환된다


Feature: 캠핑장 예약 취소
  고객이 확인 코드를 통해 기존 예약을 취소할 수 있다

  Scenario: 사전 취소 성공
    Given 오늘 날짜가 "2025-01-01"이다
    And 다음 예약이 존재한다:
      | id | customerName | siteNumber | startDate  | endDate    | confirmationCode |
      | 1  | 김철수       | A-1        | 2025-01-15 | 2025-01-17 | ABC123           |
    When 고객이 예약 "1"을 확인 코드 "ABC123"으로 취소 요청한다
    Then 예약 취소가 성공한다
    And 예약 상태가 "CANCELED"로 변경된다
    And "예약이 취소되었습니다" 메시지가 반환된다

  Scenario: 당일 취소
    Given 오늘 날짜가 "2025-01-15"이다
    And 다음 예약이 존재한다:
      | id | customerName | siteNumber | startDate  | endDate    | confirmationCode |
      | 1  | 김철수       | A-1        | 2025-01-15 | 2025-01-17 | ABC123           |
    When 고객이 예약 "1"을 확인 코드 "ABC123"으로 취소 요청한다
    Then 예약 취소가 성공한다
    And 예약 상태가 "CANCELED_SAME_DAY"로 변경된다

  Scenario: 잘못된 확인 코드로 취소 실패
    Given 다음 예약이 존재한다:
      | id | customerName | siteNumber | startDate  | endDate    | confirmationCode |
      | 1  | 홍길동       | A-1        | 2025-01-15 | 2025-01-17 | ABC123           |
    When 고객이 예약 "1"을 확인 코드 "WRONG123"으로 취소 요청한다
    Then 예약 취소가 실패한다
    And "확인 코드가 일치하지 않습니다" 오류 메시지가 반환된다

  Scenario: 존재하지 않는 예약 취소 실패
    When 고객이 예약 "999"를 확인 코드 "ABC123"로 취소 요청한다
    Then 예약 취소가 실패한다
    And "예약을 찾을 수 없습니다" 오류 메시지가 반환된다