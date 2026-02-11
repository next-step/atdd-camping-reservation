Feature: 예약 생성
  예약을 생성할 수 있어야 한다.

  Scenario: 정상 - 유효한 예약 생성
    Given today is 2026-02-05
    And campsite "A-1" exists
    And no reservation exists for site "A-1" between "2026-02-10" and "2026-02-12"
    When 다음 정보로 예약을 생성하면:
      | customerName | phoneNumber   | siteNumber | startDate   | endDate     |
      | 홍길동       | 010-1234-5678 | A-1        | 2026-02-10  | 2026-02-12  |
    Then 요청이 성공한다
    And 응답에는 6자리 영숫자로 된 확인 코드가 포함된다

  Scenario: 실패 - 기간 중복
    Given today is 2026-02-05
    And campsite "A-1" has an existing reservation from "2026-02-10" to "2026-02-12"
    When 다음 정보로 예약을 생성하면:
      | customerName | phoneNumber   | siteNumber | startDate   | endDate     |
      | 김철수       | 010-2222-3333 | A-1        | 2026-02-11  | 2026-02-13  |
    Then 오류가 발생한다

  Scenario: 실패 - 종료일이 시작일보다 이전
    Given today is 2026-02-05
    And campsite "A-1" exists
    When 다음 정보로 예약을 생성하면:
      | customerName | phoneNumber   | siteNumber | startDate   | endDate     |
      | 이영희       | 010-4444-5555 | A-1        | 2026-02-12  | 2026-02-10  |
    Then 오류가 발생한다

Feature: 연박 예약
  연박 예약 시 전체 기간이 하나의 예약으로 처리되어야 한다.

  Scenario: 정상 - 3박 예약
    Given today is 2026-02-05
    And campsite "B-1" exists
    And no reservation exists for site "B-1" between "2026-02-14" and "2026-02-16"
    When 다음 정보로 예약을 생성하면:
      | customerName | phoneNumber   | siteNumber | startDate   | endDate     |
      | 박민수       | 010-6666-7777 | B-1        | 2026-02-14  | 2026-02-16  |
    Then 요청이 성공한다

  Scenario: 실패 - 기간 중 일부 날짜에 예약 존재
    Given today is 2026-02-05
    And campsite "B-1" has an existing reservation from "2026-02-15" to "2026-02-15"
    When 다음 정보로 예약을 생성하면:
      | customerName | phoneNumber   | siteNumber | startDate   | endDate     |
      | 정수진       | 010-8888-9999 | B-1        | 2026-02-14  | 2026-02-16  |
    Then 오류가 발생한다

Feature: 예약 조회(단건)
  예약 ID로 상세를 조회할 수 있어야 한다.

  Scenario: 정상 - 예약 ID로 조회
    Given a reservation exists with id 1
    When ID 1번인 예약을 조회하면
    Then 요청이 성공한다
    And 응답에는 예약 ID 1이 포함된다

  Scenario: 실패 - 존재하지 않는 예약 ID
    When ID 99999번인 예약을 조회하면
    Then 오류가 발생한다

Feature: 예약 조회(목록)
  날짜/이름/내 예약 조회를 지원해야 한다.

  Scenario: 정상 - 날짜로 예약 조회
    Given a reservation exists for site "A-1" from "2026-02-10" to "2026-02-12"
    When "2026-02-11" 날짜의 예약을 조회하면
    Then 요청이 성공한다
    And 응답 목록에는 해당 예약이 포함된다

  Scenario: 정상 - 이름으로 예약 조회
    Given a reservation exists for customer "홍길동"
    When "홍길동" 고객의 예약을 조회하면
    Then 요청이 성공한다
    And 응답 목록에는 "홍길동" 고객의 예약이 포함된다

  Scenario: 정상 - 이름+전화번호로 내 예약 조회
    Given a reservation exists for customer "홍길동" with phone "010-1234-5678"
    When 이름 "홍길동", 전화번호 "010-1234-5678"로 내 예약을 조회하면
    Then 요청이 성공한다
    And 응답 목록에는 "홍길동" 고객의 예약이 포함된다

  Scenario: 실패 - 잘못된 날짜 포맷
    When "2026-02-30" 이라는 잘못된 날짜로 예약을 조회하면
    Then 오류가 발생한다

Feature: 예약 수정
  예약을 수정할 수 있어야 한다.

  Scenario: 정상 - 확인 코드로 예약 수정
    Given a reservation exists with id 1 and confirmation code "ABC123"
    When 확인 코드 "ABC123"으로 예약 1번을 다음 정보로 수정하면:
      | customerName | startDate   | endDate     |
      | 홍길동       | 2026-02-20  | 2026-02-22  |
    Then 요청이 성공한다
    And 응답에는 수정된 날짜가 반영되어 있다

  Scenario: 실패 - 확인 코드 불일치
    Given a reservation exists with id 1 and confirmation code "ABC123"
    When 확인 코드 "WRONG"으로 예약 1번을 다음 정보로 수정하면:
      | customerName |
      | 홍길동       |
    Then 오류가 발생한다

  Scenario: 실패 - 종료일이 시작일보다 이전
    Given a reservation exists with id 1 and confirmation code "ABC123"
    When 확인 코드 "ABC123"으로 예약 1번을 다음 정보로 수정하면:
      | startDate   | endDate     |
      | 2026-02-12  | 2026-02-10  |
    Then 오류가 발생한다

