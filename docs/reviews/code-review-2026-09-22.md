# Code review — cambios sin commitear (`chore/pre-baseline-setup`)

**Fecha:** 2026-09-22
**Skill aplicada:** `java21-reactive-code-review` (`.claude/skills/java21-reactive-code-review/`)
**Alcance:** `git diff HEAD` completo (38 archivos; no hay `??` untracked —
todo lo nuevo ya está en el índice). Foco principal: `src/main/java/skaro/trainer/**`
y sus tests de integración. `README.md`, `mvnw`, `docs/AI-WORKFLOW.md` quedan
fuera (no son código Java) salvo mención puntual donde afectan al código nuevo.
**Modo:** solo lectura — ningún archivo del proyecto fue modificado. Los
arreglos abajo son antes/después documentales, no se aplicaron.

Decisiones ya documentadas en `docs/DESIGN.md` (D1 JPA+boundedElastic, D2
constantes de mecánica Pokémon hardcodeadas, D3 lock pesimista) **no se
repiten como hallazgo** — están justificadas y la skill pide no marcarlas.

---

## Hallazgos

### 1. [Alta — a verificar] `move()` y `capture()` no manejan explícitamente "no encontrado", a diferencia de `detail()`

`PokemonMoveTransaction.execute()` y `PokemonCaptureTransaction.execute()`
devuelven `null` cuando el `Trainer` (o el `PokemonInstance`, en `move`) no
existe. Ese `null` atraviesa `Mono.fromCallable(...)` en
`TrainerRosterRepository` (`src/main/java/skaro/trainer/persistence/TrainerRosterRepository.java:38-41`)
y se convierte en un `Mono` **vacío** (contrato de `fromCallable`: valor
`null` ⇒ completar sin emitir). Ese vacío llega sin transformar a los
controllers:

`src/main/java/skaro/trainer/api/PokemonController.java:59-65`
```java
@PatchMapping("/{id}")
public Mono<ResponseEntity<PokemonInstanceResponse>> move(@PathVariable Long trainerId, @PathVariable Long id,
        @Valid @RequestBody MoveRequest request) {
    Location target = Location.valueOf(request.location().toUpperCase());
    return rosterService.move(trainerId, id, target)
            .map(instance -> ResponseEntity.ok(PokemonInstanceResponse.from(instance)));
}
```

Compárese con `detail()` (`PokemonController.java:52-57`), que sí resuelve
el vacío explícitamente:

```java
return rosterService.findById(trainerId, id)
        .map(instance -> ResponseEntity.ok(PokemonInstanceResponse.from(instance)))
        .defaultIfEmpty(ResponseEntity.notFound().build());
```

**A verificar, no confirmado en esta revisión:** Spring WebFlux tiene un
comportamiento especial documentado para `Mono<ResponseEntity<T>>` vacío en
el valor de retorno de un controller (`ResponseEntityResultHandler`), que
podría estar resolviendo esto en 404 automáticamente incluso sin
`defaultIfEmpty`. No pude confirmar la versión exacta de ese comportamiento
en Spring Framework 6.2.19 (la que trae este proyecto) contra la fuente
real durante esta revisión. Si el comportamiento automático **no** aplica
acá (p. ej. porque el resultado type se infiere como el tipo genérico
`ResponseEntity<PokemonInstanceResponse>` y no hay value emitido para
setear status), el efecto real sería un 200 con body vacío en vez de 404 —
inconsistente con `detail()` y sin test que lo cubra (ni
`PokemonMoveIntegrationTest` ni `PokemonCaptureIntegrationTest` prueban
"trainer inexistente" o "pokemon inexistente en move").

**Antes** (`PokemonController.java:59-65`):
```java
@PatchMapping("/{id}")
public Mono<ResponseEntity<PokemonInstanceResponse>> move(@PathVariable Long trainerId, @PathVariable Long id,
        @Valid @RequestBody MoveRequest request) {
    Location target = Location.valueOf(request.location().toUpperCase());
    return rosterService.move(trainerId, id, target)
            .map(instance -> ResponseEntity.ok(PokemonInstanceResponse.from(instance)));
}
```

