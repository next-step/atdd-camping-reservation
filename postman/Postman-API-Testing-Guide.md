# Postman API 테스트 가이드

## 개요
캠핑장 예약 시스템의 모든 API를 테스트할 수 있는 Postman 컬렉션과 환경 설정 파일을 제공합니다.

## 파일 구성
- `Camping-Reservation-API.postman_collection.json`: API 컬렉션 파일
- `Camping-Reservation-Environment.postman_environment.json`: 환경 변수 파일
- `Postman-API-Testing-Guide.md`: 이 가이드 문서

## 설치 및 설정

### 1. Postman 설치
- [Postman 공식 웹사이트](https://www.postman.com/downloads/)에서 다운로드
- 계정 생성 후 로그인 (선택사항)

### 2. 컬렉션 가져오기
1. Postman 실행
2. **Import** 버튼 클릭
3. `Camping-Reservation-API.postman_collection.json` 파일 선택
4. **Import** 클릭

### 3. 환경 설정
1. Postman에서 **Environments** 탭 클릭
2. **Import** 버튼 클릭
3. `Camping-Reservation-Environment.postman_environment.json` 파일 선택
4. **Import** 클릭
5. 환경 목록에서 **"Camping Reservation Environment"** 선택

## API 테스트 순서

### 1. 서버 실행 확인
먼저 Spring Boot 애플리케이션이 실행 중인지 확인하세요:
```bash
./gradlew bootRun
```

### 2. 기본 테스트 순서

#### 2.1 사이트 관리 API 테스트
1. **모든 사이트 조회** - 전체 사이트 목록 확인
2. **특정 사이트 조회** - 개별 사이트 상세 정보 확인
3. **사이트 가용성 확인** - 특정 날짜 가용성 확인
4. **특정 날짜 가용 사이트 조회** - 해당 날짜 예약 가능한 사이트들
5. **사이트 검색** - 기간별 사이트 검색

#### 2.2 예약 관리 API 테스트
1. **예약 생성** - 새 예약 등록
2. **예약 조회 (ID로)** - 생성된 예약 확인
3. **예약 목록 조회** - 전체 예약 목록 확인
4. **내 예약 조회** - 고객별 예약 조회
5. **예약 캘린더 조회** - 월별 예약 현황 확인
6. **예약 수정** - 기존 예약 정보 변경
7. **예약 취소** - 예약 취소

#### 2.3 웹 페이지 API 테스트
- 각 페이지가 정상적으로 렌더링되는지 확인

## 환경 변수 설정

### 주요 변수들
- `baseUrl`: 서버 주소 (기본값: http://localhost:8080)
- `siteId`: 테스트할 사이트 ID
- `siteNumber`: 테스트할 사이트 번호
- `reservationId`: 테스트할 예약 ID
- `customerName`: 테스트 고객명
- `phoneNumber`: 테스트 전화번호
- `confirmationCode`: 예약 확인 코드

### 날짜 변수들
- `startDate`: 체크인 날짜
- `endDate`: 체크아웃 날짜
- `checkDate`: 가용성 확인 날짜
- `filterDate`: 필터링 날짜

## 테스트 시나리오

### 시나리오 1: 기본 예약 플로우
1. **모든 사이트 조회** → 사용 가능한 사이트 확인
2. **사이트 가용성 확인** → 원하는 날짜 가용성 확인
3. **예약 생성** → 새 예약 등록
4. **예약 조회** → 생성된 예약 확인
5. **내 예약 조회** → 고객 예약 목록 확인

### 시나리오 2: 예약 관리 플로우
1. **예약 목록 조회** → 전체 예약 현황 확인
2. **예약 캘린더 조회** → 월별 예약 현황 확인
3. **예약 수정** → 예약 정보 변경
4. **예약 취소** → 예약 취소

### 시나리오 3: 사이트 검색 플로우
1. **사이트 검색** → 기간별 사이트 검색
2. **특정 날짜 가용 사이트 조회** → 해당 날짜 예약 가능한 사이트들
3. **특정 사이트 조회** → 선택한 사이트 상세 정보

## 주의사항

### 1. 날짜 형식
- 모든 날짜는 `YYYY-MM-DD` 형식으로 입력
- 예: `2024-12-25`

### 2. 예약 생성 시 필수 정보
- `customerName`: 고객명
- `startDate`: 체크인 날짜
- `endDate`: 체크아웃 날짜
- `siteNumber`: 사이트 번호
- `phoneNumber`: 전화번호
- `numberOfPeople`: 인원수

### 3. 예약 수정/취소 시
- `confirmationCode`가 필요합니다
- 예약 생성 시 응답에서 확인 코드를 받아서 사용

### 4. 에러 처리
- 400: 잘못된 요청 파라미터
- 404: 리소스를 찾을 수 없음
- 409: 예약 충돌 (중복 예약 등)
- 500: 서버 내부 오류

## 문제 해결

### 1. 연결 오류
- 서버가 실행 중인지 확인
- `baseUrl`이 올바른지 확인 (기본값: http://localhost:8080)

### 2. 404 에러
- 요청한 리소스가 존재하지 않음
- 사이트 ID나 예약 ID가 올바른지 확인

### 3. 409 에러
- 예약 충돌 (같은 사이트, 같은 날짜에 이미 예약 존재)
- 다른 날짜나 사이트로 시도

### 4. 날짜 관련 오류
- 날짜 형식이 `YYYY-MM-DD`인지 확인
- 과거 날짜로 예약하려고 하지 않는지 확인

## 추가 기능

### 1. 자동화된 테스트
- Postman의 **Collection Runner**를 사용하여 전체 API 테스트 자동화 가능
- **Tests** 탭에서 응답 검증 로직 추가 가능

### 2. 문서화
- 각 API 요청의 **Description**에 상세한 설명 포함
- **Examples**를 추가하여 샘플 요청/응답 제공 가능

### 3. 협업
- Postman Workspace를 통해 팀원들과 컬렉션 공유 가능
- 버전 관리 및 변경 이력 추적 가능
