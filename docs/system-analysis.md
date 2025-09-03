
# 초록 캠핑장 예약 시스템 분석 결과

## 시스템 개요
초록 캠핑장 예약 시스템은 Spring Boot 기반의 REST API로 구현된 캠핑장 예약 관리 시스템입니다.
예약 CRUD 기능과 사이트 정보 조회 기능을 제공합니다.

## API 목록

### 예약 관리 API
| Endpoint | Method | 목적 | 주요 파라미터 | 비고 |
|----------|--------|------|--------------|------|
| /api/reservations | POST | 예약 생성 | customerName, startDate, endDate, siteNumber, phoneNumber | 6자리 확인 코드 자동 생성 |
| /api/reservations/{id} | GET | 예약 조회 | id (Path) |  |
| /api/reservations | GET | 예약 목록 조회 | date, customerName (Query, Optional) | 조건부 필터링 |
| /api/reservations/{id} | DELETE | 예약 취소 | id (Path), confirmationCode (Query) |  |
| /api/reservations/{id} | PUT | 예약 수정 | id (Path), ReservationRequest (Body), confirmationCode (Query) |  |
| /api/reservations/my | GET | 내 예약 조회 | name, phone (Query) | 개인정보 기반 조회 |
| /api/reservations/calendar | GET | 월별 예약 현황 | year, month, siteId (Query) | 캘린더 형태 데이터 |

### 사이트 관리 API
| Endpoint | Method | 목적 | 주요 파라미터 | 비고 |
|----------|--------|------|--------------|------|
| /api/sites | GET | 사이트 목록 조회 | 없음 | 전체 캠핑 사이트 |
| /api/sites/{siteId} | GET | 사이트 상세 조회 | siteId (Path) | 개별 사이트 정보 |
| /api/sites/{siteNumber}/availability | GET | 사이트 가용성 확인 | siteNumber (Path), date (Query) | 특정 날짜 예약 가능 여부 |
| /api/sites/available | GET | 가용 사이트 조회 | date (Query) | 특정 날짜의 예약 가능 사이트 목록 |
| /api/sites/search | GET | 사이트 검색 | startDate, endDate, size (Query) | 기간별 가용 사이트 검색 |

## 🔍 발견한(숨겨진) 비즈니스 규칙
- **전기 사용 가능 표기** : 대형 사이트만 전기를 제공한다.
- **예약 상태 기본값 자동 설정** : 예약 생성 시 자동으로 CONFIRMED(확정)으로 설정 (기본 상태에서 취소 상태로의 단방향 전환만 허용)
- 몇명이 머물건지는 중요하지 않다.
- 화장실 거리는 siteNumber의 하이픈 뒤 숫자 × 10으로 산출(예: A-03 → 30)
- 공통 시설/규칙 문구: SiteResponse의 facilities와 rules는 모든 사이트에 동일한 고정값을 제공.
