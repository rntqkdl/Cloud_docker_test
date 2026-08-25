# 🐧 STAGE 00 : 컨테이너 리눅스 내부 구조 & 앱 컨테이너화 3단계 진화

<p align="center">
  <img src="https://img.shields.io/badge/Linux%20Kernel-PID%201%20Init%20Process-blue?style=for-the-badge&logo=linux" alt="Kernel">
  <img src="https://img.shields.io/badge/FastAPI-v1.0%20%7C%20v1.1%20%7C%20v1.2-emerald?style=for-the-badge&logo=fastapi" alt="FastAPI">
  <img src="https://img.shields.io/badge/OCI%20Image-Layer%20Deep%20Inspection-orange?style=for-the-badge&logo=docker" alt="OCI">
  <img src="https://img.shields.io/badge/Pip%20Optimization---no--cache--dir%20Check-teal?style=for-the-badge" alt="Pip">
</p>

> **교재 연계 출처**:
> - 📖 `Cloud_컨테이너 이해 및 앱 컨테이너화_2608.pdf` — **3. 웹 서비스 실행 컨테이너 만들기 (34~49쪽)** & **4. 컨테이너 이미지 구조 (50~65쪽)**
> - 📑 `Cloud_컨테이너 이해 및 앱 컨테이너화 실습_2608.pdf` — **컨테이너화 기초 실습**

---

## ⚡ 빠른 시작 (Quick Start)

```bash
# 1. 3단계 앱 컨테이너화 진화 과정 자동 실행 & 검증
./run.sh

# 2. OCI 이미지 구조 및 레이어 심층 뜯어보기 (OCI Layout, pip 캐시 누수 진단)
./inspect-image.sh
```

---

## 💡 컨테이너화 3단계 진화 매트릭스 (Evolution Matrix)

| 구분 | 🔴 v1.0 (외장 볼륨 껍데기) | 🟡 v1.1 (코드 내장, 수동 실행) | 🟢 v1.2 (완전 자동화 엔터프라이즈) |
| :--- | :--- | :--- | :--- |
| **코드 위치** | 호스트 머신 (`-v` 볼륨 마운트) | 컨테이너 이미지 내부 (`COPY`) | 컨테이너 이미지 내부 (`COPY`) |
| **포트 매핑** | 미개방 (호스트 `curl` 시 `000` 실패) | `-p 8888:8080` 포트 포워딩 | `-p 8888:8080` 포트 포워딩 |
| **실행 주체** | 사람이 수동으로 `docker exec` | 사람이 수동으로 `docker exec` | 컨테이너가 스스로 구동 (`CMD`) |
| **컨테이너 부팅 직후** | 빈 껍데기 대기 (`sleep infinity`) | 빈 껍데기 대기 (`sleep infinity`) | **부팅 즉시 200 OK 서비스 개시** |
| **재시작 (`restart`) 시** | 웹서버 사망 ➔ 수동 재실행 필요 | 웹서버 사망 ➔ 수동 재실행 필요 | **웹서버 자동 복구 (`200 OK`)** |
| **PID 1 역할** | `sh -c sleep infinity` | `sh -c sleep infinity` | **`python /mycode/webserver.py`** |

---

## 🛡️ 컨테이너 프로세스 4대 핵심 철칙

1. **`0.0.0.0` 바인딩 원칙 (Host Binding)**:
   - 컨테이너 내부 웹서버는 반드시 `uvicorn.run(..., host="0.0.0.0")`으로 바인딩해야 합니다.
   - `127.0.0.1`로 실행할 경우 컨테이너 내부 루프백에서만 통신되고 호스트 포트포워딩으로 접근 시 `000 Connection Refused`가 발생합니다.
2. **`EXPOSE` vs `-p` 격리 원칙**:
   - `Dockerfile`의 `EXPOSE`는 단순 문서화 명세일 뿐 실제 포트를 열지 않습니다. 실제로 호스트 네트워크 길을 여는 것은 `docker run` 시의 `-p 8888:8080`입니다.
3. **PID 1 생명주기 일치 원칙 (PID 1 Lifecycle)**:
   - 컨테이너의 수명은 **PID 1 프로세스의 수명**과 정확히 일치합니다. 메인 애플리케이션(`webserver.py`)이 PID 1이어야 비정상 종료 시 컨테이너가 감지하고 오케스트레이터(K8s)가 자가 치유(Self-healing)할 수 있습니다.
4. **빌드 캐시 다이어트 원칙 (`--no-cache-dir`)**:
   - `pip install` 시 `--no-cache-dir`을 주지 않으면 불필요한 pip 휠 캐시(293개 파일)가 불변 레이어에 영구 고착되어 이미지 크기가 낭비됩니다.

---

## 🏛️ OCI 이미지 구조 분석 (Image Anatomy)

`inspect-image.sh` 실측 결과 (Apple Silicon M-Series):
* `docker save` 결과물: 최신 **OCI 이미지 레이아웃**(`index.json`, `manifest.json`, `blobs/sha256/`)
* 전체 레이어 수: 8개 (Base 6개 + `pip install` 1개 + `COPY mycode` 1개)
* 레이어 공유: `container-linux:1.0`, `1.1`, `1.2`는 공통 레이어를 100% 공유하여 디스크 용량을 낭비하지 않습니다.

---

## 📁 파일 구성

```
00_container_linux/
├── v1.0/Dockerfile     # 껍데기 컨테이너 (sleep infinity)
├── v1.1/Dockerfile     # 코드 내장 컨테이너 (COPY)
├── v1.2/Dockerfile     # 완전 자동화 컨테이너 (CMD)
├── webserver.py        # FastAPI 실습 웹서버 (8080 포트)
├── mycode.py           # 볼륨 검증용 스크립트
├── run.sh              # 3단계 진화 시연 자동화 스크립트
├── inspect-image.sh    # OCI 레이어 심층 분석 스크립트
└── README.md           # 본 기술 가이드 문서
```
