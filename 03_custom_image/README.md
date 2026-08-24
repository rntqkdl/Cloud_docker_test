# 🐳 [실습 3] Dockerfile로 나만의 커스텀 이미지 만들기

교재: `Cloud_컨테이너 이해 및 앱 컨테이너화 실습_2608.pdf` (6쪽)

---

## 📌 1. 핵심 코드 구성

### 1) `app.js` (내가 작성한 자바스크립트 코드)
```javascript
console.log("Hello from custom Docker image!");
```

### 2) `Dockerfile` (3줄짜리 이미지 레시피)
```dockerfile
FROM node:18-alpine
COPY app.js .
CMD ["node", "app.js"]
```

---

## 🚀 2. 빌드 및 실행 명령어

```bash
# 1. 실습 폴더로 이동
cd /Users/seongminan/workspace/cloud-docker-intensive-labs/03_custom_image

# 2. 나만의 이미지 빌드하기 (이름: my-node-app)
docker build -t my-node-app .

# 3. 컨테이너 실행 및 자동 청소 (--rm)
docker run --rm my-node-app
```

---

## 🔍 3. 비전공자 맞춤 개념 해설

* **`FROM node:18-alpine`**:
  * 자바스크립트를 돌릴 수 있는 Node.js 18 버전이 설치된 초경량(Alpine) 리눅스 바닥판을 가져옵니다.
* **`COPY app.js .`**:
  * 내 컴퓨터에 있는 `app.js` 파일을 컨테이너 내부의 현재 작업 폴더(`.`)로 복사해 넣습니다.
* **`CMD ["node", "app.js"]`**:
  * 컨테이너가 켜지는 순간 자동으로 `node app.js` 명령을 실행합니다.
* **`--rm` (Auto Remove)**:
  * 컨테이너가 할 일을 마치고 종료되는 즉시 **자동으로 껍데기(Exited)를 삭제**해 주는 깔끔한 청소 옵션입니다.
