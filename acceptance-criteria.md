# 인수 기준(AC)

각 기능별로 `Feature Name`, `Class/Method`, `Acceptance Point`, `Verify Point`를 정리한다.

## 예약 생성
- Feature Name: 예약 생성
- Class/Method: `com.camping.legacy.controller.ReservationController#createReservation`, `com.camping.legacy.service.ReservationService#createReservation`
- Acceptance Point:
  - `siteNumber`는 필수이며 존재하는 사이트여야 한다.
  - `startDate`/`endDate`는 필수이며 종료일은 시작일보다 이전일 수 없다.
  - 시작일은 과거 날짜일 수 없다.
  - 예약 기간(종료-시작 일수) 30일 초과는 불가하다.
  - 예약자 이름은 필수이며 2~20자여야 한다.
  - 전화번호는 선택이며, 입력 시 숫자 10~11자리(하이픈 허용)여야 한다.
  - 동일 사이트에서 예약 기간이 겹치면 예약할 수 없다.
  - 예약 완료 시 6자리 영숫자 확인 코드가 발급된다.
- Verify Point:
  - 유효성 오류는 409 반환.
  - 겹치는 기간 예약 시 409 반환.
  - 성공 응답은 201이며 `confirmationCode`가 포함된다.

## 예약 생성 - 가격 계산
- Feature Name: 예약 가격 계산
- Class/Method: `com.camping.legacy.service.ReservationService#createReservation` (STEP 5)
- Acceptance Point:
  - 가격은 예약 기간(startDate~endDate) 각 날짜별로 개별 계산 후 합산한다.
  - **기본 가격(1일 기준)**:
    - A 사이트(대형): ₩80,000
    - B 사이트(소형): ₩50,000
    - 기타 사이트: ₩60,000
  - **할증 정책(일별로 적용)**:
    - 주말(토, 일): 기본가 × 1.3 (30% 할증)
    - 성수기(7~8월): 기본가 × 1.5 (50% 할증)
    - 주말 + 성수기: 기본가 × 1.7 (70% 할증)
    - 평일 + 비수기: 할증 없음
  - 할증 적용 후 `(int)` 캐스팅으로 소수점 이하 버림 처리한다.
- Verify Point:
  - 평일 비수기 예약 시 기본가만 적용된다.
  - 주말만 포함된 예약 시 30% 할증이 적용된다.
  - 성수기 평일 예약 시 50% 할증이 적용된다.
  - 성수기 주말 예약 시 70% 할증이 적용된다.
  - 혼합 기간(평일+주말, 비수기+성수기) 예약 시 각 날짜별로 정확히 계산된다.
  - 응답의 `totalPrice` 필드가 계산 결과와 일치한다.

## 예약 생성 - 포인트 적립
- Feature Name: 예약 포인트 적립
- Class/Method: `com.camping.legacy.service.ReservationService#createReservation` (STEP 6)
- Acceptance Point:
  - **기본 적립률**: 총 금액의 5%
  - **주말 포함 적립률**: 예약 기간 중 주말(토, 일)이 1일이라도 포함되면 총 금액의 10%
  - 적립 포인트는 `(int)` 캐스팅으로 소수점 이하 버림 처리한다.
  - 포인트 적립률은 전체 기간에 대해 단일 비율로 적용된다(일별 적용이 아님).
- Verify Point:
  - 평일만 포함된 예약 시 총 금액의 5%가 적립된다.
  - 주말이 1일이라도 포함된 예약 시 총 금액의 10%가 적립된다.
  - 소수점 이하는 버림 처리된다(예: ₩75,000 × 5% = 3,750P, 버림 없음 / ₩78,000 × 5% = 3,900P).
  - 응답의 `earnedPoints` 필드가 계산 결과와 일치한다.

## 연박 예약
- Feature Name: 연박 예약
- Class/Method: `com.camping.legacy.controller.ReservationController#createReservation`, `com.camping.legacy.service.ReservationService#createReservation`
- Acceptance Point:
  - 시작일~종료일 구간을 하나의 예약으로 저장한다.
  - 동일 사이트에서 기간이 겹치면 예약할 수 없다.
- Verify Point:
  - 겹치는 기간이 있으면 409 반환.
  - 겹치지 않으면 예약 성공(201).

## 예약 조회(단건)
- Feature Name: 예약 단건 조회
- Class/Method: `com.camping.legacy.controller.ReservationController#getReservation`, `com.camping.legacy.service.ReservationService#getReservation`
- Acceptance Point:
  - 예약 ID로 예약 정보를 조회할 수 있다.
- Verify Point:
  - 유효한 ID는 예약 상세를 반환.
  - 존재하지 않는 ID는 404 반환.

