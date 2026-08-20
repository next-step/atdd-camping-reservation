# 인수 조건

확정한 규칙과 예시와 이유. 예시는 전부 서버를 띄워 직접 호출해 받은 응답이다.

실측 기준일 **2026-08-14**, `POST http://localhost:8080/api/reservations`.
요청 본문은 `siteNumber`/`startDate`/`endDate`/`customerName`/`phoneNumber` 다섯 필드,
사이트는 시드 예약이 없는 것만 골라 충돌 검사를 피했다.

---

## T-1 예약 가능 기간

### AC-1 시작일은 오늘로부터 30일 이내여야 한다

**규칙** — `startDate`가 오늘로부터 31일 이후면 예약을 거부한다. 30일째는 허용한다
(`ChronoUnit.DAYS.between(today, startDate) <= 30`).

**예시** — 리드타임 31일. **지금은 통과하고, 이 티켓 이후 거부돼야 한다.**

```
POST {"siteNumber":"A-18","startDate":"2026-09-14","endDate":"2026-09-14",
      "customerName":"Tester","phoneNumber":"010-1234-5678"}

현재 실측 →  201 Created
{"id":7,"customerName":"Tester","startDate":"2026-09-14","endDate":"2026-09-14",
 "siteNumber":"A-18","status":"CONFIRMED","confirmationCode":"RU0AGT"}

이 티켓 이후 →  409 Conflict   (문구는 Q-2)
```

경계 반대편. 리드타임 30일은 지금도 통과하고, **이 티켓 이후에도 통과해야 한다.**

```
POST {"siteNumber":"A-17","startDate":"2026-09-13","endDate":"2026-09-13", ...}
현재 실측 →  201 Created   {"id":8,...,"confirmationCode":"Y997HP"}
```

상한이 아예 없다는 근거. 1년 뒤도 들어간다.

```
POST {"siteNumber":"A-16","startDate":"2027-08-14","endDate":"2027-08-14", ...}
현재 실측 →  201 Created   {"id":9,...,"confirmationCode":"YE3Y95"}
```

**이유** — 티켓 T-1의 신고문 "오늘로부터 30일 넘게 남은 날짜인데 예약이 됩니다"를
"리드타임 > 30일이면 거부"로 읽었다. 코드에는 `today`와 `startDate`의 거리를 재는 곳이
아예 없다. `today`가 쓰이는 자리는 과거 날짜 검사(AC-2) 하나뿐이고,
`ReservationService:96`의 `days > 30`은 `startDate`와 `endDate`의 거리(AC-3)라
리드타임과 무관하다. 즉 이 규칙은 지금 어디에도 구현돼 있지 않다.

---

### AC-2 시작일은 과거일 수 없다 (기존 동작, 유지)

**규칙** — `startDate`가 오늘보다 이전이면 거부한다. 오늘 당일은 허용한다.

**예시**

```
POST {"siteNumber":"A-14","startDate":"2026-08-13","endDate":"2026-08-13", ...}
현재 실측 →  409 Conflict
{"message":"과거 날짜로 예약할 수 없습니다."}
```

**이유** — AC-1을 넣으면서 `startDate`의 하한 검사를 건드리게 된다.
이 티켓에서 바꾸지 않는 동작이므로, 회귀 확인 기준으로 못을 박아 둔다.

---

### AC-3 숙박 길이는 최대 30일이다 (기존 동작, 유지)

**규칙** — `endDate - startDate`가 30일을 넘으면 거부한다. 30일째는 허용한다.
**AC-1과 다른 규칙이다.** AC-1은 오늘→시작일, AC-3은 시작일→종료일을 잰다.

**예시** — 경계 양쪽.

```
POST {"siteNumber":"A-20","startDate":"2026-08-15","endDate":"2026-09-14", ...}   숙박 30일
현재 실측 →  201 Created   {"id":6,...,"confirmationCode":"P528GX"}

POST {"siteNumber":"A-19","startDate":"2026-08-15","endDate":"2026-09-15", ...}   숙박 31일
현재 실측 →  409 Conflict
{"message":"예약 기간은 최대 30일입니다."}
```

