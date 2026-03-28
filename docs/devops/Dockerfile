# =================================================================
# Multi-stage Dockerfile for claude-j
# Build: docker build -f docs/devops/Dockerfile -t claude-j .
# Run:   docker run -p 8080:8080 claude-j
# =================================================================

# Stage 1: Build
FROM eclipse-temurin:8-jdk AS builder

WORKDIR /app
COPY pom.xml .
COPY claude-j-domain/pom.xml claude-j-domain/
COPY claude-j-application/pom.xml claude-j-application/
COPY claude-j-adapter/pom.xml claude-j-adapter/
COPY claude-j-infrastructure/pom.xml claude-j-infrastructure/
COPY claude-j-start/pom.xml claude-j-start/

# Download dependencies (cached layer)
RUN apt-get update && apt-get install -y maven && \
    mvn dependency:go-offline -B || true

COPY . .
RUN mvn package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:8-jre

WORKDIR /app

COPY --from=builder /app/claude-j-start/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=dev"]
