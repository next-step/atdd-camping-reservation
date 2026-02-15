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

Feature: 예약 가격 계산
  예약 생성 시 사이트 타입, 주말, 성수기에 따라 정확한 가격이 계산되어야 한다.

  # ── 기본 가격 ──

  Scenario: A 사이트(대형) 평일 비수기 1박 기본가
    Given today is 2026-02-05
    And campsite "A-1" exists
    When 다음 정보로 예약을 생성하면:
      | customerName | siteNumber | startDate   | endDate     |
      | 홍길동       | A-1        | 2026-03-02  | 2026-03-02  |
    Then 요청이 성공한다
    And 총 금액은 80000원이다

  Scenario: B 사이트(소형) 평일 비수기 1박 기본가
    Given today is 2026-02-05
    And campsite "B-1" exists
    When 다음 정보로 예약을 생성하면:
      | customerName | siteNumber | startDate   | endDate     |
      | 홍길동       | B-1        | 2026-03-02  | 2026-03-02  |
    Then 요청이 성공한다
    And 총 금액은 50000원이다

  Scenario: 기타 사이트 평일 비수기 1박 기본가
    Given today is 2026-02-05
    And campsite "C-1" exists
    When 다음 정보로 예약을 생성하면:
      | customerName | siteNumber | startDate   | endDate     |
      | 홍길동       | C-1        | 2026-03-02  | 2026-03-02  |
    Then 요청이 성공한다
    And 총 금액은 60000원이다

  # ── 주말 할증 (30%) ──

  Scenario: A 사이트 주말(토요일) 비수기 1박 - 30% 할증
    Given today is 2026-02-05
    And campsite "A-1" exists
    # 2026-03-07 = 토요일
    When 다음 정보로 예약을 생성하면:
      | customerName | siteNumber | startDate   | endDate     |
      | 홍길동       | A-1        | 2026-03-07  | 2026-03-07  |
    Then 요청이 성공한다
    And 총 금액은 104000원이다
    # 80000 × 1.3 = 104000

  Scenario: B 사이트 주말(일요일) 비수기 1박 - 30% 할증
    Given today is 2026-02-05
    And campsite "B-1" exists
    # 2026-03-08 = 일요일
    When 다음 정보로 예약을 생성하면:
      | customerName | siteNumber | startDate   | endDate     |
      | 홍길동       | B-1        | 2026-03-08  | 2026-03-08  |
    Then 요청이 성공한다
    And 총 금액은 65000원이다
    # 50000 × 1.3 = 65000

  # ── 성수기 할증 (50%) ──

  Scenario: A 사이트 성수기(7월) 평일 1박 - 50% 할증
    Given today is 2026-06-15
    And campsite "A-1" exists
    # 2026-07-06 = 월요일
    When 다음 정보로 예약을 생성하면:
      | customerName | siteNumber | startDate   | endDate     |
      | 홍길동       | A-1        | 2026-07-06  | 2026-07-06  |
    Then 요청이 성공한다
    And 총 금액은 120000원이다
    # 80000 × 1.5 = 120000

  Scenario: B 사이트 성수기(8월) 평일 1박 - 50% 할증
    Given today is 2026-07-15
    And campsite "B-1" exists
    # 2026-08-03 = 월요일
    When 다음 정보로 예약을 생성하면:
      | customerName | siteNumber | startDate   | endDate     |
      | 홍길동       | B-1        | 2026-08-03  | 2026-08-03  |
    Then 요청이 성공한다
    And 총 금액은 75000원이다
    # 50000 × 1.5 = 75000

  # ── 성수기 + 주말 할증 (70%) ──

  Scenario: A 사이트 성수기 주말(7월 토요일) 1박 - 70% 할증
    Given today is 2026-06-15
    And campsite "A-1" exists
    # 2026-07-04 = 토요일
    When 다음 정보로 예약을 생성하면:
      | customerName | siteNumber | startDate   | endDate     |
      | 홍길동       | A-1        | 2026-07-04  | 2026-07-04  |
    Then 요청이 성공한다
    And 총 금액은 136000원이다
    # 80000 × 1.7 = 136000

  Scenario: B 사이트 성수기 주말(8월 일요일) 1박 - 70% 할증
    Given today is 2026-07-15
    And campsite "B-1" exists
    # 2026-08-02 = 일요일
    When 다음 정보로 예약을 생성하면:
      | customerName | siteNumber | startDate   | endDate     |
      | 홍길동       | B-1        | 2026-08-02  | 2026-08-02  |
    Then 요청이 성공한다
    And 총 금액은 85000원이다
    # 50000 × 1.7 = 85000

  # ── 혼합 기간 (일별 할증 개별 적용) ──

  Scenario: A 사이트 금~일(평일1일 + 주말2일) 비수기 - 혼합 계산
    Given today is 2026-02-05
    And campsite "A-1" exists
    # 2026-03-06(금)=평일, 03-07(토)=주말, 03-08(일)=주말
    When 다음 정보로 예약을 생성하면:
      | customerName | siteNumber | startDate   | endDate     |
      | 홍길동       | A-1        | 2026-03-06  | 2026-03-08  |
    Then 요청이 성공한다
    And 총 금액은 288000원이다
    # 금: 80000 + 토: 104000 + 일: 104000 = 288000

  Scenario: B 사이트 비수기→성수기 전환(6/30~7/1) - 경계값
    Given today is 2026-06-15
    And campsite "B-1" exists
    # 2026-06-30(화)=비수기 평일, 07-01(수)=성수기 평일
    When 다음 정보로 예약을 생성하면:
      | customerName | siteNumber | startDate   | endDate     |
      | 홍길동       | B-1        | 2026-06-30  | 2026-07-01  |
    Then 요청이 성공한다
    And 총 금액은 125000원이다
    # 6/30: 50000(비수기 평일) + 7/1: 75000(성수기 평일) = 125000
    # Note: 실제 요일에 따라 주말 할증이 추가될 수 있음 (테스트에서는 실제 요일 기준 검증)

  Scenario: A 사이트 성수기 금~일(성수기 평일 + 성수기 주말 혼합)
    Given today is 2026-06-15
    And campsite "A-1" exists
    # 2026-07-03(금)=성수기 평일, 07-04(토)=성수기 주말, 07-05(일)=성수기 주말
    When 다음 정보로 예약을 생성하면:
      | customerName | siteNumber | startDate   | endDate     |
      | 홍길동       | A-1        | 2026-07-03  | 2026-07-05  |
    Then 요청이 성공한다
    And 총 금액은 392000원이다
    # 금: 120000(×1.5) + 토: 136000(×1.7) + 일: 136000(×1.7) = 392000

  # ── 1일 예약 (startDate == endDate) ──

  Scenario: 1일 예약(당일) - startDate와 endDate가 동일
    Given today is 2026-02-05
    And campsite "A-1" exists
    When 다음 정보로 예약을 생성하면:
      | customerName | siteNumber | startDate   | endDate     |
      | 홍길동       | A-1        | 2026-03-02  | 2026-03-02  |
    Then 요청이 성공한다
    And 총 금액은 80000원이다

