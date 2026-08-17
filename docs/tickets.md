T-1 30일 넘게 남은 날짜인데 예약이 됨

내용: 고객센터 신고 — "오늘로부터 30일 넘게 남은 날짜인데 예약이 됩니다."

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

