# 1. Base Image (JAVA 17 실행 환경)
FROM openjdk:17-jdk-slim

# 2. Jar file locatio (빌드된 결과물 경로)
ARG JAR_FILE=build/libs/*.jar

# 3. Copy Jar (컨테이너 내부로 파일 복사)
COPY ${JAR_FILE} app.jar

# 4. Run (실행 명령어)
# -Duser.timezone=Asia/Seoul : 서버 시간을 한국 시간을 고정
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "/app.jar"]