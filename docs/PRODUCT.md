# Producto

Interpretación operativa del PRD — con las decisiones ya tomadas. El PRD
(`docs/PRD.md`) no se toca; este documento sí evoluciona si una decisión
cambia más adelante (dejando registro de cuándo y por qué).

## Qué es esto y para quién

Extensión de dominio sobre `pokeapi-reactor` (cliente reactivo de PokéAPI)
para el Desafío Técnico Betwarrior Java Developer: gestión de Pokémon
individuales de un entrenador (Equipo Activo / Baúl), con su genética y
metadata propia. El "para quién" es doble: el sistema en sí (una app que un
entrenador real usaría) y, en la práctica inmediata, el equipo de
desarrollo de Betwarrior que revisa el repo y lo evalúa en una charla
técnica — el diseño tiene que poder explicarse y defenderse en vivo.

No hubo canal de consulta directa al evaluador. Las 8 preguntas del PRD
(§16, Q1-Q8) y las ambigüedades A1-A12 se resolvieron acá como asunciones
explícitas, no quedaron abiertas.

## Decisiones

Cada una referencia el ID de ambigüedad del PRD (`docs/PRD.md`) y, cuando
aplica, el dolor del AS-IS (`docs/AS-IS.md`) que la motiva.

| Tema | Decisión | Motivo / riesgo si cambia |
|---|---|---|
| Modelo de concurrencia (A5) | **Tentativo:** WebFlux + R2DBC end-to-end | Coherente con el cliente PokéAPI ya reactivo (Dolor P7). **A revisar en F4/DESIGN.md** de la spec de persistencia si el esfuerzo de R2DBC resulta desproporcionado; fallback preferido: WebFlux + JPA aislado en `Schedulers.boundedElastic()`, no un salto directo a MVC bloqueante. |
| Motor de persistencia (A6) | H2 en **modo archivo** (no en memoria pura) vía R2DBC | Cero infraestructura externa para correr/demostrar el proyecto, pero sobrevive a un restart durante la charla técnica. Evolución a Postgres documentada en DESIGN.md como paso siguiente — el driver R2DBC abstrae la migración a solo cambiar connection string + dialecto. |
| Límite Equipo Activo (A1) | **6**, configurable (no hardcodeado) | Valor canónico de los juegos oficiales. Mismo patrón de config que `maxBytesToBuffer` del AS-IS. |
| Límite Baúl / PC Box (A2) | **300**, configurable (no hardcodeado) | Orden de magnitud de los juegos oficiales (múltiplos de cajas de 30). |
| Prioridad de asignación al capturar (A3) | Equipo Activo primero, Baúl si está lleno | Comportamiento esperable para quien conozca el dominio; FR-019 solo pide "automático", no dice cuál gana. |
| Sin espacio en ningún destino (A4) | **409 Conflict** con mensaje explicativo | Es un conflicto de estado del recurso (límites llenos), no un request mal formado — pesa directo en el criterio explícito de "manejo adecuado de códigos HTTP". |
| Modelo de IVs (A8) | Se guarda **solo** el IV (0-31) por stat en el ejemplar. La "base" de la especie (el "arranca desde 1" del enunciado) se consulta on-demand vía `PokeApiClient` (`PokemonStat`, ya existe), no se duplica en nuestra DB | Evita un dato redundante que puede desincronizarse de PokéAPI; el IV es lo único genuinamente nuevo por ejemplar. |
| Naturaleza — tabla completa (A7) | Se usa la tabla **oficial** de 25 naturalezas | No es ambigüedad real: las 5 "neutras" (Hardy, Docile, Serious, Bashful, Quirky) tienen el mismo stat como incrementado y decrementado — la regla "+10%/-10% siempre" del PRD se cumple sin excepción. Dato de dominio público, verificable, no una asunción de riesgo. |
| Entrenador / OT (A9) | **Entidad de dominio propia**, no solo un campo id | Alineado con FR-012/013 (límites *por entrenador*); dejar la puerta abierta a auth futura sin remodelar. |
| Ubicación de origen (A10) | **Texto libre** | Una captura puede no corresponder a ningún `Location` que PokéAPI conozca (evento, trade, etc.); acoplarlo a esa taxonomía limita casos legítimos. |
| Fecha de captura (A11) | **Generada por el sistema** al crear el registro | Evita validar fechas provistas por el cliente; el PRD no pide poder declarar una fecha distinta a la del alta. |
| Autenticación/autorización (omisión, no cubierta por ninguna ambigüedad puntual) | **Fuera de alcance**, decisión consciente y documentada | El PRD no la pide y no es el foco del challenge (dominio de negocio). Se deja una nota en DESIGN.md de cómo se agregaría (Spring Security + JWT, atado a la entidad Entrenador de A9) para cubrir "visión de evolución" sin gastar el tiempo limitado en implementarla. |
| Stack — Spring Boot / Java (Dolor P6 del AS-IS) | **Revisado 2026-09-20 → Spring Boot 3.5.16 + Java 21** (reemplaza la decisión original de mantener 2.4.3/Java 11) | Ver `## Cambios` al final de este documento para el detalle completo. |
| Bonus de evolución (A12, FR-024 a FR-028) | **No priorizado** para esta entrega | Se documenta en README/DESIGN.md como extensión identificada (diseño posible, no implementado) en vez de improvisarlo si sobra tiempo al final. |

## Plan — próximas tareas, en orden

