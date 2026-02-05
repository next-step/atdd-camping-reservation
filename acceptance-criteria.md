# 인수 기준(AC)

각 기능별로 `Feature Name`, `Acceptance Point`, `Verify Point`를 정리한다.

## 예약 생성
- Feature Name: 예약 생성
- Class/Method: `com.camping.legacy.controller.ReservationController#createReservation`, `com.camping.legacy.service.ReservationService#createReservation`
- Acceptance Point:
  - 시작일/종료일은 필수이며 과거 날짜는 허용하지 않는다.
  - 종료일은 시작일보다 이전일 수 없다.
  - 예약은 오늘 기준 30일 이내 기간만 가능하다.
  - 예약자 이름/전화번호/사이트 번호는 필수다.
  - 동일 사이트-동일 기간 중복 예약은 허용하지 않는다.
  - 예약 완료 시 6자리 영숫자 확인 코드가 발급된다.
- Verify Point:
  - 필수값 누락/과거 날짜/역전 날짜 입력 시 400 반환.
  - 30일 이후 날짜 입력 시 400 반환.
  - 동일 기간 중복 요청 시 하나만 성공(동시 요청 포함).
  - 성공 응답에 `confirmationCode`(6자리 영숫자) 포함.

## 연박 예약
- Feature Name: 연박 예약
- Class/Method: `com.camping.legacy.controller.ReservationController#createReservation`, `com.camping.legacy.service.ReservationService#createReservation`
- Acceptance Point:
  - 시작일~종료일의 모든 날짜가 가용해야 예약 가능하다.
- Verify Point:
  - 중간 날짜에 예약이 존재하면 예약 실패.
  - 모든 날짜가 비어 있으면 예약 성공.

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
- Class/Method: `com.camping.legacy.controller.ReservationController#getReservations`, `com.camping.legacy.controller.ReservationController#getMyReservations`, `com.camping.legacy.service.ReservationService#searchReservations`
- Acceptance Point:
  - 날짜 기준 예약 조회를 지원한다.
  - 이름 기준 예약 조회를 지원한다.
  - 이름+전화번호로 내 예약 조회를 지원한다.
  - 키워드(이름/전화번호 포함) 검색을 지원한다.
- Verify Point:
  - 각 조건으로 조회 시 해당 조건을 만족하는 예약만 반환.
  - 잘못된 조회 파라미터 조합은 400 반환(또는 빈 결과).

## 예약 취소
- Feature Name: 예약 취소
- Class/Method: `com.camping.legacy.controller.ReservationController#cancelReservation`, `com.camping.legacy.service.ReservationService#cancelReservation`
- Acceptance Point:
  - 취소 시 확인 코드 검증이 필수다.
  - 당일 취소는 환불 불가, 사전 취소는 전액 환불이다.
  - 취소된 예약은 중복 체크에서 제외되어 즉시 재예약 가능하다.
- Verify Point:
  - 잘못된 확인 코드는 취소 실패(400/403).
  - 당일 취소 시 `refundAmount` = 0.
  - 사전 취소 시 `refundAmount` = 전액.
  - 취소 후 동일 기간 재예약 가능.

## 사이트 조회
- Feature Name: 전체 사이트 조회
- Class/Method: `com.camping.legacy.controller.SiteController#getAllSites`, `com.camping.legacy.service.SiteService#getAllSites`
- Acceptance Point:
  - 모든 캠핑 사이트 목록을 제공한다.
- Verify Point:
  - `/sites` 응답에 전체 사이트가 포함된다.

## 사이트 상세
- Feature Name: 사이트 상세 조회
- Class/Method: `com.camping.legacy.controller.SiteController#getSiteDetail`, `com.camping.legacy.service.SiteService#getSiteById`
- Acceptance Point:
  - 사이트 번호로 상세 정보를 조회할 수 있다.
- Verify Point:
  - 유효한 사이트 번호는 상세 정보를 반환.
  - 존재하지 않는 사이트 번호는 404 반환.

## 가용성 조회(단일 날짜)
- Feature Name: 특정 날짜 가용 사이트 조회
- Class/Method: `com.camping.legacy.controller.SiteController#getAvailableSites`, `com.camping.legacy.service.SiteService#getAvailableSites`
- Acceptance Point:
  - 특정 날짜에 예약 가능한 사이트를 반환한다.
  - 사이트 크기(대형/소형) 필터를 지원한다.
- Verify Point:
  - 해당 날짜에 예약된 사이트는 목록에서 제외된다.
  - `size` 필터 적용 시 해당 크기만 반환된다.

## 가용성 조회(기간)
- Feature Name: 기간 가용 사이트 조회
- Class/Method: `com.camping.legacy.controller.SiteController#searchSites`, `com.camping.legacy.service.SiteService#searchAvailableSites`
- Acceptance Point:
  - 시작일~종료일 전체 기간에 가용한 사이트만 반환한다.
  - 사이트 크기(대형/소형) 필터를 지원한다.
- Verify Point:
  - 기간 중 하루라도 예약된 사이트는 제외된다.
  - `size` 필터 적용 시 해당 크기만 반환된다.

## 월별 예약 현황
- Feature Name: 월별 예약 현황 조회
- Class/Method: `com.camping.legacy.controller.ReservationController#getReservationCalendar`, `com.camping.legacy.service.ReservationService#getMonthlyCalendar`
- Acceptance Point:
  - 특정 사이트의 월별 예약 상태(예약/가능)를 제공한다.
- Verify Point:
  - 해당 월의 일자별 상태가 응답에 포함된다.
  - 예약된 날짜는 `available=false`로 표시된다.
