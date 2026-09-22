# Review adversarial — Desafío Técnico Betwarrior Java Developer (5ª pasada)

**Fecha:** 2026-09-21
**Documentos revisados:**
- Enunciado: `.claude/information/Desafío Técnico Betwarrior Java Developer.pdf`
- PRD: `docs/PRD.md` (sin cambios)
- PRODUCT.md: `docs/PRODUCT.md` (sin cambios respecto a la 4ª pasada — H-11 se resolvió solo en la spec, consistente con el patrón ya visto en H-05/H-06: hallazgos de completitud interna de la spec no generan fila en la tabla de Decisiones)
- Spec: `docs/specs/001-equipo-activo-y-baul.md` (working tree, sin trackear — bullet "Timeout en tests" agregado a Restricciones, fila nueva en `## Parametrización`)
- DESIGN.md: `docs/DESIGN.md` (sin cambios)
- Reviews anteriores: `docs/reviews/adversarial-review-2026-09-21.md`, `-r2.md`, `-r3.md`, `-r4.md`

## Resumen ejecutivo

H-11 (la 4ª pasada: escenarios de timeout sin definir cómo se mantienen rápidos en la suite) se resolvió, pero la forma elegida introduce un riesgo nuevo: el override de `50ms` se aplica a nivel de **perfil de test completo**, no solo a los 2 escenarios que necesitan simular una demora. Eso significa que **todo** el resto de los escenarios de esta spec que también llaman a `PokeApiClient` (captura con validación de Habilidad, resolución de especie, la vista compuesta de FR-021, etc. — la mayoría de los ~24 escenarios Gherkin de la spec) queda con una ventana de 50ms para que `MockWebServer` responda, en vez de los 3s de producción. Es un valor agresivamente bajo para aplicarlo de forma global bajo cualquier variación de carga de CI (JIT warmup, GC, ejecución en paralelo) — el riesgo no es que el diseño esté mal pensado, sino que el radio de impacto de la corrección es mucho mayor que los 2 escenarios que la motivaron.

| Severidad | Cantidad |
|---|---|
| 🔴 Bloqueante | 0 |
| 🟠 Alta | 1 |
| 🟡 Media | 0 |
| 🔵 Baja | 0 |

## Seguimiento de la 4ª pasada

| Hallazgo previo | Estado | Nota |
|---|---|---|
| H-11 (🟡 escenarios de timeout sin mecanismo declarado para mantener la suite rápida) | ✅ Resuelto (con riesgo nuevo derivado) | Restricciones (spec:171-178) y `## Parametrización` (spec:209) declaran `50ms` como override de test con `MockWebServer.setBodyDelay(...)`. La declaración en sí cierra la ambigüedad de la 4ª pasada — el problema es el alcance del override, no que falte. Ver **H-12**. |

## Hallazgos nuevos de esta pasada

### H-12 🟠 Alta — El override de test (`50ms`) se aplica a todo el perfil de test, no solo a los 2 escenarios de timeout — riesgo de flakiness en el resto de la suite

- **Tipo:** Hueco / riesgo no funcional (fiabilidad de la suite de tests)
- **Requisitos afectados:** — (no es un requisito del PDF; es una tensión entre la corrección de H-11 y la estabilidad general del método TDD del proyecto)
- **Evidencia:**
  - Spec, Restricciones (línea 171-172): *"el valor de `skaro.trainer.pokeapi-call-timeout` se overridea a un valor bajo (`50ms`) **en el perfil/properties de test**"* — sin acotar el override a los tests que efectivamente necesitan simular una demora.
  - Spec, `## Parametrización` (línea 209): la fila dice *"(perfil `test`)"*, reforzando que es un valor de todo el perfil, no de una clase de test puntual.
  - Spec, tabla de endpoints (línea 79-83) y Restricciones (línea 143-145, 146-170): `PokeApiClient` se llama para resolver especie/habilidad en **toda** captura (BR-006 es obligatorio, no opcional) y para armar la vista compuesta de **todo** listado de Equipo Activo (FR-021) — es decir, la gran mayoría de los ~24 escenarios de "Criterios de aceptación" de esta spec dependen de que `MockWebServer` responda dentro de esos `50ms` para no disparar un `504` espurio, no solo los 2 escenarios que prueban el timeout a propósito.
  - Repo: `MockWebServer` es un servidor HTTP real sobre sockets locales (no un mock puramente en memoria) — su latencia bajo ejecución en paralelo, warmup de JIT o un runner de CI cargado no está garantizada por debajo de `50ms` de forma consistente; no hay ningún test hoy en el repo que dependa de una ventana de tiempo tan ajustada (confirmado en la pasada anterior: no hay precedente de timeouts cortos en la suite existente).
