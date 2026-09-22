# AI Workflow — notas de implementación

Registro de lo no trivial durante `sdd-implement` de la spec 001, para no
perderlo. No es un changelog completo — el diff y los tests ya lo
documentan; esto es el "por qué" de las decisiones que no estaban
previstas en la spec al nivel de detalle de implementación.

## Spec 001 — Nivel 1 (1a + 1b), 2026-09-21

Alcance: 15 de los 26 escenarios Gherkin (ver
`~/.claude/plans/mighty-dancing-bonbon.md` para la estrategia de recorte
por tiempo). Los 5 endpoints funcionan de punta a punta contra H2 real.
40/40 tests verdes (25 baseline + 15 nuevos), `BUILD SUCCESS`.

### Correcciones no anticipadas por la spec

- **Colisión de columnas Ivs/Evs**: ambos `@Embeddable` comparten los
  mismos 6 nombres de campo (`hp`, `attack`, ...) — Hibernate fallaba al
  arrancar (`Column 'attack' is duplicated`). Se resolvió con
  `@AttributeOverrides` en `PokemonInstance` (prefijo `iv_`/`ev_` por
  columna). No estaba en la spec porque es un detalle de mapeo JPA, no de
  diseño.
- **`moves` con `LazyInitializationException`**: la colección
  `@ElementCollection` es lazy por default; al serializar la respuesta
  fuera de la sesión JPA (dominio solo ve `Mono`/`Flux`, sin
  `open-in-view`), Jackson fallaba al tocarla. Se cambió a
  `fetch = FetchType.EAGER` — justificado porque es una colección acotada
  (máx. 4, BR-008) que siempre se necesita completa en la respuesta.
- **Lock pesimista sin transacción**: `@Lock(PESSIMISTIC_WRITE)` requiere
  una transacción activa (`TransactionRequiredException` si no). Las tres
  operaciones (lock, conteo, insert) vivían en `Mono.fromCallable`
  independientes, sin transacción compartida. Se resolvió moviendo el
  ciclo completo a un único método `@Transactional` en una clase separada
  (`PokemonCaptureTransaction`/`PokemonMoveTransaction`) — separada a
  propósito, porque la auto-invocación dentro de la misma clase que llama
  saltea el proxy de `@Transactional` de Spring (gotcha conocido de Spring
  AOP, no estaba anotado en `docs/DESIGN.md` D3 a este nivel de detalle).
- **Bean `HttpClient` faltante**: `PokeApiReactorBaseConfiguration.webClient(HttpClient, ...)`
  nunca se había ejecutado dentro de un contexto Spring real (los tests
  del baseline lo armaban a mano). Al arrancar la app hubo que agregar
  `@Bean HttpClient httpClient()` en `PokeApiReactorApplication` — es
  proveer el contrato que la librería ya declaraba, no tocar
  `skaro.pokeapi`.
- **Doble `@Configuration` de PokéAPI**: `PokeApiReactorCachingConfiguration`
  y `PokeApiReactorNonCachingConfiguration` registran el mismo bean
  `PokeApiClient` — escanear ambas por component-scan default colisiona.
  Se acotó `scanBasePackages` a `skaro.trainer` y se importó explícitamente
  la variante **non-caching** (más simple, sin depender de un
  `CacheManager` no configurado hoy) vía `@Import`.

### Decisión de implementación no explícita en la spec

- Se usaron **records de Java 21** (`CaptureRequest`, `StatBlockPayload`,
  `PokemonInstanceResponse`, `TeamMemberResponse`, `MoveRequest`) para las
  DTOs de `skaro.trainer.api` — no para las entidades/embeddables JPA
  (`Trainer`, `PokemonInstance`, `Ivs`, `Evs`), que se quedan como clases
  mutables porque JPA no soporta records de forma confiable. El resto del
  proyecto (`skaro.pokeapi.resource.**`) usa POJOs por ser código previo a
  Java 16, no por convención a replicar.

### Pendiente (Nivel 2 y 3, no implementado — ver el plan)

11 escenarios sin implementar: 6 de Nivel 2 (naturaleza inválida, especie
inexistente, payload sin `species`, `trainerId` no coincide, traslados sin
espacio en ambas direcciones) y 5 de Nivel 3 (fallas de PokéAPI 502/504 en
captura y listado, captura concurrente con lock). Diseñados y justificados
en la spec/DESIGN.md, no implementados por el plazo acotado del challenge.
