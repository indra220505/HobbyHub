# Build Stage - use official Gradle image with JDK 17
FROM gradle:8.5-jdk17-jammy AS build
WORKDIR /app
COPY . /app
RUN gradle bootJar -x test --no-daemon

# Run Stage - lightweight JRE only
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/build/libs/app.jar app.jar

# Run as non-root user for security
RUN addgroup --system spring && adduser --system --group spring
USER spring:spring

# Expose port
EXPOSE 8080

# Healthcheck for Docker/Railway
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:8080/actuator/health/liveness || exit 1

# Environment variable for JVM tuning (can be overridden in Railway)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+HeapDumpOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
