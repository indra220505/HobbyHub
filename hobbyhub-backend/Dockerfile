# Build Stage
FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY . /app
RUN gradle bootJar -x test --no-daemon

# Run Stage
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/build/libs/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
