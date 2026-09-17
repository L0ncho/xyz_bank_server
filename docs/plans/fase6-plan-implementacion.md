# FASE 6 — Plan de Implementación

Fecha: 2026-09-16

## Overview

Extraer el módulo interests de core-service a un microservicio independiente con Spring Cloud Config, Eureka y Resilience4j, siguiendo TDD y sin romper producción.

## Tareas

- [ ] Implementar config-server con repositorio native
- [ ] Implementar eureka-server standalone
- [ ] Crear estructura base de interests-service con TDD
- [ ] Integrar Resilience4j (circuit breaker, retry, timelimiter)
- [ ] Configurar OAuth2 client credentials (si auth-server existe)
- [ ] Agregar feature flag en core-service
- [ ] Agregar router y flag en bff-web
- [ ] Actualizar docker-compose.yml con nuevos servicios
- [ ] Tests E2E de circuit breaker y fallback

---

## Orden de implementación

```
1. config-server
2. eureka-server  
3. interests-service (estructura + dominio)
4. Integración con core-service (Resilience4j + OAuth2)
5. Feature flags de desactivación
6. Migración de bff-web
```

---

## PASO 1: config-server

### Qué se crea
- Módulo `platform/config-server/`
- Repositorio `config-repo/` con `interests-service.yml`
- Entrada en `pom.xml` raíz y `docker-compose.yml`

### Archivos principales
- `platform/config-server/pom.xml`
- `platform/config-server/src/main/java/.../ConfigServerApplication.java`
- `platform/config-server/src/main/resources/application.yml`
- `platform/config-server/Dockerfile`
- `config-repo/interests-service.yml`

### Riesgos

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|--------------|---------|------------|
| Conflicto de puerto 8888 | Baja | Bajo | Verificar puertos libres antes |
| Config server no arranca | Baja | Medio | Test de smoke en CI |
| Dependencias Spring Cloud incompatibles | Media | Alto | Usar BOM 2024.0.0 compatible con Boot 3.5 |

### Validación
```bash
# 1. Arrancar config-server
mvn -pl platform/config-server spring-boot:run

# 2. Verificar health
curl http://localhost:8888/actuator/health
# Esperado: {"status":"UP"}

# 3. Verificar configuración servida
curl http://localhost:8888/interests-service/default
# Esperado: JSON con core-service.base-url, resilience4j.*
```

---

## PASO 2: eureka-server

### Qué se crea
- Módulo `platform/eureka-server/`
- Entrada en `pom.xml` raíz y `docker-compose.yml`

### Archivos principales
- `platform/eureka-server/pom.xml`
- `platform/eureka-server/src/main/java/.../EurekaServerApplication.java`
- `platform/eureka-server/src/main/resources/application.yml`
- `platform/eureka-server/Dockerfile`

### Riesgos

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|--------------|---------|------------|
| Conflicto de puerto 8761 | Baja | Bajo | Verificar puertos libres |
| Self-preservation bloquea servicios | Media | Medio | Desactivar en dev |
| Eureka no detecta servicios caídos | Baja | Medio | eviction-interval: 5000ms en dev |

### Validación
```bash
# 1. Arrancar eureka-server
mvn -pl platform/eureka-server spring-boot:run

# 2. Verificar health
curl http://localhost:8761/actuator/health
# Esperado: {"status":"UP"}

# 3. Verificar dashboard (browser)
# http://localhost:8761
# Esperado: Dashboard con "No instances available"
```

---

## PASO 3: interests-service (estructura + dominio)

### Qué se crea
- Módulo `platform/interests-service/`
- Estructura hexagonal: domain, application, infrastructure
- Copia de DTOs y puertos desde core-service (solo lectura)
- Registro en Eureka

### Archivos principales
- `platform/interests-service/pom.xml`
- `platform/interests-service/src/main/java/.../InterestsServiceApplication.java`
- `platform/interests-service/src/main/resources/application.yml`
- `.../interestview/application/dto/InterestSummaryResponse.java`
- `.../interestview/application/ports/CoreServicePort.java`
- `.../interestview/application/usecases/GetInterestSummaryUseCase.java`
- `.../interestview/infrastructure/rest/InterestController.java`
- `.../interestview/infrastructure/adapters/HttpCoreServiceAdapter.java`
- Tests unitarios y E2E

