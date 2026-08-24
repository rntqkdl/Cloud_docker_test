# 🐳 [실습 8] Scale-out(서버 증설) 및 무중단 로드밸런싱 실습

교재: `Cloud_컨테이너 이해 및 앱 컨테이너화 실습_2608.pdf` (24~27쪽)

---

## 📌 핵심 명령어 흐름

```bash
# 1. 컨테이너 3-Tier 실행
docker compose up -d

# 2. WAS 서버를 단 1초 만에 3벌로 증설 (--scale was=3)
docker compose up -d --scale was=3

# 3. 60번 요청을 보내 로드밸런싱 분산 확인
for i in $(seq 1 60); do curl -s localhost/whoami; echo; done | grep -o '"container":"[^"]*"' | sort | uniq -c

# 4. 1대가 갑자기 죽었을 때 무중단 failover 테스트
docker stop 08_scale_and_loadbalancing-was-2
for i in $(seq 1 15); do curl -s -o /dev/null -m 5 -w '%{http_code}/%{time_total}s\n' localhost/whoami; done
```
