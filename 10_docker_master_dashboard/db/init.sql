CREATE DATABASE IF NOT EXISTS dashboard_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE dashboard_db;

-- 1. 사용자 퀘스트 진행도 테이블
CREATE TABLE IF NOT EXISTS quest_progress (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL UNIQUE,
    user_name VARCHAR(100) NOT NULL,
    current_level INT DEFAULT 1,
    completed_labs JSON,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 2. 실습 Q&A 및 공부 메모 방명록 테이블
CREATE TABLE IF NOT EXISTS study_notes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    author VARCHAR(50) NOT NULL,
    category VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 초기 시드 데이터 삽입
INSERT INTO quest_progress (user_id, user_name, current_level, completed_labs)
VALUES ('skala-g124', '4반 G124 안성민', 9, '["lab1", "lab2", "lab3", "lab4", "lab5", "lab6", "lab7", "lab8", "lab9"]')
ON DUPLICATE KEY UPDATE current_level = 9;

INSERT INTO study_notes (author, category, content) VALUES
('4반 G124 안성민', '실습 팁', 'uvicorn이나 express 웹서버를 컨테이너화할 때는 반드시 host를 0.0.0.0으로 열어야 호스트 브라우저에서 200 OK가 뜹니다!'),
('도커 마스터', '커널 핵심', 'cgroups v2로 메모리를 제한하고 OverlayFS로 불필요한 OS 파일 중복을 막는 것이 컨테이너 다이어트의 핵심 원리입니다.'),
('클라우드 멘토', 'Compose 꿀팁', 'depends_on만 쓰지 말고 condition: service_healthy를 걸어주어야 DB가 완전히 준비된 후 WAS가 뜹니다.');
