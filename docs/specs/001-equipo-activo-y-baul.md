# Spec 001 — Equipo Activo y Baúl

## Motivación

Resuelve P1 (sin capa de persistencia), P2 (sin capa HTTP), P3 (sin capa de
dominio/servicio) y P8 (sin traducción de errores HTTP a errores de dominio)
de `docs/AS-IS.md`. Sirve O1 (nueva funcionalidad de dominio de negocio),
O2-O4 (mantenibilidad/escalabilidad/incorporación de nuevos integrantes vía
capas separadas y claras) y O5-O6 (calidad de diseño, trade-offs
documentados) de `docs/PRD.md`.

Implementa FR-001 a FR-023 del PRD (captura, listado de Equipo Activo y
Baúl, detalle, traslado entre ambos, con sus validaciones de negocio).
FR-024 a FR-028 (bonus de evolución) quedan fuera — ver `docs/PRODUCT.md`.

## Alcance del cambio

**Se agrega**, sin tocar el cliente PokéAPI existente:

- `pom.xml`: se quita `<skip>true</skip>` del `spring-boot-maven-plugin`
  (el módulo pasa a ser también una app ejecutable); se agregan
  dependencias de persistencia JPA (Hibernate) + driver H2 — ver
  Restricciones y `docs/DESIGN.md` D1 para el porqué frente a R2DBC.
- Clase `@SpringBootApplication` nueva (no existe ninguna hoy).
- `src/main/resources/application.yml` nuevo (no existe hoy) — datasource
  H2 en modo archivo, límites de Equipo/Baúl configurables.
- Paquete nuevo `skaro.trainer` (dominio → capas adentro), hermano de
  `skaro.pokeapi.**`, sin mezclarse con la librería cliente existente.
  Modelo: **Entrenador es el aggregate root** — Equipo Activo y Baúl no
  son conceptos independientes, son dos particiones de la colección que
  le pertenece a un Entrenador; el invariante de capacidad (6/300) es una
  regla del Entrenador, no del ejemplar individual. Por eso el dominio se
  llama `trainer`, no `pokemon`, aunque en esta entrega el Entrenador sea
  un único registro fijo sin API propia de alta (ver Restricciones):
  - `skaro.trainer.domain` — `Trainer` (aggregate root), `PokemonInstance`
    (entidad, referencia al `Trainer` por FK) y las reglas de negocio
    (límites, asignación automática, traslado, validación de
    IVs/EVs/naturaleza/habilidad), orquestadas por un
    `TrainerRosterService`. **Nota de nombre:** la entidad del ejemplar se
    llama `PokemonInstance`, no `Pokemon`, para no colisionar con
    `skaro.pokeapi.resource.Pokemon` (el DTO de especie que ya existe) —
    son conceptos distintos: uno es el recurso estático de PokéAPI, el
    otro es el ejemplar propio de un entrenador.
  - `skaro.trainer.persistence` — repositorio(s) y entidades de
    persistencia JPA sobre H2 (ver Restricciones), para `Trainer` y
    `PokemonInstance`.
  - `skaro.trainer.api` — controladores REST y DTOs de request/response
    (todos sobre `PokemonInstance`; no hay endpoint propio de Trainer en
    esta entrega).
  - `skaro.trainer.config` — límites de Equipo/Baúl configurables (mismo
    patrón que `PokeApiConfigurationProperties`).
- Reutiliza `skaro.pokeapi.client.PokeApiClient` (ya probado) para resolver
  especie/habilidad al capturar y al armar la vista compuesta de FR-021.

Si más adelante se suma el bonus de evolución, sería un dominio hermano
nuevo, `skaro.evolution`, sin tocar `skaro.trainer` — no se crea en esta
entrega (ver Fuera de alcance).

**No se toca:** `skaro.pokeapi.client`, `skaro.pokeapi.cache`,
`skaro.pokeapi.resource.**`, ni la configuración existente — la suite de
`docs/specs/000-baseline.md` sigue siendo la condición de fondo.

## Comportamiento

### Se preserva

