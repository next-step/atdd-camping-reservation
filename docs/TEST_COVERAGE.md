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
- [X] 동시 예약 요청

### 3.2 예약 수정
- [x] 정상 수정 → 200 + 날짜 변경 반영 + confirmationCode 유지
- [x] 잘못된 confirmationCode → 400
- [X] 다른 예약과 충돌하는 날짜로 수정

### 3.3 예약 취소
- [X] 정상 취소(사전 취소) → 200 + status=CANCELLED
- [x] 당일 취소 → 200 + status=CANCELLED_SAME_DAY (LocalDate.now 기반)
- [X] 잘못된 confirmationCode 취소

## 4. Edge Case Coverage (P1 - Critical)

> 리스크: 금전적 손실, 데이터 무결성, 안전 문제
> 우선순위: 반드시 테스트 필요

### 4.1 경계값 (Boundary)
| 케이스 | 설명 | 예상 결과 | 테스트 |
|--------|------|----------|--------|
| 예약 기간 30일 초과 | 31일 이상 예약 시도 | 400 Bad Request | [ ] |
| 최대 인원수 초과 | maxPeople(6명) 초과 시 | 400 Bad Request | [ ] |

### 4.2 날짜 경계 (Date Boundary)
| 케이스 | 설명 | 예상 결과 | 테스트 |
|--------|------|----------|--------|
| 과거 날짜 예약 | 어제 날짜로 예약 시도 | 400 Bad Request | [ ] |
| 종료일 < 시작일 | 날짜 역전 | 400 Bad Request | [ ] |

### 4.3 중복 데이터 (Duplicate)
| 케이스 | 설명 | 예상 결과 | 테스트 |
|--------|------|----------|--------|
| 날짜 범위 경계 중복 | 기존(2/1~2/3) 후 2/3~2/5 시도 | 409 Conflict | [ ] |
| 시작일 경계 중복 | 기존(2/1~2/3) 후 2/1~2/2 시도 | 409 Conflict | [ ] |
| 종료일 경계 중복 | 기존(2/1~2/3) 후 2/2~2/3 시도 | 409 Conflict | [ ] |

