---
name: skala-engineering-rules
description: >-
  Core engineering rules, security guidelines, and architectural standards for SKALA Cloud & Docker Platform.
  Enforces Apple Silicon Metal GPU native routing, Git zero-leak security sanitization, strict 'skala' filename
  filtering for RAG ingestion, dual-source SHA-256 sync, n8n 2~3 day scheduling, and human-like crawling delays.
---

# 🛡️ SKALA Platform Core Engineering Rules (안성민 전용 철칙)

본 스킬은 SKALA Cloud & Docker 플랫폼 개발, RAG 지식 인덱싱, 도커 인프라 운영 및 Git 형상 관리 시 준수해야 하는 **6대 절대 철칙**을 정의합니다.

---

## 1. 🍎 Apple Silicon Metal GPU 직접 가속 원칙 (M-Series Optimization)
* **금지**: Docker 가상머신 내부 CPU 에뮬레이션으로 로컬 LLM을 구동하는 행위 (15~90초 지연 및 타임아웃 유발).
* **철칙**: 백엔드 WAS(Spring Boot/Python)는 반드시 호스트 머신의 네이티브 Ollama 포트(`http://host.docker.internal:11434` 또는 `http://localhost:11434`)로 라우팅하여 **Apple M5 Metal GPU 가속(0.15초 TTFT, 초당 80+ 토큰)**을 활용합니다.
* **상주 설정**: 모델 언로딩으로 인한 웜업 지연을 방지하기 위해 `keep_alive: "24h"`를 유지합니다.

---

## 2. 🔒 Git 제로-누출 보안 격리 원칙 (Zero-Leak Git Policy)
* **금지**: 사내 수강생 인증 정보(학번 `G124`, 수강생 코드, 세션 쿠키), 비공개 교육 포털 도메인(`tech-learning-hub.pages.dev`), 로컬 RAG 데이터베이스(`*.db`, `*.sqlite`), 원본 강의자료(`*.pdf`, `*.pptx`)를 Git에 커밋/푸시하는 행위.
* **철칙**:
  1. 모든 인증 정보는 `os.getenv()` 환경변수로 격리합니다.
  2. 커밋 전 `git status` 및 `.gitignore`를 통해 `*.db`, `ebooks/`, `materials/`, `*.plist`, `.env*`가 철저히 제외되었는지 교차 검증합니다.
  3. 공개 문서(README, UI 코드)에는 일반적인 플레이스홀더나 로컬 인터랙티브 액션으로 대체합니다.

---

## 3. 🎯 로컬 교재 엄격 선별 필터링 원칙 (Strict SKALA Filter)
* **금지**: 다운로드 폴더(`~/Downloads`)의 모든 파일을 무분별하게 RAG DB에 넣는 행위 (개인 영수증, 공고문, 계약서 등 유입 방지).
* **철칙**:
  1. 파일명(소문자)에 **`skala` 또는 `스칼라`**가 명시적으로 포함된 파일만 RAG 색인 파이프라인에 진입시킵니다.
  2. 행정 문서 블랙리스트(`영수증`, `공고문`, `청구서`, `신청서`, `계약서`, `invoice`, `receipt`)를 감지하여 100% 차단합니다.

---

## 4. 🌐 헤드리스 듀얼 RAG 동기화 원칙 (Headless Dual-Source Sync)
* **철칙**: 지식 동기화는 2개 소스를 동시에 검토합니다:
  1. **온라인 웹 포털**: 16대 전 과목 전자책의 **SHA-256 해시 Diff**를 백그라운드에서 감지하여 변경된 과목만 0.5초 만에 증분 업데이트합니다.
  2. **로컬 다운로드**: `skala` 명명 교재를 부모(슬라이드)-자식(개념/코드) 계층형으로 청킹하여 FTS5 BM25 인덱스에 삽입합니다.

---

## 5. ⏰ n8n 2~3일 주기 정기 자동화 & Gmail 리포트 원칙
* **철칙**:
  1. 매일 반복하지 않고 **2일 또는 3일 주기(`0 9 */2 * *`)**로 정기 검토를 실행합니다.
  2. 검토 완료 후 변경된 통계와 신규 교재 목록을 카드형 HTML 이메일로 생성하여 **`tjdals2299@gmail.com`**으로 발송합니다.

---

## 6. 🚶 사람 속도 예의 바른 크롤링 원칙 (Polite Crawling Policy)
* **철칙**: 교육 사이트 수집 시 DoS/부하를 방지하기 위해 각 요청마다 **1.0~2.5초의 자연스러운 무작위 딜레이(`random.uniform`)**와 표준 브라우저 User-Agent를 사용합니다.
