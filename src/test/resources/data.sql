-- 테스트 전용 시드 데이터 (main/resources/data.sql 과 격리)
-- acceptance 테스트에서 필요한 최소 데이터만 넣는다.

INSERT INTO campsites (site_number, description, max_people)
VALUES ('A-1', '테스트용 사이트 A-1', 6);

INSERT INTO campsites (site_number, description, max_people)
VALUES ('B-1', '테스트용 사이트 B-1', 4);
