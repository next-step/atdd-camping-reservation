---
name: acceptance-test
description: docs/acceptance-criteria.md의 규칙 블록을 읽어 아직 테스트가 없는 것을 찾고,
  docs/plan.md "2. 인수 테스트" 절차에 따라 인수 테스트 코드를 작성한 뒤 run-tests.sh로 돌려
  지금 실패하는지 확인한다. 규칙에 남은 "질문"은 docs/tickets.md에 티켓으로 남긴다. "인수
  테스트 만들어줘", "acceptance-criteria 기반으로 테스트 짜줘", "테스트 초안 만들어줘" 같은
  요청에 사용.
---

이 스킬은 `docs/plan.md`의 "2. 인수 테스트" 단계를 실행한다. 절차의 권위 있는 출처는
언제나 `docs/plan.md`와 `docs/principles.md`다 — 이 저장소마다 고쳐 쓰는 뼈대 문서이므로,
아래 요약이 아니라 **매번 그 두 파일을 다시 읽고** 따른다. 이 SKILL.md는 그 절차를 이
저장소의 실제 파일·컨벤션에 맞게 구체화한 것뿐이다.

경로는 모두 저장소 루트(`atdd-camping-reservation/`) 기준이다.

## 입력

없음 — `docs/acceptance-criteria.md` 전체가 대상이다. 사용자가 특정 규칙만 짚어주면
그 규칙만 다룬다.

## 절차

1. **뼈대 문서 재확인** — `docs/plan.md`의 "2. 인수 테스트" 절과 `docs/principles.md`
   전체를 읽는다.
2. **`acceptance-criteria.md` 파싱** — `---`로 구분된 각 블록을 읽는다. 블록 구조는
   `규칙` → `이유` → `Given` / `When ... Then`(여러 줄) → (있으면) `질문`.
3. **기존 커버리지 확인** — `src/test/java/com/camping/legacy/acceptance/`의 기존
   테스트를 읽고, 각 블록의 Given/When/Then이 이미 테스트로 있는지 확인한다. 메서드
   이름이 비슷해 보여도 실제로 그 규칙의 조건과 기대값을 단언하는지 코드를 읽고
   판단한다 — 파일 존재만으로 커버됐다고 넘기지 않는다.
4. **초안 작성** — 커버 안 된 블록마다 When/Then 한 줄당 테스트 메서드 하나를 만든다.
   기존 파일의 컨벤션을 그대로 따른다:
   - `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@LocalServerPort` + `RestAssured.port`
     (기존 클래스의 `@BeforeEach setUp()` 그대로).
   - 메서드명은 한글, `@DisplayName`도 한글로 시나리오를 설명.
   - 기존 `reservationRequest(siteNumber, startOffsetDays, endOffsetDays)` 같은 헬퍼를
     재사용하거나 그 자리에 맞게 확장한다 — 새로 만들 때도 같은 스타일로.
   - `test-guide.md`의 격리 규칙을 따른다: 실제 서버를 호출하고 롤백되지 않으므로,
     `data.sql` 시드 및 **기존 테스트가 이미 쓰고 있는 사이트·날짜**와 겹치지 않는
     사이트를 고른다(아래 Gotchas의 "이미 쓰인 사이트" 참고).
   - 상태(취소/확정)에 따라 달라지는 시나리오는 실제 서버에 대고 생성 → (필요시)
     `DELETE /api/reservations/{id}?confirmationCode=...`로 취소까지 실제로 호출해서
     전제 조건을 만든다 — DB를 직접 건드리지 않는다.
5. **질문 처리** — 블록에 `질문`이 남아 있으면 `docs/tickets.md`를 읽어 이미 같은
   내용의 티켓이 있는지 확인한다. 없으면 기존 형식(`T-N 제목` / `내용: ...` / `---`)대로
   새 티켓을 추가한다. **그 질문에 대한 테스트는 만들지 않는다** — 답이 정해지지
   않았으므로.
6. **실행해서 실패 확인** — `.claude/skills/acceptance-test/run-tests.sh --tests "<새로
   쓴 클래스 또는 메서드 패턴>"`로 새로 만든 테스트만 돌린다. 이미 지켜지고 있는
   규칙(예: 확정 예약끼리의 중복 금지)이 아니라면 새 테스트는 지금 실패해야 한다.
   통과해버리면 지금 동작을 겨눈 초안이니 다시 본다 — 테스트가 틀렸거나, 이미
   해결된 규칙이거나.
7. **제약 후보 제시** — 초안을 쓰며 적용한 격리/작성 규칙이 `test-guide.md`에 아직
   없는 새로운 것이면, 그 한 줄 후보를 채팅으로 알려준다. **`test-guide.md`에 직접
   쓰지 않는다** — 사용자가 직접 판단해서 쓴다(1단계 스킬이 `acceptance-criteria.md`를
   대하는 것과 같은 방식).

## 출력

