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
infrastructure-persistence   Adaptadores JPA + Flyway + optimistic locking + outbox
infrastructure-kafka         Relay del outbox + consumer Kafka
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
| La capa de aplicación no depende de Spring | `applicationLayerDoesNotDependOnSpring` |
| Los controladores no acceden a repositorios | `controllersDoNotAccessRepositories` |
| Solo infraestructura implementa los puertos | `onlyInfrastructureImplementsPorts` |
| Sin dependencias cíclicas entre módulos | `noCyclicDependenciesBetweenModules` |
| Los bounded contexts no se llaman entre sí | `domainModulesDoNotDependOnEachOther` (los 20 pares) |

El import de ArchUnit falla de forma explícita si no analiza ninguna clase: una regla que
pasa en vacío es peor que no tenerla.

## Consistencia: outbox, transacciones e idempotencia

**Un evento nunca se publica directamente a Kafka.** El caso de uso escribe el agregado y su
evento en el mismo commit; `OutboxRelay` publica después y solo marca `published = true`
cuando el broker confirma. Si Kafka está caído, la orden se crea igual y el evento se
reintenta en el siguiente ciclo.

```
CreateOrderUseCase ──┐ (una transacción)
                     ├─→ orders
                     └─→ outbox_events           OutboxRelay ──→ Kafka ──→ OrderEventsConsumer
```

La frontera transaccional entra en la capa de aplicación por el puerto `UnitOfWork`, no por
`@Transactional`: así los casos de uso siguen sin conocer Spring y la regla ArchUnit lo vigila.

Pagar exige cabecera `Idempotency-Key`; repetir la petición devuelve el pago existente sin
volver a cobrar. La unicidad la garantiza el índice de `payments.idempotency_key`.

```bash
curl -X POST localhost:8080/api/v1/orders/$ORDER_ID/payments \
  -H "Idempotency-Key: $(uuidgen)"
```

## Pruebas

```bash
./gradlew build -x :app:test     # unitarias + arquitectura
./gradlew :app:test              # integración: Postgres y Kafka reales (requiere Docker)
```

`OrderLifecycleIT` recorre create → outbox → relay → Kafka → pay contra contenedores reales,
y verifica que un reintento con la misma clave no cobra dos veces, que `created_at` no se
reescribe y que la versión del optimistic locking avanza.

## Ejecutar

```bash
docker compose -f docker/docker-compose.yml up -d
./gradlew :app:bootRun
```

## Java 26 y bytecode 25

El build **compila con el JDK 26** (toolchain, auto-aprovisionado vía foojay) pero emite
bytecode 25 (`options.release = 25`). El motivo es concreto: el ASM que empaqueta ArchUnit
1.4.1 solo lee hasta class file V25, y con bytecode V26 descarta todas las clases *en
silencio*, dejando las reglas de arquitectura pasando en vacío. Se revisa en cada subida
de ArchUnit.

Por lo mismo, el código de producción no usa APIs preview: `GetOrderDetailsUseCase`
paraleliza con `Executors.newVirtualThreadPerTaskExecutor()` en vez de `StructuredTaskScope`.

- **Virtual Threads**: `spring.threads.virtual.enabled=true` (todos los endpoints REST).
- **Records**: VOs como `Money`, `OrderId`, `CustomerId`.

## Roadmap

Redis cache, Resilience4j, OAuth2 Resource Server, OpenTelemetry, PITest, ADRs y diagramas C4.
Deduplicación del consumidor respaldada por base de datos (hoy es en memoria: se pierde al
reiniciar y no se comparte entre réplicas).
