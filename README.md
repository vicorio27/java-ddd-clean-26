# sandbox-java26

Monolito modular empresarial construido con **Java 26**, **Spring Boot 4** y **Gradle multi-module**.
Diseñado como sandbox de arquitectura y como base preparada para evolucionar a microservicios sin una migración traumática.

## Módulos

```
app                          Composition root (Spring Boot, REST, wiring)
application                  Casos de uso (orquestación, sin framework)
domain-orders                Aggregate Order, puertos, eventos, OrderDomainService
domain-payments              Aggregate Payment, puertos
domain-inventory             StockItem, reglas de reserva
domain-customers             Customer aggregate
domain-notifications         Notification, NotificationSender port
shared-kernel                DomainEvent, AggregateRoot, Money, CustomerId, DomainException
infrastructure-persistence   Adaptadores JPA + Flyway + optimistic locking
infrastructure-kafka         Publisher/consumer Kafka (consumer idempotente)
infrastructure-llm           Implementaciones de LLMPort (mock, Ollama)
infrastructure-notifications NotificationSender de logging
infrastructure-payment-gateway PaymentGateway fake
architecture-tests           Reglas ArchUnit
```

Regla de dependencias entre módulos:

```
app → infrastructure-* → application → domain-* → shared-kernel
```

Los módulos de dominio **no dependen de Spring** ni entre ellos; se comunican mediante
`shared-kernel` (IDs, Money) y eventos de dominio.

## Por qué evoluciona fácil a microservicios

1. Cada bounded context es un módulo Gradle aislado con su propio paquete raíz.
2. Todo acceso externo pasa por puertos (`..port..`) implementados solo en infraestructura.
3. La comunicación entre contextos es por eventos, no por llamadas directas.
4. Extraer un microservicio = mover `domain-X` + sus adaptadores + publicar sus eventos en Kafka.
5. Las reglas ArchUnit bloquean cualquier acoplamiento que rompa esa frontera.

## Pruebas de arquitectura (`architecture-tests`)

`./gradlew :architecture-tests:test` valida automáticamente:

| Regla | Test |
|---|---|
| El dominio no depende de Spring/JPA | `domainDoesNotDependOnSpring` |
| Los controladores no acceden a repositorios | `controllersDoNotAccessRepositories` |
| Solo infraestructura implementa los puertos | `onlyInfrastructureImplementsPorts` |
| Sin dependencias cíclicas entre módulos | `noCyclicDependenciesBetweenModules` |
| Los bounded contexts no se llaman entre sí | `domainModulesDoNotDependOnEachOther` |

## Ejecutar

```bash
docker compose -f docker/docker-compose.yml up -d
./gradlew :app:bootRun
```

## Demostraciones de Java 26

- **Virtual Threads**: `spring.threads.virtual.enabled=true` (todos los endpoints REST).
- **Structured Concurrency**: `GetOrderDetailsUseCase` usa `StructuredTaskScope` para consultar
  orden, cliente y pago en paralelo.
- **Records + sealed-friendly domain**: VOs como `Money`, `OrderId`, `CustomerId`.

## Roadmap (según AGENTS.md)

Redis cache, Outbox relay completo, Resilience4j, OAuth2 Resource Server, OpenTelemetry,
Testcontainers, PITest, ADRs y diagramas C4.
