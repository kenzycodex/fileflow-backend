FROM openjdk:17-slim as build

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

RUN ./mvnw package -DskipTests

FROM openjdk:17-slim

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]