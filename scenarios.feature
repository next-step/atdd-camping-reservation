Feature: 초록 캠핑장 핵심 인수 시나리오
  고객은 정책에 맞게 예약을 생성/조회/취소할 수 있고,
  시스템은 중복과 동시성을 올바르게 제어하며,
  연박 가용성 검색은 기간 전체를 정확히 반영해야 한다.

  Background:
    Given 사이트 "A-1" ~ "A-50"이 등록되어 있다

  # 1) 예약 생성
  Scenario: 30일 이내 유효 기간으로 예약을 생성한다
    Given A-1 사이트가 비어있다
    When 고객이 다음 정보로 POST /api/reservations 요청을 보낸다
      | customerName | phoneNumber   | siteNumber | startDate   | endDate     |
      | 홍길동          | 010-1111-2222 | A-1        | 2025-09-20  | 2025-09-21  |
    Then 응답 상태코드는 201 이다
    And 응답 본문에는 예약 id 와 6자리 confirmationCode 가 존재한다

  Scenario: 과거 날짜로는 예약할 수 없다
    Given A-1 사이트가 비어있다
    When 고객이 startDate=2024-12-01, endDate=2024-12-02 로 예약 생성 요청을 보낸다
    Then 응답 상태코드는 400 이고 메시지에 "과거 날짜" 문구가 포함된다

  Scenario: 30일 제한을 초과하면 예약이 거부된다
    Given A-1 사이트가 비어있다
    When 고객이 startDate=2025-10-10, endDate=2025-10-12 로 예약 생성 요청을 보낸다
    Then 응답 상태코드는 400 이고 메시지에 "30일 이내" 문구가 포함된다

  Scenario: 전화번호는 필수다
    Given A-1 사이트가 비어있다
    When 고객이 phoneNumber 를 누락하여 예약 생성 요청을 보낸다
    Then 응답 상태코드는 400 이고 메시지에 "전화번호" 문구가 포함된다

  # 2) 중복/동시성
  Scenario: 동일 기간/사이트 중복 예약은 거부된다
    Given A-2 사이트에 2025-09-25 ~ 2025-09-26 기간의 예약이 존재한다
    When 다른 고객이 동일 기간/사이트로 예약 생성 요청을 보낸다
    Then 응답 상태코드는 409 이고 메시지에 "이미 예약" 문구가 포함된다

  Scenario: 동시에 같은 사이트를 예약하면 하나만 성공한다
    Given A-3 사이트가 2025-09-27 ~ 2025-09-28 기간 비어있다
    When 5명이 동일 바디로 동시 POST /api/reservations 를 호출한다
    Then 성공 응답은 정확히 1건(201)이고 나머지는 409 로 실패한다

  Scenario: 취소된 예약은 즉시 재예약 가능하다
    Given A-4 사이트 2025-09-29 ~ 2025-09-30 예약이 존재하고 confirmationCode 를 보유한다
    And 고객이 DELETE /api/reservations/{id}?confirmationCode=XXXX 로 취소한다
    When 동일 기간/사이트로 다시 예약 생성 요청을 보낸다
    Then 응답 상태코드는 201 이다

  # 3) 기간 가용성(연박)
  Scenario: 기간 전체가 가능한 사이트만 반환한다
    Given A-5 는 2025-09-20 은 예약됨, 2025-09-21 은 비어있다
    When GET /api/sites/search?startDate=2025-09-20&endDate=2025-09-21 를 호출한다
    Then 응답 목록에는 A-5 가 포함되지 않는다

  Scenario: 대형 사이트만 필터링하여 조회한다
    Given 최소 1개의 대형 사이트가 등록되어 있다
    When GET /api/sites/search?startDate=2025-09-22&endDate=2025-09-23&size=대형 를 호출한다
    Then 응답의 모든 siteNumber 는 "A-" 로 시작한다
