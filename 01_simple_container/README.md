# 🐳 [실습 1] 단순한 컨테이너 생성과 생명주기

교재: `Cloud_컨테이너 이해 및 앱 컨테이너화 실습_2608.pdf` (4쪽)

---

## 📌 1. 핵심 명령어 및 옵션 풀이

```bash
# 1. 컨테이너 실행 (없으면 Docker Hub에서 자동 다운로드 후 실행)
docker run hello-world

# 2. 현재 살아있는(Running) 컨테이너 조회
docker ps

# 3. 멈춘(Exited) 컨테이너까지 포함한 전체 목록 조회
docker ps -a

# 4. 다 쓴 컨테이너 삭제
docker rm <컨테이너ID 또는 이름>
```

---

## 🔍 2. 비전공자를 위한 도커의 4단계 동작 드라마

```text
[1. 내 컴퓨터 검색] ──(없음)──> [2. Docker Hub 자동 다운]
                                        │
[4. 결과 출력 후 자동 종료] <── [3. 컨테이너 생성 및 실행]
```

1. **`Unable to find image 'hello-world:latest' locally`**:
   * 내 컴퓨터(로컬) 냉장고를 열었는데 `hello-world`라는 밀키트(이미지)가 없다는 뜻입니다.
2. **`latest: Pulling from library/hello-world`**:
   * 인터넷 중앙 창고인 **Docker Hub**에서 해당 밀키트를 즉시 다운로드(Pull)합니다.
3. **`Hello from Docker!`**:
   * 밀키트를 뜯어서 냄비(컨테이너)에 넣고 끓여, 안내 메시지를 화면에 출력합니다.
4. **`STATUS: Exited (0)`**:
   * 안내문 출력이 끝나자마자 냄비 불을 끄고 **스스로 종료(Exited)**됩니다.
   * `(0)`은 에러 없이 정상적으로 임무를 완수하고 은퇴했다는 뜻입니다.

---

## 💡 3. 왜 `docker ps`에는 안 보이고 `docker ps -a`에만 보일까?

* **`docker ps`**: "지금 실시간으로 일하고 있는(Running) 컨테이너만 보여줘!"
* **`docker ps -a` (All)**: "과거에 일하다가 멈춘(Exited) 컨테이너까지 싹 다 보여줘!"
