# ========================================================
# FoodExpress Multi-Stage Docker Build
# Stage 1: Build JAR using Maven & Java 21
# Stage 2: Minimal Production JRE Runtime
# ========================================================

FROM maven:3.9.6-eclipse-temurin-21-jammy AS build
WORKDIR /app

# Cache Maven dependencies
COPY pom.xml .
COPY backend/pom.xml backend/
RUN mvn dependency:go-offline -B -f backend/pom.xml || true

# Copy source code and build executable JAR
COPY backend/src backend/src
RUN mvn clean package -DskipTests -f backend/pom.xml

# Runtime Stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Add unprivileged user for security
RUN useradd -m fooduser && chown -R fooduser:fooduser /app
USER fooduser

# Copy JAR from build stage
COPY --from=build --chown=fooduser:fooduser /app/backend/target/foodexpress-backend-1.0.0.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
