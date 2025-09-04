# 시스템 분석 결과

## api 목록
### 1. 모든 사이트 조회 
   1. Endpoint : /api/sites
   2. Method: Get
   3. 목적: 캠핑장에 등록된 모든 사이트를 조회
   4. 주요 파라메터: 없음
   5. 응답
      - 아래 사이트 정보의 List
        - id
        - siteNumber: 사이트 번호 (e.g A-1)
        - description: 사이트 설명
        - maxPeople: 최대 수용 인원
        - hasElectricity: 전기 사용 가능 여부
        - toiletDistance: 화장실까지의 거리
        - facilities: 주요 편의시설 (e.g. 샤워장, 화장실)
        - rules: 이용수칙
### 2. 모든 사이트 조회
   1. Endpoint : /api/sites/{siteId}
   2. Method: Get
   3. 목적: 특정 사이트 조회
   4. 주요 파라메터: 사이트 id (e.g. 12)
   5. 응답
      - id
      - siteNumber: 사이트 번호 (e.g A-1)
      - description: 사이트 설명
      - maxPeople: 최대 수용 인원
      - hasElectricity: 전기 사용 가능 여부
      - toiletDistance: 화장실까지의 거리
      - facilities: 주요 편의시설 (e.g. 샤워장, 화장실)
      - rules: 이용수칙