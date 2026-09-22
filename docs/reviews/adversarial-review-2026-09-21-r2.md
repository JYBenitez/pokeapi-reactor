# Review adversarial — Desafío Técnico Betwarrior Java Developer (2ª pasada)

**Fecha:** 2026-09-21
**Documentos revisados:**
- Enunciado: `.claude/information/Desafío Técnico Betwarrior Java Developer.pdf`
- PRD: `docs/PRD.md` (sin cambios respecto a la 1ª pasada)
- PRODUCT.md: `docs/PRODUCT.md` (working tree, modificado — filas H-01/H-02/H-03 agregadas a "Decisiones" y entrada nueva en "## Cambios")
- Spec: `docs/specs/001-equipo-activo-y-baul.md` (working tree, sin trackear — spec aún no implementada, ampliada con "## Parametrización", "## Valores por defecto del payload de captura" y 5 escenarios Gherkin nuevos)
- DESIGN.md: `docs/DESIGN.md` (working tree, sin trackear — D1 ampliado con nota de cross-referencia; D2 y D3 nuevos)
- Repo: sin cambios de código respecto a la 1ª pasada (la spec sigue sin implementarse)
- Review anterior: `docs/reviews/adversarial-review-2026-09-21.md`

## Resumen ejecutivo

De los 6 hallazgos de la 1ª pasada, 5 quedaron resueltos de forma sólida y verificable (H-01, H-03, H-04, H-05, H-06) — cada uno con evidencia concreta en el documento correspondiente, no solo una mención de que "ya se resolvió". El sexto (H-02) se resolvió a medias: el camino de escritura (captura) que motivó el hallazgo ya tiene su escenario Gherkin y su código de error, pero el camino de lectura (FR-021, vista compuesta del Equipo Activo) sigue sin escenario, pese a que tanto la spec como `PRODUCT.md` ahora afirman textualmente que el problema se resuelve "en su totalidad". Además, arreglar H-04 en la spec dejó un párrafo sin actualizar en `PRODUCT.md` que todavía dice lo contrario de la decisión ya tomada — la misma clase de problema que H-04 reapareció en otro lugar del mismo documento. Ningún hallazgo nuevo es bloqueante; el estado general mejoró de forma clara respecto a la 1ª pasada.

| Severidad | Cantidad |
|---|---|
| 🔴 Bloqueante | 0 |
| 🟠 Alta | 1 |
| 🟡 Media | 1 |
| 🔵 Baja | 1 |

## Seguimiento de la 1ª pasada

| Hallazgo previo | Estado | Nota |
|---|---|---|
| H-01 (🟠 tabla de errores sin escenarios para naturaleza inválida / especie inexistente / 400) | ✅ Resuelto | 3 escenarios Gherkin agregados: "Captura con una naturaleza inválida" (spec:305-310), "Captura con una especie inexistente en PokéAPI" (spec:312-316), "Captura sin el campo obligatorio 'especie'" (spec:318-321). Defaults documentados en tabla nueva y con su propio escenario ("payload mínimo aplica los valores por defecto", spec:323-329). |
| H-02 (🟠 P8 solo traduce especie inexistente, sin contrato para timeout/5xx de PokéAPI) | ⚠️ Parcial | Fila `502 Bad Gateway` agregada a la tabla de errores (spec:108) y escenario "PokéAPI no responde durante la captura" (spec:331-335) — cubre el camino de captura. El camino de lectura (FR-021) sigue sin escenario. Ver **H-08** (hallazgo nuevo de esta pasada). |
| H-03 (🟡 check-then-act sin exclusión mutua en límites 6/300) | ✅ Resuelto | Lock pesimista sobre la fila de `Trainer` (`PESSIMISTIC_WRITE`), documentado con alternativas descartadas en `docs/DESIGN.md` D3, y escenario Gherkin nuevo "Dos capturas concurrentes no superan el límite del Equipo Activo" (spec:266-272). |
| H-04 (🟡 contradicción interna R2DBC/JPA en la spec) | ✅ Resuelto en la spec / ⚠️ reaparece en otro documento | "Alcance del cambio" ya no presenta R2DBC como opción activa (spec:20-23). Pero ver **H-07** (hallazgo nuevo): la misma clase de contradicción persiste en `docs/PRODUCT.md`, sección "Plan". |
| H-05 (🔵 falta escenario simétrico de traslado Equipo→Baúl sin espacio) | ✅ Resuelto | Escenario "Mover del Equipo Activo al Baúl sin espacio disponible" agregado (spec:385-389). |
| H-06 (🔵 DESIGN.md D1 no menciona Shiny/Held Item/datos de origen) | ✅ Resuelto | D1 ahora aclara explícitamente que la enumeración es ilustrativa y remite a la tabla completa de la spec (`docs/DESIGN.md`:38-44), que sí lista los 11 campos con sus defaults. |

## Hallazgos nuevos de esta pasada

### H-07 🟡 Media — Contradicción residual en PRODUCT.md: la sección "Plan" no se actualizó tras revisar el modelo de concurrencia (A5)

