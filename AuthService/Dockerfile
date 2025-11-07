FROM maven:3.8.4-openjdk-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/target/AuthService-0.0.1-SNAPSHOT.jar api.jar
EXPOSE 8083
ENV SPRING_PROFILES_ACTIVE=docker
ENTRYPOINT ["java", "-jar", "api.jar"]