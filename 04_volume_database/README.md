# 🐳 [실습 4] 볼륨(Volume) - 데이터는 어디에 남는가?

교재: `Cloud_컨테이너 이해 및 앱 컨테이너화 실습_2608.pdf` (7~9쪽)

---

## 📌 핵심 원리 요약
* **컨테이너 (Container)**: 언제든 부수고 버릴 수 있는 **소모품 냄비**
* **볼륨 (Volume)**: 컨테이너가 죽어도 데이터를 영구히 지켜주는 **개인 금고/사물함**

---

## 🚀 단계별 실습 명령어

### 1단계: 볼륨 생성 및 MariaDB 실행
```bash
# 1. 영구 사물함(볼륨) 생성
docker volume create mariadb-data

# 2. 볼륨을 달고 MariaDB 컨테이너 실행
docker run -d --name mariadb \
  -e MYSQL_ROOT_PASSWORD=password123 \
  -e MYSQL_DATABASE=skala \
  -e MYSQL_USER=user \
  -e MYSQL_PASSWORD=password123 \
  -p 3306:3306 \
  -v mariadb-data:/var/lib/mysql \
  mariadb:11
```

---

### 2단계: 테이블 생성 및 데이터 3건 삽입
```bash
docker exec mariadb mariadb -u user -ppassword123 skala -e "
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO users (username, email) VALUES
('alice', 'alice@example.com'),
('bob', 'bob@example.com'),
('carol', 'carol@example.com');
SELECT * FROM users;
"
```

---

### 3단계: [검증 1] 컨테이너 삭제 후 데이터 보존 확인
```bash
# 1. 컨테이너를 가차없이 삭제
docker rm -f mariadb

# 2. 똑같은 볼륨을 연결해 새 컨테이너 실행
docker run -d --name mariadb \
  -e MYSQL_ROOT_PASSWORD=password123 \
  -e MYSQL_DATABASE=skala \
  -e MYSQL_USER=user \
  -e MYSQL_PASSWORD=password123 \
  -p 3306:3306 \
  -v mariadb-data:/var/lib/mysql \
  mariadb:11

# 3. 데이터가 살아있는지 확인!
docker exec mariadb mariadb -u user -ppassword123 skala -e "SELECT * FROM users;"
# -> alice, bob, carol 데이터 3건이 그대로 유지됨!
```

---

### 4단계: [검증 2] 볼륨까지 삭제하면 어떻게 될까?
```bash
# 컨테이너와 볼륨을 함께 완전 삭제
docker rm -f mariadb && docker volume rm mariadb-data

# 볼륨 없이 다시 띄우면
docker run -d --name mariadb -e MYSQL_ROOT_PASSWORD=password123 -e MYSQL_DATABASE=skala -e MYSQL_USER=user -e MYSQL_PASSWORD=password123 -p 3306:3306 mariadb:11

# 확인 -> 데이터가 완전히 사라짐 (Empty set)
docker exec mariadb mariadb -u user -ppassword123 skala -e "SHOW TABLES;"
```