Todo el comportamiento characterizado en `docs/specs/000-baseline.md`
(cliente PokéAPI, cache, deserialización) — sigue 100% verde durante todo
el ciclo de esta spec.

### Cambia

El proyecto deja de ser exclusivamente una librería y expone un servidor
HTTP con 5 endpoints nuevos sobre una capa de dominio y persistencia
nuevas:

| Método | Path | FR | Descripción |
|---|---|---|---|
| `POST` | `/trainers/{trainerId}/pokemon` | FR-014, FR-018, FR-019 | Captura un ejemplar nuevo, lo asigna automáticamente a Equipo Activo o Baúl |
| `GET` | `/trainers/{trainerId}/pokemon?location=team` | FR-015, FR-021 | Lista el Equipo Activo, vista compuesta (metadata propia + atributos estáticos de la especie desde PokéAPI) |
| `GET` | `/trainers/{trainerId}/pokemon?location=box` | FR-016, FR-022 | Lista los ejemplares almacenados en el Baúl |
| `GET` | `/trainers/{trainerId}/pokemon/{id}` | FR-020 | Detalle completo de un ejemplar (metadata técnica, genética, origen) |
| `PATCH` | `/trainers/{trainerId}/pokemon/{id}` | FR-017, FR-023 | Traslada un ejemplar entre Equipo Activo y Baúl (body: `{"location": "team"\|"box"}`), validando límites |

Convenciones aplicadas: **sin verbos en el path** (el traslado se modela
como actualización parcial del recurso vía `PATCH`, no como
`/pokemon/{id}/mover`) y **todo el contrato en inglés** (`team`/`box`
como valores de `location`, no `equipo`/`baul`) — Equipo Activo y Baúl no
son sub-rutas propias, son un filtro (`location`) sobre la misma
colección `/pokemon`, evitando además la ambigüedad de ruteo entre un
literal (`equipo`) y la variable `{id}` en el mismo nivel del path.

`{trainerId}` se valida contra el único Entrenador fijo de esta entrega
(ver Restricciones) — no hay CRUD de entrenadores, pero la URL ya refleja
la jerarquía de ownership real (`PokemonInstance` pertenece a `Trainer`),
para no romper el contrato el día que haya multi-entrenador real.

**Modelo de errores HTTP** (nuevo — resuelve P8):

| Código | Cuándo |
|---|---|
| `201 Created` | Captura exitosa |
| `200 OK` | Lectura o traslado exitosos |
| `400 Bad Request` | Payload malformado o con campos obligatorios faltantes |
| `422 Unprocessable Entity` | Payload bien formado pero viola una regla de negocio (IV fuera de 0-31, EVs sobre 510 total o 252 individual, naturaleza inválida, habilidad que no pertenece a la especie, más de 4 movimientos, especie inexistente en PokéAPI) |
| `404 Not Found` | `{id}` de ejemplar inexistente, o `{trainerId}` que no coincide con el entrenador fijo |
| `409 Conflict` | Sin espacio disponible en el destino (Equipo y Baúl llenos al capturar; destino lleno al mover) — decisión ya tomada en `docs/PRODUCT.md` (A4) |
| `502 Bad Gateway` | PokéAPI responde `5xx` o rechaza la conexión al resolver especie o habilidad durante la captura, o al armar la vista compuesta de FR-021 |
| `504 Gateway Timeout` | PokéAPI no responde dentro del timeout configurado al resolver especie o habilidad durante la captura, o al armar la vista compuesta de FR-021 |

## Restricciones

- **Entrenador fijo/único**, constante en esta entrega — sin endpoint de
  alta, sin id variable. Sigue modelado como entidad de dominio propia
  (`Trainer`, aggregate root — ver Alcance del cambio), persistido como
  un único registro; lo que no existe es su API REST (alta/baja/listado
  de entrenadores). El campo OT (FR-009) se persiste por ejemplar como FK
  a ese registro único. **Recorte explícito** respecto a la lectura
  original de A9 en `PRODUCT.md` ("límites *por entrenador*" sugería
  multi-entrenador con su propia API) — decidido así por tiempo, anotado
  acá para no perderlo.
