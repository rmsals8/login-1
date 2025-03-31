FROM openjdk:17-jdk-slim

# 작업 디렉토리 설정
WORKDIR /app

# 환경변수 기본값 설정
ENV SPRING_DATASOURCE_URL=jdbc:mariadb://localhost:3306/login
ENV SPRING_DATASOURCE_USERNAME=root
ENV SPRING_DATASOURCE_PASSWORD=1q2w3e$
ENV SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.mariadb.jdbc.Driver
ENV SERVER_PORT=8080

# JAR 파일 복사
COPY backend/build/libs/*.jar app.jar

# 포트 노출
EXPOSE ${SERVER_PORT}

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "/app/app.jar"]