Feature: 예약 취소
  확인 코드로 예약을 취소할 수 있어야 한다.

  Scenario: 정상 - 확인 코드로 취소
    Given a reservation exists with id 1 and confirmation code "ABC123"
    When 확인 코드 "ABC123"으로 예약 1번을 취소하면
    Then 요청이 성공한다
    And 응답에는 "예약이 취소되었습니다." 메시지가 포함된다

  Scenario: 실패 - 확인 코드 불일치
    Given a reservation exists with id 1 and confirmation code "ABC123"
    When 확인 코드 "WRONG"으로 예약 1번을 취소하면
    Then 오류가 발생한다

Feature: 사이트 조회
  전체 사이트 목록을 조회할 수 있어야 한다.

  Scenario: 정상 - 전체 사이트 조회
    Given campsites exist
    When 전체 사이트 목록을 조회하면
    Then 요청이 성공한다
    And 응답 목록에는 사이트 정보가 포함된다

  Scenario: 예외 - 사이트가 없으면 빈 목록
    Given no campsites exist
    When 전체 사이트 목록을 조회하면
    Then 요청이 성공한다
    And 응답 목록은 비어있다

Feature: 사이트 상세
  사이트 ID로 상세 정보를 조회할 수 있어야 한다.

  Scenario: 정상 - 사이트 상세 조회
    Given a campsite exists with id 1
    When ID 1번인 사이트를 조회하면
    Then 요청이 성공한다
    And 응답에는 캠핑장 ID 1이 포함된다

  Scenario: 실패 - 존재하지 않는 사이트 ID
    When ID 99999번인 사이트를 조회하면
    Then 오류가 발생한다

Feature: 사이트 가용성 조회(단건)
  사이트 번호와 날짜로 가용 여부를 조회한다.

  Scenario: 정상 - 사이트 가용 여부 반환
    Given campsite "A-1" exists
    When "A-1" 사이트의 "2026-02-10" 날짜 가용성을 확인하면
    Then 요청이 성공한다
    And 응답에는 "available" 상태가 포함된다

  Scenario: 실패 - 과거 날짜 조회
    Given today is 2026-02-05
    And campsite "A-1" exists
    When "A-1" 사이트의 "2026-02-01" (과거) 날짜 가용성을 확인하면
    Then 오류가 발생한다

Feature: 가용성 조회(단일 날짜)
  특정 날짜에 예약 가능한 사이트 목록을 조회한다.

  Scenario: 정상 - 특정 날짜 가용 사이트 목록
    Given a reservation exists for site "A-1" with reservationDate "2026-02-10"
    When "2026-02-10" 날짜에 예약 가능한 사이트를 조회하면
    Then 요청이 성공한다
    And 응답 목록에는 "A-1" 사이트가 포함되지 않는다

  Scenario: 실패 - date 파라미터 누락
    When 날짜를 지정하지 않고 예약 가능한 사이트를 조회하면
    Then 오류가 발생한다

Feature: 가용성 조회(기간)
  시작일/종료일 기간 가용 사이트를 조회한다.

  Scenario: 정상 - 기간 가용 사이트 + size 필터
    Given campsites exist
    And no reservation exists for site "A-1" on "2026-02-20" and "2026-02-22"
    When "2026-02-20"부터 "2026-02-22"까지 "대형" 사이즈로 예약 가능한 사이트를 조회하면
    Then 요청이 성공한다
    And 응답 목록에는 "대형" 사이즈의 사이트만 포함된다

  Scenario: 실패 - 종료일이 시작일보다 이전
    When 시작일이 "2026-02-22", 종료일이 "2026-02-20"으로 예약 가능한 사이트를 조회하면
    Then 오류가 발생한다

Feature: 월별 예약 현황
  특정 사이트의 월별 예약 상태를 조회한다.

  Scenario: 정상 - 월별 캘린더 조회
    Given a campsite exists with id 1
    When 2026년 2월의 1번 사이트 예약 캘린더를 조회하면
    Then 요청이 성공한다
    And 응답에는 일별 상태 정보가 포함된다
    And 예약된 날짜는 "available=false"로 표시된다

  Scenario: 실패 - 존재하지 않는 사이트 ID
    When 2026년 2월의 99999번 사이트 예약 캘린더를 조회하면
    Then 오류가 발생한다