**Después** (explícito, sin depender del comportamiento por defecto):
```java
@PatchMapping("/{id}")
public Mono<ResponseEntity<PokemonInstanceResponse>> move(@PathVariable Long trainerId, @PathVariable Long id,
        @Valid @RequestBody MoveRequest request) {
    Location target = Location.valueOf(request.location().toUpperCase());
    return rosterService.move(trainerId, id, target)
            .map(instance -> ResponseEntity.ok(PokemonInstanceResponse.from(instance)))
            .defaultIfEmpty(ResponseEntity.notFound().build());
}
```
Mismo tratamiento en `capture()` (`PokemonController.java:31-36`) para el
caso "trainerId no existe". Y, ya en el service, valdría la pena que
`TrainerRosterService.move`/`capture` distingan "trainer no existe" de
"pokemon no existe" con una excepción de dominio propia en vez de dejar
que ambos casos colapsen al mismo `null`/vacío silencioso — hoy no hay
forma de diferenciarlos desde afuera.

---

### 2. [Alta] `pokeapi-call-timeout` está configurado pero nunca se aplica

`TrainerConfigurationProperties` declara y documenta el timeout para
llamadas a PokéAPI (`src/main/java/skaro/trainer/config/TrainerConfigurationProperties.java:22-23`):

```java
@NotNull
private Duration pokeapiCallTimeout = Duration.ofSeconds(3);
```

y está en `application.yml` (`pokeapi-call-timeout: 3s`, tanto en
`src/main/resources/application.yml:8` como en `src/test/resources/application.yml:8`).
Pero ningún lugar del código nuevo llama a `.timeout(properties.getPokeapiCallTimeout())`
— ni `resolveAbility` ni `listTeam`, los dos puntos donde
`TrainerRosterService` llama a `pokeApiClient.getResource(...)`:

`src/main/java/skaro/trainer/domain/TrainerRosterService.java:58-73`
```java
private Mono<String> resolveAbility(CaptureRequest request) {
    return pokeApiClient.getResource(Pokemon.class, request.species()).flatMap(species -> {
        ...
    });
}
```

`src/main/java/skaro/trainer/domain/TrainerRosterService.java:106-110`
```java
public Flux<TeamMember> listTeam(Long trainerId) {
    return repository.findByLocation(trainerId, Location.TEAM)
            .flatMap(instance -> pokeApiClient.getResource(Pokemon.class, instance.getSpecies())
                    .map(species -> new TeamMember(instance, species)));
}
```

Tampoco hay timeout a nivel `HttpClient` — `PokeApiReactorApplication`
crea el `HttpClient` sin configurar (`src/main/java/skaro/PokeApiReactorApplication.java:29-32`:
`HttpClient.create()`, sin `.responseTimeout(...)`). Es decir: hoy no hay
ningún límite de tiempo real en la ruta de captura ni de listado del
equipo — si PokéAPI no responde, el request de `POST /trainers/{id}/pokemon`
o `GET .../pokemon?location=team` queda colgado indefinidamente. El valor
configurado en `application.yml` es efectivamente muerto.

**Antes** (`TrainerRosterService.java:58-60`):
```java
private Mono<String> resolveAbility(CaptureRequest request) {
    return pokeApiClient.getResource(Pokemon.class, request.species()).flatMap(species -> {
```

**Después:**
```java
private Mono<String> resolveAbility(CaptureRequest request) {
    return pokeApiClient.getResource(Pokemon.class, request.species())
            .timeout(properties.getPokeapiCallTimeout())
            .flatMap(species -> {
```
(y análogamente en `listTeam`, encadenando `.timeout(properties.getPokeapiCallTimeout())`
antes del `.map(...)`). Falta además decidir qué status HTTP corresponde
a un timeout de PokéAPI — la skill sugiere 502/503/504, y
`TrainerExceptionHandler` hoy no tiene un `@ExceptionHandler` para
`TimeoutException`/`WebClientRequestException` (el comentario en
`TrainerExceptionHandler.java:11-13` ya anota esto como "punto distinto",
pero con el timeout sin aplicar, ese punto ni siquiera se alcanza a
ejercitar).

---

### 3. [Media] `@Valid` en el método `@Bean` no activa la validación de `TrainerConfigurationProperties`

`src/main/java/skaro/trainer/config/TrainerConfiguration.java:17-22`:
```java
@Bean
@Valid
@ConfigurationProperties(CONFIGURATION_PROPERTIES_PREFIX)
public TrainerConfigurationProperties trainerConfigurationProperties() {
    return new TrainerConfigurationProperties();
}
```