**이유** — 신고자가 말한 "30일"과 코드가 재는 "30일"이 다른 값이라는 것이 이 티켓의
핵심이다. 두 규칙을 문서에서 분리해 두지 않으면 AC-1을 구현하며 AC-3을 덮어쓰거나
반대로 AC-3을 AC-1로 착각해 티켓을 닫게 된다. 이 규칙은 처음엔 요구사항에 근거가 없어
발견 티켓(구 T-2)으로 등록했는데, **2026-08-20 사양으로 확인받았다** — "체류 기간이
31일 이상이면 거절되는 규칙은 사양이 맞습니다." 발견 티켓은 종결하고 이 규칙을 확정한다.

---

## T-1 질문 (확정 못 함)

### Q-1 AC-1은 시작일만 보는가, 예약 기간 전체를 보는가

시작일이 창 안이어도 종료일은 창 밖으로 나갈 수 있다. 지금은 통과한다.

```
POST {"siteNumber":"A-13","startDate":"2026-09-13","endDate":"2026-10-13", ...}
     시작일 리드타임 30일(창 안), 종료일 리드타임 60일(창 밖), 숙박 30일
현재 실측 →  201 Created   {"id":11,...,"confirmationCode":"DFW8QX"}
```

AC-1을 시작일 기준으로만 쓰면 이 예약은 계속 통과한다. 신고자가 "30일 넘게 남은 날짜"라고
할 때 그 날짜가 시작일만 뜻하는지, 예약이 점유하는 모든 날짜를 뜻하는지 티켓은 말하지 않는다.

**AC-1 구현 후 재실측 (2026-08-14).** 같은 날짜 `2026-09-14`(리드타임 31일)를
시작일로 넣으면 막히고, 종료일로 넣으면 통과한다. 그리고 그 날짜는 실제로 점유된다.

```
POST {"siteNumber":"A-9","startDate":"2026-09-14","endDate":"2026-09-14", ...}
  →  409   {"message":"오늘로부터 30일 이내만 예약할 수 있습니다."}

POST {"siteNumber":"A-8","startDate":"2026-08-15","endDate":"2026-09-14", ...}
  →  201   {"id":9,...,"endDate":"2026-09-14","confirmationCode":"H9OAQI"}

GET  /api/sites/A-8/availability?date=2026-09-14
  →  200   {"date":"2026-09-14","available":false,"siteNumber":"A-8"}
```

즉 "30일 넘게 남은 날짜에 예약이 잡힌다"는 신고 증상 자체는 이 경로로 아직 재현된다.
Q-1이 정해지기 전까지 T-1을 닫을지는 이 실측을 보고 판단해야 한다.

**무엇이 확인되면 정리되는가** — 신고 원문 또는 예약 정책 문서에서 "예약 가능 기간"의
기준일이 시작일인지 확인되면 정리된다. 예약 기간 전체 기준으로 정해지면 AC-1의 규칙 문장과
구현(`ReservationService`의 `leadDays`)을 `endDate` 기준으로 함께 고쳐야 한다.

### Q-2 거부 응답의 문구와 상태 코드

AC-1 위반의 상태 코드는 409로 잡았다. 컨트롤러가 생성 실패를 전부 409로 매핑하는 것을
AC-2·AC-3 실측으로 확인했기 때문이고, 이 티켓에서 그 매핑을 바꾸지 않는다(**F-4**).
다만 `{"message": ...}`의 문구는 확정되지 않았다. 기존 문구와 결이 맞으려면
"오늘로부터 30일 이내만 예약할 수 있습니다." 정도인데, 인수 테스트가 문구를 단언할지
상태 코드만 단언할지도 함께 정해야 한다.
**무엇이 확인되면 정리되는가** — 문구를 단언 대상으로 삼을지 `test-guide.md`에서 정하면 정리된다.

