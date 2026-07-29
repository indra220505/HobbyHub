# Build Stage
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY . /app
RUN chmod +x gradlew || true
RUN if [ -f gradlew ]; then ./gradlew bootJar -x test --no-daemon; else gradle bootJar -x test --no-daemon; fi

# Run Stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
