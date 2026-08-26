# ==============================================================================
# [SKALA Cloud & K8s] Spring Boot 3 프로덕션 멀티스테이지 Dockerfile
# ==============================================================================
# • 목적: 700MB가 넘는 무거운 JDK 빌드 환경을 최종 런타임에서 완전히 제거하여
#         137MB 크기의 초경량, 고보안 JRE 컨테이너 이미지로 다이어트합니다.
# • 핵심 원칙:
#   1. 빌드/실행 환경 물리적 분리 (Multi-Stage Build)
#   2. 빌드 캐시 극대화 (의존성 파일 먼저 COPY -> 소스코드 나중에 COPY)
#   3. 보안 강화: Non-Root 사용자(UID 10001) 실행 (K8s runAsNonRoot 호환)
#   4. 컨테이너 인식 JVM 옵션(-XX:MaxRAMPercentage=75) 및 cgroups OOM 즉시 종료
# ==============================================================================

# ── 1단계: 빌드 스테이지 (JDK 21 컴파일러 환경) ──────────────────────────
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /build

# [캐시 최적화 1단계]: gradle 래퍼와 빌드 명세서만 먼저 복사
# 소스코드가 바뀌어도 의존성 라이브러리가 그대로면 이 레이어는 캐시되어 0.1초 만에 통과합니다.
COPY gradlew settings.gradle build.gradle ./
COPY gradle/ gradle/
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# [캐시 최적화 2단계]: 자주 변경되는 Java 소스코드를 가장 마지막에 복사하여 빌드
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

# ── 2단계: 런타임 스테이지 (가벼운 JRE 21 실행 환경) ─────────────────────────
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# [보안 철칙]: root 권한(UID 0) 탈취 방지를 위해 전용 비특권 시스템 계정(app:10001) 생성
# K8s SecurityContext의 runAsNonRoot: true 정책을 100% 충족합니다.
RUN addgroup --system --gid 10001 app  && adduser --system --uid 10001 --ingroup app app

# 1단계 빌더 컨테이너에서 최종 완성된 Spring Boot 단일 실행 JAR 파일만 쏙 가져옴
COPY --from=builder /build/build/libs/*.jar app.jar

# 생성한 비특권 계정으로 컨테이너 실행 권한 전환
USER 10001

# 8080: 비즈니스 API 포트 / 8081: K8s 헬스체크 및 메트릭 전용 내부 관리 포트
EXPOSE 8080 8081

# [실행 철칙 - Exec Form 필수]:
# 반드시 JSON 배열 형태 ["java", ...] 로 작성해야 리눅스 PID 1을 Java가 직접 차지하여
# K8s의 SIGTERM 종료 신호를 받아 Graceful Shutdown(진행 중인 요청 마무리)이 동작합니다.
# -XX:MaxRAMPercentage=75: K8s Pod 메모리 Limit의 75%를 힙 메모리로 동적 자동 할당
# -XX:+ExitOnOutOfMemoryError: OOM 발생 시 프로세스를 즉시 종료시켜 K8s가 새 Pod로 자가치유 유도
ENTRYPOINT ["java",   "-XX:MaxRAMPercentage=75",   "-XX:+ExitOnOutOfMemoryError",   "-jar", "/app/app.jar"]
