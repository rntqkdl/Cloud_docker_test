-- PostgreSQL 초기 테이블 및 시드 데이터
CREATE TABLE IF NOT EXISTS quest_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL UNIQUE,
    user_name VARCHAR(100) NOT NULL,
    current_level INT DEFAULT 1,
    completed_labs TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS study_notes (
    id BIGSERIAL PRIMARY KEY,
    author VARCHAR(50) NOT NULL,
    category VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO quest_progress (user_id, user_name, current_level, completed_labs)
VALUES ('skala-g124', '4반 G124 안성민', 9, '["lab1", "lab2", "lab3", "lab4", "lab5", "lab6", "lab7", "lab8", "lab9"]')
ON CONFLICT (user_id) DO UPDATE SET current_level = 9;

INSERT INTO study_notes (author, category, content) VALUES
('4반 G124 안성민', '실습 팁', 'Spring Boot 앱을 컨테이너화할 때는 server.port=8080과 Docker EXPOSE 8080을 맞추고, DB URL에 localhost 대신 compose 서비스명(db)을 써야 합니다!'),
('도커 멘토', 'PostgreSQL 팁', 'PostgreSQL은 pg_isready 헬스체크로 완벽히 부팅되었는지 확인 후 Spring Boot Data JPA가 접속하게 해야 커넥션 에러를 방지할 수 있습니다.'),
('클라우드 아키텍트', '커널 가이드', 'cgroups v2로 Java JVM의 힙 메모리(-Xmx256m)를 통제하지 않으면 컨테이너가 호스트의 전체 RAM을 탐색하여 불필요한 OOM Killer를 유발할 수 있습니다.');
