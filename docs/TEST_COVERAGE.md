# Test Coverage - Reservation (Acceptance)

## 1. Scope
- 대상: 예약 도메인 (생성/수정/취소)
- API
    - POST /api/reservations
    - PUT /api/reservations/{id}
    - DELETE /api/reservations/{id}
    - GET /api/reservations/{id} (취소/상태 확인 목적)
    - GET /api/sites/search (사전 조건 검증 목적)

## 2. Test Level
- Acceptance Test (API E2E)
    - 대표 사용자 흐름 일부에 대해 사이트 조회 → 예약 생성까지 포함
    - 나머지 시나리오는 예약 API 중심 검증으로 구성

## 3. Scenario Coverage

### 3.1 예약 생성
- [x] 정상 예약 생성 → 201 + confirmationCode(6자리) + status=CONFIRMED
- [x] 중복 예약 방지 → 409 Conflict
- [ ] 동시 예약 요청 → (현재 Disabled / 원인: 서버 측 동시성 제어 미적용 또는 DB 제약 미구성)

### 3.2 예약 수정
- [x] 정상 수정 → 200 + 날짜 변경 반영 + confirmationCode 유지
- [x] 잘못된 confirmationCode → 400
- [ ] 다른 예약과 충돌하는 날짜로 수정 → Disabled

### 3.3 예약 취소
- [ ] 정상 취소(사전 취소) → 200 + status=CANCELLED → Disabled
- [x] 당일 취소 → 200 + status=CANCELLED_SAME_DAY (LocalDate.now 기반)
- [ ] 잘못된 confirmationCode 취소 → Disabled

## 4. Data / Environment Coverage
- DB 초기화: @Sql(truncate.sql, data.sql)
- 사이트 데이터는 seed로 존재한다고 가정하되,
  일부 테스트는 "사이트 조회 API가 정상 동작"도 함께 확인하기 위해 GET /api/sites/search를 호출함.
- 날짜:
    - 당일 취소 테스트는 LocalDate.now() 기반으로 동작
    - 시간 의존 이슈를 줄이기 위해 추후 Clock 주입 방식 고려 가능

## 5. Non-functional Coverage
- 동시성:
    - 동시 예약 테스트는 작성되어 있으나 현재 Disabled
    - 향후 DB Unique 제약 또는 Lock 전략 적용 후 활성화 예정
- 보안/권한: Out of Scope (인증/인가 없음 가정)
