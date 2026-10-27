# ----------------------------------------------------
# Stage 1: Build the Spring Boot Application
# ----------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

ENV MAVEN_OPTS="-Xmx350m -XX:+UseSerialGC"

# Copy pom.xml and src from backend directory
COPY backend/pom.xml ./pom.xml
COPY backend/src ./src

# Package production executable jar
RUN mvn clean package -DskipTests

# ----------------------------------------------------
# Stage 2: Production JRE Runtime (Ubuntu/glibc based)
# ----------------------------------------------------
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

ENV PORT=10000
EXPOSE 10000

# Strict JVM memory constraints to fit within Render Free Tier 512MB RAM cap
ENTRYPOINT ["java", "-Xmx300m", "-Xms150m", "-XX:+UseSerialGC", "-XX:MaxMetaspaceSize=128m", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
