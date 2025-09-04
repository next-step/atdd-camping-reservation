# 시스템 분석 결과 (레거시 예약 시스템) — Step 1

> 소스 기반 정적 분석 초안. 이후 실측(curl/RestAssured) 결과로 응답 예시/메시지를 갱신하세요.

## API 목록 (확정 경로/파라미터)

### 예약(Reservation)
| Endpoint | Method | 목적 | 요청 파라미터/바디 | 응답(성공) | 실패·예외 처리(Controller 코드 기준) | 비고 |
|----------|--------|------|--------------------|------------|-------------------------------------|------|
| `/api/reservations` | POST | 예약 생성 | **Body(ReservationRequest)**: `customerName`, `phoneNumber`, `siteNumber`, `startDate`, `endDate` | 201 + `ReservationResponse` | RuntimeException → **409 CONFLICT** + `{"message":...}` | 동시성 재현용 `Thread.sleep(100)` 존재. 기간 중복 체크 O. **30일 이내/과거 금지/전화번호 필수 미검증** |
| `/api/reservations/{id}` | GET | 예약 단건 조회 | path: `id` | 200 + `ReservationResponse` | 미존재 → **404 NOT_FOUND** + `{"message":...}` |  |
| `/api/reservations` | GET | 목록/검색 | query: `date`(YYYY-MM-DD) 또는 `customerName` (둘 다 없으면 전체) | 200 + `List<ReservationResponse>` | - | `date`는 start~end 범위 포함 여부로 필터 |
| `/api/reservations/{id}` | DELETE | 예약 취소 | path: `id`, query: `confirmationCode` | 200 + `{"message":"예약이 취소되었습니다."}` | 코드 불일치 등 → **400 BAD_REQUEST** + `{"message":...}` | **당일 취소 시 status="CANCELLED_SAME_DAY"**, 그 외 "CANCELLED". 환불액 계산/정책 메시지는 없음 |
| `/api/reservations/{id}` | PUT | 예약 변경 | path: `id`, query: `confirmationCode`, **Body(ReservationRequest)** (부분 변경 허용) | 200 + `ReservationResponse` | 오류 → **400 BAD_REQUEST** + `{"message":...}` | 기간/중복 재검증 로직 **부재** |
| `/api/reservations/my` | GET | 내 예약 조회 | query: `name`, `phone` | 200 + `List<ReservationResponse>` | - | 정확 일치 검색 |

### 사이트(Site)
| Endpoint | Method | 목적 | 요청 | 응답(성공) | 실패 | 비고 |
|----------|--------|------|------|------------|------|------|
| `/api/sites` | GET | 사이트 목록 | - | 200 + `List<SiteResponse>` | - |  |
| `/api/sites/{siteId}` | GET | 사이트 상세 | path: `siteId` | 200 + `SiteResponse` | 미존재 시 500(예외) 가능 | Controller는 예외 매핑 없음 |
| `/api/sites/{siteNumber}/availability` | GET | 단일 날짜 가용성 | path: `siteNumber`, query: `date` | 200 + `{siteNumber,date,available}` | - | CampsiteService.isAvailable 사용(하루 단위) |
| `/api/sites/available` | GET | 특정 날짜 가용 사이트 | query: `date` | 200 + `List<SiteAvailabilityResponse>` | - | **existsByCampsiteAndReservationDate**만 검사 → 단일일 전용 |
| `/api/sites/search` | GET | 기간/필터 가용 검색 | query: `startDate`, `endDate`, `size?` | 200 + `List<SiteAvailabilityResponse>` | - | **시작일·종료일 두 날만 검사 → 구간 중간 날짜 미검사(버그)** |

### 캘린더(Calendar)
| Endpoint | Method | 목적 | 요청 | 응답(성공) | 비고 |
|----------|--------|------|------|------------|------|
| `/api/reservations/calendar` | GET | 월별 예약 현황 | query: `year`, `month`, `siteId` | 200 + `CalendarResponse` | Service에서 모든 예약 조회 후 해당 사이트/월 범위 매핑 |

---

## 도메인/서비스 관찰 포인트 (코드 정적 분석)

### 비즈니스 규칙 구현 현황 (요구사항 대비)
- **기간 중복 방지:** 구현 O
    - `existsByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual(campsite, end, start)`
- **동시성 제어:** 구현 **미흡**
    - `Thread.sleep(100)`로 경합 유도, 별도의 락/격리 수준/유니크 인덱스 없음 → **동시 요청 시 중복 생성 가능성 매우 높음**
- **취소 후 재예약:** 요구사항은 *취소는 중복 체크에서 제외*인데, **상태 조건 없이 단순 exists로 중복 검사** → **위반 가능성 높음**
- **예약 30일 제한:** **미구현**
- **과거 날짜 예약 불가:** **미구현**
- **전화번호 필수:** Controller/Service 어디에도 **미검증**
- **연박 예약 전체 기간 검증:**
    - 생성 시에는 start~end 범위에 겹침 검사 O (정상)
    - **가용성 검색(`/api/sites/search`)은 시작/끝 **두 날만** 검사 → **중간 날짜 미검증(버그)**
- **취소 정책(환불 계산):** 상태 플래그만 존재, **금액/정책 로직 미구현**
- **업데이트**: 변경 시 기간·중복·정합성 재검증 **없음** → **무결성 깨질 수 있음**
- **API 에러 응답 형식:** `{"message": "..."} ` 통일. 상태코드는 엔드포인트별로 409/404/400로 일관 비슷.

### 일관성/리포지토리 사용 이슈
- 같은 “가용성” 판단에 **두 가지 기준** 혼재
    - 단일일: `existsByCampsiteAndReservationDate(...)`
    - 범위: `existsByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual(...)`
- `CampsiteService.isAvailable`(단일일 기준) vs `SiteService.isAvailable`(범위 기준) → **API마다 상이한 기준 사용**

---