---

## T-2 전화번호 필수

실측 기준일 **2026-08-20**, 같은 엔드포인트(`POST /api/reservations`).
사이트는 시드·기존 테스트가 안 쓰는 B-1~B-4를 썼다.

### AC-4 전화번호는 필수다

**규칙** — `phoneNumber`가 누락(null)이거나 공백을 지운 뒤 빈 문자열이면 예약을 거부한다
(`phoneNumber == null || phoneNumber.trim().isEmpty()`).
요구사항의 "없으면"은 세 형태로 들어온다 — 필드 누락, 빈 문자열 `""`, 공백만 `"   "`.
셋 다 없음으로 치고 거부한다.

**예시** — 셋 다 **지금은 통과하고, 이 티켓 이후 거부돼야 한다.**
저장까지 된다는 것은 응답의 `phoneNumber` 값이 보여 준다.

```
POST {"siteNumber":"B-1","startDate":"2026-08-25","endDate":"2026-08-25",
      "customerName":"Tester"}                          ← phoneNumber 필드 자체가 없음
현재 실측 →  201 Created
{"id":6,...,"phoneNumber":null,"status":"CONFIRMED","confirmationCode":"A3LFL8"}

POST {"siteNumber":"B-2", ..., "phoneNumber":""}
현재 실측 →  201 Created   {"id":7,...,"phoneNumber":""}

POST {"siteNumber":"B-3", ..., "phoneNumber":"   "}
현재 실측 →  201 Created   {"id":8,...,"phoneNumber":"   "}

이 티켓 이후 →  409 Conflict   (문구는 Q-3)
```

경계 반대편. 유효한 전화번호는 지금도 통과하고, **이 티켓 이후에도 통과해야 한다.**

```
POST {"siteNumber":"B-4", ..., "phoneNumber":"010-1234-5678"}
현재 실측 →  201 Created   {"id":9,...,"phoneNumber":"010-1234-5678"}
```

**이유** — 신고문 "전화번호를 안 넣었는데 예약이 완료됐습니다"와 요구사항 "전화번호는
필수다. 없으면 예약할 수 없다". 코드 근거: `ReservationService:125`의 전화번호 검증이
`if (phoneNumber != null && !phoneNumber.trim().isEmpty())` **안에** 있어서, 없거나 빈
값이면 형식 검사를 통째로 건너뛰고 그대로 저장한다. 검증이 "있으면 형식을 본다"이지
"없으면 거부한다"가 아니다. 공백만을 없음으로 치는 근거는 이 코드베이스의 필수 검사
관례다 — 사이트 번호(`:74`)·이름(`:113`)·확인 코드(`:370`)가 전부 `trim().isEmpty()`를
없음으로 본다. 형식(하이픈 제거 후 10~11자리 숫자)은 이 티켓이 아니다 — **F-3** 그대로.

**구현 후 재실측 (2026-08-20, 서버 재기동).** 세 형태 전부 거부되고 유효 번호는 그대로 통과한다.

```
누락 / "" / "   "  →  409   {"message":"전화번호를 입력해주세요."}   (셋 다 동일)
"010-1234-5678"    →  201   {"id":6,...,"phoneNumber":"010-1234-5678","status":"CONFIRMED"}
```

### Q-3 거부 응답의 문구

상태 코드는 409로 잡았다. 컨트롤러가 생성 실패를 전부 409로 매핑하고(**F-4**), 이번에도
그 매핑을 바꾸지 않는다. 문구는 조회 경로(`ReservationService:447`)에 이미 있던
"전화번호를 입력해주세요."를 그대로 썼다 — 기존과 결은 맞지만 사양으로 확인받은 것은
아니므로 인수 테스트는 상태 코드만 단언한다.
**무엇이 확인되면 정리되는가** — 거부 문구 사양이 확인되면 정리된다. Q-2와 같은 성격이다.

