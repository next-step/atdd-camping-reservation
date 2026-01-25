# 1. 시스템 요약 (One-page Facts)

| 항목 | 내용 |
|---|---|
| 인증/식별 방식 | 로그인 없음 (이름/전화번호 기반) |
| 핵심 도메인 | 캠핑장 예약 |
| 주요 엔티티/상태 | `Reservation` (예약), `Campsite` (캠핑장) |
| 트래픽/운영 특징 | 일 평균 500건, 성수기 2,000건. 테스트 코드 전무, 수동 QA 중심 운영. |
| 분석 범위 및 한계 | 정적 코드 분석 기반. 실제 실행/부하 테스트는 포함하지 않음. |

# 2. API 엔드포인트 전체 목록 (Fact Table)

| # | Endpoint | Method | 기능 요약(1문장) | 핵심 입력/제약(키워드) | 핵심 결과/상태 변화 | 관련 도메인/테이블 | 리스크 힌트(정성) |
|---|---|---|---|---|---|---|---|
| 1 | /api/reservations | POST | 새로운 캠핑장 예약을 생성합니다. | `ReservationRequest`, 날짜 범위, 고객 정보, 사이트 존재 | `Reservation` 레코드 생성, 예약 ID 반환 | `Reservation`, `Campsite` | 동시성, 유효성 검증 누락 |
| 2 | /api/reservations/{id} | GET | 특정 예약 ID로 예약 상세 정보를 조회합니다. | `id` | `ReservationResponse` 반환 | `Reservation` | 존재하지 않는 ID |
| 3 | /api/reservations | GET | 조건에 따라 예약 목록을 조회합니다. | `date`, `customerName` (선택) | `List<ReservationResponse>` 반환 | `Reservation` | 대량 데이터 조회 성능 |
| 4 | /api/reservations/{id} | DELETE | 특정 예약을 취소합니다. | `id`, `confirmationCode` | `Reservation` 상태 변경 (CANCELLED) | `Reservation` | `confirmationCode` 무단 사용, 상태 전이 오류 |
| 5 | /api/reservations/{id} | PUT | 특정 예약 정보를 업데이트합니다. | `id`, `ReservationRequest`, `confirmationCode` | `Reservation` 레코드 업데이트 | `Reservation`, `Campsite` | `confirmationCode` 무단 사용, 예약 가용성 재확인 |
| 6 | /api/reservations/my | GET | 이름과 전화번호로 본인의 예약 목록을 조회합니다. | `name`, `phone` | `List<ReservationResponse>` 반환 | `Reservation` | 인증 부재, 개인 정보 노출 |
| 7 | /api/reservations/calendar | GET | 특정 캠핑장의 월별 예약 캘린더 정보를 조회합니다. | `year`, `month`, `siteId` | `CalendarResponse` 반환 | `Reservation`, `Campsite` | 대량 데이터 조회 성능 |
| 8 | /api/sites | GET | 모든 캠핑장 목록을 조회합니다. | 없음 | `List<SiteResponse>` 반환 | `Campsite` | 대량 데이터 조회 성능 |
| 9 | /api/sites/{siteId} | GET | 특정 캠핑장 상세 정보를 조회합니다. | `siteId` | `SiteResponse` 반환 | `Campsite` | 존재하지 않는 ID |
| 10 | /api/sites/{siteNumber}/availability | GET | 특정 날짜의 캠핑장 예약 가능 여부를 확인합니다. | `siteNumber`, `date` | `boolean` (available) 반환 | `Campsite`, `Reservation` | 동시성, 잘못된 가용성 정보 |

# 3. 전역 핵심 비즈니스 규칙 (Global Business Rules)