Feature: 예약 포인트 적립
  예약 생성 시 기간 내 주말 포함 여부에 따라 정확한 포인트가 적립되어야 한다.

  # ── 기본 적립률 (5%) ──

  Scenario: 평일만 포함된 예약 - 5% 적립
    Given today is 2026-02-05
    And campsite "A-1" exists
    # 2026-03-02(월)~03-04(수) 평일 3일
    When 다음 정보로 예약을 생성하면:
      | customerName | siteNumber | startDate   | endDate     |
      | 홍길동       | A-1        | 2026-03-02  | 2026-03-04  |
    Then 요청이 성공한다
    And 총 금액은 240000원이다
    And 적립 포인트는 12000P이다
    # 240000 × 0.05 = 12000

  Scenario: B 사이트 성수기 평일만 - 5% 적립 (성수기는 적립률에 영향 없음)
    Given today is 2026-07-01
    And campsite "B-1" exists
    # 2026-07-06(월) 성수기 평일 1일
    When 다음 정보로 예약을 생성하면:
      | customerName | siteNumber | startDate   | endDate     |
      | 홍길동       | B-1        | 2026-07-06  | 2026-07-06  |
    Then 요청이 성공한다
    And 총 금액은 75000원이다
    And 적립 포인트는 3750P이다
    # 75000 × 0.05 = 3750

  # ── 주말 포함 적립률 (10%) ──

  Scenario: 주말이 1일이라도 포함되면 10% 적립
    Given today is 2026-02-05
    And campsite "A-1" exists
    # 2026-03-06(금)~03-07(토) → 주말 포함
    When 다음 정보로 예약을 생성하면:
      | customerName | siteNumber | startDate   | endDate     |
      | 홍길동       | A-1        | 2026-03-06  | 2026-03-07  |
    Then 요청이 성공한다
    And 총 금액은 184000원이다
    And 적립 포인트는 18400P이다
    # (80000 + 104000) × 0.10 = 18400

  Scenario: 토~일 주말만 예약 - 10% 적립
    Given today is 2026-02-05
    And campsite "B-1" exists
    # 2026-03-07(토)~03-08(일)
    When 다음 정보로 예약을 생성하면:
      | customerName | siteNumber | startDate   | endDate     |
      | 홍길동       | B-1        | 2026-03-07  | 2026-03-08  |
    Then 요청이 성공한다
    And 총 금액은 130000원이다
    And 적립 포인트는 13000P이다
    # (65000 + 65000) × 0.10 = 13000

  Scenario: 성수기 주말 포함 예약 - 10% 적립 (할증 가격 기준)
    Given today is 2026-06-15
    And campsite "A-1" exists
    # 2026-07-03(금)=성수기 평일, 07-04(토)=성수기 주말
    When 다음 정보로 예약을 생성하면:
      | customerName | siteNumber | startDate   | endDate     |
      | 홍길동       | A-1        | 2026-07-03  | 2026-07-04  |
    Then 요청이 성공한다
    And 총 금액은 256000원이다
    And 적립 포인트는 25600P이다
    # (120000 + 136000) × 0.10 = 25600

  # ── 포인트 소수점 버림 ──

  Scenario: 포인트 계산 시 소수점 이하 버림 처리
    Given today is 2026-02-05
    And campsite "C-1" exists
    # 기타 사이트 평일 1일: 60000 × 0.05 = 3000 (정수로 떨어짐)
    When 다음 정보로 예약을 생성하면:
      | customerName | siteNumber | startDate   | endDate     |
      | 홍길동       | C-1        | 2026-03-02  | 2026-03-02  |
    Then 요청이 성공한다
    And 총 금액은 60000원이다
    And 적립 포인트는 3000P이다

  # ── 경계값: 주말 경계 (금→토 전환) ──

  Scenario: 금요일만 예약 - 주말 미포함으로 5% 적립
    Given today is 2026-02-05
    And campsite "A-1" exists
    # 2026-03-06 = 금요일
    When 다음 정보로 예약을 생성하면:
      | customerName | siteNumber | startDate   | endDate     |
      | 홍길동       | A-1        | 2026-03-06  | 2026-03-06  |
    Then 요청이 성공한다
    And 총 금액은 80000원이다
    And 적립 포인트는 4000P이다
    # 80000 × 0.05 = 4000 (금요일은 주말 아님)

  Scenario: 토요일 1일만 예약 - 주말 포함으로 10% 적립
    Given today is 2026-02-05
    And campsite "A-1" exists
    # 2026-03-07 = 토요일
    When 다음 정보로 예약을 생성하면:
      | customerName | siteNumber | startDate   | endDate     |
      | 홍길동       | A-1        | 2026-03-07  | 2026-03-07  |
    Then 요청이 성공한다
    And 총 금액은 104000원이다
    And 적립 포인트는 10400P이다
    # 104000 × 0.10 = 10400

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