---

## T-3 취소된 자리의 재예약

실측 기준일 **2026-08-20**, 같은 엔드포인트. 취소는 `DELETE /api/reservations/{id}?confirmationCode=`.
사이트는 시드·기존 테스트가 안 쓰는 A-5·A-7·B-5를 썼다.

### AC-5 동일 사이트·동일 기간에 중복 예약은 불가하다 (기존 동작, 유지)

**규칙** — 같은 사이트의 겹치는 기간에 취소되지 않은 예약이 있으면 거부한다.

**예시** — 같은 자리를 두 번. 두 번째가 거부된다. 이 동작은 이 티켓 이후에도 같아야 한다.

```
POST {"siteNumber":"A-5","startDate":"2026-08-28","endDate":"2026-08-29","customerName":"First",...}
현재 실측 →  201 Created   {"id":7,...,"status":"CONFIRMED","confirmationCode":"33MIDJ"}

POST {"siteNumber":"A-5","startDate":"2026-08-28","endDate":"2026-08-29","customerName":"Second",...}
현재 실측 →  409 Conflict
{"message":"해당 기간에 이미 예약이 존재합니다."}
```

**이유** — 요구사항 첫 줄 그대로다. AC-6(취소 제외)을 넣으며 중복 체크 쿼리를 건드리게
되므로, 살아 있는 예약의 중복 거부가 안 깨진다는 못을 박아 둔다. 문구는 실측으로 받았고
이 티켓에서 바꾸지 않으므로 단언 대상이다.

### AC-6 취소된 예약은 중복 체크에서 제외된다

**규칙** — `status`가 `CANCELLED` 또는 `CANCELLED_SAME_DAY`인 예약은 중복 체크에서
세지 않는다. 취소된 자리에는 같은 사이트·같은 기간이라도 새 예약이 된다.
(상태값은 `cancelReservation`이 만드는 두 가지가 전부다 — 당일 취소만 이름이 다르다.)

**예시** — 신고 시나리오 그대로. **지금은 마지막 걸음이 거부되고, 이 티켓 이후 성공해야 한다.**

```
POST   {"siteNumber":"B-5","startDate":"2026-08-26","endDate":"2026-08-27",...}
  →  201   {"id":6,...,"status":"CONFIRMED","confirmationCode":"2K0JE5"}

DELETE /api/reservations/6?confirmationCode=2K0JE5
  →  200   {"message":"예약이 취소되었습니다."}

GET    /api/reservations/6
  →  200   {"id":6,...,"status":"CANCELLED",...}      ← 취소는 행 삭제가 아니라 상태 변경

POST   {"siteNumber":"B-5","startDate":"2026-08-26","endDate":"2026-08-27","customerName":"Tester2",...}
현재 실측 →  409 Conflict   {"message":"해당 기간에 이미 예약이 존재합니다."}   ← 신고 증상

이 티켓 이후 →  201 Created
```

당일 취소도 같다. 시작일 당일에 취소하면 status가 `CANCELLED_SAME_DAY`가 되는데,
이 상태도 자리를 막는다 — 규칙이 두 상태를 모두 제외해야 하는 근거.

```
POST A-7 오늘~오늘 → 201 (id 8) → DELETE → 200 → status: CANCELLED_SAME_DAY
POST A-7 오늘~오늘 → 현재 실측 409 {"message":"해당 기간에 이미 예약이 존재합니다."}
이 티켓 이후 → 201
```

**이유** — 신고문과 요구사항 둘째 줄 그대로다. 코드 근거: 취소(`cancelReservation:309-325`)는
행을 지우지 않고 status만 바꾸는데, 중복 체크
(`ReservationService:144` → `ReservationRepository:21`의
`existsByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual`)는 status 조건이
아예 없어 취소된 행도 그대로 센다. 엔티티 `@PrePersist`가 status를 `CONFIRMED`로
채우고 시드도 전부 `CONFIRMED`라, 취소 상태 두 가지를 제외하면 나머지가 전부 산 예약이다.

