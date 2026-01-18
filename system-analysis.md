# 캠핑 예약 시스템 분석 보고서

## 1. 시스템 개요

캠핑장 예약 관리를 위한 REST API 시스템으로, 예약 관리와 사이트 관리 기능을 제공합니다.


---

## 2. API 목록

### 2.1 예약 API (`/api/reservations`)

| Method | URL | 설명 |
|--------|-----|------|
| POST | /api/reservations | 예약 생성 |
| GET | /api/reservations/{id} | 예약 단건 조회 |
| GET | /api/reservations | 예약 목록 조회 |
| DELETE | /api/reservations/{id} | 예약 취소 |
| PUT | /api/reservations/{id} | 예약 수정 |
| GET | /api/reservations/my | 내 예약 조회 |
| GET | /api/reservations/calendar | 월별 캘린더 조회 |

### 2.2 사이트 API (`/api/sites`)

| Method | URL | 설명 |
|--------|-----|------|
| GET | /api/sites | 전체 사이트 목록 |
| GET | /api/sites/{siteId} | 사이트 상세 조회 |
| GET | /api/sites/{siteNumber}/availability | 특정 날짜 가용성 확인 |
| GET | /api/sites/available | 가용 사이트 목록 |
| GET | /api/sites/search | 기간별 사이트 검색 |

---

## 5. 핵심 비즈니스 규칙

### 5.1 예약 생성 규칙

| 규칙 | 설명 |
|------|------|
| 예약 기간 제한 | 최대 30일까지만 예약 가능 |
| 과거 날짜 제한 | 과거 날짜로 예약 불가 |
| 날짜 순서 검증 | 종료일이 시작일보다 이전일 수 없음 |
| 중복 예약 방지 | 동일 사이트, 동일 기간 중복 예약 불가 |
| 연박 예약 검증 | 시작일~종료일 전체 기간 가용성 확인 필요 |

### 5.2 필수 입력값 검증

| 항목 | 규칙 |
|------|------|
| 예약자명 | 필수, 2~20자 |
| 전화번호 | 필수, 하이픈 제외 10~11자리 숫자 |
| 사이트 번호 | 필수, 존재하는 사이트여야 함 |
| 시작일/종료일 | 필수 |

### 5.3 예약 변경/취소 규칙

| 규칙 | 설명 |
|------|------|
| 본인 확인 | 확인 코드 검증 필수 |
| 당일 취소 | 별도 상태(CANCELLED_SAME_DAY)로 관리 |
| 취소된 예약 | 해당 사이트 즉시 재예약 가능 |

### 5.4 확인 코드

| 항목 | 설명 |
|------|------|
| 형식 | 6자리 영문+숫자 조합 |
| 생성 시점 | 예약 완료 시 자동 생성 |
| 용도 | 예약 수정/취소 시 본인 확인 |

---

## 6. 숨겨진 비즈니스 규칙

> 기능 명세서(requirements.md)에 명시되지 않았으나 코드에서 발견된 규칙들

### 6.1 입력값 검증

| 항목 | 규칙 |
|------|------|
| 예약자명 길이 | 최소 2자 ~ 최대 20자 |
| 전화번호 형식 | 하이픈 제외 10~11자리 숫자만 허용 |

### 6.2 예약 상태

| 상태 | 설명 |
|------|------|
| CONFIRMED | 예약 확정 (기본값) |
| CANCELLED | 예약 취소 |
| CANCELLED_SAME_DAY | 당일 취소 (별도 구분) |

### 6.3 사이트 속성

| 접두어 | 크기 | 전기 사용 |
|--------|------|----------|
| A- | 대형 | 가능 |
| B- | 소형 | 불가 |

### 6.4 가격 정책

**기본 요금 (1박 기준)**

| 사이트 | 가격 |
|--------|------|
| 대형 (A-) | 80,000원 |
| 소형 (B-) | 50,000원 |
| 기타 | 60,000원 |

**할증 정책**