- 새/수정된 테스트 코드 (파일에 직접 씀 — 1단계 스킬(acceptance-criteria)과 달리 여기는
  후보만 제시하지 않고 실제로 작성한다. `principles.md`의 "AI가 만든 것은 사람이 읽고
  판정한 뒤에 쓴다" 원칙에 따라 이후 4단계(판정)에서 사람이 읽고 확정한다.)
- 새로 추가한 티켓 목록(있다면)과 그 이유
- `test-guide.md` 후보 한 줄(있다면) — 파일에는 쓰지 않고 채팅으로만 제시
- `run-tests.sh` 실행 결과 요약: 어떤 테스트가 실패/통과했는지, 실패 메시지

## 하지 않는 것

- `docs/acceptance-criteria.md`는 읽기만 한다 — 수정하지 않는다(1단계 스킬의 산출물).
- `docs/test-guide.md`도 직접 수정하지 않는다 — 후보만 제시한다(사용자가 직접 쓴다).
- 커밋하지 않는다(사용자가 말할 때만).
- "질문"이 안 풀린 경계에 대한 테스트는 만들지 않는다 — 티켓만 남긴다.

## run-tests.sh 사용법

경로: `.claude/skills/acceptance-test/run-tests.sh` (bash, Git Bash에서 실행, 저장소
루트 기준 상대 경로 계산이 내장돼 있어 어디서 실행해도 된다).

```bash
.claude/skills/acceptance-test/run-tests.sh --tests "*ReservationCreationAcceptanceTest*"
# ...
# BUILD SUCCESSFUL in 16s   (또는 실패한 테스트 목록과 함께 BUILD FAILED)

.claude/skills/acceptance-test/run-tests.sh
# 전체 인수/단위 테스트 실행
```

`--tests` 뒤에 클래스 패턴(`"*ClassName*"`)이나 `"*ClassName.메서드명"`을 넘겨 새로 쓴
테스트만 골라 돌릴 수 있다. 실패 상세는 `build/reports/tests/test/index.html`과
`build/test-results/test/*.xml`에 남는다.

## Gotchas

- **`test` 태스크에 `JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8`을 주면 안 된다.** —
  `acceptance-criteria` 스킬의 `verify-api.sh`는 `bootRun`에 이 방식을 쓰지만, `test`
  태스크에서 똑같이 하면 Gradle 테스트 워커가 stdout에 찍는 "Picked up
  JAVA_TOOL_OPTIONS" 메시지 때문에 워커 IPC가 깨져서 **모든 테스트가
  `ClassNotFoundException`으로 실패한다**(직접 재현해서 확인함 — 컴파일된 `.class`
  파일은 정상 존재해도 워커가 못 읽음). `run-tests.sh`는 그래서 이 플래그를 안 쓴다.
  지금 인수 테스트는 `data.sql`의 한글 시드값(고객명 등)을 직접 비교하지 않고 컴파일된
  한글 리터럴끼리만 비교해서 인코딩 강제가 필요 없었다 — 앞으로 시드 데이터의 한글을
  직접 검증하는 테스트가 필요해지면, 환경변수 대신 `build.gradle`의
  `test { systemProperty 'file.encoding', 'UTF-8' }`처럼 태스크 전용으로 넘기는 방법을
  먼저 검증한다(아직 검증 안 함).
- **`./gradlew test` 결과가 캐시로 안 보일 수 있다** — 직전과 소스가 안 바뀌었으면
  Gradle이 `UP-TO-DATE`로 건너뛰고 테스트별 통과/실패 줄이 아예 안 나온다(직접 확인:
  `BUILD SUCCESSFUL in 2s`, `4 up-to-date`만 찍힘). `run-tests.sh`는 항상
  `--rerun-tasks`를 붙여서 이 상황을 피한다.
- **이미 쓰인 사이트** — `data.sql` 시드는 A-1,A-2,A-3,A-4,A-6을 쓰고,
  `ReservationCreationAcceptanceTest`의 기존 테스트는 A-11,A-12,A-15~A-20,B-1~B-4를
  쓴다(A-11,A-12는 5번 규칙 블록 테스트에서 씀). 새 테스트는 이 목록과 겹치지 않는
  사이트(A-5,A-7~A-10,A-13,A-14,B-5~B-15 등)를 고른다 — 실제로 쓰인 사이트 목록은
  이 파일보다 테스트 파일 자체가 최신이니, 헷갈리면 직접 grep해서 확인한다.
- **`Campsite`에는 `status` 필드가 없다** — `acceptance-criteria.md`에 "사이트 상태"라고
  적힌 규칙(취소/확정 관련)이 있어도, 실제로 상태를 갖는 건 `Campsite`가 아니라 그
  사이트·기간에 걸리는 기존 `Reservation`의 `status`다. 테스트를 짤 때 사이트 자체의
  속성이 아니라 "그 사이트·기간에 예약을 만들고 취소/유지해서 상태를 세팅"하는 식으로
  구성해야 한다.
- **인수 테스트는 실제 서버(랜덤 포트)를 호출하고 롤백하지 않는다** — 상태를 바꾸는
  시나리오(생성 후 취소 등)를 짤 때 다른 테스트와 사이트가 겹치면 실행 순서에 따라
  서로 간섭한다.
