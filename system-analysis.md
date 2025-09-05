# 시스템 분석 결과

## API 목록
| Endpoint | Method | 목적 | 주요 파라미터 | 비고 |
|---|---|---|---|---|
| `/api/sites` | GET | 모든 캠핑장 사이트 목록 조회 | 없음 | |
| `/api/sites/{siteId}` | GET | 특정 캠핑장 사이트 상세 정보 조회 | `siteId` (Path) | |
| `/api/sites/search` | GET | 기간/조건별 예약 가능 사이트 검색 | `startDate`, `endDate` (Query) | |
| `/api/sites/{siteNumber}/availability` | GET | 특정 사이트의 날짜별 예약 가능 여부 확인 | `siteNumber` (Path), `date` (Query) | |
| `/api/reservations` | POST | 신규 예약 생성 | `ReservationRequest` (Body) | 성공 시 201 Created, 중복 시 409 Conflict 반환 |
| `/api/reservations` | GET | 조건별 예약 목록 조회 | `date` 또는 `customerName` (Query) | |
| `/api/reservations/{id}` | GET | 특정 예약 상세 정보 조회 | `id` (Path) | 예약이 없으면 404 Not Found 반환 |
| `/api/reservations/{id}` | PUT | 특정 예약 정보 수정 | `id` (Path), `confirmationCode` (Query), `ReservationRequest` (Body) | 실패 시 400 Bad Request 반환 |
| `/api/reservations/{id}` | DELETE | 특정 예약 취소 | `id` (Path), `confirmationCode` (Query) | 실패 시 400 Bad Request 반환 |
| `/api/reservations/my` | GET | 이름과 전화번호로 자신의 예약 조회 | `name`, `phone` (Query) | |
| `/api/reservations/calendar` | GET | 월별 예약 현황 데이터 조회 | `year`, `month`, `siteId` (Query) | |

## 발견한 비즈니스 규칙
- 예약을 수정하거나 취소하기 위해서는 반드시 예약 ID와 함께 `confirmationCode`(예약 확정 코드)가 필요하다.
  - `confirmationCode`는 사용자가 직접 기억하고 있어야 한다.
- 이미 예약이 있는 날짜에 동일한 사이트를 예약하려고 하면 '중복 예약'으로 간주되어 실패 처리(409 Conflict)된다.
- 예약 취소 시 취소된 예약은 당일 취소 혹은 그냥 취소 상태로 변경된다.
- 사용자는 별도의 로그인 없이 자신의 '이름'과 '전화번호'만으로 예약 내역을 조회할 수 있다.
