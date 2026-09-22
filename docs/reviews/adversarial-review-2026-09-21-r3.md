# Review adversarial — Desafío Técnico Betwarrior Java Developer (3ª pasada)

**Fecha:** 2026-09-21
**Documentos revisados:**
- Enunciado: `.claude/information/Desafío Técnico Betwarrior Java Developer.pdf`
- PRD: `docs/PRD.md` (sin cambios)
- PRODUCT.md: `docs/PRODUCT.md` (working tree — fila H-09 agregada a "Decisiones", párrafo de "Plan" reescrito para H-07)
- Spec: `docs/specs/001-equipo-activo-y-baul.md` (working tree, sin trackear — tabla de errores con `502`/`504` separados, 2 escenarios Gherkin nuevos para fallas de PokéAPI al listar el Equipo Activo)
- DESIGN.md: `docs/DESIGN.md` (sin cambios respecto a la 2ª pasada)
- Repo: `src/main/java/skaro/pokeapi/PokeApiReactorBaseConfiguration.java` (consultado para verificar si existe timeout configurado hoy — no existe)
- Reviews anteriores: `docs/reviews/adversarial-review-2026-09-21.md`, `docs/reviews/adversarial-review-2026-09-21-r2.md`

## Resumen ejecutivo

Los tres hallazgos abiertos de la 2ª pasada (H-07, H-08, H-09) quedaron resueltos con evidencia verificable: el párrafo contradictorio de `PRODUCT.md` se reescribió, se agregaron los dos escenarios Gherkin simétricos para fallas de PokéAPI al listar el Equipo Activo, y `502`/`504` quedaron separados de forma consistente en las tres tablas relevantes (spec, Restricciones, `PRODUCT.md`). Al resolver H-09 apareció, sin embargo, un hueco nuevo: el propio texto de la spec ahora habla de un "timeout configurado" para disparar el `504`, pero esa configuración no existe en ningún lado — ni como propiedad en `## Parametrización`, ni en el cliente PokéAPI existente (verificado contra el código: `PokeApiReactorBaseConfiguration` no tiene ningún timeout hoy). Es un hueco de la misma familia que los que motivaron H-01/H-02 en la 1ª pasada: la prosa promete algo ("configurado") que ningún otro lugar del documento respalda.

| Severidad | Cantidad |
|---|---|
| 🔴 Bloqueante | 0 |
| 🟠 Alta | 1 |
| 🟡 Media | 0 |
| 🔵 Baja | 0 |

## Seguimiento de la 2ª pasada

| Hallazgo previo | Estado | Nota |
|---|---|---|
| H-07 (🟡 `PRODUCT.md` decía "sigue tentativo" pese a que ya estaba revisado) | ✅ Resuelto | Párrafo reescrito (`docs/PRODUCT.md:74-77`), cita el propio hallazgo que motivó el cambio. |
| H-08 (🟠 P8 solo traduce errores en el camino de captura, no en FR-021) | ✅ Resuelto | 2 escenarios nuevos: "PokéAPI responde 5xx o rechaza la conexión al listar el Equipo Activo" y "PokéAPI no responde dentro del timeout al listar el Equipo Activo" (spec:356-368), simétricos a los de captura. Tabla de errores (spec:108-109) ya dice "durante la captura, o al armar la vista compuesta de FR-021" en ambas filas. |
| H-09 (🔵 `502` usado también para timeout, sin `504`) | ✅ Resuelto (con hueco nuevo derivado) | `504 Gateway Timeout` agregado como fila propia (spec:109), con escenarios dedicados tanto en captura (spec:343-347) como en listado (spec:363-368). Ver **H-10** — la separación en sí está bien hecha, pero introdujo un concepto nuevo ("timeout configurado") sin declarar dónde vive esa configuración. |

## Hallazgos nuevos de esta pasada

### H-10 🟠 Alta — El "timeout configurado" que dispara `504` no está declarado en ningún lado, ni existe hoy en el cliente PokéAPI