| 조건 | 할증률 |
|------|--------|
| 주말 (토, 일) | +30% |
| 성수기 (7~8월) | +50% |
| 성수기 주말 | +70% |

### 6.5 포인트 적립 정책

**예약 기간 기준**

| 조건 | 적립률 |
|------|--------|
| 기본 | 5% |
| 주말 포함 | 10% |
| 성수기 | 3% |

**결제 수단 기준**

| 결제 수단 | 적립률 |
|-----------|--------|
| 카드 (CARD) | 10% |
| 모바일 (MOBILE) | 8% |
| 계좌이체 (TRANSFER) | 5% |
| 현금 (CASH) | 3% |

---

## 7. 잠재적 버그 및 코드 스멜

### 7.1 동시성 문제

| 위치 | 문제 | 영향 |
|------|------|------|
| ReservationService.createReservation() | `Thread.sleep(100)` 존재 | Race Condition 유발 가능 |
| ReservationService.createReservation() | 낙관적/비관적 락 미구현 | 동시 예약 시 중복 예약 발생 가능 |
| 예약 중복 체크 | 취소된 예약(CANCELLED) 제외 로직 미구현 | 취소된 자리 재예약 불가 가능성 |

### 7.2 비즈니스 로직 버그

| 위치 | 문제 | 영향 |
|------|------|------|
| SiteService.searchAvailableSites() | 시작일/종료일만 체크, 중간 날짜 미검증 | 연박 예약 시 중간에 예약된 날짜 무시 |
| ReservationService.updateReservation() | 날짜 변경 시 가용성 재검증 누락 | 수정으로 인한 중복 예약 가능 |

### 7.3 코드 중복 (DRY 위반)

| 중복 코드 | 발생 위치 | 횟수 |
|-----------|----------|:---:|
| 날짜 유효성 검증 | createReservation, updateReservation, searchAvailableSites | 3회 |
| 확인 코드 검증 | cancelReservation, updateReservation | 2회 |
| 사이트 크기 판별 (A-, B-) | searchAvailableSites 내부 | 2회 |
| DTO 변환 로직 | searchReservations, updateReservation, getReservationsByNameAndPhone | 3회 |
| 가격 계산 로직 | createReservation, processReservationWithPayment, calculateReservationPrice | 3회 |

### 7.4 하드코딩된 값

| 항목 | 값 | 위치 |
|------|---|------|
| 기본 가격 | 80,000 / 50,000 / 60,000 | ReservationService (3곳) |
| 할증률 | 1.3 / 1.5 / 1.7 | ReservationService (2곳) |
| 성수기 기간 | 7~8월 | ReservationService |
| 예약 기간 제한 | 30일 | ReservationService |
| 전화번호 길이 | 10~11자리 | ReservationService, SiteService |

### 7.5 긴 메서드 및 깊은 중첩

| 메서드 | 라인 수 | 중첩 깊이 | 문제 |
|--------|:------:|:--------:|------|
| createReservation() | ~212줄 | 4단계 | 단일 책임 원칙 위반, 테스트 어려움 |
| searchAvailableSites() | ~75줄 | 2단계 | 로직 중복 |
| updateReservation() | ~73줄 | 2단계 | 검증 로직 중복 |

### 7.6 잠재적 Null 처리 문제

| 위치 | 문제 |
|------|------|
| ReservationResponse.from() | campsite가 null일 경우 NPE 발생 가능 |
| getReservationsByDate() | startDate, endDate null 체크 후 사용하나 일부 누락 |
| searchReservations() | phoneNumber null 체크는 있으나 다른 필드 누락 |

### 7.7 성능 이슈

| 위치 | 문제 | 영향 |
|------|------|------|
| getMonthlyCalendar() | 전체 예약 조회 후 메모리에서 필터링 | 데이터 증가 시 성능 저하 |
| getReservationsByDate() | findAll() 후 stream 필터링 | N+1 쿼리 가능성 |

---

## 8. 관련 문서

- [기능 명세서](./requirements.md)
- [API 상세 명세서](./api-specs.md)