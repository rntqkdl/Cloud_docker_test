# 1단계: 빌드 환경 (Maven + JDK 17)
FROM maven:3.9-amazoncorretto-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# 2단계: 순수 실행 환경 (경량 Amazon Corretto JRE 17)
FROM amazoncorretto:17-alpine
WORKDIR /app
COPY --from=builder /app/target/dashboard-backend-*.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS="-Xms128m -Xmx256m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
