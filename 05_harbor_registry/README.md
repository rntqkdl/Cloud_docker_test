# 🐳 STAGE 05 : Harbor Registry & Local OCI Registry 예행연습

<p align="center">
  <img src="https://img.shields.io/badge/OCI%20Registry-Harbor%20%7C%20Registry%3Av2-blue?style=for-the-badge&logo=docker" alt="Registry">
  <img src="https://img.shields.io/badge/SKALA%20Class-4반%20G124%20안성민-emerald?style=for-the-badge" alt="Class">
  <img src="https://img.shields.io/badge/Port-5005%20(AirPlay%20Safe)-orange?style=for-the-badge" alt="Port">
  <img src="https://img.shields.io/badge/Layer%20Optimization-8%2F10%20Layer%20Reused-teal?style=for-the-badge" alt="Cache">
</p>

> **교재 연계 출처**:
> - 📖 `Cloud_컨테이너 이해 및 앱 컨테이너화_2608.pdf` — **6. Registry Push 실습 (148~158쪽)**
> - 📑 `Cloud_컨테이너 이해 및 앱 컨테이너화 실습_2608.pdf` — **실습 5. Harbor Registry에 올리고 내려받기 (10~11쪽)**

---

## ⚡ 빠른 시작 (Quick Start)

### 1. 캠퍼스 Harbor 원격 배포 (`harbor.skala-gj.com`)
```bash
# 1. Harbor 로그인 (4반 계정)
docker login harbor.skala-gj.com -u skala-gj4

# 2. 빌드 ➔ 태그 ➔ 푸시 원클릭 실행
./push.sh skala-gj4 1.0.0

# 3. 브라우저에서 Harbor 콘솔 확인
# https://harbor.skala-gj.com (skala-gj4 프로젝트)

# 4. 로컬 이미지 삭제 후 Harbor에서 Pull & 실행 검증
docker rmi skala-webserver:1.0.0 harbor.skala-gj.com/skala-gj4/skala-webserver:1.0.0
docker pull harbor.skala-gj.com/skala-gj4/skala-webserver:1.0.0
docker run -d --name myweb -p 8081:80 harbor.skala-gj.com/skala-gj4/skala-webserver:1.0.0
curl localhost:8081
```

### 2. 오프라인 / 로컬 `registry:2` 예행연습 (Rehearsal)
```bash
# 로컬 5005 포트로 registry:2 구동 및 OCI Distribution 전체 사이클 자동 검증
./rehearsal-local.sh
```

---

## 💡 왜 사내 레지스트리(Harbor)가 필요한가?

| 🔴 로컬 도커 환경의 한계 | 🟢 Harbor 사내 중앙 레지스트리 도입 효과 |
| :--- | :--- |
| **로컬 격리**: 내 PC(`localhost`)에만 빌드된 이미지는 동료 개발자나 CI/CD 파이프라인에서 접근 불가 | **중앙 집중화**: 사내 모든 쿠버네티스(EKS/K8s) 클러스터 및 개발자가 동일한 버전의 이미지를 즉시 공유 |
| **보안 취약점 방치**: 공개 Docker Hub 업로드 시 사내 비즈니스 로직 및 소스코드 외부 유출 위험 | **엔터프라이즈 보안**: 사내 RBAC 권한 분리, 취약점 정적 스캔(Trivy), 서명(Cosign) 기반 무단 배포 차단 |
| **대역폭 낭비 & 속도 저하**: 외부 인터넷망을 통한 다운로드로 대규모 Pod 스케일링 시 네트워크 병목 | **초고속 내부망 캐싱**: 사내 10Gbps 네트워크를 통해 수초 만에 수 기가바이트 이미지 고속 전송 |

---

## 🛡️ 레지스트리 운영 4대 핵심 원칙

1. **태그 네이밍 네임스페이스 규칙 (Strict Naming)**:
   - `[레지스트리 도메인]/[프로젝트(네임스페이스)]/[이미지명]:[버전태그]` 형식을 지켜야 합니다.
   - 예: `harbor.skala-gj.com/skala-gj4/skala-webserver:1.0.0`
   - 앞의 도메인을 생략하면 기본값인 `docker.io/library/`로 전송되어 `push access denied` 에러가 발생합니다.
2. **레이어 재사용성 (Layer Caching & Deduplication)**:
   - 도커 이미지는 계층(Layer) 구조입니다. 베이스 이미지(`nginx:alpine`)가 이미 레지스트리에 존재하면, 새 버전 배포 시 변경된 파일 레이어(`index.html`)만 업로드되어 **전송 대역폭과 시간을 90% 이상 절감**합니다 (`Layer already exists`).
3. **인증 정보 격리 & 보안 (Zero-Leak Auth)**:
   - `docker login` 시 자격 증명은 macOS의 경우 `credsStore: "desktop"`(OS 키체인)에 암호화 보관됩니다.
   - 리눅스 서버의 경우 `~/.docker/config.json`에 `base64` 인코딩으로 저장되므로 공용 서버 사용 후에는 반드시 `docker logout`을 수행해야 합니다.
