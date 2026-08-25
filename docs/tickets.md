T-1 30일 넘게 남은 날짜인데 예약이 됨
내용: 고객센터 신고 — "오늘로부터 30일 넘게 남은 날짜인데 예약이 됩니다."
---
T-2 전화번호 없이 예약이 완료됨
내용: 고객센터 신고 — "전화번호를 안 넣었는데 예약이 완료됐습니다."
---
T-3 취소한 예약의 자리에 다시 예약되지 않음
내용: 고객센터 신고 — "예약을 취소했는데 같은 날짜에 다시 예약하려니 이미 예약이 있다고 나옵니다."
---
T-4 예약 수정으로 30일 제한 우회 가능
내용: `updateReservation`에는 T-1 규칙(및 체류기간 규칙)이 없어 수정으로 우회 가능 — 적용 여부 미정.
---
T-5 숙박 기간 30일 제한
내용: `createReservation`(95~98행)이 체류 기간 ≤30일을 강제 — T-1의 "오늘로부터 30일"과 이름이 겹쳐 혼동 주의, 유지할지 확정 필요.
---
T-6 예약자 이름 길이 2~20자 제한
내용: `createReservation`(110~114행)이 강제. 요구사항에 넣을지 확정 필요.
---
T-7 전화번호 형식 제한 ㅜ
내용: `createReservation`(118~131행)은 10~11자리 숫자만 확인(01 시작 조건 없음), 안 쓰이는 `ValidationUtils.isValidPhoneNumber`는 01 시작까지 요구 — 둘 중 뭐가 맞는지 확정 필요.
---
T-8 사이트·기간 중복 예약 금지
내용: `createReservation`(137~141행)이 강제. 의도된 도메인 규칙으로 보이나 요구사항 문서에 명문화 필요.
---
T-9 오늘+30일째가 "30일 이내"에 포함되는지 확정 필요
내용: acceptance-criteria.md 질문 — 정확히 30일째는 포함인가?
---
T-10 당일(오늘) 시작 예약 허용 여부 확정 필요
내용: acceptance-criteria.md 질문 — 시작일이 현재(오늘)인 경우는 예약이 가능한가?
---
T-11 시작일=종료일(0박) 허용 여부 확정 필요
내용: acceptance-criteria.md 질문 — 시작일과 종료일이 같을 수 있는가?
---
T-12 확인 코드 중복 시 처리 없음
내용: `createReservation`(227~238행)이 확인 코드 생성 시 DB 유니크 제약도, 중복 체크도 없음 — 중복 나면 어떻게 할지 미정.
---
T-13 예약 수정으로 전화번호를 검증 없이 지울 수 있음
내용: `updateReservation`(416~418행)은 `phoneNumber`가 null이 아니면 형식 검증 없이 그대로 저장 — 빈 문자열("")을 보내면 기존 전화번호를 지울 수 있음. T-4(수정으로 T-1 규칙 우회 가능)와 같은 결의 문제, T-2(전화번호 필수화) 처리 시 함께 볼 것.
---
T-14 예약 수정으로 사이트·기간 중복 예약 금지(T-8) 우회 가능
내용: T-3 조사 중 발견. `updateReservation`(363~436행)은 사이트나 날짜를 바꿀 때 `createReservation`에 있는 중복 예약 검사(`existsByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual`, 142~146행)를 전혀 하지 않음 — PUT으로 이미 다른 예약이 있는 사이트·기간으로 수정해도 그대로 통과됨. T-4(수정으로 30일 제한 우회 가능)와 같은 결의 문제.
---
T-15 이미 취소된 예약을 다시 취소할 수 있음
내용: T-3 조사 중 발견. `cancelReservation`(307~323행)은 확인 코드 일치 여부만 확인하고 현재 `status`는 보지 않음 — 이미 `CANCELLED`/`CANCELLED_SAME_DAY` 상태인 예약도 다시 취소 요청을 보내면 그대로 통과되어 `status`와 취소 판정 로직(당일 여부)이 다시 실행됨. 방어(예: 이미 취소된 예약이면 거부) 필요 여부 확정 필요.
---
T-16 당일 취소(CANCELLED_SAME_DAY) 상태도 재예약 허용 여부 확정 필요
내용: acceptance-criteria.md 질문 — 당일 취소가 된 사이트도 예약이 가능한가? 현재 수정된 코드는 CONFIRMED만 충돌로 보므로 CANCELLED_SAME_DAY도 재예약이 되지만, 이것이 의도된 동작인지 확정 필요. T-3과 관련.
---
T-17 취소된 예약이 있으면 조회에서 계속 예약 불가로 나옴
내용: T-3 구현 중 발견. `existsByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual`(status 필터 없음)를 쓰는 곳이 `createReservation` 말고 두 곳 더 있음 — `SiteService.isAvailable`(159행, `GET /api/sites/{siteNumber}/availability`로 실제 노출됨)과 `ReservationService.checkAvailability`(1042~1052행, 컨트롤러에서 안 씀). `createReservation`과 같은 원인으로, 취소된 예약이 있는 사이트·날짜를 계속 "예약 불가(available: false)"로 응답할 것으로 보임 — 특히 `SiteService.isAvailable`은 실사용자에게 노출된 API라 확인 필요.
---