#!/usr/bin/env bash
# 인수 테스트를 UTF-8 인코딩으로, 캐시 없이 새로 돌리는 도우미.
# 저장소 루트에서 실행한다: .claude/skills/acceptance-test/run-tests.sh [gradle test 인자...]
#
# 사용법:
#   run-tests.sh                                              전체 테스트 실행
#   run-tests.sh --tests "*ReservationCreationAcceptanceTest*"  클래스 단위 실행
#   run-tests.sh --tests "*ReservationCreationAcceptanceTest.취소된_예약과_겹치는_기간은_예약된다"
#                                                              메서드 하나만 실행
#
# docs/plan.md: "./gradlew test 결과 줄이 안 보이면 캐시다 -- --rerun-tasks로 재실행".
# 이 스크립트는 매번 --rerun-tasks를 붙여 캐시된 결과를 보고 오판하지 않도록 한다.
#
# acceptance-criteria 스킬의 verify-api.sh와 달리 JAVA_TOOL_OPTIONS로 UTF-8을 강제하지
# 않는다 — 여기서 그렇게 하면 Gradle 테스트 워커가 stdout에 찍는
# "Picked up JAVA_TOOL_OPTIONS" 메시지 때문에 워커 IPC가 깨져서 매 테스트가
# ClassNotFoundException으로 실패한다(직접 재현 확인). 지금 인수 테스트는 data.sql의
# 한글 시드값을 직접 비교하지 않아서 인코딩 강제가 필요 없다 — 필요해지면 build.gradle의
# `test { systemProperty 'file.encoding', 'UTF-8' }`처럼 태스크 전용으로 넘긴다.

set -euo pipefail
cd "$(dirname "$0")/../../.."

./gradlew test --rerun-tasks "$@"