- **Tipo:** Contradicción
- **Requisitos afectados:** — (calidad interna del documento, no un requisito del PDF)
- **Evidencia:**
  - `docs/PRODUCT.md:28` (tabla "Decisiones"): *"Modelo de concurrencia (A5) | **Revisado 2026-09-21 → WebFlux + JPA aislado en `Schedulers.boundedElastic()`** (reemplaza el tentativo original de R2DBC end-to-end)"*.
  - `docs/PRODUCT.md:80-103` (sección "## Cambios"): registro completo y fechado del mismo cambio, con motivo y alternativa descartada.
  - `docs/PRODUCT.md:73-76` (sección "## Plan", después de la lista de tareas): *"El modelo de concurrencia (A5) sigue **tentativo**: si al llegar a la spec de persistencia el costo de R2DBC es alto, se vuelve acá y se actualiza esta tabla con la fecha y el motivo del cambio, no se decide en silencio en medio de la implementación."*
- **Por qué es un problema:** es exactamente el mismo tipo de hallazgo que H-04 de la 1ª pasada (una sección de un documento no se actualizó al mismo ritmo que el resto), solo que ahora ocurre en `PRODUCT.md` en vez de en la spec — y es más marcado, porque no es una omisión de lenguaje sino una afirmación activa ("sigue tentativo") que contradice directamente la fila de arriba y el registro de "Cambios" del mismo documento. Un lector que entre a `PRODUCT.md` por la sección "Plan" en vez de por la tabla de "Decisiones" se queda con la versión vieja de la decisión.
- **Recomendación:** eliminar o reescribir el párrafo de la línea 73-76 (el "Plan" ya está desactualizado en general — los ítems 1-3 están tildados como hechos y el modelo de concurrencia ya no es tentativo). Alcanza con una frase corta: *"Modelo de concurrencia (A5): revisado y cerrado, ver tabla de Decisiones y `## Cambios`."*, o borrar el párrafo directamente si ya no aporta nada que la tabla no diga.
- **Confianza:** alta.

### H-08 🟠 Alta — La resolución de H-02 cubre captura pero no la vista compuesta del Equipo Activo (FR-021), pese a la afirmación de que P8 se resuelve "en su totalidad"

- **Tipo:** Hueco
- **Requisitos afectados:** R-30 (implícito)
- **Evidencia:**
  - `docs/specs/001-equipo-activo-y-baul.md:145-154` (Restricciones): *"**Traducción de errores de PokéAPI** (resuelve P8 del AS-IS **en su totalidad**, no solo el caso puntual de especie inexistente): en el único punto donde `TrainerRosterService` llama a `PokeApiClient` para resolver especie/habilidad (captura) **o para armar la vista compuesta (FR-021)**, un `404` ... y un timeout, `5xx` o conexión rechazada se traduce a `502 Bad Gateway`."*
  - `docs/PRODUCT.md` (fila "Traducción de errores de PokéAPI", hallazgo H-02): *"Cierra el dolor P8 del AS-IS **en su totalidad**"*.
  - Sección "Criterios de aceptación" de la spec: el único escenario para fallas de PokéAPI es "PokéAPI no responde durante la captura" (spec:331-335), que cubre exclusivamente `POST /trainers/{trainerId}/pokemon`. No hay escenario para `GET /trainers/{trainerId}/pokemon?location=team` (FR-021) cuando PokéAPI no responde al enriquecer cada ejemplar con los atributos estáticos de la especie.
- **Por qué es un problema:** es el mismo razonamiento que ya justificó H-01 y H-02 como Alta en la 1ª pasada — bajo el método TDD del proyecto, un caso sin escenario Gherkin corre riesgo real de no implementarse. Acá el riesgo es mayor que un hueco silencioso porque el documento afirma explícitamente que el caso está cubierto "en su totalidad", cuando en los hechos solo un camino de los dos tiene test. Si `GET .../pokemon?location=team` se implementa sin envolver la llamada a PokéAPI en el mismo manejo de errores que la captura, un `500` crudo de WebClient se filtraría justo en el endpoint de lectura que compone datos de dos fuentes — el escenario más probable de fallar en una demo en vivo si PokéAPI está lenta o caída, y exactamente el criterio explícito de "manejo adecuado de errores" (NFR-004/R-28) que el PDF pide evaluar.
- **Recomendación:** agregar un escenario Gherkin simétrico, por ejemplo "PokéAPI no responde al listar el Equipo Activo" → `502 Bad Gateway`. Alternativa más económica: si de verdad se decide compartir el mismo punto de traducción de errores entre captura y FR-021 (como ya dice Restricciones), un solo escenario que verifique ese punto de traducción de forma más genérica alcanzaría — pero hoy no existe ninguno para el camino de lectura, y la redacción de Restricciones sigue prometiendo cobertura total que el criterio de aceptación no respalda.
- **Confianza:** alta.

### H-09 🔵 Baja — `502 Bad Gateway` se usa para timeout y para 5xx/conexión rechazada sin distinguir `504 Gateway Timeout`

