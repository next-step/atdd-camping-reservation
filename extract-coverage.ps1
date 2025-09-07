# JaCoCo XML 파싱하여 전체 커버리지 정보 추출
$xmlPath = "build/reports/jacoco/test/jacocoTestReport.xml"

# XML 로드
[xml]$xml = Get-Content $xmlPath

# 전체 프로젝트 counter 찾기 (마지막 counter 태그들이 전체 통계)
$reportCounters = $xml.report.counter

Write-Host "=== 전체 프로젝트 테스트 커버리지 리포트 ===" -ForegroundColor Green
Write-Host ""

foreach ($counter in $reportCounters) {
    $type = $counter.type
    $missed = [int]$counter.missed
    $covered = [int]$counter.covered
    $total = $missed + $covered
    $coverage = if ($total -gt 0) { [math]::Round(($covered / $total) * 100, 2) } else { 0 }
    
    switch ($type) {
        "INSTRUCTION" { 
            Write-Host "명령어 커버리지    : $coverage% ($covered/$total)" -ForegroundColor Cyan
        }
        "BRANCH" { 
            Write-Host "분기 커버리지      : $coverage% ($covered/$total)" -ForegroundColor Yellow
        }
        "LINE" { 
            Write-Host "라인 커버리지      : $coverage% ($covered/$total)" -ForegroundColor Magenta
        }
        "COMPLEXITY" { 
            Write-Host "복잡도 커버리지    : $coverage% ($covered/$total)" -ForegroundColor Red
        }
        "METHOD" { 
            Write-Host "메소드 커버리지    : $coverage% ($covered/$total)" -ForegroundColor Blue
        }
        "CLASS" { 
            Write-Host "클래스 커버리지    : $coverage% ($covered/$total)" -ForegroundColor Green
        }
    }
}

Write-Host ""
Write-Host "HTML 리포트 위치: build/reports/jacoco/test/html/index.html" -ForegroundColor White