# Guía detallada de las capas del proyecto

Este documento explica cada capa del monolito modular `sandbox-java26`, su responsabilidad,
qué puede y qué no puede hacer, y cómo defenderlo en una entrevista técnica.

---

## 0. Visión general

```
┌─────────────────────────────────────────────────────────────┐
│  app (Composition Root)                                     │
│  Controllers REST · wiring de beans · configuración         │
└──────────────────────────┬──────────────────────────────────┘
                           │ usa
┌──────────────────────────▼──────────────────────────────────┐
│  infrastructure-* (Adapters)                                │
│  JPA · Kafka · LLM · Payment Gateway · Notifications        │
│  Única capa que IMPLEMENTA los puertos                      │
└──────────────────────────┬──────────────────────────────────┘
                           │ implementa puertos de
┌──────────────────────────▼──────────────────────────────────┐
│  application (Use Cases)                                    │
│  Orquestación de flujos de negocio, sin framework           │
└──────────────────────────┬──────────────────────────────────┘
                           │ manipula
┌──────────────────────────▼──────────────────────────────────┐
│  domain-* (orders, payments, inventory, customers, notif.)  │
│  Agregados · Entidades · Value Objects · Eventos · PUERTOS  │
│  Java puro: prohibido Spring/JPA                            │
└──────────────────────────┬──────────────────────────────────┘
                           │ comparte
┌──────────────────────────▼──────────────────────────────────┐
│  shared-kernel                                              │
│  Money · CustomerId · DomainEvent · AggregateRoot           │
└─────────────────────────────────────────────────────────────┘
```

La dependencia siempre apunta **hacia adentro** (hacia el dominio). El dominio no conoce a nadie.

---

## 1. shared-kernel

**Módulo:** `shared-kernel`
**Paquete:** `com.sandbox.shared.kernel`

### Responsabilidad
Contiene los conceptos transversales que varios bounded contexts necesitan compartir
sin acoplarse entre sí.

### Contenido
| Clase | Qué es | Por qué existe |
|---|---|---|
| `DomainEvent` | Interfaz marcadora con `eventId()` y `occurredOn()` | Contrato común de todos los eventos de dominio |
| `AggregateRoot` | Clase base que acumula eventos (`registerEvent` / `pullDomainEvents`) | Garantiza que los eventos se publican solo tras persistir |
| `Money` | Record con `BigDecimal` + `Currency`, inmutable | Evita errores de redondeo y de mezcla de monedas |
| `CustomerId` | Record que envuelve un `UUID` | Tipado fuerte: imposible pasar un OrderId donde va un CustomerId |
| `DomainException` | Excepción base del negocio | El handler REST la traduce a RFC7807 sin conocer detalles |

### Reglas
- **Cero dependencias externas.** Ni Spring, ni JPA. Solo el JDK.
- Debe ser **pequeño y estable**. Si crece demasiado, es señal de que dos contextos
  deberían tener su propio modelo del concepto.

### Frase para la entrevista
> "El shared-kernel es el único punto de contacto entre contextos. Si mañana extraigo
> Orders a un microservicio, solo me llevo `domain-orders` + `shared-kernel`."

---

## 2. domain-* (la capa de dominio)

**Módulos:** `domain-orders`, `domain-payments`, `domain-inventory`, `domain-customers`, `domain-notifications`
**Paquetes:** `com.sandbox.<context>.domain`

Es el corazón del sistema. **Toda la lógica de negocio vive aquí** y solo aquí.

### 2.1 Estructura interna (ejemplo: domain-orders)

```
com.sandbox.orders.domain
├── model/        Order (agregado), OrderLine, OrderStatus, OrderId
├── event/        OrderCreatedEvent
├── port/         OrderRepository, OrderEventPublisher   ← interfaces (salida)
└── service/      OrderDomainService                     ← lógica que no cabe en un agregado
```

### 2.2 Piezas DDD que demuestra

**Aggregate — `Order`** (`domain-orders/.../model/Order.java`)
- Constructor privado + factory `Order.create(...)`: imposible crear una orden inválida.
- `reconstitute(...)`: usado únicamente por infraestructura para reconstruir desde la BD
  sin disparar eventos ni validaciones de creación.