- **Tipo:** Hueco / contradicción prosa-vs-parametrización
- **Requisitos afectados:** R-30 (implícito); regla del proyecto "Sin hardcode, sin magic strings" (`~/.claude/CLAUDE.md`, `CLAUDE.md` del proyecto)
- **Evidencia:**
  - Spec, tabla de errores (línea 109): *"`504 Gateway Timeout` | PokéAPI no responde dentro del **timeout configurado** al resolver especie o habilidad durante la captura, o al armar la vista compuesta de FR-021"*.
  - Spec, Restricciones (línea 152-153): *"un timeout se traduce específicamente a `504 Gateway Timeout`"* — no dice cuánto dura ni de dónde sale el valor.
  - Spec, escenarios (líneas 343-347 y 363-368): ambos dicen *"PokéAPI no responde dentro del timeout configurado"*, sin más detalle.
  - Spec, `## Parametrización` § "Configurables" (líneas 185-189): la tabla lista `team-limit`, `box-limit` y `default-trainer-id` — **no hay ninguna propiedad de timeout**, pese a que esta sección se presenta como el "repaso explícito de todos los valores, mínimos, máximos y cotas que aparecen en esta spec" (línea 175-177).
  - Repo: `src/main/java/skaro/pokeapi/PokeApiReactorBaseConfiguration.java` — el `WebClient`/`HttpClient` de PokéAPI se construye sin ningún `responseTimeout`, `ChannelOption.CONNECT_TIMEOUT_MILLIS` ni operador `.timeout(Duration)`. Confirmado también por grep sin resultados de "timeout" en todo `src/main/java/skaro/pokeapi/`. Hoy, una llamada a PokéAPI que no responde **no tiene ningún límite de tiempo** — se colgaría indefinidamente, no en `504`.
  - Spec, "Alcance del cambio" (línea 59-61): *"No se toca: `skaro.pokeapi.client`, ... ni la configuración existente"* — si el timeout se agregara ahí, contradice esta restricción; si se agrega como operador `.timeout(Duration)` en `skaro.trainer` (la opción que no toca `skaro.pokeapi`), la spec tampoco lo dice.
- **Por qué es un problema:** es el mismo patrón de riesgo que ya motivó H-01 y H-02 en la 1ª pasada — la palabra "configurado" da a entender que existe una propiedad, pero no hay ninguna, y sin una duración concreta no hay forma de escribir un test determinístico para el escenario Gherkin correspondiente ("¿espero 5 segundos en el test? ¿30?"). Peor: como el cliente PokéAPI hoy no tiene ningún timeout, el comportamiento por defecto real (sin esta spec) es "cuelga para siempre" — muy distinto de "responde 504 rápido", y la spec no aclara si el timeout se agrega tocando la configuración existente (lo cual violaría "no se toca") o mediante un operador Reactor en la capa nueva (`skaro.trainer`), que sería la opción consistente pero no está declarada. También es una violación directa y sin justificación de la regla explícita del proyecto de no dejar valores de configuración como magic numbers — a diferencia de las constantes de D2 (`IV_MIN`, etc.), acá no hay ningún argumento de por qué este valor no debería ser configurable; simplemente no está.
- **Recomendación:** agregar una propiedad al bean `TrainerConfigurationProperties` (p. ej. `skaro.trainer.pokeapi-call-timeout`, default sugerido `3s`–`5s`) en la tabla de `## Parametrización`, e implementarla como `.timeout(Duration)` sobre el `Mono` que devuelve `PokeApiClient` en el punto donde `TrainerRosterService` lo llama — así queda declarado explícitamente que no se toca `skaro.pokeapi` (el timeout vive en la capa nueva, envolviendo la llamada, no en el `WebClient` compartido). Aclarar esto en Restricciones junto a la fila de "Traducción de errores de PokéAPI".
- **Confianza:** alta.

## Lo que está bien (nuevo en esta pasada)

### ✔ La separación 502/504 se aplicó de forma completa y consistente en las tres tablas afectadas
- **Evidencia:** spec (tabla de errores línea 108-109, Restricciones línea 146-160, escenarios 337-368) y `docs/PRODUCT.md` (línea 44) usan el mismo lenguaje y las mismas dos causas (`5xx`/conexión rechazada vs. timeout) sin ninguna de las tres quedar desalineada.
- **Por qué suma:** a diferencia de H-04/H-07 (donde una corrección dejó un rincón del documento sin actualizar), acá la corrección de H-09 sí se propagó a todos los lugares relevantes — muestra que el patrón de "buscar todas las menciones" se aprendió de la pasada anterior.

## Preguntas probables en la defensa (actualización)

| # | Pregunta | ¿Los documentos la responden ahora? | Dónde / qué falta preparar |
|---|---|---|---|
| 11 (de la 2ª pasada) | ¿Resolvés P8 en su totalidad? ¿Y el listado del Equipo Activo? | Sí | spec:356-368 |
| 12 (de la 2ª pasada) | ¿Por qué `PRODUCT.md` decía "sigue tentativo"? | Sí (corregido) | — |
| 14 (nueva) | ¿Cuánto dura el timeout que dispara el `504`, y dónde se configura? | No | Preparar respuesta o cerrar el hueco — ver H-10 |
| 15 (nueva) | Si el cliente PokéAPI hoy no tiene timeout, ¿cómo es posible que la spec diga "no se toca `skaro.pokeapi.client`" y a la vez agregue un timeout? | No | Aclarar la capa de implementación — ver H-10 |

## Nota metodológica

Como en la 2ª pasada, no se repiten las secciones de requisitos del PDF ni la matriz de trazabilidad completa porque no cambiaron. El único requisito cuyo estado se ve afectado por el hallazgo de esta pasada es R-30 (implícito, manejo de fallas de PokéAPI), que pasa de "✅ cubierto" a "⚠️ parcial" hasta que se declare la configuración del timeout.
