# Build Stage - use official Gradle image with JDK 17
FROM gradle:8.5-jdk17-jammy AS build
WORKDIR /app
COPY . /app
RUN gradle bootJar -x test --no-daemon

# Run Stage - lightweight JRE only
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/build/libs/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