Spring Boot valida un bean `@ConfigurationProperties` cuando la **clase**
de propiedades está anotada con `@Validated` (o el propio `@Bean` está
anotado con `@Validated`, no `@Valid`) — es el mecanismo que dispara
`ConfigurationPropertiesBindingPostProcessor` para correr Bean Validation
sobre el binding. `@Valid` puesto así, solo en el método factory, no
engancha ese mecanismo: en la práctica, `@Min(1)` en `teamLimit`/`boxLimit`
y `@NotNull` en `defaultTrainerId`/`pokeapiCallTimeout`
(`TrainerConfigurationProperties.java:13-23`) no se están evaluando al
arrancar — un `team-limit: -1` en `application.yml` no fallaría el
arranque como se espera. **Nota:** este mismo patrón ya existe en
`skaro.pokeapi.PokeApiReactorBaseConfiguration:40-43` (preexistente, fuera
de alcance de este diff) — el código nuevo lo replica fielmente
("mismo patrón", dice el comentario en
`TrainerConfigurationProperties.java:8-10`), así que no es una
inconsistencia introducida acá, pero sí extiende un problema real a una
segunda clase de propiedades.

**Antes** (`TrainerConfiguration.java:17-22`):
```java
@Bean
@Valid
@ConfigurationProperties(CONFIGURATION_PROPERTIES_PREFIX)
public TrainerConfigurationProperties trainerConfigurationProperties() {
    return new TrainerConfigurationProperties();
}
```

**Después:**
```java
@Bean
@Validated
@ConfigurationProperties(CONFIGURATION_PROPERTIES_PREFIX)
public TrainerConfigurationProperties trainerConfigurationProperties() {
    return new TrainerConfigurationProperties();
}
```
(`org.springframework.validation.annotation.Validated`, no
`jakarta.validation.Valid`).

---

### 4. [Media] `location` sin validar en `GET /trainers/{trainerId}/pokemon`

`src/main/java/skaro/trainer/api/PokemonController.java:38-50`:
```java
@GetMapping
public Mono<ResponseEntity<List<?>>> list(@PathVariable Long trainerId, @RequestParam String location) {
    if ("team".equalsIgnoreCase(location)) {
        return rosterService.listTeam(trainerId)
                .map(TeamMemberResponse::from)
                .collectList()
                .map(ResponseEntity::ok);
    }
    return rosterService.listBox(trainerId)
            .map(PokemonInstanceResponse::from)
            .collectList()
            .map(ResponseEntity::ok);
}
```
Cualquier valor de `location` que no sea `"team"` (case-insensitive) cae
en la rama de `listBox` — incluidos typos (`locaton=team`), valores
inválidos (`location=foo`) o ausentes de contrato (`location=TEAM `, con
espacio). No hay ningún test que pruebe un `location` inválido, y la API
nunca devuelve 400 para este parámetro: silenciosamente devuelve el Baúl.
Contrasta con `move()`, que si valida (aunque sin capturar el error, ver
hallazgo 5) y con `MoveRequest`/`CaptureRequest`, que sí usan Bean
Validation. Además el tipo de retorno `List<?>` es un wildcard sin
información — dificulta tanto la legibilidad como la seguridad de tipos
del contrato.

**Antes:**
```java
@GetMapping
public Mono<ResponseEntity<List<?>>> list(@PathVariable Long trainerId, @RequestParam String location) {
    if ("team".equalsIgnoreCase(location)) {
        return rosterService.listTeam(trainerId)
                .map(TeamMemberResponse::from)
                .collectList()
                .map(ResponseEntity::ok);
    }
    return rosterService.listBox(trainerId)
            .map(PokemonInstanceResponse::from)
            .collectList()
            .map(ResponseEntity::ok);
}
```

**Después:**
```java
@GetMapping
public Mono<ResponseEntity<List<?>>> list(@PathVariable Long trainerId, @RequestParam String location) {
    Location parsed;
    try {
        parsed = Location.valueOf(location.toUpperCase());
    } catch (IllegalArgumentException ex) {
        return Mono.just(ResponseEntity.badRequest().build());
    }
    return switch (parsed) {
        case TEAM -> rosterService.listTeam(trainerId)
                .map(TeamMemberResponse::from)
                .collectList()
                .map(ResponseEntity::ok);
        case BOX -> rosterService.listBox(trainerId)
                .map(PokemonInstanceResponse::from)
                .collectList()
                .map(ResponseEntity::ok);
    };
}
```
(El `try/catch` acá es aceptable porque `Location.valueOf` es código
síncrono de ensamblado, no una señal reactiva en ejecución — no envuelve
un pipeline esperando capturar errores de una llamada async.)

