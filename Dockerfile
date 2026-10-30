# Stage 1: Build React / Vite
FROM node:22-alpine AS frontend-build

WORKDIR /app/frontend

COPY frontend/package*.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build

# Stage 2: Build Spring Boot
FROM maven:3.9.6-eclipse-temurin-21 AS backend-build

WORKDIR /app/backend

COPY backend/ ./

# Copy compiled React assets into Spring Boot
COPY --from=frontend-build /app/frontend/dist/ ./src/main/resources/static/

RUN mvn clean package -DskipTests


# Stage 3: Production
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=backend-build /app/backend/target/zeroed-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
