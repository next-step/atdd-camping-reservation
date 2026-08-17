T-1 30일 넘게 남은 날짜인데 예약이 됨

내용: 고객센터 신고 — "오늘로부터 30일 넘게 남은 날짜인데 예약이 됩니다."

---

T-2 전화번호 없이 예약이 완료됨

내용: 고객센터 신고 — "전화번호를 안 넣었는데 예약이 완료됐습니다."

---

T-3 취소한 예약의 자리에 다시 예약되지 않음

내용: 고객센터 신고 — "예약을 취소했는데 같은 날짜에 다시 예약하려니 이미 예약이 있다고 나옵니다."

---

T-4 예약 수정 시 체류 기간 30일 제한이 적용되지 않음

내용: `createReservation`에는 체류 기간(startDate~endDate)이 30일을 초과하면 거부하는 체크가 있으나,
`updateReservation`에는 동일한 체크가 없다. 수정 경로에서 30일을 초과하는 체류 기간으로 변경해도 허용된다.

---

T-5 확인 코드 생성 로직 중복 — generateConfirmationCode() 데드코드

내용: `generateConfirmationCode()`(line 661, 작성자: boorownie)는 확인 코드를 생성하는 private 메서드이나
어디서도 호출되지 않는다. 실제 예약 생성(line 233–243)에서는 동일한 로직을 인라인으로 재구현하고 있다.
메서드를 실제로 호출하도록 교체하거나, 데드코드를 제거해야 한다.

---

T-6 전화번호 중복(유일성) 검증 로직이 없음

내용: T-2 작업 중 확인. `phoneNumber` 컬럼(`Reservation.java:37`)에는 `unique` 제약이 없고,
`ReservationRepository`에도 전화번호 기준 조회/중복 체크 메서드(`existsByPhoneNumber` 등)가 없다.
`createReservation`/`updateReservation` 어디에도 같은 전화번호로 이미 예약이 있는지 확인하는 로직이 없어
동일한 전화번호로 몇 건이든 예약을 만들 수 있다. 전화번호가 유일해야 하는지는 요구사항이 침묵하므로
이 티켓에서는 판단하지 않는다.

---

T-7 전화번호 형식(유효성) 검증이 인수 조건으로 명시되어 있지 않음

내용: T-2 작업 중 확인. `createReservation`(`ReservationService.java:122-137`)에는 phoneNumber 형식
검증(`-` 제거 후 길이 10~11자리, 숫자만)이 이미 있고 정상 동작하지만, `acceptance-criteria.md`에는
이 규칙이 자기 `요구사항` 절·`이유` 없이 T-2 요구사항 1의 예시 하나로만 "참고용"으로 인용돼 있을 뿐,
정식 인수 조건으로 명시된 적이 없다. 또한 `updateReservation`(`ReservationService.java:429-431`)에는
형식 검증 자체가 아예 없어 PUT으로는 `"abc"` 같은 값도 그대로 저장된다(T-2 작업 중 실측 확인).
전화번호 형식이 어떠해야 하는지, PUT에도 동일하게 적용해야 하는지는 요구사항이 침묵하므로
이 티켓에서는 판단하지 않는다.

---

T-8 예약 수정(PUT) 시 사이트/기간 중복 체크가 아예 없음

내용: T-3 작업 중 확인. `createReservation`(`ReservationService.java:140-147`, STEP 4)에는
`existsByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual`로 같은 사이트·겹치는 기간의
예약이 있는지 확인하는 체크가 있으나, `updateReservation`(`ReservationService.java:364-455`)에는
이 체크가 전혀 없다. 확인 코드·날짜 유효성·이름·전화번호만 검증한 뒤 바로
`reservation.setCampsite/setStartDate/setEndDate`로 저장한다(427-432행).

실측: 시드 예약 id=2(김철수, A-3, 2026-08-31~2026-09-01, CONFIRMED)가 있는 상태에서, 별도 예약
id=9(A-12, 2026-08-18~2026-08-19)를 `PUT /api/reservations/9?confirmationCode=SP65GD`로
`{"siteNumber":"A-3","startDate":"2026-08-31","endDate":"2026-09-01"}`로 수정 시도하면 HTTP
200으로 성공하며 완전히 겹치는 이중 예약이 그대로 저장된다.

수정 경로에도 생성과 동일한 중복 체크를 적용해야 하는지, 적용한다면 자기 자신(수정 대상 예약)을
겹침 판정에서 어떻게 제외할지는 요구사항이 침묵하므로 이 티켓에서는 판단하지 않는다.

---

T-9 당일 취소(CANCELLED_SAME_DAY)된 예약 자리의 재예약 가능 여부가 정책으로 정해지지 않음

내용: T-3 작업 중 확인. `cancelReservation`(`ReservationService.java:308-324`)은 취소 시점에
`startDate`가 오늘이면 `"CANCELLED_SAME_DAY"`, 아니면 `"CANCELLED"`로 상태를 나눠 저장한다. T-3은
신고 원문("취소했는데 같은 날짜에 다시 예약하려니 이미 예약이 있다고 나온다")을 따라 `"CANCELLED"`
상태만 중복 체크에서 제외하도록 범위를 좁혔고, `"CANCELLED_SAME_DAY"`는 현재 동작(재예약 불가,
HTTP 409) 그대로 남겨뒀다.

당일 취소된 자리를 같은 날 바로 재예약 가능하게 할지(예: 노쇼 방지, 당일 취소 위약금 정책과 얽힐
수 있음)는 요구사항이 침묵하므로 이 티켓에서는 판단하지 않는다.

---