- Encapsula invariantes: transiciones de estado ilegales lanzan `DomainException`.
- Registra `OrderCreatedEvent` al crearse; el evento se publica **después** de persistir.

**State pattern (light) — `OrderStatus`**
- `canTransitionTo(target)` implementado con `switch` exhaustivo sobre el enum.
- Las transiciones válidas viven en el dominio, no en un `if` disperso por servicios.

**Value Objects — `OrderLine`, `OrderId`, `Money`**
- Records inmutables con validación en el constructor compacto.
- `OrderLine.subtotal()` y `Order.total()` son ejemplos de programación funcional
  (`stream().map().reduce()`).

**Puertos de salida — `OrderRepository`, `OrderEventPublisher`**
- Interfaces definidas por el dominio según **sus** necesidades, no según la tecnología.
- El dominio dice "necesito guardar y buscar órdenes"; no sabe que existe PostgreSQL.

**Domain Service — `OrderDomainService`**
- Para reglas que involucran al agregado completo pero no pertenecen a una entidad
  (`requiresManualApproval`, `assertCanBeCreated`).

### 2.3 Reglas estrictas (validadas por ArchUnit)

| Regla | Consecuencia |
|---|---|
| No importar `org.springframework.*` ni `jakarta.persistence.*` | El dominio compila sin ningún framework en el classpath |
| Un contexto no puede depender de otro | Orders no importa nada de Payments |
| Los puertos son interfaces, nunca clases | La implementación vive fuera |

### Frase para la entrevista
> "Puedo ejecutar los tests del dominio sin levantar Spring, sin base de datos y sin Kafka.
> El dominio es un JAR de Java puro — eso es lo que me permite migrar a microservicios
> o incluso cambiar de framework."

---

## 3. application (casos de uso)

**Módulo:** `application`
**Paquete:** `com.sandbox.application`

### Responsabilidad
**Orquestar** flujos de negocio: coordinar repositorios, agregados y publicación de eventos.
No contiene reglas de negocio (eso es del dominio) ni tecnología (eso es de infraestructura).

### Contenido
| Clase | Patrón | Qué hace |
|---|---|---|
| `CreateOrderCommand` | Command | DTO de entrada del caso de uso (record) |
| `CreateOrderUseCase` | Application Service | Valida cliente → crea `Order` → persiste → publica eventos |
| `PayOrderUseCase` | Application Service | Carga orden → cobra vía `PaymentGateway` → actualiza ambos agregados |
| `GetOrderDetailsUseCase` | Query + Structured Concurrency | Consulta orden, cliente y pago **en paralelo** con `StructuredTaskScope` |
| `LLMPort` | Puerto de salida | Contrato de IA: el dominio/aplicación nunca conocen el proveedor |

### Detalles clave

**Sin Spring.** Los casos de uso son clases Java planas con inyección por constructor.
El wiring se hace en `app/config/ApplicationConfig.java`. Esto mantiene la capa testeable
con `new CreateOrderUseCase(fakeRepo, ...)` sin contexto de Spring.

**Publicación de eventos después de persistir** (`CreateOrderUseCase`):
```java
var saved = orderRepository.save(order);
saved.pullDomainEvents().forEach(eventPublisher::publish);
```
El agregado acumuló eventos al crearse; el caso de uso los "drena" y publica solo tras
un save exitoso. Nunca se publica un evento de algo que no se persistió.

**Structured Concurrency** (`GetOrderDetailsUseCase`):
```java
try (var scope = StructuredTaskScope.open(Joiner.allSuccessfulOrThrow())) {
    var customerTask = scope.fork(() -> customerRepository.findById(...));
    var paymentTask  = scope.fork(() -> paymentRepository.findByOrderReference(...));
    scope.join();
    ...
}
```
- Las consultas son independientes → se ejecutan en paralelo.
- Si una falla, el `Joiner` cancela las demás automáticamente (no hay hilos huérfanos).
- Combinado con virtual threads, el coste de cada fork es mínimo.