- **Por qué es un problema:** antes de esta corrección, el timeout de producción (`3s`) aplicado sin overridear en los tests daba margen de sobra para cualquier variación de latencia local — ningún escenario corría riesgo de fallar por lentitud incidental. Después de la corrección, cualquier escenario que llama a PokéAPI (la mayoría de la spec) pasa a depender de una ventana de `50ms`, y un solo `504` espurio en, por ejemplo, "Captura exitosa con espacio en el Equipo Activo" haría fallar un test que no tiene nada que ver con timeouts — un clásico de tests intermitentes (*flaky tests*) que, con el tiempo, entrena al equipo a re-ejecutar en vez de investigar, exactamente lo opuesto de la disciplina rojo/verde/refactor que el método SDD de este proyecto pide.
- **Recomendación:** acotar el override a los 2 escenarios que lo necesitan en vez de a todo el perfil de test — por ejemplo, `@TestPropertySource(properties = "skaro.trainer.pokeapi-call-timeout=50ms")` (o el mecanismo equivalente de Spring Test) solo en la clase de test que cubre esos 2 escenarios de timeout, dejando el resto de la suite con el default de producción (`3s`) o un valor intermedio holgado (p. ej. `1s`) si se prefiere no depender del default real. Ajustar el bullet de Restricciones y la fila de `## Parametrización` para reflejar que el override es **por clase de test**, no por perfil completo.
- **Confianza:** alta.

## Lo que está bien (nuevo en esta pasada)

### ✔ El mecanismo elegido para simular la demora (`MockWebServer.setBodyDelay`) reutiliza la herramienta ya validada del proyecto
- **Evidencia:** Restricciones (línea 174-175) cita `WebClientEntityFactoryTest` como precedente del mismo mecanismo de stub HTTP.
- **Por qué suma:** evita introducir una herramienta nueva (p. ej. WireMock, o una implementación propia de servidor lento) solo para 2 escenarios — coherente con la fortaleza ya señalada en pasadas anteriores de reusar patrones existentes del proyecto en vez de inventar uno nuevo por caso.

## Preguntas probables en la defensa (actualización)

| # | Pregunta | ¿Los documentos la responden ahora? | Dónde / qué falta preparar |
|---|---|---|---|
| 16 (de la 4ª pasada) | ¿Los tests de timeout esperan los 3 segundos reales? | Sí (ya no aplica) | spec:171-178 |
| 17 (nueva) | Si overrideaste el timeout a `50ms` para todo el perfil de test, ¿no corrés el riesgo de que un test lento por CI falle con un `504` que no tiene nada que ver con lo que estás probando? | No | Preparar respuesta o acotar el override — ver H-12 |

## Nota metodológica

No se repiten las secciones de requisitos del PDF ni la matriz de trazabilidad: no cambiaron. H-12, igual que H-11, es un hallazgo sobre viabilidad práctica de la implementación de tests, no sobre fidelidad al enunciado — no afecta ningún ID de requisito de la matriz. Si `sdd-implement` ya arrancó para estos escenarios, el hallazgo se puede cerrar directo en el código de test (acotando el `@TestPropertySource`) sin otra vuelta de spec.
