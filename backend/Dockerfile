# ----------------------------------------------------
# Stage 1: Build the Spring Boot Application
# ----------------------------------------------------
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copy pom.xml and src
COPY pom.xml .
COPY src ./src

# Package production executable jar
RUN mvn clean package -DskipTests

# ----------------------------------------------------
# Stage 2: Minimal Production JRE Runtime
# ----------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

ENV PORT=10000
EXPOSE 10000

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