### Frase para la entrevista
> "La capa de aplicación es el director de orquesta: sabe el orden de los pasos, pero
> no sabe tocar ningún instrumento. Cada caso de uso es un método `execute` que se
> puede testear con dobles de prueba en milisegundos."

---

## 4. infrastructure-* (adaptadores)

**Módulos:** `infrastructure-persistence`, `infrastructure-kafka`, `infrastructure-llm`,
`infrastructure-notifications`, `infrastructure-payment-gateway`
**Paquete:** `com.sandbox.infrastructure.*`

### Responsabilidad
Implementar los puertos definidos por dominio/aplicación y conectar el sistema con el
mundo exterior. **Es la única capa autorizada a implementar puertos** (ArchUnit lo garantiza).

### 4.1 infrastructure-persistence

| Pieza | Rol |
|---|---|
| `OrderJpaEntity` / `OrderLineJpaEntity` | Modelo de tabla. Anémico a propósito: **cero lógica de negocio** |
| `OrderJpaRepository` | Spring Data JPA (detalle de Spring, oculto aquí) |
| `OrderRepositoryAdapter` | **Adapter**: implementa `OrderRepository` (puerto del dominio) traduciendo dominio ↔ JPA |
| `V1__init.sql` | Migración Flyway (incluye tabla `outbox_events` para el Outbox Pattern) |

Puntos destacables:
- **Optimistic locking** con `@Version` en `OrderJpaEntity`.
- **Soft delete** con columna `deleted`.
- La conversión dominio↔entidad vive en el adapter: la entidad JPA nunca se filtra
  fuera de infraestructura.
- El dominio tiene su propio modelo y la BD el suyo; si el esquema cambia, el dominio no se entera.

### 4.2 infrastructure-kafka

| Pieza | Rol |
|---|---|
| `KafkaOrderEventPublisher` | Implementa `OrderEventPublisher` con `KafkaTemplate` |
| `OrderEventsConsumer` | `@KafkaListener` que notifica; **idempotente** (descarta eventos repetidos por clave) |

El consumer demuestra el principio de idempotencia: procesar el mismo evento dos veces
tiene el mismo efecto que procesarlo una (obligatorio en Kafka, que garantiza *at-least-once*).

### 4.3 infrastructure-llm

| Pieza | Rol |
|---|---|
| `MockLLMAdapter` | Implementación por defecto (`@ConditionalOnProperty`) |
| `OllamaLLMAdapter` | Implementación real contra Ollama vía `RestClient` |

Se selecciona el proveedor con `sandbox.llm.provider: mock|ollama` sin tocar código —
demostración del patrón Strategy + Open/Closed: añadir Claude u OpenAI es crear otra
clase, sin modificar las existentes.

### 4.4 infrastructure-notifications / infrastructure-payment-gateway

Adaptadores mínimos (`LoggingNotificationSender`, `FakePaymentGateway`) que muestran que
**todo** recurso externo — incluso un email o un cobro — pasa por un puerto.

### Frase para la entrevista
> "Infraestructura es intercambiable: hoy PostgreSQL, mañana Mongo; hoy mock de LLM,
> mañana Ollama. El cambio se limita a un módulo Gradle y el dominio ni se entera."

---

## 5. app (composition root)

**Módulo:** `app`
**Paquete:** `com.sandbox.app`

### Responsabilidad
Es el **único** módulo que conoce a todos los demás. Arranca Spring Boot, expone la API
REST y conecta (wiring) los casos de uso con los adaptadores.

### Contenido

**`SandboxApplication`**
- `scanBasePackages = "com.sandbox"` + `@EntityScan` + `@EnableJpaRepositories`:
  el composition root es quien decide qué se escanea; los otros módulos no se
  auto-registran.

**`OrderController`** (`api/`)
- Solo: valida entrada (`@Valid`), construye el Command, delega en el caso de uso,
  traduce el resultado a HTTP.
- **Prohibido**: lógica de negocio, `@Transactional`, acceso a repositorios
  (ArchUnit `controllersDoNotAccessRepositories` lo bloquea).

**`GlobalExceptionHandler`**
- Traduce `DomainException` a **ProblemDetail (RFC 7807)** con HTTP 422.
- Errores de validación → 400 con lista de campos.