## 예약 조회(목록)
- Feature Name: 예약 목록 조회
- Class/Method: `com.camping.legacy.controller.ReservationController#getReservations`, `com.camping.legacy.controller.ReservationController#getMyReservations`, `com.camping.legacy.service.ReservationService#getReservationsByDate`, `com.camping.legacy.service.ReservationService#getReservationsByCustomerName`, `com.camping.legacy.service.ReservationService#getAllReservations`, `com.camping.legacy.service.ReservationService#getReservationsByNameAndPhone`
- Acceptance Point:
  - `/api/reservations`는 `date` 파라미터로 해당 날짜가 포함된 예약을 조회한다.
  - `/api/reservations`는 `customerName` 파라미터로 이름 기준 예약을 조회한다.
  - `/api/reservations`는 파라미터가 없으면 전체 예약을 반환한다.
  - `/api/reservations/my`는 `name`+`phone`으로 예약을 조회한다.
- Verify Point:
  - `date` 조건 조회 시 해당 기간에 포함된 예약만 반환.
  - `customerName` 조건 조회 시 해당 이름 예약만 반환.
  - `name`+`phone` 조건 조회 시 해당 예약만 반환.

## 예약 수정
- Feature Name: 예약 수정
- Class/Method: `com.camping.legacy.controller.ReservationController#updateReservation`, `com.camping.legacy.service.ReservationService#updateReservation`
- Acceptance Point:
  - 확인 코드가 필수이며 일치해야 한다.
  - `siteNumber` 변경 시 존재하는 사이트여야 한다.
  - `startDate`와 `endDate`를 함께 제공할 경우 날짜 유효성(과거 불가, 종료일>=시작일)을 만족해야 한다.
- Verify Point:
  - 확인 코드가 없거나 불일치하면 400 반환.
  - 날짜 유효성 오류 시 400 반환.
  - 유효한 요청은 수정된 예약 정보를 반환.

## 예약 취소
- Feature Name: 예약 취소
- Class/Method: `com.camping.legacy.controller.ReservationController#cancelReservation`, `com.camping.legacy.service.ReservationService#cancelReservation`
- Acceptance Point:
  - 취소 시 확인 코드 검증이 필수다.
  - 시작일이 오늘이면 `CANCELLED_SAME_DAY`, 아니면 `CANCELLED`로 변경한다.
- Verify Point:
  - 잘못된 확인 코드는 400 반환.
  - 성공 시 "예약이 취소되었습니다." 메시지 반환.

## 사이트 조회
- Feature Name: 전체 사이트 조회
- Class/Method: `com.camping.legacy.controller.SiteController#getAllSites`, `com.camping.legacy.service.SiteService#getAllSites`
- Acceptance Point:
  - 모든 캠핑 사이트 목록을 제공한다.
- Verify Point:
  - `/api/sites` 응답에 전체 사이트가 포함된다.

## 사이트 상세
- Feature Name: 사이트 상세 조회
- Class/Method: `com.camping.legacy.controller.SiteController#getSiteDetail`, `com.camping.legacy.service.SiteService#getSiteById`
- Acceptance Point:
  - 사이트 ID로 상세 정보를 조회할 수 있다.
- Verify Point:
  - 유효한 사이트 ID는 상세 정보를 반환.
  - 존재하지 않는 사이트 ID는 404 반환.

## 사이트 가용성 조회(단건)
- Feature Name: 사이트 가용성 조회(단건)
- Class/Method: `com.camping.legacy.controller.SiteController#checkAvailability`, `com.camping.legacy.service.SiteService#isAvailable`
- Acceptance Point:
  - 사이트 번호와 날짜를 입력하면 해당 사이트의 가용 여부를 반환한다.
  - 과거 날짜는 조회할 수 없다.
- Verify Point:
  - 응답에 `available` 값이 포함된다.

## 가용성 조회(단일 날짜)
- Feature Name: 특정 날짜 가용 사이트 조회
- Class/Method: `com.camping.legacy.controller.SiteController#getAvailableSites`, `com.camping.legacy.service.SiteService#getAvailableSites`
- Acceptance Point:
  - 특정 날짜에 예약 가능한 사이트 목록을 반환한다.
- Verify Point:
  - `reservationDate`가 해당 날짜인 예약이 있는 사이트는 제외된다.

## 가용성 조회(기간)
- Feature Name: 기간 가용 사이트 조회
- Class/Method: `com.camping.legacy.controller.SiteController#searchSites`, `com.camping.legacy.service.SiteService#searchAvailableSites`
- Acceptance Point:
  - `startDate`/`endDate`는 필수이며 종료일은 시작일보다 이전일 수 없다.
  - 시작일은 과거 날짜일 수 없다.
  - `size` 필터는 선택이며 값은 `대형`/`소형`/`일반`을 사용한다.
  - 시작일과 종료일 모두 예약이 없을 때만 사이트를 반환한다.
- Verify Point:
  - 시작일 또는 종료일에 예약이 있으면 제외된다.
  - `size` 필터 적용 시 해당 크기만 반환된다.

## 월별 예약 현황
- Feature Name: 월별 예약 현황 조회
- Class/Method: `com.camping.legacy.controller.ReservationController#getReservationCalendar`, `com.camping.legacy.service.ReservationService#getMonthlyCalendar`
- Acceptance Point:
  - 특정 사이트의 월별 예약 상태(예약/가능)를 제공한다.
- Verify Point:
  - 해당 월의 일자별 상태가 응답에 포함된다.
  - 예약된 날짜는 `available=false`로 표시된다.