1. ✅ **Corregir `distributionManagement`** — eliminado.
2. ✅ Aplicadas el 2026-09-20: Region (`"pokemon-region"` → `"region"`),
   cache silenciosa (`WARN` → `ERROR`, sin romper el Mono), deserialización
   silenciosa (`FAIL_ON_UNKNOWN_PROPERTIES` → `true`). `BUILD SUCCESS`,
   21/21 tests verdes. **Riesgo sin validar:** el cambio de deserialización
   no se puede probar contra los fixtures estáticos existentes — si la
   PokéAPI real agregó campos a algún recurso desde que se escribieron las
   DTOs, una llamada real (no mockeada) puede empezar a fallar donde antes
   fallaba en silencio. Falta probarlo contra la API real.
3. ✅ **CI/CD ajustado** (2026-09-20), consecuencia directa de 1 y de la
   migración de stack:
   - `maven-publish.yml` **eliminado** — publicaba a GitHub Packages como
     si esto fuera una librería consumible aparte, modelo ya descartado.
   - `coverage-report.yml` actualizado: Java `11`/`adopt` → `21`/`temurin`
     (si no, el build fallaba con el `pom.xml` ya en Java 21); y
     `actions/checkout`, `actions/setup-java`, `actions/upload-artifact`
     de `v2` → `v4` — las `v2` corren sobre Node 16, deprecado por GitHub,
     iban a fallar o marcar warning independientemente del tema Java.
4. ⬜ `sdd-baseline` (F2): spec `000-baseline.md` + characterization tests
   sobre el comportamiento heredado ya corregido (1-3).
5. ⬜ Primera spec de la feature (Equipo Activo / Baúl).

✅ **Migración de stack** (Java 21 / Spring Boot 3.5.16) — completada el
2026-09-20, ver `## Cambios`. Era parte de este plan, ya ejecutada.

El modelo de concurrencia (A5) sigue **tentativo**: si al llegar a la spec
de persistencia el costo de R2DBC es alto, se vuelve acá y se actualiza
esta tabla con la fecha y el motivo del cambio, no se decide en silencio
en medio de la implementación.

## Cambios

### 2026-09-20 — Stack: Spring Boot 2.4.3/Java 11 → Spring Boot 3.5.16/Java 21

**Decisión original** (misma fecha, antes en esta sesión): mantener
2.4.3/Java 11, asumiendo que migrar a Jakarta era un costo alto que
competía con el tiempo de la feature de negocio.

**Por qué se revisó:** esa estimación no estaba medida. Se grepeó
`javax.*` en todo `src/main` y `src/test`: **2 imports en 2 archivos**
(`javax.validation.constraints.NotNull` en `PokeApiConfigurationProperties`,
`javax.validation.Valid` en `PokeApiReactorBaseConfiguration`), cero en
tests. Sin Spring Security ni Spring Data JPA todavía (las dos fuentes
típicas de dolor en una migración 2→3), la superficie real de migración es
mínima.

**Por qué Boot 3.x y no 2.7.x** (opción intermedia considerada): `2.7.18`
es la última release de esa línea — confirmado contra Maven Central, está
en EOL igual que 2.4.3. Migrar a 2.7.x hubiera costado casi lo mismo que
migrar a 3.x (mismo bump de versión, sin siquiera el cambio de namespace)
sin resolver el problema de fondo (stack sin soporte).

**Por qué Java 21 y no 17** (el mínimo de Boot 3.0): mismo esfuerzo de bump
que 17, pero más recorrido de soporte, y deja threads virtuales
disponibles si en algún momento se activa el fallback de JPA bloqueante
que quedó anotado en la fila "Modelo de concurrencia" de este documento.

**Qué gana el proyecto:**
- R2DBC/Spring Data R2DBC considerablemente más maduro en 3.x que en la
  época de 2.4.3 (Spring Data R2DBC era prácticamente nuevo en 2021) —
  reduce directamente el riesgo que motivó dejar el modelo de concurrencia
  como "tentativo".
- Deja de estar en un stack EOL — cierra una pregunta incómoda para la
  charla técnica ("¿por qué código nuevo en 2026 sobre un stack sin
  soporte?").
- Toda la capa nueva (persistencia, dominio, REST) nace directamente en
  Jakarta — se evita migrar dos veces (el cliente heredado ahora, lo nuevo
  después).

**Qué no cambia:** el resto de las decisiones de esta tabla (límites,
modelo de IVs, formato de ubicación, etc.) no dependen del stack y siguen
igual.

**Ejecutada el 2026-09-20.** `BUILD SUCCESS`, los mismos 21 tests
preexistentes en verde, sin cambiar ninguno. Además de lo previsto (2
imports `javax→jakarta`, bump de `jacoco-maven-plugin`), el compilador
encontró dos cosas que la estimación original no había anticipado:

- `okhttp`/`mockwebserver` no tenían `<version>` explícita en `pom.xml` —
  las gestionaba implícitamente el BOM de Spring Boot 2.4.3, y el de 3.5.x
  ya no las incluye. Se fijó `5.5.0` (última estable) en ambas.
- `PropertyNamingStrategy.SNAKE_CASE` (Jackson) fue removido en la versión
  de Jackson que trae Boot 3.5.x — se usa desde Jackson 2.12 su reemplazo,
  `PropertyNamingStrategies.SNAKE_CASE` (clase separada, mismo campo).

Ninguno de los dos era visible por grep de `javax.*` — aparecieron recién
al compilar. Confirma que "medir con grep" da una cota inferior del costo,
no el costo exacto; en este caso la diferencia fue chica (dos fixes de una
línea) pero vale la pena dejarlo anotado para la próxima estimación de
este tipo.
