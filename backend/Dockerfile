# 빌드: gradle wrapper(9.5.1) 사용 — Spring Boot 4.x 호환 (이미지의 구버전 gradle 대신)
FROM public.ecr.aws/docker/library/eclipse-temurin:17-jdk AS build
WORKDIR /src
COPY . .
RUN chmod +x ./gradlew && ./gradlew build --no-daemon -x test

# 런타임: bootJar만 복사 (-plain.jar 제외)
FROM public.ecr.aws/docker/library/eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/build/libs/*-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]