4. **멀티 아키텍처 크로스 빌드 (Multi-Platform Buildx)**:
   - Apple Silicon(Mac M1~M5)에서 기본 빌드 시 `arm64`로 생성되므로, `amd64` EKS 노드 배포 시 `--platform linux/amd64` 빌드가 필수적입니다.

---

## 🏛️ OCI 레지스트리 통신 & 레이어 캐싱 다이어그램

```
[ 개발자 Mac (Apple M5) ]
    │
    ├── 1. docker build -t skala-webserver:1.0.0 .
    ├── 2. docker tag skala-webserver:1.0.0 harbor.skala-gj.com/skala-gj4/skala-webserver:1.0.0
    │
    ▼ 3. docker push (OCI Distribution API)
┌────────────────────────────────────────────────────────────────────────┐
│ 🌐 Harbor / Local Registry (v2 API)                                    │
│                                                                        │
│  [1차 푸시: 1.0.0]                                                      │
│  ├── Layer 1~9 (nginx base) ────────► [Pushed] (새로 업로드)            │
│  └── Layer 10 (index.html)  ────────► [Pushed] (새로 업로드)            │
│                                                                        │
│  [2차 푸시: 1.0.1 (index.html 수정)]                                     │
│  ├── Layer 1~9 (nginx base) ────────► [Layer already exists] (재사용!) │
│  └── Layer 10 (index.html 신규) ─────► [Pushed] (초고속 완료)           │
│                                                                        │
│  [API 엔드포인트]                                                      │
│  • GET /v2/_catalog                     ➔ 등록된 저장소 목록           │
│  • GET /v2/<repo>/tags/list             ➔ 버전 태그 목록               │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 📊 실측 검증 결과 (Apple Silicon / Docker Desktop)

### 1차 푸시 (전체 레이어 전송)
```
The push refers to repository [localhost:5005/skala-gj4/skala-webserver]
7feeb37758e4: Pushed
e6cb77a8803a: Pushed
aca9e7c5ccc1: Pushed
3be296cd3c97: Pushed
5de55e5ef9c0: Pushed
1b000889fffe: Pushed
b0b83f78ef50: Pushed
4285d6d4062c: Pushed
ed0e37fc3a99: Pushed
c3ee22b57f6b: Pushed
1.0.0: digest: sha256:757fe9a38dab030d742095601a127771c48826d9b2f7983bdb2b4d835f0323fe size: 856
```

### 2차 푸시 (레이어 캐시 재사용 테스트)
```
1b000889fffe: Layer already exists
3be296cd3c97: Layer already exists
7feeb37758e4: Layer already exists
c3ee22b57f6b: Layer already exists
aca9e7c5ccc1: Layer already exists
4285d6d4062c: Layer already exists
ed0e37fc3a99: Layer already exists
5de55e5ef9c0: Layer already exists
e6cb77a8803a: Layer already exists
1.0.1: digest: sha256:757fe9a38dab030d742095601a127771c48826d9b2f7983bdb2b4d835f0323fe size: 856
```

---

## 🔧 실전 트러블슈팅 가이드

### 1. `push access denied, repository does not exist or may require authorization`
* **원인**: `docker tag` 시 앞에 `harbor.skala-gj.com/skala-gj4/`를 붙이지 않고 `docker push skala-webserver:1.0.0`을 실행하여 퍼블릭 Docker Hub로 요청이 전송됨.
* **처방**:
  ```bash
  docker tag skala-webserver:1.0.0 harbor.skala-gj.com/skala-gj4/skala-webserver:1.0.0
  docker push harbor.skala-gj.com/skala-gj4/skala-webserver:1.0.0
  ```

### 2. macOS에서 5000번 포트 충돌 (`port is already allocated`)
* **원인**: macOS Monterey 이후 AirPlay Receiver 데몬이 기본 5000번 포트를 점유하고 있음.
* **처방**: 로컬 레지스트리는 안전하게 **5005 포트**(`-p 5005:5000`)로 우회 매핑하여 구동합니다.

### 3. 쿠버네티스 EKS 배포 시 `exec format error`
* **원인**: Mac Apple Silicon(ARM64)에서 빌드된 이미지는 x86_64(AMD64) 기반의 EKS 워커 노드에서 실행 불가능.
* **처방**:
  ```bash
  docker buildx build --platform linux/amd64 -t harbor.skala-gj.com/skala-gj4/skala-webserver:1.0.0 --push .
  ```

---

## 📁 파일 구성

```
05_harbor_registry/
├── Dockerfile          # Nginx Alpine 기반 경량 웹서버 이미지 명세서
├── index.html          # SKALA 4반 G124 안성민 전용 수료 페이지
├── push.sh             # Harbor 빌드 ➔ 태그 ➔ 푸시 원클릭 자동화 스크립트
├── rehearsal-local.sh  # 로컬 5005 포트 OCI Registry 예행연습 스크립트
└── README.md           # 본 엔지니어링 실습 가이드 문서
```