- El `trainerId` de ese único Entrenador es un valor fijo y conocido
  (constante de configuración o seed inicial de la DB) — todas las URLs lo
  exigen igual (`/trainers/{trainerId}/pokemon/...`) para que el contrato
  REST no cambie el día que haya multi-entrenador real; solo deja de haber
  un único valor válido.
- Límites y valores configurables (no hardcodeados) — detalle completo,
  con nombres de propiedad y defaults sugeridos, en ## Parametrización.
- Persistencia: H2 en modo archivo, con **JPA (Hibernate) +
  `Schedulers.boundedElastic()`** — decisión definitiva, ya no tentativa
  (ver `docs/DESIGN.md` D1 para el detalle y las alternativas
  descartadas). Las llamadas bloqueantes quedan aisladas dentro de
  `skaro.trainer.persistence`; `domain` y `api` solo ven `Mono`/`Flux`.
- **Concurrencia en la validación de límites**: `TrainerRosterService`
  toma un lock pesimista sobre la fila del `Trainer`
  (`@Lock(LockModeType.PESSIMISTIC_WRITE)` al leerlo) dentro de la misma
  transacción que cuenta ejemplares, decide destino e inserta/actualiza —
  evita que dos capturas o traslados concurrentes lean el mismo conteo y
  superen el límite de 6/300 (revisado tras hallazgo H-03 de
  `docs/reviews/adversarial-review-2026-09-21.md`; detalle y alternativas
  descartadas en `docs/DESIGN.md` D3).
- Sin autenticación/autorización (ya fuera de alcance en `PRODUCT.md`).
- Validación contra PokéAPI limitada a **Habilidad** (BR-006, explícito en
  el PRD). Movimientos y Held Item se aceptan como referencia libre
  (string), sin round-trip adicional a PokéAPI — no lo pide el PRD.
- **Traducción de errores de PokéAPI** (resuelve P8 del AS-IS **en su
  totalidad**, no solo el caso puntual de especie inexistente): en el
  único punto donde `TrainerRosterService` llama a `PokeApiClient` para
  resolver especie/habilidad (captura) o para armar la vista compuesta
  (FR-021), un `404` de PokéAPI se traduce a `422 Unprocessable Entity`
  (la especie no existe); un `5xx` o conexión rechazada se traduce a
  `502 Bad Gateway`; y un timeout se traduce específicamente a
  `504 Gateway Timeout` — distinción resuelta tras hallazgo H-09 de
  `docs/reviews/adversarial-review-2026-09-21-r2.md` (antes unificado bajo
  `502`), coherente con la diferenciación semántica que ya aplica el resto
  de la tabla de errores (`400` vs `422`). Sin esto, el error crudo de
  WebClient (`WebClientResponseException`, characterizado sin traducir en
  `docs/specs/000-baseline.md`) se propagaría tal cual — revisado
  originalmente tras hallazgo H-02 de
  `docs/reviews/adversarial-review-2026-09-21.md`. **El timeout que
  dispara el `504`** es `skaro.trainer.pokeapi-call-timeout` (ver
  Parametrización) y se aplica con un operador `.timeout(Duration)` sobre
  el `Mono` que devuelve `PokeApiClient`, en el punto donde
  `TrainerRosterService` lo llama — vive en `skaro.trainer`, no en
  `skaro.pokeapi.client` ni en su configuración, que la spec ya declara
  "no se toca" (ver Alcance del cambio). Aclarado tras hallazgo H-10 de
  `docs/reviews/adversarial-review-2026-09-21-r3.md`; antes la spec hablaba
  de un "timeout configurado" sin declarar la propiedad ni dónde se
  implementaba, y el cliente PokéAPI existente no tiene ningún timeout
  hoy (`PokeApiReactorBaseConfiguration`).
