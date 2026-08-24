# 🐳 [마스터 대시보드] 비전공자를 위한 클라우드 & 도커 마스터 대백과 WebApp

SKALA 집중 실습 1~9 총정리 및 CS 심화 지식을 담은 인터랙티브 SPA 웹 대시보드입니다.

---

## 🚀 로컬 실행 방법

```bash
cd /Users/seongminan/workspace/cloud-docker-intensive-labs/10_docker_master_dashboard

# 1. 도커 이미지 빌드
docker build -t skala-cs-encyclopedia:g124 .

# 2. 로컬 포트 8088로 실행
docker run -d -p 8088:80 --name cs-encyclopedia-app skala-cs-encyclopedia:g124

# 3. 브라우저 접속: http://localhost:8088
```

---

## 🚢 Harbor 배포 이미지

* `harbor.skala-gj.com/skala-gj4/skala-cs-encyclopedia:g124`
* `harbor.skala-gj.com/skala-gj4/skala-webserver-g124:v2.0-encyclopedia`
