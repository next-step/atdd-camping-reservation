-- 캠핑장 사이트 데이터 (50개: A-1 ~ A-50)
-- A 구역: 대형 사이트 (전기 있음)
INSERT INTO campsites (site_number, description, max_people) VALUES 
('A-1', '대형 사이트 - 전기 있음, 화장실 인근', 6),
('A-2', '대형 사이트 - 전기 있음, 화장실 인근', 6),
('A-3', '대형 사이트 - 전기 있음, 화장실 인근', 6),
('A-4', '대형 사이트 - 전기 있음, 개수대 인근', 6),
('A-5', '대형 사이트 - 전기 있음, 개수대 인근', 6),
('A-6', '대형 사이트 - 전기 있음, 놀이터 인근', 6),
('A-7', '대형 사이트 - 전기 있음, 놀이터 인근', 6),
('A-8', '대형 사이트 - 전기 있음, 계곡 전망', 6),
('A-9', '대형 사이트 - 전기 있음, 계곡 전망', 6),
('A-10', '대형 사이트 - 전기 있음, 계곡 전망', 6),
('A-11', '대형 사이트 - 전기 있음, 산 전망', 6),
('A-12', '대형 사이트 - 전기 있음, 산 전망', 6),
('A-13', '대형 사이트 - 전기 있음, 산 전망', 6),
('A-14', '대형 사이트 - 전기 있음, 중앙 위치', 6),
('A-15', '대형 사이트 - 전기 있음, 중앙 위치', 6),
('A-16', '대형 사이트 - 전기 있음, 중앙 위치', 6),
('A-17', '대형 사이트 - 전기 있음, 조용한 위치', 6),
('A-18', '대형 사이트 - 전기 있음, 조용한 위치', 6),
('A-19', '대형 사이트 - 전기 있음, 조용한 위치', 6),
('A-20', '대형 사이트 - 전기 있음, 조용한 위치', 6),
('B-1', '소형 사이트 - 전기 있음, 매점 인근', 6),
('B-2', '소형 사이트 - 전기 있음, 매점 인근', 6),
('B-3', '소형 사이트 - 전기 있음, 매점 인근', 6),
('B-4', '소형 사이트 - 전기 있음, 주차장 인근', 6),
('B-5', '소형 사이트 - 전기 있음, 주차장 인근', 6),
('B-6', '소형 사이트 - 전기 있음, 주차장 인근', 6),
('B-7', '소형 사이트 - 전기 있음, 샤워장 인근', 6),
('B-8', '소형 사이트 - 전기 있음, 샤워장 인근', 6),
('B-9', '소형 사이트 - 전기 있음, 샤워장 인근', 6),
('B-10', '소형 사이트 - 전기 있음, 샤워장 인근', 6),
('B-11', '소형 사이트 - 전기 있음, 바비큐장 인근', 6),
('B-12', '소형 사이트 - 전기 있음, 바비큐장 인근', 6),
('B-13', '소형 사이트 - 전기 있음, 바비큐장 인근', 6),
('B-14', '소형 사이트 - 전기 있음, 운동장 인근', 6),
('B-15', '소형 사이트 - 전기 있음, 운동장 인근', 6);

-- 샘플 예약 데이터 (현재 날짜 기준으로 미래 예약)
INSERT INTO reservations (customer_name, start_date, end_date, reservation_date, campsite_id, phone_number, status, confirmation_code, created_at) 
VALUES ('홍길동', DATEADD('DAY', 7, CURRENT_DATE), DATEADD('DAY', 9, CURRENT_DATE), DATEADD('DAY', 7, CURRENT_DATE), 1, '010-1234-5678', 'CONFIRMED', 'ABC123', CURRENT_TIMESTAMP);

INSERT INTO reservations (customer_name, start_date, end_date, reservation_date, campsite_id, phone_number, status, confirmation_code, created_at) 
VALUES ('김철수', DATEADD('DAY', 14, CURRENT_DATE), DATEADD('DAY', 15, CURRENT_DATE), DATEADD('DAY', 14, CURRENT_DATE), 3, '010-2345-6789', 'CONFIRMED', 'DEF456', CURRENT_TIMESTAMP);

INSERT INTO reservations (customer_name, start_date, end_date, reservation_date, campsite_id, phone_number, status, confirmation_code, created_at) 
VALUES ('이영희', DATEADD('DAY', 21, CURRENT_DATE), DATEADD('DAY', 23, CURRENT_DATE), DATEADD('DAY', 21, CURRENT_DATE), 6, '010-3456-7890', 'CONFIRMED', 'GHI789', CURRENT_TIMESTAMP);

-- 과거 예약 (버그 테스트용 - 과거 날짜 예약이 가능한 버그 확인용)
INSERT INTO reservations (customer_name, start_date, end_date, reservation_date, campsite_id, phone_number, status, confirmation_code, created_at) 
VALUES ('박민수', DATEADD('DAY', -7, CURRENT_DATE), DATEADD('DAY', -5, CURRENT_DATE), DATEADD('DAY', -7, CURRENT_DATE), 2, '010-4567-8901', 'CONFIRMED', 'JKL012', DATEADD('DAY', -14, CURRENT_TIMESTAMP));

INSERT INTO reservations (customer_name, start_date, end_date, reservation_date, campsite_id, phone_number, status, confirmation_code, created_at) 
VALUES ('정수진', DATEADD('DAY', -3, CURRENT_DATE), DATEADD('DAY', -2, CURRENT_DATE), DATEADD('DAY', -3, CURRENT_DATE), 4, '010-5678-9012', 'CONFIRMED', 'MNO345', DATEADD('DAY', -10, CURRENT_TIMESTAMP));