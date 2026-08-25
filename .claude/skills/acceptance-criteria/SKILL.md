---
name: acceptance-criteria
description: 티켓 번호(T-N)를 받아 docs/tickets.md에서 요구사항을 확인하고 docs/plan.md
  "1. 인수 조건" 절차에 따라 관련 코드 흐름을 분석한다. verify-api.sh로 서버를 띄워 API를
  직접 호출해 실제 응답을 확보하고, 인수 조건 후보(규칙/이유/Given-When-Then)와 사양-버그
  판단 질문, 요구사항 관련 코드 강제사항을 정리해 알려준다. "티켓 T-N 인수 조건 뽑아줘",
  "인수조건 분석해줘", "이 버그 사양인지 봐줘" 같은 요청에 사용.
---

이 스킬은 `docs/plan.md`의 "1. 인수 조건" 단계를 실행한다. 절차의 권위 있는 출처는
언제나 `docs/plan.md`와 `docs/principles.md`다 — 이 저장소마다 고쳐 쓰는 뼈대 문서이므로,
아래 요약이 아니라 **매번 그 두 파일을 다시 읽고** 따른다. 이 SKILL.md는 그 절차를 이
저장소의 실제 파일 경로에 맞게 구체화한 것뿐이다.

경로는 모두 저장소 루트(`atdd-camping-reservation/`) 기준이다.

## 입력

티켓 번호 하나 (`T-N`). 없으면 사용자에게 묻는다.

## 절차

1. **티켓 조회** — `docs/tickets.md`에서 `T-N` 항목을 찾는다(형식: `T-N 제목` /
   `내용: ...` / 구분선 `---`). 이것이 이 실행에서 다룰 요구사항이다.
2. **뼈대 문서 재확인** — `docs/plan.md`의 "1. 인수 조건" 절과 `docs/principles.md`
   전체를 읽는다. 특히 "실측에 근거한 판정"(코드와 AI 설명은 가설, 경계는 직접 호출해
   확인)과 "모르는 것은 질문으로"(답을 만들지 않고 질문+티켓으로 남김) 원칙을 이번
   실행에 적용한다.
3. **기존 산출물 확인** — `docs/acceptance-criteria.md`를 읽어 이 요구사항이 이미
   규칙으로 정리돼 있는지 확인한다(중복 후보를 제시하지 않기 위해). 관련 테스트가
   있는지 `src/test/java/com/camping/legacy/acceptance/`도 살펴본다.
4. **코드 흐름 탐색** — 티켓이 다루는 요구사항과 관련된 경로를 Controller → Service →
   도메인 순으로 따라간다. 예약 관련 티켓이면:
   - `src/main/java/com/camping/legacy/controller/ReservationController.java` —
     `POST /api/reservations`(생성), `PUT /api/reservations/{id}`(수정),
     `DELETE /api/reservations/{id}`(취소), `GET /api/reservations/my`(이름+전화번호
     조회), `GET /api/reservations/calendar`(캘린더) 등 엔드포인트를 여기서 확인한다.
   - `src/main/java/com/camping/legacy/service/ReservationService.java` — 실제 검증/
     분기 로직은 대부분 여기 있다(`createReservation`, `updateReservation`,
     `cancelReservation` 등). **티켓에 적힌 줄 번호는 참고용일 뿐, 실제 파일과 어긋나
     있을 수 있으니 직접 열어 확인한다.**
   - `src/main/java/com/camping/legacy/domain/Reservation.java`,
     `Campsite.java` — 필드와 기본값(`@PrePersist`로 `status` 기본값 `CONFIRMED` 부여
     등).
   - `src/main/java/com/camping/legacy/util/ValidationUtils.java` — 이름/전화번호/날짜
     검증 메서드가 정의는 돼 있지만 **`ReservationService`에서 실제로 호출되지 않는
     죽은 코드**다. 여기 있는 규칙(예: 전화번호가 `01`로 시작해야 함)을 실제 동작으로
     착각하지 않는다 — 실제 동작은 서비스 코드의 인라인 검증을 따른다.
