# 🐳 [실습 7] Docker Compose로 3-Tier 한 번에 띄우기

교재: `Cloud_컨테이너 이해 및 앱 컨테이너화 실습_2608.pdf` (18~23쪽)

---

## 📌 핵심 원리
* 실습 6에서 손으로 치던 6~7개의 명령어를 `compose.yaml` 단 한 장으로 정의!
* `depends_on + condition: service_healthy`: DB가 완벽하게 준비될 때까지 WAS가 기다렸다가 부팅되는 지능형 순서 제어!

```bash
# 1. 3-Tier 전체 자동 빌드 및 실행
docker compose up -d

# 2. 실행 상태 및 헬스체크 확인
docker compose ps

# 3. 브라우저 접속 확인
curl -s localhost/users

# 4. 종료 (볼륨 보존) vs 완전 삭제 (볼륨 삭제)
docker compose down       # 데이터 보존
docker compose down -v    # 볼륨까지 초기화
```
