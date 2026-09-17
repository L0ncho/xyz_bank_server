# Corrección de Dockerfiles para Nuevos Servicios

## Problema Identificado

Los Dockerfiles de los nuevos servicios (`config-server`, `eureka-server`, `interests-service`) usaban:
```dockerfile
COPY target/config-server-0.0.1-SNAPSHOT-exec.jar app.jar
```

Pero el `context` en docker-compose.yml es `.` (raíz del proyecto), por lo que Docker buscaba el JAR en `./target/` cuando realmente está en `./platform/config-server/target/`.

Además, los Dockerfiles asumían que el JAR ya fue compilado localmente, pero deberían usar **multi-stage build** como `platform/core-service/Dockerfile` para compilar dentro de Docker.

## Solución Aplicada

Se actualizaron los tres Dockerfiles para seguir el patrón de `core-service`:

### 1. config-server/Dockerfile

```dockerfile
# syntax=docker/dockerfile:1
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY platform platform
RUN --mount=type=cache,target=/root/.m2 mvn -pl platform/config-server -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache wget
COPY --from=build /src/platform/config-server/target/config-server-0.0.1-SNAPSHOT-exec.jar app.jar
EXPOSE 8888
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### 2. eureka-server/Dockerfile

```dockerfile
# syntax=docker/dockerfile:1
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY platform platform
RUN --mount=type=cache,target=/root/.m2 mvn -pl platform/eureka-server -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache wget
COPY --from=build /src/platform/eureka-server/target/eureka-server-0.0.1-SNAPSHOT-exec.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### 3. interests-service/Dockerfile

```dockerfile
# syntax=docker/dockerfile:1
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY platform platform
RUN --mount=type=cache,target=/root/.m2 mvn -pl platform/interests-service -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache wget
COPY --from=build /src/platform/interests-service/target/interests-service-0.0.1-SNAPSHOT-exec.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

## Archivos Modificados

- `platform/config-server/Dockerfile`
- `platform/eureka-server/Dockerfile`
- `platform/interests-service/Dockerfile`

## Validación

```bash
docker-compose up -d --build
```
