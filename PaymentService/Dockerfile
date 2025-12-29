FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

COPY --from=builder /app/target/PaymentService-0.0.1-SNAPSHOT.jar api.jar

EXPOSE 8086
ENV SPRING_PROFILES_ACTIVE=docker

ENTRYPOINT ["java", "-jar", "api.jar"]



