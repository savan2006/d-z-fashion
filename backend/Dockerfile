# Multi-stage Dockerfile for D & Z Fashion Spring Boot Microservices
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Copy Maven wrapper and POM files for layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY modules/api-gateway/pom.xml modules/api-gateway/
COPY modules/identity-service/pom.xml modules/identity-service/
COPY modules/catalog-service/pom.xml modules/catalog-service/
COPY modules/inventory-service/pom.xml modules/inventory-service/
COPY modules/commerce-service/pom.xml modules/commerce-service/
COPY modules/payment-service/pom.xml modules/payment-service/
COPY modules/fulfillment-service/pom.xml modules/fulfillment-service/

# Copy all module sources and build package
COPY modules/ modules/
RUN chmod +x mvnw && ./mvnw clean package -DskipTests -B

# Minimal Alpine JRE runtime stage
FROM eclipse-temurin:21-jre-alpine AS runner
ARG SERVICE_NAME=api-gateway
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder --chown=appuser:appgroup /workspace/modules/${SERVICE_NAME}/target/${SERVICE_NAME}-*.jar app.jar

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
