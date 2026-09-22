# Review adversarial — Desafío Técnico Betwarrior Java Developer (4ª pasada)

**Fecha:** 2026-09-21
**Documentos revisados:**
- Enunciado: `.claude/information/Desafío Técnico Betwarrior Java Developer.pdf`
- PRD: `docs/PRD.md` (sin cambios)
- PRODUCT.md: `docs/PRODUCT.md` (working tree — fila H-10 agregada a "Decisiones")
- Spec: `docs/specs/001-equipo-activo-y-baul.md` (working tree, sin trackear — propiedad `skaro.trainer.pokeapi-call-timeout` agregada a `## Parametrización`, Restricciones ampliada con dónde y cómo se aplica el timeout)
- DESIGN.md: `docs/DESIGN.md` (sin cambios respecto a la 3ª pasada)
- Repo: `src/test/java/**` (consultado para verificar si ya existe algún patrón de simulación de delays/timeouts en los tests — no existe ninguno)
- Reviews anteriores: `docs/reviews/adversarial-review-2026-09-21.md`, `-r2.md`, `-r3.md`

## Resumen ejecutivo

H-10 (el hueco de la 3ª pasada: "timeout configurado" sin propiedad declarada) quedó resuelto de forma completa — nueva propiedad `skaro.trainer.pokeapi-call-timeout` con default `3s`, justificación del valor, y una aclaración explícita de que se implementa como operador `.timeout(Duration)` en `skaro.trainer`, no tocando `skaro.pokeapi.client` ni su configuración (la ambigüedad que motivó el hallazgo). Al resolverlo apareció, sin embargo, una pregunta que las tres pasadas anteriores no habían tocado: los dos escenarios Gherkin que ahora ejercitan ese timeout ("PokéAPI no responde dentro del timeout durante la captura" / "... al listar el Equipo Activo") necesitan, tal como están descriptos, esperar el timeout real para pasar — y no hay en el repo ningún patrón existente (`MockWebServer` con delay simulado, `StepVerifier.withVirtualTime`, o un timeout más corto por perfil de test) que la spec pueda dar por sentado. No es un hallazgo sobre el enunciado del PDF, sino sobre la viabilidad práctica del propio método TDD del proyecto para estos dos escenarios puntuales.

| Severidad | Cantidad |
|---|---|
| 🔴 Bloqueante | 0 |
| 🟠 Alta | 0 |
| 🟡 Media | 1 |
| 🔵 Baja | 0 |

## Seguimiento de la 3ª pasada

| Hallazgo previo | Estado | Nota |
|---|---|---|
| H-10 (🟠 "timeout configurado" sin propiedad declarada ni lugar de implementación) | ✅ Resuelto | `skaro.trainer.pokeapi-call-timeout` (`Duration`, default `3s`) en `## Parametrización` (spec:200); Restricciones (spec:160-170) aclara que se aplica con `.timeout(Duration)` sobre el `Mono` de `PokeApiClient` dentro de `TrainerRosterService`, sin tocar `skaro.pokeapi`. `docs/PRODUCT.md:45` registra la decisión con el mismo detalle. |

## Hallazgos nuevos de esta pasada

### H-11 🟡 Media — Los escenarios Gherkin de timeout no declaran cómo se mantienen rápidos en la suite de tests

- **Tipo:** Hueco (testing / NFR de velocidad de la suite)
- **Requisitos afectados:** — (no es un requisito del PDF; es una tensión entre dos decisiones propias del proyecto)
- **Evidencia:**
  - Spec, `## Parametrización` (línea 200): `skaro.trainer.pokeapi-call-timeout`, default `3s`.
  - Spec, escenarios "PokéAPI no responde dentro del timeout durante la captura" (línea 354-358) y "... al listar el Equipo Activo" (línea 374-380): ambos requieren que, en el test, la respuesta simulada de PokéAPI tarde más que el timeout configurado para poder verificar el `504`.
  - `CLAUDE.md` del proyecto, sección "Comandos": *"Tests: JUnit 5 + Mockito + `reactor-test` (`StepVerifier`) + `okhttp`/`mockwebserver` ... para stubear HTTP"* — la herramienta ya existe, pero ninguna sección de la spec dice cómo se usa para estos dos casos puntuales.
  - Repo: grep de `setBodyDelay`, `SocketPolicy` y `withVirtualTime` en `src/test/java/**` sin resultados — no hay ningún test hoy que simule latencia o use tiempo virtual; no hay un patrón existente del que la spec pueda apoyarse implícitamente.
  - `docs/PRODUCT.md:45` (fila del hallazgo H-10): *"3s por ser un default conservador para una API pública externa sin hacer esperar de más a un cliente HTTP"* — el razonamiento está pensado desde la perspectiva de producción, no menciona el costo de ese mismo valor dentro de la suite de tests.
