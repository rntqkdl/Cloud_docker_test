# ☸️ STAGE 12 : 쿠버네티스 실전 매니페스트 (Pod, Deployment, Service, Ingress)

> **"Naked Pod의 한계부터 3대 프로브 자가 치유, 무중단 롤링 업데이트(Deployment), L4 부하 분산(Service), AWS ALB 스마트 게이트웨이(Ingress)까지 단계별 완벽 실습"**

[![Kubernetes](https://img.shields.io/badge/Kubernetes-1.28+-326ce5.svg)](https://kubernetes.io/)
[![AWS ALB](https://img.shields.io/badge/Ingress-AWS%20ALB-orange.svg)]()
[![Strategy](https://img.shields.io/badge/Deployment-RollingUpdate%20(Surge%201%2F0)-brightgreen.svg)]()
[![Architecture](https://img.shields.io/badge/Manifests-Pod%20%7C%20Deploy%20%7C%20Svc%20%7C%20Ing-purple.svg)]()

---

## 📁 디렉터리 구조 및 챕터별 매니페스트

```
12_k8s_manifests/
├── ch05-pod/
│   ├── pod.yaml                 # [5장] 단일 파드(Pod) 기본 구조 & Downward API 실습
│   └── pod-with-probes.yaml     # [5장] Startup, Liveness, Readiness 3대 프로브 완비 Pod
├── ch06-deployment/
│   ├── deployment.yaml          # [6장] Replicas 3, 무중단 롤링 배포 및 롤백 제어
│   └── job-migration.yaml       # [6장] 1회성 DB 마이그레이션 및 자동 완료 청소 Job
├── ch07-service/
│   └── service.yaml             # [7장] ClusterIP 고정 VIP, L4 로드밸런싱 및 포트 격리
└── ch08-ingress/
    └── ingress.yaml             # [8장] AWS ALB Ingress Controller L7 스마트 라우팅
```

---

## 🗺️ 단계별 실전 실습 가이드

### 📍 STEP 1. [ch05-pod] Pod 기초와 3대 프로브 자가 치유

```bash
# 1. 단일 Pod 생성 및 정보 확인
kubectl apply -f ch05-pod/pod.yaml
kubectl get pod shop-api-manual -o wide

# 2. 3대 프로브가 적용된 Pod 배포 및 READY 상태 변화 관측
kubectl apply -f ch05-pod/pod-with-probes.yaml
kubectl get pod shop-api-probes -w
# -> StartupProbe 통과 전 (0/1) -> 통과 후 트래픽 준비 완료 (1/1)

# 3. Readiness 수동 스위치 테스트 (트래픽 제외 관측)
kubectl exec -it shop-api-probes -- curl -s -X POST "http://localhost:8080/api/unready?ready=false"
kubectl get pod shop-api-probes
# -> READY 상태가 0/1로 바뀌며 Service에서 즉시 제외됨 (컨테이너는 죽지 않음!)
```

---

### 📍 STEP 2. [ch06-deployment] 무중단 롤링 배포 & 1초 롤백

```bash
# 1. Deployment 생성 (Pod 3대 기동)
kubectl apply -f ch06-deployment/deployment.yaml
kubectl get deploy,rs,pod

# 2. 새 버전(shop-api:1.0.1)으로 무중단 롤링 업데이트 실행
kubectl set image deploy/shop-api app=shop-api:1.0.1

# 3. 롤아웃 진행 상황 실시간 모니터링
kubectl rollout status deploy/shop-api --timeout=180s

# 4. 배포 이력 확인 및 비상 롤백(Rollback)
kubectl rollout history deploy/shop-api
kubectl rollout undo deploy/shop-api

# 5. DB 마이그레이션 Job 실행 및 완료 대기
kubectl apply -f ch06-deployment/job-migration.yaml
kubectl wait --for=condition=complete job/db-migration --timeout=600s
```

---

### 📍 STEP 3. [ch07-service] L4 내부 로드밸런싱 & 부하 분산

```bash
# 1. Service 생성 및 고정 ClusterIP 발급 확인
kubectl apply -f ch07-service/service.yaml
kubectl get svc shop-api
kubectl get endpoints shop-api

# 2. 임시 디버깅 Pod에서 부하 분산 반복 테스트
kubectl run nettest --rm -it --image=curlimages/curl -- sh
# (테스트 쉘 내부에서 실행)
for i in $(seq 1 10); do curl -s http://shop-api/api/info | grep -o '"pod":"[^"]*"'; done
# -> 3대의 Pod 이름이 골고루 번갈아가며 응답하는 부하 분산 확인!
```

---

### 📍 STEP 4. [ch08-ingress] AWS ALB L7 스마트 게이트웨이 연동

```bash
# 1. AWS ALB Ingress 배포
kubectl apply -f ch08-ingress/ingress.yaml

# 2. ALB 주소 할당 대기 (약 2~3분 소요)
kubectl get ingress shop-api -w

# 3. 실습 종료 후 AWS ALB 리소스 삭제 (비용 발생 방지)
kubectl delete -f ch08-ingress/ingress.yaml
```

---

## 🔧 실전 트러블슈팅 플레이북 (Troubleshooting Playbook)

| 장애 현상 | 근본 원인 (Root Cause) | 해결 처방전 (Actionable Prescription) |
| :--- | :--- | :--- |
| **`ImagePullBackOff`** | 도커 이미지 이름/태그 오타 또는 레지스트리 비인증 | `kubectl describe pod [이름]`의 `Events` 확인 후 `imagePullSecrets` 또는 태그 수정 |
| **`CrashLoopBackOff`** | Spring Boot 기동 에러 (DB 접속 불가, 포트 충돌) | `kubectl logs [이름] --previous`로 사망 직전 자바 스택트레이스 확인 |
| **`OOMKilled` (Exit 137)** | Pod의 Memory Limit(1Gi)을 초과하는 메모리 급증 | JVM `-XX:MaxRAMPercentage=75` 확인 및 YAML의 `resources.limits.memory` 증설 |
| **`Pending` 지속** | 워커 노드의 CPU/메모리 여유 공간(Requests) 부족 | `kubectl describe node`로 노드 할당량 확인 후 노드 증설 또는 Requests 하향 |
