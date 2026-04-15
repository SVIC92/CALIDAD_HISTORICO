# Multi-stage build for Spring Boot on Render (Docker)
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache dependencies first
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

# Build application
COPY src ./src
RUN mvn -q -DskipTests clean package

# Runtime image
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/GestionInscripcionCursos-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

# Render injects PORT; fallback to 8080 for local runs
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
