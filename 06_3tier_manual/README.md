# 🐳 [실습 6] 3-Tier (Web - WAS - DB) 수동 아키텍처 구성

교재: `Cloud_컨테이너 이해 및 앱 컨테이너화 실습_2608.pdf` (12~17쪽)

---

## 🏛️ 3-Tier 구조도

```text
[ 브라우저 / 외부 손님 ]
          │ (Port 80)
          ▼
[ 1계층: Web (Nginx) ] ──── (http://was:8080) ────> [ 2계층: WAS (Node.js) ] ──── (mariadb-3tier:3306) ────> [ 3계층: DB (MariaDB) ]
(유일하게 밖으로 열린 문)                                 (내부 3tier-net 통신)                                     (내부 3tier-net 통신)
```

---

## 🚀 단계별 실행 명령어 (교재 12~17쪽)

```bash
# 1. 3tier-docker 폴더로 이동
cd /Users/seongminan/workspace/cloud-docker-intensive-labs/06_3tier_manual

# 2. 컨테이너 전용 가상 사설망 생성
docker network create 3tier-net

# 3. [3계층 DB] MariaDB 실행 (-p 없음! 오직 내부망만 허용)
docker run -d \
  --name mariadb-3tier \
  --network 3tier-net \
  -e MYSQL_ROOT_PASSWORD=password123 \
  -e MYSQL_DATABASE=skala \
  -e MYSQL_USER=user \
  -e MYSQL_PASSWORD=password123 \
  -v $(pwd)/db:/docker-entrypoint-initdb.d \
  mariadb:11

# 4. [2계층 WAS] 빌드 및 실행 (-p 없음!)
cd was && docker build -t was-app .
docker run -d --name was --network 3tier-net was-app
cd ..

# 5. [1계층 Web] Nginx 실행 (유일하게 -p 80:80 개방)
docker run -d \
  --name nginx \
  --network 3tier-net \
  -p 80:80 \
  -v $(pwd)/web:/etc/nginx/conf.d \
  nginx

# 6. 최종 연동 확인 (브라우저 http://localhost/users 또는 터미널)
curl -s localhost/users
```
