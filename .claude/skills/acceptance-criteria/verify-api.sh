#!/usr/bin/env bash
# 서버를 UTF-8 인코딩으로 기동하고 API를 직접 호출해 실제 응답을 확인하는 도우미.
# repo 루트에서 실행한다: .claude/skills/acceptance-criteria/verify-api.sh <command>
#
# 사용법:
#   verify-api.sh start                          서버를 백그라운드로 기동 (기본 포트 8081, UTF-8)
#   verify-api.sh call <METHOD> <PATH> [BODY]     curl 호출, 상태 코드 + 응답 본문 그대로 출력
#   verify-api.sh stop                            기동한 서버 종료
#
# JDK17 + 한글 Windows(코드페이지 949) 환경에서 -Dfile.encoding=UTF-8 없이 bootRun을
# 실행하면 data.sql의 한글이 깨져서 저장된다 (docs/plan.md 4단계 참고). Gradle이 포크하는
# bootRun 자식 JVM에도 적용되도록 JAVA_TOOL_OPTIONS로 넘긴다 (-D를 gradlew에 직접 주면
# Gradle 데몬 JVM에만 적용되고 자식 프로세스에는 전달되지 않는다).

set -euo pipefail
cd "$(dirname "$0")/../../.."

PORT="${SERVER_PORT:-8081}"
STATE_DIR=".claude/skills/acceptance-criteria"
LOGFILE="$STATE_DIR/.server.log"

start() {
  echo "포트 $PORT 로 서버 기동 중 (UTF-8, 로그: $LOGFILE) ..."
  JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8" SERVER_PORT="$PORT" \
    ./gradlew bootRun --no-daemon > "$LOGFILE" 2>&1 &
  disown

  for _ in $(seq 1 60); do
    if curl -s -o /dev/null "http://localhost:$PORT/api/sites"; then
      echo "준비 완료: http://localhost:$PORT"
      return 0
    fi
    sleep 2
  done
  echo "타임아웃: 서버가 뜨지 않았다. $LOGFILE 확인" >&2
  return 1
}

call() {
  local method="$1" path="$2" body="${3:-}"
  if [ -n "$body" ]; then
    # 한글이 포함된 JSON을 curl -d 인자로 바로 넘기면 Git Bash -> curl.exe 사이에서
    # 코드페이지 변환이 끼어들어 깨진다("Invalid UTF-8 middle byte"). 파일에 UTF-8로
    # 써서 --data-binary @file 로 넘기면 원문 그대로 전달된다.
    local tmp
    tmp="$(mktemp)"
    printf '%s' "$body" > "$tmp"
    curl -s -w '\n%{http_code}\n' -X "$method" "http://localhost:$PORT$path" \
      -H 'Content-Type: application/json; charset=UTF-8' --data-binary "@$tmp"
    rm -f "$tmp"
  else
    curl -s -w '\n%{http_code}\n' -X "$method" "http://localhost:$PORT$path"
  fi
}

stop() {
  local pid
  pid=$(netstat -ano -p tcp 2>/dev/null | grep ":$PORT " | grep LISTENING | awk '{print $NF}' | head -n1)
  if [ -n "${pid:-}" ]; then
    taskkill //F //T //PID "$pid" >/dev/null 2>&1 || kill -9 "$pid" 2>/dev/null || true
    echo "포트 $PORT 의 프로세스(pid $pid) 종료"
  else
    echo "포트 $PORT 에서 실행 중인 프로세스를 찾지 못함"
  fi
}

case "${1:-}" in
  start) start ;;
  call) shift; call "$@" ;;
  stop) stop ;;
  *) echo "usage: $0 {start|call METHOD PATH [BODY]|stop}" >&2; exit 1 ;;
esac