- **Timeout en tests**: el valor de `skaro.trainer.pokeapi-call-timeout` se
  overridea a un valor bajo (`50ms`) **solo en la o las clases de test que
  cubren los 2 escenarios de timeout** (captura y listado del Equipo
  Activo — probablemente clases distintas, al ser dos endpoints
  separados), vía
  `@TestPropertySource(properties = "skaro.trainer.pokeapi-call-timeout=50ms")`
  (o el mecanismo equivalente de Spring Test) — no como override de todo
  el perfil `test`. Esos 2 escenarios simulan la demora con
  `MockWebServer.setBodyDelay(...)`, el mismo mecanismo de stub HTTP que ya
  usa el proyecto para PokéAPI (`WebClientEntityFactoryTest`), sin esperar
  el timeout real de producción (`3s`) ni introducir tiempo virtual. El
  resto de la suite (captura, listados, validaciones) sigue con el
  `3s` de producción — un override a nivel de perfil completo haría
  depender casi todos los escenarios de esta spec (cualquiera que llame a
  `PokeApiClient`, no solo los 2 de timeout) de que `MockWebServer`
  responda dentro de esos `50ms`, con riesgo real de `504` espurios bajo
  lentitud de CI. Aclarado tras hallazgo H-11 de
  `docs/reviews/adversarial-review-2026-09-21-r4.md`; acotado el alcance
  del override tras hallazgo H-12 de
  `docs/reviews/adversarial-review-2026-09-21-r5.md`.
- **Payload de captura — campo obligatorio y defaults**: el único campo
  obligatorio es `especie`; su ausencia responde `400 Bad Request`. El
  resto de los campos (IVs, EVs, naturaleza, habilidad, shiny, género,
  movimientos, held item, pokéball, nivel inicial, ubicación de origen)
  tiene un valor por defecto — ver `## Valores por defecto del payload de
  captura`. Revisado tras hallazgo H-01 del mismo review.
- Plazo de entrega acotado del challenge — prioridad a tener el flujo
  end-to-end funcionando por sobre exhaustividad de tests/validaciones no
  pedidas explícitamente.

## Parametrización

