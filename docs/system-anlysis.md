# 시스템 분석 결과

## API 목록

### 1. 사이트 관리 API

#### 1.1 모든 사이트 조회
- **Endpoint**: `/api/sites`
- **Method**: `GET`
- **목적**: 캠핑장에 등록된 모든 사이트를 조회
- **주요 파라미터**: 없음
- **응답**: `List<SiteResponse>`
  - `id`: 사이트 ID
  - `siteNumber`: 사이트 번호 (e.g. A-1)
  - `description`: 사이트 설명
  - `maxPeople`: 최대 수용 인원
  - `size`: 사이트 크기 (대형/소형)
  - `hasElectricity`: 전기 사용 가능 여부
  - `toiletDistance`: 화장실까지의 거리
  - `facilities`: 주요 편의시설 (e.g. 샤워장, 화장실)
  - `rules`: 이용수칙

#### 1.2 특정 사이트 조회
- **Endpoint**: `/api/sites/{siteId}`
- **Method**: `GET`
- **목적**: 특정 사이트의 상세 정보 조회
- **주요 파라미터**: 
  - `siteId`: 사이트 ID (e.g. 12)
- **응답**: `SiteResponse` (위와 동일한 구조)

#### 1.3 사이트 가용성 확인
- **Endpoint**: `/api/sites/{siteNumber}/availability`
- **Method**: `GET`
- **목적**: 특정 사이트의 특정 날짜 가용성 확인
- **주요 파라미터**:
  - `siteNumber`: 사이트 번호 (e.g. A-1)
  - `date`: 확인할 날짜 (ISO 형식)
- **응답**: `Map<String, Object>`
  - `siteNumber`: 사이트 번호
  - `date`: 확인한 날짜
  - `available`: 가용 여부 (boolean)

#### 1.4 특정 날짜 가용 사이트 조회
- **Endpoint**: `/api/sites/available`
- **Method**: `GET`
- **목적**: 특정 날짜에 예약 가능한 모든 사이트 조회
- **주요 파라미터**:
  - `date`: 확인할 날짜 (ISO 형식)
- **응답**: `List<SiteAvailabilityResponse>`
  - `siteId`: 사이트 ID
  - `siteNumber`: 사이트 번호
  - `size`: 사이트 크기
  - `hasElectricity`: 전기 사용 가능 여부
  - `date`: 확인한 날짜
  - `available`: 가용 여부
  - `maxPeople`: 최대 수용 인원
  - `description`: 사이트 설명

#### 1.5 사이트 검색
- **Endpoint**: `/api/sites/search`
- **Method**: `GET`
- **목적**: 기간과 조건에 맞는 사이트 검색
- **주요 파라미터**:
  - `startDate`: 시작 날짜 (ISO 형식)
  - `endDate`: 종료 날짜 (ISO 형식)
  - `size`: 사이트 크기 (선택사항)
- **응답**: `List<SiteAvailabilityResponse>` (위와 동일한 구조)

### 2. 예약 관리 API

#### 2.1 예약 생성
- **Endpoint**: `/api/reservations`
- **Method**: `POST`
- **목적**: 새로운 예약 생성
- **요청 Body**: `ReservationRequest`
  - `customerName`: 고객명
  - `startDate`: 체크인 날짜
  - `endDate`: 체크아웃 날짜
  - `siteNumber`: 사이트 번호
  - `phoneNumber`: 전화번호
  - `numberOfPeople`: 인원수
  - `carNumber`: 차량번호
  - `requests`: 특별 요청사항
- **응답**: `ReservationResponse` (성공 시 201)
  - `id`: 예약 ID
  - `customerName`: 고객명
  - `startDate`: 체크인 날짜
  - `endDate`: 체크아웃 날짜
  - `siteNumber`: 사이트 번호
  - `phoneNumber`: 전화번호
  - `status`: 예약 상태
  - `confirmationCode`: 확인 코드
  - `createdAt`: 예약 생성 시간

#### 2.2 예약 조회 (ID로)
- **Endpoint**: `/api/reservations/{id}`
- **Method**: `GET`
- **목적**: 특정 예약 정보 조회
- **주요 파라미터**:
  - `id`: 예약 ID
- **응답**: `ReservationResponse` (성공 시 200)