---

### 5. [Media] `Location.valueOf(...)` sin manejar en `move()` — 500 genérico en vez de 400

`src/main/java/skaro/trainer/api/PokemonController.java:62`:
```java
Location target = Location.valueOf(request.location().toUpperCase());
```
Si `request.location()` no es `"team"`/`"box"` (p. ej. `"box "`, `"Box2"`,
`"activo"`), `Location.valueOf` lanza `IllegalArgumentException` sin
capturar. No hay ningún `@ExceptionHandler` para esa excepción en
`TrainerExceptionHandler`, así que termina en el manejador por defecto de
Spring — típicamente un 500 (o el `ProblemDetail` genérico de Boot 3, pero
sin el significado semántico de "validación de entrada" que sí tienen
`CaptureValidationException`/`NoCapacityAvailableException`). No hay test
para este caso.

**Antes** (`PokemonController.java:59-65`):
```java
@PatchMapping("/{id}")
public Mono<ResponseEntity<PokemonInstanceResponse>> move(@PathVariable Long trainerId, @PathVariable Long id,
        @Valid @RequestBody MoveRequest request) {
    Location target = Location.valueOf(request.location().toUpperCase());
    return rosterService.move(trainerId, id, target)
            .map(instance -> ResponseEntity.ok(PokemonInstanceResponse.from(instance)));
}
```

**Después:**
```java
@PatchMapping("/{id}")
public Mono<ResponseEntity<PokemonInstanceResponse>> move(@PathVariable Long trainerId, @PathVariable Long id,
        @Valid @RequestBody MoveRequest request) {
    Location target;
    try {
        target = Location.valueOf(request.location().toUpperCase());
    } catch (IllegalArgumentException ex) {
        return Mono.just(ResponseEntity.badRequest().build());
    }
    return rosterService.move(trainerId, id, target)
            .map(instance -> ResponseEntity.ok(PokemonInstanceResponse.from(instance)))
            .defaultIfEmpty(ResponseEntity.notFound().build());
}
```
Alternativa más alineada con "controllers finos, sin lógica": mover el
parseo a `MoveRequest` como método de validación propio, o agregar un
`@ExceptionHandler(IllegalArgumentException.class)` en
`TrainerExceptionHandler` — pero un handler genérico sobre
`IllegalArgumentException` es agresivo (captura cualquier
`IllegalArgumentException` de cualquier origen, no solo de `Location`),
por eso la opción de arriba prefiere el `try/catch` local y acotado.

---

### 6. [Media] Reglas de negocio (BR-002 a BR-008) sin tests unitarios — solo integration tests pesados

Las cinco clases de test nuevas (`src/test/java/skaro/trainer/api/*IntegrationTest.java`)
son todas `@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)` +
`@AutoConfigureWebTestClient` — levantan el contexto completo (Spring +
H2 +, en dos de ellas, `MockWebServer`) para probar validaciones que son
lógica pura: `validateEvs`, `validateIvs`, `validateMoves`
(`TrainerRosterService.java:75-102`) no dependen de Spring, JPA ni
PokéAPI, pero solo se ejercitan indirectamente vía HTTP. La skill marca
esto como "lo que más se suele olvidar y lo primero que un revisor mira" —
acá no falta el camino de error (sí está cubierto, ver
`PokemonCaptureIntegrationTest`), pero sí falta la capa de test rápida:
no hay ningún `@Test` que instancie `TrainerRosterService` con un fake/mock
de `TrainerRosterRepository` y `PokeApiClient` (Mockito) para probar las
validaciones en microsegundos, sin Spring context ni MockWebServer. Con 8
escenarios de validación ya cubiertos solo por integration tests, el
tiempo de test suite crece más de lo necesario y cada uno paga el costo
de arrancar Spring + H2.

No es una regresión respecto a algo preexistente (es dominio 100% nuevo),
y no hay `docs/DESIGN.md` que declare "todo se prueba vía integration
test" como decisión — vale la pena registrarlo si es intencional.

**Antes:** ningún archivo en `src/test/java/skaro/trainer/domain/`.

