# 시스템 분석 2

`system-analysis.md`와 `requirements.md`를 기준으로 API와 비즈니스 규칙을 통합 정리했습니다.

## API 목록

| Method | Endpoint | 목적 | 주요 파라미터 |
|---|---|---|---|
| `POST` | `/reservations` | 예약 생성 | `startDate`, `endDate`, `guestName`, `phone`, `siteNumber` |
| `GET` | `/reservations/{reservationId}` | 예약 단건 조회 | `reservationId` (path) |
| `GET` | `/reservations` | 예약 조회(날짜/이름/이름+전화번호) | `date` 또는 `guestName` 또는 `guestName+phone` |
| `GET` | `/reservations/search` | 이름/전화번호 키워드 검색 | `keyword` |
| `POST` | `/reservations/{reservationId}/cancel` | 예약 취소 | `reservationId` (path), `confirmationCode` |
| `GET` | `/sites` | 전체 사이트 조회 | - |
| `GET` | `/sites/{siteNumber}` | 사이트 상세 조회 | `siteNumber` (path) |
| `GET` | `/availability` | 특정 날짜 가용 사이트 조회 | `date`, `size`(optional) |
| `GET` | `/availability/range` | 기간 전체 가용 사이트 조회 | `startDate`, `endDate`, `size`(optional) |
| `GET` | `/calendar/sites/{siteNumber}` | 사이트 월별 예약 현황 조회 | `siteNumber` (path), `year`, `month` |

## 발견한 비즈니스 규칙

1. 예약 시작일/종료일은 필수이며, 과거 날짜 예약은 불가하다.
2. 종료일은 시작일보다 이전일 수 없다.
3. 예약은 오늘 기준 30일 이내 기간만 허용된다.
4. 예약자 이름은 필수이며 빈 문자열이 될 수 없다.
5. 전화번호는 필수이며 null이 될 수 없다.
6. 동일 사이트-동일 기간 중복 예약은 불가하다.
7. 연박 예약은 시작일~종료일의 모든 날짜가 가용해야만 가능하다.
8. 동시에 같은 조건의 예약 요청이 들어오면 하나만 성공해야 한다(동시성 제어 필요).
9. 취소된 예약(`CANCELLED`)은 중복 체크에서 제외되며 즉시 재예약 가능해야 한다.
10. 예약 생성 시 6자리 영숫자 확인 코드가 자동 발급된다.
11. 예약 취소는 확인 코드 검증이 성공해야 수행할 수 있다.
12. 취소 환불 정책은 당일 취소 환불 불가, 사전 취소 전액 환불이다.
13. 사이트 번호 규칙은 `A*` 대형, `B*` 소형으로 해석한다.
14. 가용성 조회는 사이트 크기(`large`/`small`) 필터를 지원한다.
15. 월별 캘린더 조회는 특정 사이트의 예약일/가용일 상태를 구분해 제공해야 한다.
16. 동시성 문제 방지를 위한 적절한 트랜잭션 격리 수준이 필요하다.