Regla del proyecto (`~/.claude/CLAUDE.md` § "Sin hardcode, sin magic
strings"): todo valor de configuración o límite de negocio va
parametrizable salvo justificación fuerte de lo contrario. Repaso
explícito de todos los valores, mínimos, máximos y cotas que aparecen en
esta spec:

### Configurables

Bean `TrainerConfigurationProperties`, prefix `skaro.trainer` — mismo
patrón que `PokeApiConfigurationProperties`/`CONFIGURATION_PROPERTIES_PREFIX`
ya existente en `skaro.pokeapi`.

| Propiedad (`application.yml`) | Campo Java | Default sugerido | Origen |
|---|---|---|---|
| `skaro.trainer.team-limit` | `teamLimit` | `6` | PRD A1 (sin valor especificado en la fuente) — `PRODUCT.md` lo fija como default, no como constante |
| `skaro.trainer.box-limit` | `boxLimit` | `300` | PRD A2 (sin valor especificado en la fuente) — `PRODUCT.md` lo fija como default |
| `skaro.trainer.default-trainer-id` | `defaultTrainerId` | `1` | Restricciones de esta spec (Entrenador fijo/único) — se usa para sembrar la única fila `Trainer` al arrancar y para validar `{trainerId}` en cada request |
| `skaro.trainer.pokeapi-call-timeout` | `pokeApiCallTimeout` (`Duration`) | `3s` | Hallazgo H-10 de `docs/reviews/adversarial-review-2026-09-21-r3.md` — dispara el `504 Gateway Timeout` de la tabla de errores; ver Restricciones para dónde se aplica |
| `skaro.trainer.pokeapi-call-timeout` (override vía `@TestPropertySource`, solo en la clase de test de los 2 escenarios de timeout — no todo el perfil `test`) | `pokeApiCallTimeout` | `50ms` | Hallazgo H-11 de `docs/reviews/adversarial-review-2026-09-21-r4.md` — evita que esos 2 escenarios esperen los 3s reales de producción; alcance acotado tras hallazgo H-12 de `docs/reviews/adversarial-review-2026-09-21-r5.md`; ver Restricciones § "Timeout en tests" |

### Constantes de dominio (NO configurables — justificación explícita)

Estos valores no son una política de negocio de esta entrega: son la
definición misma de los conceptos de Pokémon tal como los fija el PRD.
No aparecen en la lista de ambigüedades A1-A12 del PRD — están dados como
regla fija en el enunciado, no como algo a decidir. Cambiarlos en runtime
no ajustaría una configuración de negocio válida, dejaría de implementar
lo que el PRD pide. Es la misma categoría de excepción que
`~/.claude/CLAUDE.md` da como ejemplo legítimo de hardcode ("un enum que
define un contrato del dominio, o un valor que cambiarlo en runtime
rompería una invariante") — se registra también como D2 en
`docs/DESIGN.md`.

| Constante sugerida | Valor | Regla | Justificación puntual |
|---|---|---|---|
| `IV_MIN` / `IV_MAX` | `0` / `31` | BR-002 | Rango fijo de la mecánica oficial de Pokémon, dado explícito en el PRD |
| `EV_TOTAL_MAX` | `510` | BR-003 | Tope oficial de puntos de esfuerzo, dado explícito en el PRD |
| `EV_STAT_MAX` | `252` | BR-004 | Tope oficial por estadística, dado explícito en el PRD |
| `MOVES_MAX` | `4` | BR-008 | Tamaño fijo del set de movimientos en todos los juegos oficiales |
| `NATURE_STAT_MODIFIER` | `0.10` (10%) | BR-005 | El ±10% es la definición de "naturaleza", no un parámetro ajustable |

`Held Item` (BR-007, máximo 1) no genera ni constante ni parámetro: se
modela estructuralmente como un campo único y nullable en
`PokemonInstance` — no hay un número que hardcodear ni que parametrizar.

Los códigos de estado HTTP (`201`/`200`/`400`/`422`/`404`/`409`) tampoco
entran en esta regla: son semántica del protocolo HTTP, no valores de
negocio.

## Valores por defecto del payload de captura

Único campo obligatorio: `especie`. Si el resto se omite, se aplican estos
defaults (decisión registrada en `docs/PRODUCT.md`, motivada por hallazgo
H-01 de `docs/reviews/adversarial-review-2026-09-21.md`):

| Campo | Default si se omite |
|---|---|
| IVs (las 6 stats) | `0` |
| EVs (las 6 stats) | `0` |
| Naturaleza | `Hardy` (una de las 5 naturalezas neutras de la tabla oficial, `PRODUCT.md` A7) |
| Habilidad | La primera habilidad de la especie según PokéAPI |
| Shiny | `false` |
| Género | Sin Género |
| Movimientos | `[]` (vacío) |
| Held Item | ninguno |
| Pokéball | Poké Ball |
| Nivel inicial | `1` |
| Ubicación de origen | `"Desconocida"` |
| Fecha de captura | No es un default: **siempre** la genera el sistema al crear el registro (ya resuelto, `PRODUCT.md` A11) — un valor provisto por el cliente se ignora, no se rechaza |

## Fuera de alcance

- CRUD de Entrenador.
- Bonus de evolución (FR-024 a FR-028) — ya decidido en `PRODUCT.md`.
- Validar Movimientos contra el moveset real de la especie, o Held Item
  contra el catálogo de Items de PokéAPI.
- Autenticación/autorización.
- Paginación de Equipo/Baúl (listas acotadas por los límites 6/300).
- Endpoint de baja/liberar ejemplar (no lo pide el PRD).

## Criterios de aceptación

### Escenario: Captura exitosa con espacio en el Equipo Activo
Dado que el Equipo Activo del entrenador tiene menos de 6 ejemplares
Cuando se envía `POST /trainers/{trainerId}/pokemon` con una especie válida, IVs (0-31),
  EVs (≤510 total, ≤252 por stat), naturaleza válida y habilidad que
  pertenece a la especie
Entonces responde `201 Created` con el ejemplar creado y `location: team`

### Escenario: Captura con el Equipo Activo lleno va al Baúl
Dado que el Equipo Activo del entrenador ya tiene 6 ejemplares y el Baúl
  tiene espacio libre
Cuando se envía `POST /trainers/{trainerId}/pokemon` con un payload válido
Entonces responde `201 Created` con `location: box`

### Escenario: Captura sin espacio en ningún destino
Dado que el Equipo Activo tiene 6 ejemplares y el Baúl tiene 300
Cuando se envía `POST /trainers/{trainerId}/pokemon` con un payload válido
Entonces responde `409 Conflict` con un mensaje explicando que no hay
  espacio disponible, y no se crea ningún ejemplar

### Escenario: Dos capturas concurrentes no superan el límite del Equipo Activo
Dado que el Equipo Activo del entrenador tiene 5 ejemplares (1 lugar libre)
Cuando se envían dos `POST /trainers/{trainerId}/pokemon` concurrentes con
  payloads válidos
Entonces exactamente uno de los dos responde `201 Created` con
  `location: team`, y el otro responde `201 Created` con `location: box`
  — nunca terminan ambos con `location: team`

### Escenario: Captura con EVs por encima del máximo total
Dado un payload de captura válido salvo que la suma de EVs es 511
Cuando se envía `POST /trainers/{trainerId}/pokemon`
Entonces responde `422 Unprocessable Entity` señalando la violación de
  BR-003

### Escenario: Captura con un EV individual por encima del máximo
Dado un payload de captura válido salvo que un EV individual es 253
Cuando se envía `POST /trainers/{trainerId}/pokemon`
Entonces responde `422 Unprocessable Entity` señalando la violación de
  BR-004

### Escenario: Captura con un IV fuera de rango
Dado un payload de captura válido salvo que un IV es 32
Cuando se envía `POST /trainers/{trainerId}/pokemon`
Entonces responde `422 Unprocessable Entity` señalando la violación de
  BR-002

### Escenario: Captura con una Habilidad que no pertenece a la especie
Dado un payload de captura válido salvo que la habilidad indicada no está
  entre las habilidades de la especie según PokéAPI
Cuando se envía `POST /trainers/{trainerId}/pokemon`
Entonces responde `422 Unprocessable Entity` señalando la violación de
  BR-006

### Escenario: Captura con más de 4 movimientos
Dado un payload de captura válido salvo que se incluyen 5 movimientos
Cuando se envía `POST /trainers/{trainerId}/pokemon`
Entonces responde `422 Unprocessable Entity` señalando la violación de
  BR-008

### Escenario: Captura con una naturaleza inválida
Dado un payload de captura válido salvo que la naturaleza indicada no es
  una de las 25 naturalezas oficiales
Cuando se envía `POST /trainers/{trainerId}/pokemon`
Entonces responde `422 Unprocessable Entity` señalando la violación de
  BR-005

### Escenario: Captura con una especie inexistente en PokéAPI
Dado un payload de captura con una especie que PokéAPI no reconoce
Cuando se envía `POST /trainers/{trainerId}/pokemon`
Entonces responde `422 Unprocessable Entity` indicando que la especie no
  existe, y no se crea ningún ejemplar

### Escenario: Captura sin el campo obligatorio "especie"
Dado un payload de captura sin el campo `especie`
Cuando se envía `POST /trainers/{trainerId}/pokemon`
Entonces responde `400 Bad Request`, y no se crea ningún ejemplar

### Escenario: Captura con payload mínimo aplica los valores por defecto
Dado un payload de captura que solo especifica `especie`
Cuando se envía `POST /trainers/{trainerId}/pokemon`
Entonces responde `201 Created` con el ejemplar creado, IVs y EVs en 0,
  naturaleza `Hardy`, la primera habilidad de la especie según PokéAPI,
  shiny `false`, género Sin Género, sin movimientos y sin objeto equipado
  (ver `## Valores por defecto del payload de captura`)

### Escenario: PokéAPI responde 5xx o rechaza la conexión durante la captura
Dado que PokéAPI responde `5xx` o rechaza la conexión al resolver la
  especie o la habilidad indicadas
Cuando se envía `POST /trainers/{trainerId}/pokemon` con un payload válido
Entonces responde `502 Bad Gateway`, y no se crea ningún ejemplar

### Escenario: PokéAPI no responde dentro del timeout durante la captura
Dado que PokéAPI no responde dentro del timeout configurado al resolver
  la especie o la habilidad indicadas
Cuando se envía `POST /trainers/{trainerId}/pokemon` con un payload válido
Entonces responde `504 Gateway Timeout`, y no se crea ningún ejemplar

### Escenario: Listar el Equipo Activo devuelve vista compuesta
Dado que el entrenador tiene al menos 1 ejemplar en el Equipo Activo
Cuando se hace `GET /trainers/{trainerId}/pokemon?location=team`
Entonces responde `200 OK` con una lista donde cada elemento combina la
  metadata propia del ejemplar (genética, origen, estado) con atributos
  estáticos de la especie obtenidos de PokéAPI (FR-021)

### Escenario: PokéAPI responde 5xx o rechaza la conexión al listar el Equipo Activo
Dado que el entrenador tiene al menos 1 ejemplar en el Equipo Activo
  y PokéAPI responde `5xx` o rechaza la conexión al resolver los
  atributos estáticos de la especie
Cuando se hace `GET /trainers/{trainerId}/pokemon?location=team`
Entonces responde `502 Bad Gateway`

### Escenario: PokéAPI no responde dentro del timeout al listar el Equipo Activo
Dado que el entrenador tiene al menos 1 ejemplar en el Equipo Activo
  y PokéAPI no responde dentro del timeout configurado al resolver los
  atributos estáticos de la especie
Cuando se hace `GET /trainers/{trainerId}/pokemon?location=team`
Entonces responde `504 Gateway Timeout`

### Escenario: Listar el Baúl
Dado que el entrenador tiene al menos 1 ejemplar en el Baúl
Cuando se hace `GET /trainers/{trainerId}/pokemon?location=box`
Entonces responde `200 OK` con la lista de ejemplares almacenados en el
  Baúl

### Escenario: Detalle de un ejemplar existente
Dado un ejemplar capturado previamente con id conocido
Cuando se hace `GET /trainers/{trainerId}/pokemon/{id}`
Entonces responde `200 OK` con toda su metadata técnica, genética y de
  origen (FR-020)

### Escenario: Detalle de un ejemplar inexistente
Dado que no existe ningún ejemplar con el id solicitado
Cuando se hace `GET /trainers/{trainerId}/pokemon/{id}`
Entonces responde `404 Not Found`

### Escenario: Request con trainerId que no coincide con el entrenador fijo
Dado el `trainerId` del entrenador fijo de esta entrega
Cuando se hace cualquier request a `/trainers/{otroTrainerId}/pokemon/...`
  con un `{otroTrainerId}` distinto
Entonces responde `404 Not Found`

### Escenario: Mover del Baúl al Equipo Activo con espacio disponible
Dado un ejemplar en el Baúl y el Equipo Activo con menos de 6 ejemplares
Cuando se hace `PATCH /trainers/{trainerId}/pokemon/{id}` con body
  `{"location": "team"}`
Entonces responde `200 OK` y el ejemplar queda con `location: team`

### Escenario: Mover del Baúl al Equipo Activo sin espacio disponible
Dado un ejemplar en el Baúl y el Equipo Activo con 6 ejemplares
Cuando se hace `PATCH /trainers/{trainerId}/pokemon/{id}` con body
  `{"location": "team"}`
Entonces responde `409 Conflict` y el ejemplar no cambia de ubicación

### Escenario: Mover del Equipo Activo al Baúl
Dado un ejemplar en el Equipo Activo y el Baúl con espacio disponible
Cuando se hace `PATCH /trainers/{trainerId}/pokemon/{id}` con body
  `{"location": "box"}`
Entonces responde `200 OK` y el ejemplar queda con `location: box`

### Escenario: Mover del Equipo Activo al Baúl sin espacio disponible
Dado un ejemplar en el Equipo Activo y el Baúl con 300 ejemplares
Cuando se hace `PATCH /trainers/{trainerId}/pokemon/{id}` con body
  `{"location": "box"}`
Entonces responde `409 Conflict` y el ejemplar no cambia de ubicación

(La suite de `docs/specs/000-baseline.md` sigue 100% verde durante todo el
ciclo — no es un escenario más, es la condición de fondo.)