- **Por qué es un problema:** con el `MockWebServer` que ya usa el proyecto, `setBodyDelay(...)` puede simular una respuesta lenta, pero si el test espera efectivamente esos 3 segundos (u otro valor igual al default de producción) antes de que el `.timeout()` dispare, cada uno de estos 2 escenarios agrega ese tiempo real a la suite — no es catastrófico para 2 escenarios puntuales, pero sí es exactamente el tipo de decisión de testing que, si no se declara, cada quien la resuelve distinto al implementar (uno espera de verdad, otro hardcodea un timeout más corto solo para tests, un tercero usa tiempo virtual) — inconsistencia que compite con el objetivo de mantenibilidad (O2-O4 del PRD) que esta misma spec dice servir. Es menor en impacto que los hallazgos de las pasadas anteriores porque no afecta corrección funcional ni un criterio del PDF, pero sigue siendo una decisión de diseño de testing sin resolver antes de escribir el primer test rojo de estos dos escenarios.
- **Recomendación:** declarar explícitamente una de estas dos opciones en Restricciones o en `## Parametrización`: (a) el timeout de test se overridea a un valor bajo (p. ej. `50ms`) vía un perfil/properties de test, con `MockWebServer.setBodyDelay(...)` simulando la demora — la opción más simple dado que ya se usa `MockWebServer` en el proyecto; o (b) el operador se envuelve de forma que permita testearse con `StepVerifier.withVirtualTime()` sin depender de un `MockWebServer` con delay real. No hace falta resolverlo en detalle en la spec — alcanza con una frase que fije el criterio, para no dejarlo a interpretación libre en medio del ciclo rojo/verde/refactor de `sdd-implement`.
- **Confianza:** media (es una preocupación de viabilidad práctica, no una que un evaluador del challenge vaya a notar leyendo la spec — pero si vaya a notarla quien la implemente, hoy mismo, al llegar a este escenario).

## Lo que está bien (nuevo en esta pasada)

### ✔ H-10 se resolvió sin dejar la ambigüedad "no se toca skaro.pokeapi" sin cerrar
- **Evidencia:** Restricciones (spec:163-166) dice explícitamente *"vive en `skaro.trainer`, no en `skaro.pokeapi.client` ni en su configuración, que la spec ya declara 'no se toca'"* — responde de forma directa a la pregunta que la 3ª pasada dejó abierta (pregunta 15 de esa pasada) en vez de dejarla para la charla técnica.
- **Por qué suma:** es la primera corrección de las cuatro pasadas que cierra explícitamente tanto el "qué" (la propiedad) como el "dónde" (la capa de implementación) en el mismo movimiento — las pasadas anteriores a veces resolvían una dimensión del hallazgo y dejaban la otra para la siguiente ronda (H-02 → H-08, H-09 → H-10).

## Preguntas probables en la defensa (actualización)

| # | Pregunta | ¿Los documentos la responden ahora? | Dónde / qué falta preparar |
|---|---|---|---|
| 14 (de la 3ª pasada) | ¿Cuánto dura el timeout que dispara el `504`, y dónde se configura? | Sí | spec:160-170, spec:200 |
| 15 (de la 3ª pasada) | ¿Cómo evitás tocar `skaro.pokeapi.client` si agregás un timeout? | Sí | spec:163-166 |
| 16 (nueva) | ¿Los tests de timeout esperan los 3 segundos reales, o hay un mecanismo para que la suite siga siendo rápida? | No | Preparar respuesta o cerrar el hueco — ver H-11 |

## Nota metodológica

No se repiten las secciones de requisitos del PDF ni la matriz de trazabilidad completa: no cambiaron desde la 1ª pasada, y H-11 no corresponde a ningún requisito del PDF (es una cuestión de viabilidad de testing interna al proyecto, no de fidelidad al enunciado). Si esta pasada se repite, conviene verificar además si `sdd-implement` ya arrancó — en ese caso el hueco de H-11 se cierra directamente en el código/test, no hace falta otra vuelta de spec.
