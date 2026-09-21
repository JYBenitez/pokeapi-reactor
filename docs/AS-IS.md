# Estado heredado

Fase F1 (Arqueología) del método SDD. Exploración de solo lectura sobre
`pokeapi-reactor`, sin tocar código. Insumo para `specs/000-baseline.md`
(F2) y para las decisiones pendientes de `docs/PRODUCT.md`.

## Hallazgo que condiciona todo lo demás

`pokeapi-reactor` **no es una aplicación con servidor HTTP** — es una
**librería cliente reactivo** (WebFlux) para consumir la API pública de
PokéAPI, pensada para ser importada por otra app Spring Boot:

- `spring-boot-maven-plugin` tiene `<skip>true</skip>` (`pom.xml:69-74`).
- No hay clase con `main()`, ni `@SpringBootApplication`.
- Cero coincidencias de `@RestController` / `@RequestMapping` en todo
  `src/main/java` (verificado por grep).
- No hay ninguna dependencia de persistencia en `pom.xml` (sin
  JPA/Hibernate/JDBC/R2DBC/Mongo) ni de seguridad, y no existe siquiera
  `src/main/resources` (sin `application.properties` versionado).

Esto significa que los endpoints REST y la capa de persistencia que pide
el PRD (FR-001 en adelante) **se construyen desde cero**, no se refactoriza
algo que ya exista con esa forma. Repo git con un único commit ("Initial
commit: base del challenge técnico"), remote `JYBenitez/pokeapi-reactor`.

## Mapa

**Entrypoint actual (no HTTP — API de librería Java):**
- `skaro.pokeapi.client.PokeApiClient` — interfaz de 5 métodos
  (`getResource(Class)`, `getResource(Class,id)`,
  `getResource(Class,PageQuery)`, `followResource`, `followResources`). Es
  el punto de entrada que un consumidor externo inyecta.

**Módulos y responsabilidad:**
- `skaro.pokeapi.client` — `PokeApiClient` (contrato);
  `ReactiveCachingPokeApiClient` / `ReactiveNonCachingPokeApiClient`
  (implementaciones, delegan a `PokeApiEntityFactory`);
  `WebClientEntityFactory` (llamadas HTTP reales vía `WebClient`);
  `PokeApiEndpointRegistry` / `MapEndpointRegistry` (mapea `Class<T>` →
  path del endpoint de PokéAPI).
- `skaro.pokeapi.cache` — `CacheFacade` / `CacheSpec` /
  `ReactiveCacheManagerCacheFacade`: envoltorio reactivo (`CacheMono` de
  reactor-extra) sobre el `CacheManager` genérico de Spring.
- `skaro.pokeapi` (raíz) — 4 `@Configuration`:
  `PokeApiReactorBaseConfiguration` (WebClient, codecs Jackson,
  `PokeApiEntityFactory`), `PokeApiReactorEndpointConfiguration` (registro
  hardcodeado de 41 tipos de recurso → path), `PokeApiReactorCachingConfiguration`
  / `PokeApiReactorNonCachingConfiguration` (registran el bean
  `PokeApiClient` según se quiera caching).
- `skaro.pokeapi.resource.**` — ~90 POJOs DTO que mapean 1:1 las
  respuestas JSON de PokéAPI (Pokemon, PokemonSpecies, Nature, Ability,
  EvolutionChain, Item, Move, Stat, Gender, etc.). Sin lógica, solo
  getters/setters.
- `skaro.pokeapi.utils.locale` — utilidades de localización de
  nombres/textos (`Localizable`, `PokeApiLocaleUtils`).

**Dependencias externas:**
- Parent `spring-boot-starter-parent:2.4.3` (`pom.xml:7-9`), Java 11
  (`pom.xml:18`).
- `spring-boot-starter-webflux` (`pom.xml:22-24`),
  `spring-boot-starter-validation` (`pom.xml:26-28`),
  `reactor-core`/`reactor-extra`/`reactor-netty` (`pom.xml:30-40`).
- Test: `spring-boot-starter-test`, `reactor-test`, `okhttp` +
  `mockwebserver` (`pom.xml:44-63`).
- `distributionManagement` apunta al paquete GitHub del repo original
  `SirSkaro/pokeapi-reactor` (`pom.xml:104-110`).

## Comportamiento observable

- Expone un cliente Java (no HTTP) para leer ~41 tipos de recurso de
  PokéAPI por id/nombre, en forma paginada, o "siguiendo" un link
  (`NamedApiResource`) a su recurso completo.
- No expone ningún endpoint REST propio; quien use la librería debe montar
  su propia app/servidor.
- Cada request saliente a PokéAPI se loguea en INFO con método + URL
  (`PokeApiReactorBaseConfiguration.java:75-79`).
- La deserialización JSON usa snake_case automático y **falla** ante
  propiedades desconocidas del JSON de origen
  (`PokeApiReactorBaseConfiguration.java:48-50`). Corregido respecto del
  hallazgo original de esta arqueología — ver Rareza #2: ignoraba estas
  propiedades en silencio.
- Con la configuración de cache activa, si el cache nombrado no existe en
  el `CacheManager`, el request se resuelve igual contra la red pero no se
  cachea nada; se loguea en **ERROR**
  (`ReactiveCacheManagerCacheFacade.java:65-72`). Corregido respecto del
  hallazgo original de esta arqueología — ver Rareza #3: antes solo
  logueaba un WARN.
- No hay manejo de errores HTTP propio: `.retrieve()` sin `onStatus`
  (`WebClientEntityFactory.java:29-36,41-45,50-58,62-66`) — un 404/500 de
  PokéAPI se propaga como excepción default de WebClient.

## Dolores

| # | Problema | Archivo:línea | Impacto |
|---|---|---|---|
| P1 | No existe ninguna capa de persistencia (DB, ORM, repos) | `pom.xml` (sin deps JPA/JDBC/Mongo), sin `src/main/resources` | Alto — hay que construir toda la capa desde cero, decidir motor y dependencias |
| P2 | No hay capa HTTP; el proyecto es una librería sin servidor | `pom.xml:69-74` (maven plugin `skip=true`), sin clase `main` | Alto — hay que decidir si este mismo módulo se convierte en app o se agrega un módulo/app consumidora nueva |
| P3 | No hay capa de dominio/servicio; el cliente solo resuelve `Class<T>` + id genérico | `WebClientEntityFactory.java:26-36` | Medio-alto — la lógica de negocio nueva (IVs/EVs/naturaleza/equipo/baúl) no tiene dónde apoyarse salvo el cliente HTTP puro |
| P4 | Registro de endpoints hardcodeado en un único mapa gigante | `PokeApiReactorEndpointConfiguration.java:60-110` (41 entradas) | Bajo-medio — ya cubre lo necesario (pokemon, species, nature, ability, evolution-chain, item, move, stat); rígido si hiciera falta extenderlo |
| P5 | `Region.class` mapeado al path `"pokemon-region"` en vez de `"region"` (el real de PokéAPI es `/api/v2/region/`) | `PokeApiReactorEndpointConfiguration.java:102` | Medio — solo rompe si la feature usa `Region` (p. ej. "ubicación de origen", A10 del PRD) |
| P6 | Stack desactualizado: Spring Boot 2.4.3 / Java 11 (2021) | `pom.xml:9,18` | Medio — condiciona qué starters de persistencia moderna son compatibles; es la ambigüedad A5 del PRD |
| P7 | Todo el código actual es reactivo extremo a extremo (Mono/Flux/WebClient); sumar un ORM bloqueante clásico mezclaría dos modelos de concurrencia en el mismo proceso | arquitectural, no puntual | Alto — decisión de diseño central a documentar explícitamente en `DESIGN.md` (reactivo end-to-end vs. bloqueante) |
| P8 | Sin traducción de errores HTTP a errores de dominio | `WebClientEntityFactory.java:29-36` | Medio — riesgo de códigos de respuesta inconsistentes hacia afuera si la nueva capa no envuelve esto (afecta NFR-004 del PRD) |

## Rarezas y trampas

Decididas por la persona usuaria el 2026-09-20. Todas quedan **anotadas
para corregir en una spec futura** — en esta fase (F1) no se toca código,
solo se registra la decisión.

1. `Region.class → "pokemon-region"` en vez de `"region"`
   (`PokeApiReactorEndpointConfiguration.java:102`).
   **Decisión: ✅ corregir.** Investigado a fondo: es un typo real (la
   PokéAPI real expone `/api/v2/region/`), pero hoy está **dormido** — nada
   lo ejercita. `Region` solo aparece como `NamedApiResource<Region>`
   seguido por link en `Location.java:17`, `Pokedex.java:21` y
   `VersionGroup.java:21`, y `followResource` resuelve esos links con la
   URL que ya viene en el JSON de PokéAPI (`WebClientEntityFactory.java:61-66`),
   no con este registro. El registro roto solo se usaría si algo llamara
   `getResource(Region.class, id)` o `getBaseResource(Region.class)`
   directo — cosa que no ocurre en ningún lado del código ni de los tests
   hoy. Corrección de una línea, sin riesgo: nada depende del valor actual.

2. `FAIL_ON_UNKNOWN_PROPERTIES = false`
   (`PokeApiReactorBaseConfiguration.java:50`).
   **Decisión: ✅ corregir.** No se preserva el descarte silencioso de
   propiedades desconocidas del JSON de PokéAPI.

3. Degradación silenciosa de cache — si el cache nombrado no existe, solo
   WARN y sigue sin cachear (`ReactiveCacheManagerCacheFacade.java:65-72`).
   **Decisión: ✅ corregir** (mismo criterio que el punto 2: nada queda
   silencioso). Todavía no se implementa — queda anotado para la spec que
   corresponda.

4. `maxBytesToBuffer` con default de 565.000 bytes
   (`PokeApiConfigurationProperties.java:9-11`).
   **Decisión: ✅ preservar.** Revisado: no es un valor hardcodeado clavado
   en lógica, es el *default* de un campo configurable (bean con
   getter/setter, quien arma la config puede sobreescribirlo) — ya cumple
   la regla de "no hardcode / parametrizable" que se agregó a nivel usuario
   (ver `~/.claude/CLAUDE.md`). Se toma como **patrón a replicar** para los
   límites nuevos de Equipo Activo y Baúl (A1/A2 del PRD), no como algo a
   corregir.

5. `distributionManagement` apunta al paquete GitHub del repo original
   `SirSkaro/pokeapi-reactor`, no a este fork (`pom.xml:104-110`).
   **Decisión: ✅ corregir (eliminar el bloque).** Se confirmó que todo el
   trabajo — cliente PokéAPI + REST + persistencia — va a vivir en este
   mismo repo, no como librería separada publicada aparte; el bloque de
   publicación a un registro Maven de otra persona ya no tiene sentido.
   Pendiente de ejecutar, no se toca código en esta fase.

## Zonas sin cobertura

- Los ~90 DTOs bajo `src/main/java/skaro/pokeapi/resource/**` están
  explícitamente excluidos de JaCoCo (`pom.xml:83`) y no tienen tests.
- Las 4 clases `@Configuration` y `PokeApiConfigurationProperties` están
  excluidas de JaCoCo (`pom.xml:81-82`) y no tienen test files — sin
  verificación automatizada del wiring de beans.
- `MapEndpointRegistry.java` no tiene test dedicado.
- `CacheSpec.java` (builder) solo se ejercita indirectamente vía
  `ReactiveCachingPokeApiClientTest`.
- No hay tests de integración end-to-end contra PokéAPI real ni contra un
  stub completo; solo mocks unitarios
  (`WebClientEntityFactoryTest.java`, 242 líneas, usa `MockWebServer`).
- Cobertura real concentrada en 5 archivos:
  `cache/ReactiveCacheManagerCacheFacadeTest.java` (136L),
  `client/ReactiveCachingPokeApiClientTest.java` (167L),
  `client/ReactiveNonCachingPokeApiClientTest.java` (114L),
  `client/WebClientEntityFactoryTest.java` (242L),
  `utils/locale/PokeApiLocaleUtilsTest.java` (49L). Framework: JUnit 5 +
  Mockito + `reactor-test` (`StepVerifier`).

## Reaprovechable para la feature nueva

- `PokeApiClient` + sus dos implementaciones + `WebClientEntityFactory`:
  cliente HTTP reactivo completo y ya probado para resolver
  Pokemon/PokemonSpecies/Nature/Ability/EvolutionChain/Move/Item/Stat/Gender
  — la capa de dominio nueva puede inyectarlo para traer "los atributos
  estáticos de la especie" que pide FR-021, sin reinventar el cliente.
- DTOs ya modelados, listos para la vista compuesta: `Pokemon.java`,
  `PokemonSpecies.java`, `PokemonAbility.java` (`isHidden`/`slot`/`ability`,
  útil para validar BR-006), `PokemonStat.java` (stat + baseStat, base para
  IVs/EVs), `Nature.java` (`increasedStat`/`decreasedStat` — el 10% de
  BR-005 no viene en el DTO, es regla de negocio a codificar aparte),
  `Gender.java`/`PokemonSpeciesGender.java` (FR-008), `PokemonMove.java`
  (movimientos aprendibles por especie, para validar BR-008),
  `PokemonHeldItem.java`/`Item.java` (referencia para el objeto equipado
  FR-011, aunque describen held items "de especie salvaje", no inventario
  propio del entrenador).
- `EvolutionChain.java` + `ChainLink.java` (recursivo:
  `evolvesTo: List<ChainLink>`) + `EvolutionDetail.java`: modelan
  exactamente lo necesario para el bonus de evolución (FR-024 a FR-028,
  BR-012/BR-016) — soportan líneas simples y ramificadas (Eevee)
  nativamente por la recursión de `ChainLink`, sin reimplementar el árbol.
- `PageQuery.java`: utilidad de paginación, reusable si se pagina el
  listado de Baúl/Equipo.
- `CacheFacade`/`CacheSpec`/`ReactiveCacheManagerCacheFacade`: mecanismo de
  cache reactivo ya probado, reusable para cachear resoluciones de
  especie/habilidad/movimiento al construir la vista compuesta del Equipo
  Activo.
- Patrón `@ConfigurationProperties` + `@Valid` con beans nombrados vía
  `@Qualifier` (`PokeApiConfigurationProperties.java`,
  `PokeApiReactorBaseConfiguration.java`): patrón replicable para la
  config nueva (p. ej. límites de Equipo Activo/Baúl — A1/A2 del PRD).