**Después** (ejemplo mínimo, no exhaustivo):
```java
class TrainerRosterServiceTest {

    TrainerRosterRepository repository = mock(TrainerRosterRepository.class);
    TrainerConfigurationProperties properties = new TrainerConfigurationProperties();
    PokeApiClient pokeApiClient = mock(PokeApiClient.class);
    TrainerRosterService service = new TrainerRosterService(repository, properties, pokeApiClient);

    @Test
    void capturaConEvIndividualPorEncimaDe252RechazaSinLlamarAlRepositorioNiAPokeApi() {
        var evs = new StatBlockPayload(253, 0, 0, 0, 0, 0);
        var request = new CaptureRequest("pikachu", null, evs, null, null, null, null, null, null, null, null, null);

        StepVerifier.create(service.capture(1L, request))
                .expectErrorMatches(e -> e instanceof CaptureValidationException
                        && e.getMessage().contains("BR-004"))
                .verify();

        verifyNoInteractions(repository, pokeApiClient);
    }
}
```

---

### 7. [Baja] `spring.jpa.hibernate.ddl-auto: update` en `application.yml` de producción

`src/main/resources/application.yml:16-19`:
```yaml
jpa:
  open-in-view: false
  hibernate:
    ddl-auto: update
```
`update` deja que Hibernate mute el schema en cada arranque contra el H2
de archivo — cómodo para el plazo del challenge (coincide con lo que
`docs/DESIGN.md` D1 dice sobre priorizar velocidad), pero es una fuente
clásica de drift de schema silencioso. No bloqueante para esta entrega;
si el proyecto crece, vale una migración con Flyway/Liquibase o al menos
`validate` + script versionado. `open-in-view: false` sí está bien
elegido (evita mantener la sesión JPA abierta durante todo el ciclo de
vida reactivo del request).

---

### 8. [Baja] `seedDefaultTrainer` llama JPA bloqueante fuera del patrón `boundedElastic` de D1

`src/main/java/skaro/trainer/config/TrainerConfiguration.java:27-36`:
```java
@Bean
public CommandLineRunner seedDefaultTrainer(TrainerRepository trainerRepository,
        TrainerConfigurationProperties properties) {
    return args -> {
        Long defaultTrainerId = properties.getDefaultTrainerId();
        if (!trainerRepository.existsById(defaultTrainerId)) {
            trainerRepository.save(new Trainer(defaultTrainerId));
        }
    };
}
```
Llama a `trainerRepository.existsById`/`.save` (JPA bloqueante) directo,
sin pasar por `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())`
como el resto de `skaro.trainer.persistence` (D1). No es un bug real —
`CommandLineRunner` corre en el hilo principal durante el arranque, antes
de que Netty empiece a aceptar conexiones, así que no compite con el
event loop — pero rompe la regla "todo acceso a JPA pasa por
boundedElastic" sin que quede dicho en ningún lado por qué acá no aplica.
Alcanza con un comentario breve tipo `// arranque, no hay event loop
corriendo todavía — no hace falta boundedElastic acá` para que quien lea
el código no lo lea como un descuido.

---

## Resumen

| # | Severidad | Archivo:línea | Resumen |
|---|---|---|---|
| 1 | Alta (a verificar) | `PokemonController.java:59-65`, `:31-36` | Posible 200 vacío en vez de 404 en `move`/`capture` sin `defaultIfEmpty` |
| 2 | Alta | `TrainerRosterService.java:58-73,106-110` | `pokeapi-call-timeout` configurado pero nunca aplicado — sin timeout real a PokéAPI |
| 3 | Media | `TrainerConfiguration.java:17-22` | `@Valid` en `@Bean` no valida `TrainerConfigurationProperties` (falta `@Validated`) |
| 4 | Media | `PokemonController.java:38-50` | `location` sin validar en `list()` — valores inválidos caen en "box" |
| 5 | Media | `PokemonController.java:62` | `Location.valueOf` sin manejar en `move()` — 500 en vez de 400 |
| 6 | Media | `src/test/java/skaro/trainer/**` | Reglas de negocio sin tests unitarios, solo integration tests pesados |
| 7 | Baja | `application.yml:16-19` | `ddl-auto: update` en producción |
| 8 | Baja | `TrainerConfiguration.java:27-36` | Blocking JPA fuera del patrón boundedElastic (sin riesgo real, falta comentario) |

No se tocó ningún archivo del proyecto salvo este reporte.