- **Tipo:** Ambigüedad / verificabilidad
- **Requisitos afectados:** R-28, R-30 (implícito)
- **Evidencia:**
  - Spec, tabla de errores (línea 108): *"`502 Bad Gateway` | PokéAPI no responde (timeout, `5xx` o conexión rechazada)..."* — un solo código para tres causas distintas.
  - Spec, escenario "PokéAPI no responde durante la captura" (línea 331-335): mismo tratamiento, sin distinguir timeout de 5xx.
- **Por qué es un problema:** no contradice nada, pero es una precisión que la sección "Lo que está bien" de la 1ª pasada ya destacó como fortaleza ("modelo de errores HTTP semánticamente diferenciado") — usar `504 Gateway Timeout` específicamente para el caso de timeout sería más fiel a esa misma diferenciación semántica que el resto de la tabla ya aplica (400 vs 422, por ejemplo). Es una pregunta de defensa fácil de anticipar ("¿por qué no usaste 504 para el timeout?").
- **Recomendación:** si el tiempo lo permite, separar la fila en dos (`502` para 5xx/conexión rechazada, `504` para timeout); si no, dejarlo como está pero anotar la simplificación como decisión consciente en Restricciones ("se unifica bajo 502 por simplicidad, ver H-09").
- **Confianza:** media (es una preferencia de precisión HTTP, no un error).

## Lo que está bien (nuevo en esta pasada)

### ✔ Las correcciones citan su propio hallazgo de origen
- **Evidencia:** cada fila nueva de `docs/PRODUCT.md` ("Payload de captura...", "Traducción de errores de PokéAPI...", "Concurrencia en validación de límites...") referencia explícitamente el hallazgo de `docs/reviews/adversarial-review-2026-09-21.md` que la motivó, y la spec hace lo mismo en sus secciones nuevas ("revisado tras hallazgo H-01/H-02/H-03").
- **Por qué suma:** mantiene la misma trazabilidad de tres niveles que la 1ª pasada ya destacó como punto fuerte (AS-IS → PRD → spec), extendida ahora a review → decisión → spec. Facilita que un evaluador reconstruya el razonamiento sin tener que preguntarlo en la charla técnica.

### ✔ D3 justifica la elección de lock pesimista frente a dos alternativas concretas, no solo frente a "no hacer nada"
- **Evidencia:** `docs/DESIGN.md` D3 descarta explícitamente lock optimista (con el motivo puntual: alta tasa de reintento esperable con un solo `Trainer`) y constraint/trigger de base (motivo: costo de mantenimiento no justificado dado el plazo), y anticipa el caso de multi-entrenador futuro.
- **Por qué suma:** responde directamente al criterio "Alternativas posibles a la solución propuesta" (PDF, p.3) con dos alternativas reales evaluadas, no una — mismo patrón que ya funcionó bien en D1 (R2DBC vs. JPA) en la 1ª pasada.

## Preguntas probables en la defensa (actualización)

| # | Pregunta | ¿Los documentos la responden ahora? | Dónde / qué falta preparar |
|---|---|---|---|
| 3 (de la 1ª pasada) | ¿Qué pasa si PokéAPI está caída durante la demo? | Parcial | Captura: sí (`502`, spec:331-335). Listado del Equipo Activo (FR-021): no — ver H-08 |
| 4 (de la 1ª pasada) | ¿Cómo evitás que dos capturas simultáneas excedan el límite del Equipo Activo? | Sí | `docs/DESIGN.md` D3, spec:266-272 |
| 8 (de la 1ª pasada) | ¿Por qué la tabla de errores menciona "naturaleza inválida" y "especie inexistente" pero no hay tests para esos casos? | Sí (ya no aplica, hay tests) | spec:305-321 |
| 11 (nueva) | Decís que "resolvés P8 en su totalidad", pero el listado del Equipo Activo no tiene test de falla de PokéAPI — ¿por qué? | No | Preparar respuesta o cerrar el hueco — ver H-08 |
| 12 (nueva) | En `PRODUCT.md` hay un párrafo que dice que el modelo de concurrencia "sigue tentativo" justo debajo de la tabla que dice que ya se revisó — ¿cuál es el estado real? | No (es un error del documento) | Corregir — ver H-07 |
| 13 (nueva) | ¿Por qué usás `502` tanto para timeout como para errores 5xx, en vez de `504` para el timeout? | Parcial | Respuesta preparable en el momento — ver H-09, no bloquea la defensa |

## Nota metodológica

Esta pasada no repite las secciones "Requisitos extraídos del enunciado" ni la matriz de trazabilidad completa de la 1ª pasada porque el PDF y el PRD no cambiaron desde entonces — solo `PRODUCT.md`, la spec y `DESIGN.md` se corrigieron. Las filas de la matriz de trazabilidad afectadas por los hallazgos de esta pasada son: R-19/R-28 (parcialmente, por H-08) y ninguna otra cambia de estado. El resto de la matriz de la 1ª pasada sigue vigente sin modificaciones.