| 규칙 영역(Category) | 규칙 요약 | 적용 기능/API | 코드 근거 | 규칙 위반 시 시스템 영향 |
|---|---|---|---|---|
| 날짜/기간 제약 | 예약 종료일은 시작일보다 이전일 수 없습니다. | 예약 생성, 예약 업데이트, 사이트 검색 | `endDate.isBefore(startDate)` | 예외 발생 (RuntimeException) |
| 날짜/기간 제약 | 예약 시작일은 과거 날짜일 수 없습니다. | 예약 생성, 예약 업데이트, 사이트 검색 | `startDate.isBefore(today)` | 예외 발생 (RuntimeException) |
| 날짜/기간 제약 | 단일 예약 기간은 최대 30일입니다. | 예약 생성 | `ChronoUnit.DAYS.between > MAX_RESERVATION_DAYS` | 예외 발생 (RuntimeException) |
| 중복/가용성 제약 | 동일 캠핑장에는 지정된 기간에 중복 예약될 수 없습니다. | 예약 생성 | `reservationRepository.existsByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual` | 예외 발생 (RuntimeException, HTTP 409 CONFLICT) |
| 중복/가용성 제약 | 캠핑장은 시스템에 등록된 유효한 번호 또는 ID여야 합니다. | 예약 생성/조회/수정, 사이트 조회/가용성 확인 | `campsiteRepository.findBySiteNumber` / `findById` | 예외 발생 (RuntimeException, HTTP 404 NOT FOUND) |
| 가격/요금 규칙 | 캠핑장 기본 가격은 사이트 유형(A: 8만원, B: 5만원, 기타: 6만원)에 따라 다릅니다. | 예약 생성, 가격 계산 | `siteNumber.startsWith("A")`, `siteNumber.startsWith("B")` | 잘못된 요금 계산 |
| 가격/요금 규칙 | 주말(토/일) 예약 시 30% 할증, 성수기(7,8월) 예약 시 50% 할증, 성수기 주말 예약 시 70% 할증이 적용됩니다. | 예약 생성, 가격 계산 | `current.getDayOfWeek()`, `current.getMonthValue()` | 잘못된 요금 계산 |
| 포인트/적립 규칙 | 결제 금액의 기본 5%가 적립됩니다. | 예약 생성, 포인트 계산 | `pointRate = 0.05` | 잘못된 포인트 적립 |
| 포인트/적립 규칙 | 주말 포함 예약 시 10%의 포인트가 적립됩니다. | 예약 생성, 포인트 계산 | `hasWeekend ? 0.10 : ...` | 잘못된 포인트 적립 |
| 포인트/적립 규칙 | 성수기 예약 시 3%의 포인트가 적립됩니다. | 예약 생성, 포인트 계산 | `isPeakSeason ? 0.03 : ...` | 잘못된 포인트 적립 |
| 취소/변경 제약 | 예약을 취소하거나 변경하려면 정확한 확인 코드가 필요합니다. | 예약 취소, 예약 업데이트 | `!reservation.getConfirmationCode().equals(confirmationCode)` | 예외 발생 (RuntimeException, HTTP 400 BAD REQUEST) |
| 취소/변경 제약 | 예약 당일 취소 시 `CANCELLED_SAME_DAY` 상태로 변경됩니다. | 예약 취소 | `reservation.getStartDate().equals(today)` | 잘못된 예약 상태 관리 |

# 4. 신규 또는 암묵적 기능 (명세서 외)

| # | 기능/동작 | 설명 | 의도(의도/우연/미확인) | 운영/테스트 영향 | 코드 근거 |
|---|---|---|---|---|---|
| 1 | 결제 및 알림 통합 처리 | 예약 생성, 가격 계산, 결제 처리(시뮬레이션), 포인트 적립, 알림 발송을 한 번에 처리합니다. | 의도 (과거 기능, 현재 Deprecated) | 단일 책임 원칙 위배, 복잡성 증가 | `ReservationService.processReservationWithPayment` |
| 2 | 동시성 문제 재현을 위한 지연 | `createReservation` 메서드 내에 100ms의 의도적인 지연(`Thread.sleep`) 코드가 삽입되어 있습니다. | 의도 (테스트/데모 목적) | 불필요한 성능 저하, 동시성 문제 발생 유도 | `ReservationService.createReservation` (STEP 7) |
| 3 | 알림 발송 시뮬레이션 | 이메일/SMS 알림 발송이 실제 외부 시스템 연동 없이 로깅으로 처리됩니다. | 의도 (기능 미구현/시뮬레이션) | 실제 알림 미발송, 고객 커뮤니케이션 누락 | `ReservationService.send...Notification` 메서드들, `createReservation` (STEP 10) |
| 4 | 내부 통계/리포트 생성 | 일별/월별 예약 통계, 취소율, 월간 리포트 생성 기능이 존재합니다. | 의도 (어드민/내부 리포팅용 추정) | 내부 분석에 활용 가능, `ReservationService`의 책임 과중 | `ReservationService.getDailyReservationCount`, `getMonthlyReservationCount`, `getCancellationRate`, `generateMonthlyReport` |