5. **직접 호출로 실측** — `.claude/skills/acceptance-criteria/verify-api.sh`로 서버를
   띄우고 경계 케이스를 실제로 호출해 상태 코드·응답 본문을 확보한다(아래 "verify-api.sh
   사용법" 참고). 코드를 읽고 예상한 값이 아니라 이 실제 응답을 근거로 쓴다. H2는
   인메모리(`ddl-auto: create-drop`, `sql.init.mode: always`)라 서버를 새로 띄울 때마다
   `data.sql` 시드로 초기화되므로 여러 번 실험해도 안전하다.
6. **정리해서 사용자에게 제시** — 아래 "출력" 형식을 따른다. 파일에 바로 쓰지 않는다.

## 출력

채팅으로 아래 네 가지를 제시한다:

1. **코드 흐름 요약** — 사용자가 코드를 따라가며 보기 쉬운 순서로. 파일:줄 형태로
   위치를 짚어준다.
2. **사양/버그 질문** — 코드 동작이 요구사항과 어긋나는데 의도인지 버그인지 애매하면
   AskUserQuestion으로 물어본다. **사용자가 버그라고 판단하면 그 자리에서
   `docs/tickets.md`에 기존 형식(`T-N 제목` / `내용: ...` / `---`)대로 새 티켓을
   추가한다.** 판단이 안 서면 사용자 말대로 "질문"으로만 남기고 파일 수정은 하지 않는다.
3. **`acceptance-criteria.md` 후보** — 기존 4개 블록과 같은 형식으로 제시한다:
   `규칙` → `이유` → `Given` / `When ... Then`(4단계에서 직접 호출해 받은 실제 HTTP
   상태 코드와 에러 메시지) → `질문`(경계가 안 정해졌으면). **이 파일에 직접 쓰지
   않는다** — 사용자가 직접 작성하므로 후보만 제시한다.
4. **요구사항 관련 코드 강제사항** — 티켓의 요구사항과 직접 관련된, 코드가 이미
   강제하고 있는 제약을 별도로 짚어준다(예: 다른 규칙에 딸려 나오는 부수 효과인지,
   요구사항이 요청한 것과 정확히 일치하는지).

## 하지 않는 것

- `docs/acceptance-criteria.md`를 직접 수정하지 않는다.
- 커밋하지 않는다(사용자가 말할 때만).
- 검증에 쓴 서버는 확인이 끝나면 `verify-api.sh stop`으로 반드시 정리한다.

## verify-api.sh 사용법

경로: `.claude/skills/acceptance-criteria/verify-api.sh` (bash, Git Bash에서 실행,
저장소 루트 기준 상대 경로 계산이 내장돼 있어 어디서 실행해도 된다).

```bash
.claude/skills/acceptance-criteria/verify-api.sh start
# 포트 8081 로 서버 기동 중 (UTF-8, 로그: .claude/skills/acceptance-criteria/.server.log) ...
# 준비 완료: http://localhost:8081

.claude/skills/acceptance-criteria/verify-api.sh call GET /api/reservations
# [{"id":1,"customerName":"홍길동",...}]
# 200

.claude/skills/acceptance-criteria/verify-api.sh call POST /api/reservations \
  '{"siteNumber":"A-10","startDate":"2026-08-25","endDate":"2026-08-27","customerName":"테스트","phoneNumber":"010-9999-0003"}'
# {"id":8,"customerName":"테스트",...}
# 201

.claude/skills/acceptance-criteria/verify-api.sh call DELETE "/api/reservations/8?confirmationCode=NVSFMI"
# {"message":"예약이 취소되었습니다."}
# 200

.claude/skills/acceptance-criteria/verify-api.sh stop
# 포트 8081 의 프로세스(pid 18144) 종료
```

`call`은 `<METHOD> <PATH> [JSON_BODY]`를 받는다. 응답 본문 다음 줄에 HTTP 상태 코드가
그대로 찍힌다 — 이게 인수 조건 예시에 쓸 실측값이다.

## Gotchas

- **JVM 인코딩**: JDK17 + 한글 Windows(코드페이지 949)에서 `-Dfile.encoding=UTF-8`
  없이 `bootRun`을 실행하면 `data.sql`의 한글이 깨져서 저장된다(`docs/plan.md` 4단계에도
  명시). `-D`를 `gradlew`에 직접 주면 Gradle 데몬 JVM에만 적용되고 `bootRun`이 포크하는
  자식 JVM에는 전달되지 않는다 — `verify-api.sh`는 `JAVA_TOOL_OPTIONS`로 넘겨서 해결한다.
- **curl 인자로 넘긴 한글이 깨짐(별도 문제)**: 이 환경(Git Bash → curl.exe)에서 한글이
  포함된 JSON을 `curl -d '...'` 인자로 직접 넘기면 `Invalid UTF-8 middle byte` 400
  에러가 난다 — bash 내부(`printf`)에서는 바이트가 멀쩡한데, Git Bash가 non-MSYS
  실행 파일(curl.exe)에 인자를 넘기는 과정에서 깨진다. `verify-api.sh`의 `call`은 body를
  임시 파일에 UTF-8로 써서 `--data-binary @file`로 넘기는 방식으로 이미 우회한다 — 이
  스크립트를 거치지 않고 curl을 직접 칠 때는 같은 함정을 조심한다.
- **`ValidationUtils`는 죽은 코드**: import는 돼 있지만 `ReservationService`의 실제
  검증 로직(`createReservation`/`updateReservation`)은 전부 인라인 코드이고
  `ValidationUtils`를 호출하지 않는다. 거기 있는 규칙(전화번호 `01` 시작 필수 등)을
  실제 사양으로 착각하지 않는다.
- **티켓의 코드 줄 번호는 어긋날 수 있다**: 예를 들어 T-5는 "95~98행"이라 적혀 있지만
  실제로는 97~101행이다. 티켓을 읽을 때 줄 번호를 믿지 말고 파일을 열어 직접 찾는다.
- **시드 데이터에 "버그 재현용" 예약이 섞여 있다**: `src/main/resources/data.sql`의
  마지막 두 예약(박민수, 정수진)은 주석으로 "과거 날짜 예약이 가능한 버그 확인용"이라고
  명시돼 있다 — 정상 시드로 착각해 다른 검증에 끌어쓰지 않는다.
- **`data.sql` 상단 주석과 실제 INSERT 문이 불일치**: 주석은 "50개: A-1~A-50"이라
  적혀 있지만 실제로는 A-1~A-20, B-1~B-15 총 35개만 들어간다. 이런 주석-코드 불일치
  자체가 "사양인지 오기(誤記)인지" 질문거리가 될 수 있다.