**`ApplicationConfig`** (`config/`)
- Beans de los casos de uso: aquí es donde `application` (que no conoce Spring)
  recibe sus dependencias.

**`application.yml`**
- `spring.threads.virtual.enabled: true` → cada request REST corre en un **virtual thread**.
- `ddl-auto: validate` → el esquema lo gobierna Flyway, nunca Hibernate.
- `server.shutdown: graceful`.

### Virtual Threads vs Reactive (decisión consciente)
- CRUDs y endpoints bloqueantes → **virtual threads**: código imperativo simple,
  stack traces legibles, sin impuesto de Reactor.
- Reactor (`Mono.zip`) se reservaría para pipelines reactivos de eventos donde el
  back-pressure aporta valor.

### Frase para la entrevista
> "El controller es tan delgado que podría generarse. Toda la decisión de negocio está
> tres capas más abajo, y el composition root es el único punto con acoplamiento a todo —
> por diseño, alguien tiene que ensamblar el sistema."

---

## 6. architecture-tests (el guardián)

**Módulo:** `architecture-tests`
**Paquete:** `com.sandbox.architecture`

### Responsabilidad
Convertir las decisiones de arquitectura en **tests ejecutables** que fallan en CI si
alguien rompe las fronteras. La arquitectura deja de ser un documento que nadie lee.

### Reglas (`ArchitectureRulesTest.java`)

```java
// 1. El dominio no depende de Spring/JPA/validación
noClasses().that().resideInAPackage("..domain..")
    .should().dependOnClassesThat()
    .resideInAnyPackage("org.springframework..", "jakarta.persistence..", ...)

// 2. Los controladores no acceden a repositorios
noClasses().that().areAnnotatedWith(RestController.class)
    .should().accessClassesThat().haveNameMatching(".*Repository")

// 3. Solo infraestructura implementa los puertos
noClasses().that().implement(resideInAPackage("..port.."))
    .should().resideOutsideOfPackage("..infrastructure..")

// 4. Sin dependencias cíclicas entre módulos
slices().matching("com.sandbox.(*)..").should().beFreeOfCycles()

// 5. Los bounded contexts no se llaman entre sí
noClasses().that().resideInAPackage("..orders.domain..")
    .should().accessClassesThat().resideInAPackage("..payments.domain..")
```

Cada regla incluye un `because(...)` que explica **el motivo de negocio/arquitectónico**,
no solo el qué.

### Frase para la entrevista
> "Estos tests son la razón por la que puedo afirmar que la migración a microservicios
> no será traumática: cualquier acoplamiento indebido rompe el build hoy, no dentro
> de dos años cuando sea caro arreglarlo."

---

## 7. Flujo completo de ejemplo: crear una orden

```
POST /api/v1/orders
       │
       ▼
OrderController (app)                    ← valida @Valid, crea CreateOrderCommand
       │
       ▼
CreateOrderUseCase (application)         ← orquesta
       │  1. customerRepository.findById()      ──► CustomerRepositoryAdapter (JPA)
       │  2. Order.create(...)                   ──► dominio: valida, calcula total,
       │                                             registra OrderCreatedEvent
       │  3. orderRepository.save(order)         ──► OrderRepositoryAdapter (JPA + Flyway)
       │  4. eventPublisher.publish(...)         ──► KafkaOrderEventPublisher
       ▼
Kafka topic: orders.events
       │
       ▼
OrderEventsConsumer (idempotente)        ──► NotificationSender (LoggingNotificationSender)
```

Fíjate: cada flecha que cruza una frontera lo hace **a través de un puerto**, y el flujo
se puede trazar capa por capa sin que ninguna conozca la tecnología de la siguiente.

---

## 8. Resumen de restricciones por capa

| Capa | Puede usar | NO puede |
|---|---|---|
| shared-kernel | JDK | Todo framework |
| domain-* | JDK + shared-kernel | Spring, JPA, Kafka, otros contextos |
| application | JDK + domain-* | Spring, tecnologías concretas |
| infrastructure-* | Todo (Spring, JPA, Kafka...) | Lógica de negocio |
| app | Todo | Lógica de negocio, acceso directo a repos |