# 5. 잠재적 버그 / 코드 스멜 후보 (정성)

| # | 유형(동시성/성능/보안/상태 등) | 의심 위치(API/코드) | 위험한 이유(정성) | 발생 가능 시나리오 | 코드 근거 |
|---|---|---|---|---|---|
| 1 | 동시성 | `POST /api/reservations` (예약 생성 로직) | `Thread.sleep` 이후 `exists`로 예약 가능 확인 시, `save` 전에 다른 요청이 먼저 예약할 수 있습니다. | 동시에 여러 고객이 동일한 캠핑장-기간을 예약하여 오버부킹이 발생할 수 있습니다. | `ReservationService.createReservation` (STEP 4, STEP 7) |
| 2 | 보안 | `GET /api/reservations/my` | 이름과 전화번호만으로 본인 확인 없이 예약 정보를 조회합니다. | 타인의 이름과 전화번호를 아는 경우, 개인 예약 정보를 무단 조회할 수 있습니다. | `ReservationService.getReservationsByNameAndPhone` |
| 3 | 성능 | `GET /api/reservations` (예약 목록 조회), `GET /api/reservations/calendar` (월별 예약 캘린더 조회) | `findAll()` 후 애플리케이션 내에서 필터링하는 방식으로, 데이터 증가 시 성능 저하가 예상됩니다. | 예약 데이터가 많아질 경우, API 응답 시간이 지연되거나 서버 메모리 부족 현상이 발생할 수 있습니다. | `ReservationService.getReservationsByDate`, `ReservationService.getAllReservations`, `ReservationService.getMonthlyCalendar` |
| 4 | 코드 스멜 | `ReservationService.createReservation` | 단일 메서드에 너무 많은 비즈니스 로직(검증, 가격, 포인트, 알림 등)이 응집되어 있습니다. | 기능 변경 시 영향을 받는 범위가 넓고, 가독성이 떨어져 유지보수 효율이 낮아질 수 있습니다. | `ReservationService.createReservation` |
| 5 | 데이터 정합성 | 예약 취소/변경 로직의 `confirmationCode` 검증 | `confirmationCode`가 단순 문자열 비교로, 탈취 시 무단으로 예약 취소/변경이 가능합니다. | 악의적인 사용자가 확인 코드를 알게 되면, 타인의 예약을 임의로 취소하거나 변경할 수 있습니다. | `ReservationService.cancelReservation`, `ReservationService.updateReservation` |
| 6 | 데이터 모델링 | `Reservation` 엔티티의 `status` 필드 (String) | 예약 상태를 String으로 관리하여 오타나 일관되지 않은 상태 값 입력 가능성이 있습니다. | 잘못된 상태 값으로 인해 특정 상태의 예약을 정확히 조회하거나 처리하기 어려울 수 있습니다. | `Reservation.setStatus("CANCELLED")` |
| 7 | 중복 코드 | 가격 계산, 포인트 계산, 날짜 유효성 검증 로직 | 여러 서비스 메서드에서 유사한 로직이 중복되어 구현되어 있습니다. | 비즈니스 규칙 변경 시 여러 곳을 수정해야 하므로, 수정 누락으로 인한 오류 발생 가능성이 있습니다. | `ReservationService.createReservation` (STEP 5, 6), `processReservationWithPayment`, `calculateReservationPrice` 등 |

# 6. Self-check Checklist (필수)

-   [x] 외부 노출 API 10개가 모두 식별되었는가?
-   [x] 시스템 전반 공통 비즈니스 규칙이 정리되었는가?
-   [x] 명세서 외 신규/암묵 기능이 식별되었는가?
-   [x] 잠재적 버그/코드 스멜이 후보 수준으로 기술되었는가?
-   [x] 판단/점수/우선순위가 문서에 포함되지 않았는가?
-   [x] docs/system-analysis.md 외 파일을 변경하지 않았는가?
