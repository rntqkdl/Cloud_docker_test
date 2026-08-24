# 🐳 [실습 5] Harbor Registry에 올리고 내려받기

교재: `Cloud_컨테이너 이해 및 앱 컨테이너화 실습_2608.pdf` (10~11쪽)

---

## 📌 핵심 명령어 흐름 (교재 10~11쪽)

```bash
# 1. Harbor 로그인
docker login harbor.skala-gj.com

# 2. 로컬 이미지 빌드
docker build -t skala-webserver:1.0.0 .

# 3. 레지스트리 주소 명찰(태그) 달기 (★ 핵심)
docker tag skala-webserver:1.0.0 harbor.skala-gj.com/skala-gj4/skala-webserver:1.0.0

# 4. Harbor로 업로드 (Push)
docker push harbor.skala-gj.com/skala-gj4/skala-webserver:1.0.0

# 5. 로컬 이미지 2개 완전 삭제 (진짜 받아오는지 검증용)
docker rmi harbor.skala-gj.com/skala-gj4/skala-webserver:1.0.0
docker rmi skala-webserver:1.0.0

# 6. Harbor에서 순수 다운로드 (Pull)
docker pull harbor.skala-gj.com/skala-gj4/skala-webserver:1.0.0

# 7. 다운받은 이미지로 컨테이너 실행
docker run -d -p 8080:80 --name from-harbor harbor.skala-gj.com/skala-gj4/skala-webserver:1.0.0
```
