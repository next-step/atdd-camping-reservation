# 시스템 분석 결과

## API 목록
| Endpoint | Method | 목적 | 주요 파라미터 | 비고 |
|----------|--------|------|--------------|------|
| `/api/reservations` | POST | 예약 생성 | ReservationRequest (name, phone, startDate, endDate, siteId) | 6자리 확인코드 생성 |
| `/api/reservations/{id}` | GET | 예약 조회 | id (예약ID) | 단일 예약 상세 |
| `/api/reservations` | GET | 예약 목록 조회 | date, customerName | 날짜별/고객명별 검색 |
| `/api/reservations/{id}` | DELETE | 예약 취소 | id, confirmationCode | 본인확인 필수 |
| `/api/reservations/{id}` | PUT | 예약 수정 | id, ReservationRequest, confirmationCode | 본인확인 필수 |
| `/api/reservations/my` | GET | 내 예약 조회 | name, phone | 이름+전화번호로 검색 |
| `/api/reservations/calendar` | GET | 월별 예약 현황 | year, month, siteId | 캘린더 뷰 |
| `/api/sites` | GET | 사이트 목록 조회 | - | 전체 사이트 |
| `/api/sites/{siteId}` | GET | 사이트 상세 조회 | siteId | 개별 사이트 정보 |
| `/api/sites/{siteNumber}/availability` | GET | 단일 날짜 가용성 | siteNumber, date | 특정 사이트 날짜별 |
| `/api/sites/available` | GET | 날짜별 가용 사이트 | date | 해당 날짜 가능한 모든 사이트 |
| `/api/sites/search` | GET | 기간별 사이트 검색 | startDate, endDate, size | 연박 예약용 |

## 발견한 비즈니스 규칙
- **예약 기간 제한**: 오늘로부터 30일 이내만 예약 가능
- **동시성 제어**: 동일 사이트/기간 중복 예약 방지 (단 하나만 성공)
- **연박 검증**: 시작일~종료일 전체 기간 가용성 확인 필수
- **확인코드 시스템**: 예약시 6자리 영숫자 코드 자동생성, 취소/수정시 필수
- **취소된 예약 제외**: CANCELLED 상태는 중복체크에서 제외
- **필수 입력값**: 고객명(빈 문자열 불가), 전화번호(null 불가), 사이트번호
- **날짜 검증**: 과거날짜 불가, 종료일 >= 시작일
- **사이트 분류**: A로 시작(대형), B로 시작(소형)
- **취소 정책**: 당일취소 환불불가, 사전취소 전액환불
- **본인확인**: 예약 수정/취소시 확인코드 검증 필수