### Riesgos

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|--------------|---------|------------|
| No conecta a config-server | Media | Medio | `optional:` prefix, defaults locales |
| No se registra en Eureka | Media | Medio | Verificar logs de registro |
| Puerto 8084 ocupado | Baja | Bajo | Configurar puerto diferente |
| Timeout conectando a core-service | Media | Medio | Resilience4j (paso 4) |

### Validación
```bash
# 1. Arrancar (requiere config-server y eureka-server UP)
mvn -pl platform/interests-service spring-boot:run

# 2. Verificar health
curl http://localhost:8084/actuator/health
# Esperado: {"status":"UP"}

# 3. Verificar registro en Eureka
# http://localhost:8761 → INTERESTS-SERVICE (1 instancia UP)

# 4. Verificar endpoint (requiere core-service UP)
curl "http://localhost:8084/accounts/22222222-2222-2222-2222-222222222222/interest-summary?year=2025"
# Esperado: JSON con accountId, year, interestAmount, etc.
```

---

## PASO 4: Integración con core-service (Resilience4j + OAuth2)

### Qué se agrega
- Dependencias Resilience4j en interests-service
- Configuración de circuit breaker, retry, timelimiter
- Cliente OAuth2 para autenticación service-to-service
- Tabla `pending_adjustments` (para fallback de escritura futura)
- Tests de circuit breaker con WireMock

### Archivos principales
- Modificar `platform/interests-service/pom.xml` (agregar resilience4j)
- `config-repo/interests-service.yml` (configuración resilience4j)
- `.../shared/infrastructure/resilience/CircuitBreakerConfig.java`
- `.../interestview/infrastructure/adapters/HttpCoreServiceAdapter.java` (agregar anotaciones)
- Tests: `CircuitBreakerIntegrationTest.java`

### Riesgos

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|--------------|---------|------------|
| Circuit breaker muy sensible | Media | Medio | Tuning en config-repo (ajustable sin redeploy) |
| OAuth2 token no refresh | Baja | Alto | Spring Security maneja automáticamente |
| Fallback no persiste | Baja | Alto | Test específico de fallback |
| Retry duplica llamadas POST | Alta | Alto | Solo retry en GET, no en POST |

### Validación del Circuit Breaker

```bash
# TEST 1: Comportamiento normal
# core-service UP → interests-service responde OK

# TEST 2: Simular falla de core-service
docker stop xyz-bank-core-service

# Llamar a interests-service 6 veces (supera minimumNumberOfCalls=5)
for i in {1..6}; do
  curl -s "http://localhost:8084/accounts/22222222.../interest-summary?year=2025"
done

# Verificar logs de interests-service:
# Esperado: "CircuitBreaker 'coreServiceInterests' is OPEN"

# TEST 3: Verificar que circuit breaker rechaza llamadas
curl "http://localhost:8084/accounts/22222222.../interest-summary?year=2025"
# Esperado: 503 Service Unavailable (sin intentar llamar a core-service)

# TEST 4: Verificar health indicator
curl http://localhost:8084/actuator/health
# Esperado: circuitBreakers.coreServiceInterests.state = "OPEN"

# TEST 5: Esperar waitDurationInOpenState (30s) y verificar HALF_OPEN
sleep 35
curl http://localhost:8084/actuator/health
# Esperado: state = "HALF_OPEN"

# TEST 6: Restaurar core-service y verificar recuperación
docker start xyz-bank-core-service
# Esperar health
curl "http://localhost:8084/accounts/22222222.../interest-summary?year=2025"
# Esperado: 200 OK, circuit breaker vuelve a CLOSED
```

---

## PASO 5: Feature flags de desactivación

### Qué se agrega
- Feature flag en core-service (`features.interests.enabled`)
- Métricas Prometheus para detección de duplicidad
- Logs estructurados

### Archivos principales
- Modificar `platform/core-service/src/main/resources/application.yml`
- `.../interests/config/InterestsFeatureFlag.java`
- Modificar `.../interests/infrastructure/rest/InterestController.java`
- Tests: `InterestControllerFeatureFlagTest.java`

