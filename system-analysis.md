# 시스템 분석 결과

## API 목록

> 각 항목의 비고란에는 예약자/관리자용을 따로 기재. 기재하지 않았으면 모든 권한 이용 가능.

| Endpoint | Method | 목적 | 주요 파라미터 | 비고 |
|----------|--------|------|--------------|------|
| /api/reservations | POST | 예약 생성 | ReservationRequest | 동시성 제어 필요, 확인코드 자동생성(예약자용) |
| /api/reservations/{id} | GET | 예약 조회 | id (Long) | - |
| /api/reservations | GET | 예약 목록 조회 | date (Optional), customerName (Optional) | 날짜별/고객명별 필터링(관리자용) |
| /api/reservations/{id} | DELETE | 예약 취소 | id (Long), confirmationCode (String) | 본인 확인 필수(예약자용)/관리자용 |
| /api/reservations/{id} | PUT | 예약 수정 | id (Long), ReservationRequest, confirmationCode (String) | 본인 확인 필수(예약자용)/관리자용 |
| /api/reservations/my | GET | 내 예약 조회 | name (String), phone (String) | 본인 정보 기반 조회(예약자용) |
| /api/reservations/calendar | GET | 예약 캘린더 | year (Integer), month (Integer), siteId (Long) | 월별 예약 현황 |
| /api/sites | GET | 캠핑장 목록 | 없음 | 전체 캠핑장 조회 |
| /api/sites/{siteId} | GET | 캠핑장 상세 | siteId (Long) | 캠핑장 상세 정보 |
| /api/sites/{siteNumber}/availability | GET | 예약 가능 캠핑장인지 확인 | siteNumber (String), date (LocalDate) | 특정 날짜 예약 가능 여부 |
| /api/sites/available | GET | 예약 가능 캠핑장 검색 | date (LocalDate) | 특정 날짜 예약 가능한 모든 캠핑장 |
| /api/sites/search | GET | 캠핑장 검색 | startDate (LocalDate), endDate (LocalDate), size (optional) | 기간별 예약 가능 캠핑장 검색 |

## 발견한 비즈니스 규칙

### 예약 관련 규칙
- **30일 제한**: 예약은 오늘로부터 30일 이내에만 가능 (MAX_RESERVATION_DAYS = 30)
- **날짜 검증**: 종료일이 시작일보다 이전일 수 없음
- **필수 정보**: 예약자 이름(빈 문자열 불가), 전화번호(null 불가), 캠핑장 번호
- **확인 코드**: 예약 완료 시 6자리 영숫자 확인 코드 자동 생성 (A-Z, 0-9)
- **중복 예약 방지**: 동일 캠핑장, 동일 기간에 중복 예약 불가
- **동시성 제어**: Thread.sleep(100ms) 지연으로 동시성 문제 방어 필요
- **취소된 예약 제외**: `CANCELED` 상태는 중복 체크에서 제외

### 취소 정책
- **당일 취소**: `CANCELED_SAME_DAY` 상태로 변경 (환불 불가)
- **사전 취소**: `CANCELED` 상태로 변경 (전액 환불 가능)
- **본인 확인**: 확인 코드 검증 필수

### 캠핑장 분류 규칙
- **A로 시작**: 대형 캠핑장 (전기 사용 가능)
- **B로 시작**: 소형 캠핑장 (전기 사용 가능)
- **화장실 거리**: 캠핑장 번호 뒷자리 × 10m
- **기본 시설**: 화장실, 샤워장, 개수대
- **기본 규칙**: 22시 이후 소음 금지, 직화 금지

### 데이터 무결성 규칙
- **예약 상태**: 기본값 `CONFIRMED`
- **생성 시간**: 예약 생성 시 자동으로 현재 시간 설정
- **연관 관계**: Campsite와 Reservation은 N:1 관계

### 검색 및 조회 규칙
- **연박 예약**: 시작일부터 종료일까지 모든 날짜에 대해 예약 가능 여부 확인
- **가용성 확인**: existsByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual 쿼리 사용
- **캘린더 뷰**: 예약 기간 내 모든 날짜에 예약 정보 표시

### 숨겨진 비즈니스 규칙
- **동시성 이슈**: 의도적인 100ms 지연으로 race condition 테스트 가능
- **캠핑장 번호 파싱**: toiletDistance 계산 시 캠핑장 번호를 `-(하이픈)`으로 분할하여 뒷자리 사용
- **예약 날짜 중복**: reservationDate와 startDate가 별도로 관리됨
- **상태 관리**: 취소 시점에 따라 다른 상태값 설정 (당일/사전)
- **검색 최적화**: 전체 예약 조회 후 스트림 필터링 방식 사용 (성능 이슈 가능성 존재)