#### 2.3 예약 목록 조회
- **Endpoint**: `/api/reservations`
- **Method**: `GET`
- **목적**: 예약 목록 조회 (필터링 가능)
- **주요 파라미터**:
  - `date`: 특정 날짜의 예약 조회 (선택사항)
  - `customerName`: 고객명으로 조회 (선택사항)
- **응답**: `List<ReservationResponse>`

#### 2.4 내 예약 조회
- **Endpoint**: `/api/reservations/my`
- **Method**: `GET`
- **목적**: 고객의 예약 목록 조회
- **주요 파라미터**:
  - `name`: 고객명
  - `phone`: 전화번호
- **응답**: `List<ReservationResponse>`

#### 2.5 예약 수정
- **Endpoint**: `/api/reservations/{id}`
- **Method**: `PUT`
- **목적**: 기존 예약 정보 수정
- **주요 파라미터**:
  - `id`: 예약 ID
  - `confirmationCode`: 확인 코드
- **요청 Body**: `ReservationRequest` (예약 생성과 동일)
- **응답**: `ReservationResponse` (성공 시 200)

#### 2.6 예약 취소
- **Endpoint**: `/api/reservations/{id}`
- **Method**: `DELETE`
- **목적**: 예약 취소
- **주요 파라미터**:
  - `id`: 예약 ID
  - `confirmationCode`: 확인 코드
- **응답**: `Map<String, String>`
  - `message`: 취소 완료 메시지

#### 2.7 예약 캘린더 조회
- **Endpoint**: `/api/reservations/calendar`
- **Method**: `GET`
- **목적**: 특정 사이트의 월별 예약 현황 캘린더 조회
- **주요 파라미터**:
  - `year`: 연도
  - `month`: 월
  - `siteId`: 사이트 ID
- **응답**: `CalendarResponse`
  - `year`: 연도
  - `month`: 월
  - `siteId`: 사이트 ID
  - `siteNumber`: 사이트 번호
  - `days`: 일별 상태 목록
    - `date`: 날짜
    - `available`: 가용 여부
    - `customerName`: 예약 고객명 (예약된 경우)
    - `reservationId`: 예약 ID (예약된 경우)
  - `summary`: 요약 정보

### 3. 웹 페이지 API (View Controller)

#### 3.1 홈페이지
- **Endpoint**: `/`
- **Method**: `GET`
- **목적**: 메인 홈페이지 표시

#### 3.2 예약 목록 페이지
- **Endpoint**: `/reservations`
- **Method**: `GET`
- **목적**: 예약 목록 페이지 표시
- **주요 파라미터**:
  - `date`: 특정 날짜의 예약만 표시 (선택사항)

#### 3.3 예약 등록 페이지
- **Endpoint**: `/reservations/new`
- **Method**: `GET`
- **목적**: 새 예약 등록 폼 페이지 표시

#### 3.4 예약 검색 페이지
- **Endpoint**: `/reservations/search`
- **Method**: `GET`
- **목적**: 예약 검색 페이지 표시

#### 3.5 사이트 목록 페이지
- **Endpoint**: `/sites`
- **Method**: `GET`
- **목적**: 사이트 목록 페이지 표시

#### 3.6 사이트 상세 페이지
- **Endpoint**: `/sites/{siteNumber}`
- **Method**: `GET`
- **목적**: 특정 사이트 상세 페이지 표시
- **주요 파라미터**:
  - `siteNumber`: 사이트 번호

## 에러 처리

모든 API는 다음과 같은 에러 응답을 반환할 수 있습니다:

- **400 Bad Request**: 잘못된 요청 파라미터
- **404 Not Found**: 리소스를 찾을 수 없음
- **409 Conflict**: 예약 충돌 (중복 예약 등)

에러 응답 형식:
```json
{
  "message": "에러 메시지"
}
```

## 발견한 비즈니스 규칙
### 버그
- 명세서: 예약은 오늘로부터 30일 이내에만 가능해야 한다
  - 코드: MAX_RESERVATION_DAYS = 30 상수 정의되어 있지만 실제 검증 로직이 없음
- 명세서: 과거 날짜로 예약 불가
  - 코드: 제한 없음
## 명세서 미기재
- 존재하지 않는 캠핑장에 대해 예약 불가