### Riesgos

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|--------------|---------|------------|
| Flag mal configurado en prod | Media | Alto | Default=true (backward compatible) |
| Olvidar activar shadow mode | Baja | Medio | Checklist de deploy |
| Logs no distinguen origen | Baja | Bajo | Campo `source` en logs |

### Validación
```bash
# TEST 1: Flag enabled=true (default)
curl "http://localhost:8080/internal/accounts/.../interest-summary?year=2025"
# Esperado: 200 OK (comportamiento normal)

# TEST 2: Flag shadow-mode=true
FEATURE_INTERESTS_SHADOW=true docker compose up -d core-service
curl "http://localhost:8080/internal/accounts/.../interest-summary?year=2025"
# Esperado: 503 Service Unavailable + header X-Shadow-Mode: true
# Log: "SHADOW_MODE: Interest request received but not processed"

# TEST 3: Flag enabled=false
FEATURE_INTERESTS_ENABLED=false docker compose up -d core-service
curl "http://localhost:8080/internal/accounts/.../interest-summary?year=2025"
# Esperado: 410 Gone + header X-Deprecated-Endpoint
```

---

## PASO 6: Migración de bff-web

### Qué se agrega
- Feature flag en bff-web (`features.interests.use-interests-service`)
- Router que decide entre core-service e interests-service
- Nuevo adapter `HttpInterestsServiceAdapter`

### Archivos principales
- Modificar `bff/bff-web/src/main/resources/application.yml`
- `.../interestview/infrastructure/adapters/HttpInterestsServiceAdapter.java`
- `.../interestview/config/InterestServiceRouter.java`
- Tests: `InterestServiceRouterTest.java`

### Riesgos

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|--------------|---------|------------|
| bff-web no encuentra interests-service | Media | Alto | URL en config, no Eureka discovery |
| Respuesta incompatible | Baja | Alto | Mismo DTO, tests de contrato |
| Rollback lento | Baja | Medio | Solo cambio de env var |

### Validación
```bash
# TEST 1: Flag use-interests-service=false (default)
curl -k "https://localhost:8081/accounts/.../interest-summary?year=2025" \
  -H "Authorization: Bearer $WEB_JWT"
# Verificar logs bff-web: "Routing interest request to core-service (legacy)"

# TEST 2: Flag use-interests-service=true
FEATURE_USE_INTERESTS_SERVICE=true docker compose up -d bff-web
curl -k "https://localhost:8081/accounts/.../interest-summary?year=2025" \
  -H "Authorization: Bearer $WEB_JWT"
# Verificar logs bff-web: "Routing interest request to interests-service"
# Respuesta debe ser idéntica

# TEST 3: Verificar que core-service NO recibe requests (shadow mode)
# Activar shadow en core-service, verificar 0 logs de INTEREST_REQUEST source=core-service
```

---

## Orden de dependencias en Docker Compose

```yaml
# Orden de arranque:
# 1. postgres (healthcheck: pg_isready)
# 2. config-server (healthcheck: /actuator/health)
# 3. eureka-server (depends_on: config-server)
# 4. core-service (depends_on: postgres)
# 5. interests-service (depends_on: config-server, eureka-server, core-service)
# 6. bff-web, bff-mobile, bff-atm (depends_on: core-service, interests-service)
```

---

## Resumen de riesgos por paso

| Paso | Riesgo principal | Mitigación |
|------|------------------|------------|
| 1. config-server | Incompatibilidad Spring Cloud | BOM 2024.0.0 |
| 2. eureka-server | Self-preservation | Desactivar en dev |
| 3. interests-service | No conecta a core | Resilience4j (paso 4) |
| 4. Resilience4j | Circuit breaker muy sensible | Config centralizada, tuning |
| 5. Feature flags | Flag mal configurado | Default backward compatible |
| 6. Migración bff-web | Respuesta incompatible | Tests de contrato |

---

## Criterio de éxito final

- [ ] interests-service responde en <500ms p99
- [ ] Circuit breaker corta conexión tras 5 fallos
- [ ] Circuit breaker se recupera automáticamente
- [ ] bff-web usa interests-service con flag activo
- [ ] core-service en shadow mode muestra 0 requests
- [ ] Alerta de duplicidad NO se dispara
- [ ] Rollback funciona en <5 minutos
