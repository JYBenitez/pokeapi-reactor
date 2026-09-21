# pokeapi-reactor

Cliente reactivo (WebFlux) de PokéAPI, en proceso de extensión de dominio
para el Desafío Técnico Betwarrior Java Developer (gestión de Pokémon
individuales de un entrenador: Equipo Activo / Baúl). Ver `docs/PRD.md`
(el encargo) y `docs/PRODUCT.md` (decisiones tomadas) antes de asumir nada
que no esté ahí.

## Permisos de git

**Sin autorización explícita para cada vez: no `git commit`, no `git push`.**
Se escribe/edita código y se deja como cambios sin commitear — el
checkpoint de revisión es el working tree (`git diff`, `git status`), no
un resumen después del hecho. Preparar un PR significa dejar el código
listo para revisar, no commitearlo y mostrar el resultado. Recién con un
"sí, commiteá" (o equivalente) explícito de la persona usuaria se corre
`git commit`; recién con un "sí, pusheá" explícito se corre `git push`.
Una instrucción general como "arranquemos las correcciones" o "preparame
el PR" no es esa autorización — hay que pedirla en el momento de
commitear/pushear, no asumirla de una instrucción anterior.

## Método de trabajo

Este proyecto sigue Spec-Driven Development: la spec es la fuente de
verdad, el código es una derivación. Método completo, con plantillas y
gates, en `SDD-CONVENTION.md`. En resumen:

- Cambio de comportamiento → spec primero, siempre (`docs/specs/NNN-*.md`).
- Los criterios de aceptación son escenarios Gherkin (Dado/Cuando/Entonces).
- Implementación con TDD real: ciclo rojo/verde/refactor, un escenario a
  la vez — nunca todos los tests seguidos de todo el código.
- Es un proyecto heredado (Modo B): antes de tocar código nuevo hay que
  respetar el baseline en `docs/specs/000-baseline.md` (characterization
  tests, tienen que seguir verdes siempre).
- Estado actual y decisiones: `docs/AS-IS.md` (mapa del código heredado) y
  `docs/PRODUCT.md` (ambigüedades del PRD ya resueltas).

## Stack

- **Java 21, Spring Boot 3.5.16** (parent) — migrado el 2026-09-20 desde
  Java 11 / Spring Boot 2.4.3, ver `docs/PRODUCT.md § Cambios` para el
  motivo completo. Código en `jakarta.validation` (no `javax`).
- WebFlux + Project Reactor (`reactor-core`, `reactor-extra`,
  `reactor-netty`) — todo el cliente PokéAPI es reactivo, sin variante
  bloqueante.
- Maven (`./mvnw`), sin plugin de arranque activo (`spring-boot-maven-plugin`
  con `skip=true`): esto es una librería, no una app con `main()`.
- Persistencia nueva: H2 en modo archivo vía R2DBC (tentativo, ver
  `docs/PRODUCT.md` — puede revisarse a JPA+boundedElastic si R2DBC
  resulta desproporcionado).

## Comandos

Esta máquina tiene varios JDKs instalados — **siempre** exportar el 21
antes de correr Maven:

```
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./mvnw test              # unit tests + reporte JaCoCo (bindeado a la fase test)
./mvnw clean verify       # build completo con tests
```

Reporte de cobertura: `target/site/jacoco/index.html` después de `test`.
Confirmado 2026-09-20: 21 tests, `BUILD SUCCESS` con Temurin 21 (arm64).
Tests: JUnit 5 + Mockito + `reactor-test` (`StepVerifier`) +
`okhttp`/`mockwebserver` 5.5.0 para stubear HTTP (versión fijada a mano —
el BOM de Boot 3.5.x ya no la gestiona).

Un `ERROR` de Netty (`Unable to load ... MacOSDnsServerAddressStreamProvider`)
es ruido cosmético de logging, no un fallo real — cae a resolución DNS del
sistema y los tests pasan igual.

## Reglas de codificación

- **Sin hardcode, sin magic strings.** Todo valor de configuración, límite
  de negocio o nombre de recurso va parametrizable (config bean, env var),
  no clavado en la lógica — salvo que parametrizarlo sea desaconsejable o
  no razonablemente posible, y en ese caso queda justificado con comentario
  o entrada en `docs/DESIGN.md`. Ejemplo ya existente a replicar:
  `PokeApiConfigurationProperties.maxBytesToBuffer` (default con
  getter/setter, no una constante suelta).
- Un commit no mezcla cambio de comportamiento con cambio de estructura.
- No arreglar bugs "de paso" fuera de la spec activa — anotarlos y
  proponer una spec aparte.

## Estructura (detalle completo en `docs/AS-IS.md`)

- `skaro.pokeapi.client` — `PokeApiClient` y sus implementaciones reactivas.
- `skaro.pokeapi.cache` — cache reactivo sobre `CacheManager`.
- `skaro.pokeapi.resource.**` — ~90 DTOs de PokéAPI, sin lógica.
- `skaro.pokeapi` (raíz) — `@Configuration` de WebClient, cache y registro
  de endpoints.
