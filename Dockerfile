# 1. Base Image (안정적인 Eclipse Temurin Java 17 사용)
FROM eclipse-temurin:17-jdk-focal

# 2. Jar file location
ARG JAR_FILE=build/libs/*.jar

# 3. Copy Jar
COPY ${JAR_FILE} app.jar

# 4. Run
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "/app.jar"]