### Q-4 "동일 기간"의 범위 — 부분 겹침도 중복인가

요구사항은 "동일 사이트, 동일 기간"이라고만 말한다. 코드는 기간이 **하루라도 겹치면**
거부한다(경계 포함 겹침 판정). 문자 그대로 "동일 기간"만 막는 것이라면 부분 겹침은
허용해야 하지만, 겹침 거부가 상식적이라 이 티켓은 기존 판정(겹침 전부)을 유지한다.
**무엇이 확인되면 정리되는가** — 부분 겹침 허용이 사양인지 확인되면 정리된다.

발견 — 취소된 자리를 조회는 여전히 "불가"로 안내한다. **F-10**으로 등록했고 이번 티켓
(중복 체크 = 생성 경로)에서 다루지 않는다.

**구현 후 재실측 (2026-08-20, 서버 재기동).** 신고 시나리오가 끝까지 통과하고,
산 예약의 중복 거부는 그대로다.

```
POST B-5 → 201 → DELETE → 200 → status CANCELLED → 같은 자리 POST → 201 (id 7)
POST A-5 → 201 → 같은 기간 POST → 409 {"message":"해당 기간에 이미 예약이 존재합니다."}
```

---

## F-6 생성·수정 응답의 createdAt

실측 기준일 **2026-08-20**. 발견 티켓 처리 — 신고가 아니라 내부 불일치라 정책 확인이
필요 없다: 같은 리소스는 조회 경로와 무관하게 같은 데이터를 줘야 한다.

### AC-7 생성·수정 응답의 createdAt은 단건 조회와 같은 저장 값이다

**규칙** — `POST`·`PUT`(그리고 검색 경로) 응답의 `createdAt`이 null이 아니고,
`GET /api/reservations/{id}`가 주는 저장 값과 같다.

**예시** — 같은 리소스(id 9)인데 경로마다 값이 다르다. **지금은 null이고, 이 티켓 이후 채워져야 한다.**

```
POST {"siteNumber":"B-8","startDate":"2026-08-30","endDate":"2026-08-31",...}
현재 실측 →  201   {"id":9,...,"createdAt":null}

GET  /api/reservations/9
현재 실측 →  200   createdAt: "2026-08-20T14:25:08.906025"      ← 저장은 돼 있다

PUT  /api/reservations/9?confirmationCode=B0BDEG {"customerName":"CreatedAt2"}
현재 실측 →  200   {...,"createdAt":null}

GET  /api/reservations/my?name=CreatedAt2&phone=010-7777-8888
현재 실측 →  200   [{...,"createdAt":null}]

이 티켓 이후 →  네 경로 모두 GET 단건과 같은 값
```

**이유** — 엔티티 `@PrePersist`가 `createdAt`을 저장하고 `ReservationResponse.from()`은
그것을 채우는데, `createReservation`·`updateReservation`·`searchReservations`·
`getReservationsByNameAndPhone` 네 곳만 `from()` 대신 손 매핑을 쓰며 `createdAt`을
빠뜨린다. 손 매핑과 `from()`의 나머지 여덟 필드는 동일하므로, 고치는 방향은 네 곳이
`from()`을 쓰게 하는 것이다 — 필드를 하나 더 베끼는 것은 다음 필드에서 또 어긋난다.

**구현 후 재실측 (2026-08-20, 서버 재기동).** 같은 리소스(id 6)의 네 경로가 같은 값을 준다.

```
POST → "createdAt":"2026-08-20T14:28:59.9123999"   (나노초 표기만 GET과 다름 — 같은 시각)
GET  → "createdAt":"2026-08-20T14:28:59.9124"
PUT  → "createdAt":"2026-08-20T14:28:59.9124"
/my  → "createdAt":"2026-08-20T14:28:59.9124"
```
