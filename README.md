![Coverage](.github/badges/jacoco.svg)
![Branches](.github/badges/branches.svg)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

# pokeapi-reactor
A non-blocking, reactive API client for [PokeAPI](https://pokeapi.co/) with caching for Spring Boot projects.

---

## Desafío Técnico Betwarrior — Baúl de Pokémon y Equipo Activo

Este fork extiende `pokeapi-reactor` (hasta acá, una librería cliente sin
servidor propio) con una app Spring Boot real que gestiona los Pokémon
individuales de un entrenador: **Equipo Activo** (máximo 6) y **Baúl**
(máximo 300), con la genética y metadata propia de cada ejemplar (IVs,
EVs, naturaleza, habilidad, etc.).

El proceso completo — PRD, arqueología del código heredado, spec con
criterios Gherkin, decisiones de diseño y 5 rondas de review adversarial —
está documentado en `docs/`:

- [`docs/PRD.md`](docs/PRD.md) — el encargo, traducido a requisitos.
- [`docs/PRODUCT.md`](docs/PRODUCT.md) — las ambigüedades del PRD, resueltas con su motivo.
- [`docs/AS-IS.md`](docs/AS-IS.md) — mapa del código heredado antes de tocarlo.
- [`docs/specs/001-equipo-activo-y-baul.md`](docs/specs/001-equipo-activo-y-baul.md) — la spec de esta feature, con los 26 escenarios Gherkin.
- [`docs/DESIGN.md`](docs/DESIGN.md) — decisiones técnicas no obvias (JPA vs. R2DBC, lock pesimista, etc.) con las alternativas descartadas.
- [`docs/AI-WORKFLOW.md`](docs/AI-WORKFLOW.md) — ajustes no triviales que aparecieron al implementar y no estaban anticipados en la spec.
- [`docs/reviews/`](docs/reviews/) — 5 pasadas de review adversarial del PRD/spec contra el enunciado original.

### Cómo correrlo

```
export JAVA_HOME=$(/usr/libexec/java_home -v 21)   # o el JDK 21 que tengas
./mvnw spring-boot:run
```

Levanta en `http://localhost:8080`, con una base H2 en modo archivo
(`./data/pokeapi-reactor.mv.db`, se crea sola) y un único Entrenador
sembrado con id `1` (`skaro.trainer.default-trainer-id`, configurable en
`src/main/resources/application.yml`). No hace falta nada más instalado —
no hay Docker, no hay Postgres, no hay que correr migraciones a mano.

Para correr la suite de tests (baseline del cliente PokéAPI + los tests
nuevos de esta feature):

```
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./mvnw test
```

### Endpoints

Todos bajo `/trainers/{trainerId}/pokemon` — con el entrenador fijo de
esta entrega, `{trainerId}` es siempre `1`.

| Método | Path | Qué hace |
|---|---|---|
| `POST` | `/trainers/1/pokemon` | Captura un ejemplar nuevo; lo asigna a Equipo Activo o Baúl según espacio |
| `GET` | `/trainers/1/pokemon?location=team` | Lista el Equipo Activo — vista compuesta (metadata propia + atributos estáticos de la especie desde PokéAPI) |
| `GET` | `/trainers/1/pokemon?location=box` | Lista el Baúl |
| `GET` | `/trainers/1/pokemon/{id}` | Detalle completo de un ejemplar |
| `PATCH` | `/trainers/1/pokemon/{id}` | Traslada un ejemplar (body `{"location": "team"}` o `{"location": "box"}`) |

Ejemplo de captura — único campo obligatorio es `species`, el resto tiene
defaults (ver la spec, § "Valores por defecto del payload de captura"):

```
curl -X POST http://localhost:8080/trainers/1/pokemon \
  -H "Content-Type: application/json" \
  -d '{
    "species": "pikachu",
    "ivs": {"hp":1,"attack":2,"defense":3,"specialAttack":4,"specialDefense":5,"speed":6},
    "evs": {"hp":10,"attack":10,"defense":10,"specialAttack":10,"specialDefense":10,"speed":10},
    "nature": "HARDY",
    "ability": "static"
  }'

curl "http://localhost:8080/trainers/1/pokemon?location=team"

curl -X PATCH http://localhost:8080/trainers/1/pokemon/1 \
  -H "Content-Type: application/json" \
  -d '{"location": "box"}'
```

### Decisiones y trade-offs (resumen — detalle en `docs/DESIGN.md` y `docs/PRODUCT.md`)

- **JPA (Hibernate) en vez de R2DBC end-to-end**, con las llamadas
  bloqueantes aisladas en `skaro.trainer.persistence` vía
  `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())` — el
  resto de la app (dominio, API, y todo el cliente PokéAPI reactivo
  existente) solo ve `Mono`/`Flux`. Spring Data R2DBC no soporta objetos
  embebidos ni colecciones de elementos con el nivel de madurez que este
  modelo necesitaba (IVs/EVs embebidos, hasta 4 movimientos), y el plazo
  del challenge no daba para mapeo manual fila a fila (`docs/DESIGN.md` D1).
- **Lock pesimista** (`@Lock(PESSIMISTIC_WRITE)`) sobre la fila del
  Entrenador al capturar/mover, en una transacción dedicada — evita que
  dos requests concurrentes exceedan el límite de 6/300 (`docs/DESIGN.md` D3).
- **Entrenador fijo/único** en esta entrega (id `1`, sembrado al
  arrancar) pero modelado como entidad de dominio propia, no un simple
  campo — la URL ya anida por `trainerId` para no romper el contrato el
  día que haya multi-entrenador real (`docs/PRODUCT.md`, A9).
- **`records` de Java 21** para las DTOs de la API (`CaptureRequest`,
  `PokemonInstanceResponse`, etc.) — inmutables y sin boilerplate. Las
  entidades JPA (`Trainer`, `PokemonInstance`, `Ivs`, `Evs`) siguen siendo
  clases mutables, porque Hibernate no soporta records de forma confiable
  como tipo administrado.
- **Sin autenticación/autorización** — fuera de alcance a propósito, no
  es el foco del challenge (`docs/PRODUCT.md`).

### Qué falta (recorte consciente de alcance)

El propio enunciado del challenge prioriza "la calidad del diseño por
sobre la completitud" dado el tiempo disponible. Con ese criterio, de los
26 escenarios Gherkin de la spec se implementaron 15 — el flujo completo
(los 5 endpoints funcionando de punta a punta contra H2 real) más las
validaciones de negocio núcleo. Quedaron sin implementar, **diseñados y
documentados pero no codeados** (detalle en `docs/AI-WORKFLOW.md`):

- Naturaleza inválida, especie inexistente en PokéAPI, payload sin
  `species`, `trainerId` que no coincide con el entrenador fijo — casos de
  validación adicionales sobre el mismo mecanismo ya implementado.
- Traslado sin espacio disponible en ambas direcciones (409) — el caso
  "sin espacio" ya está implementado para la captura, falta el mismo
  chequeo aplicado al traslado.
- Traducción de fallas de PokéAPI (timeout → `504`, `5xx`/conexión
  rechazada → `502`) durante la captura y el listado del Equipo Activo —
  el mecanismo de traducción de errores para "especie inexistente" (`422`)
  sí está implementado; falta el resto de los casos de falla externa.
- El escenario de dos capturas concurrentes compitiendo por el último
  lugar del Equipo Activo — el lock pesimista que lo resuelve ya está
  implementado (se usa en todas las capturas/traslados), pero no hay un
  test que ejercite específicamente la concurrencia.

Ninguno de estos afecta el camino feliz de los 5 endpoints ni las
validaciones de negocio centrales (IVs, EVs, movimientos, habilidad).

### Mantenimiento — estructura del código nuevo

```
skaro.trainer
├── domain        Trainer, PokemonInstance, reglas de negocio (TrainerRosterService)
├── persistence   Repositorios JPA + wrappers reactivos (Mono.fromCallable + boundedElastic)
├── api           Controladores REST y DTOs (records)
└── config        TrainerConfigurationProperties (límites, timeout — mismo patrón que PokeApiConfigurationProperties)
```

No se tocó nada bajo `skaro.pokeapi.**` (el cliente PokéAPI original) — la
suite de characterization tests de `docs/specs/000-baseline.md` sigue
100% verde. Los tests nuevos están bajo `src/test/java/skaro/trainer/**`,
uno por endpoint, usando `WebTestClient` contra un servidor real en
puerto aleatorio y `MockWebServer` para stubear PokéAPI (mismo patrón que
ya usaba el proyecto en `WebClientEntityFactoryTest`).

---

## El cliente PokéAPI original (sin cambios)

Todo lo que sigue es la librería cliente original de `pokeapi-reactor`
(`skaro.pokeapi.**`) — no se tocó nada acá, documentado tal cual estaba.

### Features
* Simple, single [entry point](../master/src/main/java/skaro/pokeapi/client/PokeApiClient.java) for all client operations.
* Non-blocking HTTP operations and non-blocking caching.
* Fully customizable caching. Supports Spring Boot's generic [CacheManager](https://docs.spring.io/spring-boot/docs/1.3.0.M1/reference/html/boot-features-caching.html#_supported_cache_providers) for caching.
* Caching and non-caching configurations.

## Getting started
### Project Configuration

#### Properties
You can (must) configure the location of the PokeAPI instance you want to use. Add the following property to your `application.properties`:

```
skaro.pokeapi.base-uri=https://pokeapi.co/api/v2/ #or the url of your own instance
```

You may also configure the max buffer size for the WebClient, which is used to fetch resources from PokeAPI.
Its default value is 565000 bytes, while the API request for "/pokemon/mew" can grow over the time, you may want to increase it yourself to a higher value.
To achieve this, add the following property to your `application.properties`:
```
skaro.pokeapi.max-buffer-size=565000
```

#### Application Context
Import one of pokeapi-reactor's configurations as well as specify your own [reactor.netty.http.client.HttpClient](https://projectreactor.io/docs/netty/release/api/reactor/netty/http/client/HttpClient.html) bean. Two configurations are available: caching and non-caching. Below is an example of a caching configuration which uses a flexible `HttpClient` tuned for high parallel throughput.

```java
@Configuration
@Import(PokeApiReactorCachingConfiguration.class)
@EnableCaching
public class MyPokeApiReactorCachingConfiguration {
	@Bean
	public ConnectionProvider connectionProvider() {
	    return ConnectionProvider.builder("Auto refresh & no connection limit")
		    .maxIdleTime(Duration.ofSeconds(10))
		    .maxConnections(500)
		    .pendingAcquireMaxCount(-1)
		    .build();
	}

	@Bean
	public HttpClient httpClient(ConnectionProvider connectionProvider) {
		return HttpClient.create(connectionProvider)
                  .compress(true)
                  .resolver(DefaultAddressResolverGroup.INSTANCE);
	}
}
```
Or, if you'd rather not enable caching:
```java
@Configuration
@Import(PokeApiReactorNonCachingConfiguration.class)
public class MyPokeApiReactorNonCachingConfiguration {
	@Bean
	public ConnectionProvider connectionProvider() { ... }
	
	@Bean
	public HttpClient httpClient(ConnectionProvider connectionProvider) { ... }
}
```
Both the `PokeApiReactorCachingConfiguration` and `PokeApiReactorNonCachingConfiguration` will register the appropriate `PokeApiClient` bean.

### Fetching a resource
Inject the registered `PokeApiClient` into your class and request a resource.
```java
@Autowired
private PokeApiClient pokeApiClient;

...

public void printPokemon() {
   pokeApiClient.getResource(Pokemon.class, "pikachu")
      .map(pokemon -> String.format("%s has %d forms", pokemon.getName(), pokemon.getForms().size()))
      .subscribe(System.out::println);
}
```
If you don't mind blocking, you can simply block for the resource.
```java
public void printPokemon() {
   Pokemon pokemon = pokeApiClient.getResource(Pokemon.class, "pikachu").block();
   String pokemonInfo = String.format("%s has %d forms", pokemon.getName(), pokemon.getForms().size()));
   System.out.println(pokemonInfo);
}
```

### Following a resource
Following links to other resources also has first-class support.

```java
public void printPokemonForms() {
   pokeApiClient.getResource(Pokemon.class, "pikachu")
      .flatMapMany(pokemon -> pokeApiClient.followResources(pokemon::getForms, PokemonForm.class))
      .map(form -> String.format("Pikachu has a form called %s", form.getName()))
      .subscribe(System.out::println);
}
```
Again, with blocking:
```java
public void printPokemonForms() {
   Pokemon pokemon = pokeApiClient.getResource(Pokemon.class, "pikachu").block();
   List<PokemonForm> forms = pokeApiClient.followResources(pokemon::getForms, PokemonForm.class)
      .collectList()
      .block();

   for(PokemonForm form : forms) {
      String formInfo = String.format("Pikachu has a form called %s", form.getName());
      System.out.println(formInfo);
   }
}
```
