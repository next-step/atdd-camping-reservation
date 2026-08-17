T-1 30일 넘게 남은 날짜인데 예약이 됨

내용: 고객센터 신고 — "오늘로부터 30일 넘게 남은 날짜인데 예약이 됩니다."

---

T-2 전화번호 없이 예약이 완료됨

내용: 고객센터 신고 — "전화번호를 안 넣었는데 예약이 완료됐습니다."

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

