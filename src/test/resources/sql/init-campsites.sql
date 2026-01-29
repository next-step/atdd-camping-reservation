-- 테스트용 캠핑사이트 초기화 SQL
-- AcceptanceTest에서 @Sql로 실행되어 테스트 데이터를 초기화합니다.
-- 기존 TestDataFactory.initCampsites()를 대체합니다.

INSERT INTO campsites (site_number, description, max_people) VALUES
('A1', '전기 사용 가능한 대형 사이트', 6),
('A2', '전기 사용 가능한 대형 사이트', 6),
('B1', '아늑한 소형 사이트', 4),
('B2', '아늑한 소형 사이트', 4);
