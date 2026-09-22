# Review adversarial — Desafío Técnico Betwarrior Java Developer (6ª pasada)

**Fecha:** 2026-09-21
**Documentos revisados:**
- Enunciado: `.claude/information/Desafío Técnico Betwarrior Java Developer.pdf`
- PRD: `docs/PRD.md` (sin cambios)
- PRODUCT.md: `docs/PRODUCT.md` (sin cambios respecto a la 4ª/5ª pasada)
- Spec: `docs/specs/001-equipo-activo-y-baul.md` (working tree, sin trackear — bullet "Timeout en tests" y fila de `## Parametrización` acotados a `@TestPropertySource` por clase, ya no por perfil completo)
- DESIGN.md: `docs/DESIGN.md` (sin cambios)
- Reviews anteriores: `docs/reviews/adversarial-review-2026-09-21.md`, `-r2.md` a `-r5.md`

## Resumen ejecutivo

H-12 (override de timeout de test aplicado a todo el perfil, con riesgo de flakiness) quedó resuelto: el override ahora se declara explícitamente acotado a `@TestPropertySource` sobre la clase de test correspondiente, y tanto Restricciones como `## Parametrización` explican por qué (evitar que el resto de la suite dependa de una ventana de 50ms). No encontré ningún hallazgo nuevo de severidad Media o superior en esta pasada — solo una imprecisión menor de redacción, de la misma clase que H-05/H-06 en su momento pero de impacto casi nulo. A esta altura (6 pasadas, 12 hallazgos), los documentos de nivel PDF↔PRD↔spec están sólidos; lo que queda por encontrar es cada vez más específico de implementación (no de fidelidad al enunciado), que es exactamente el terreno que las fases F5/F6 (`sdd-implement`) y F7 (`sdd-verify`) del método del proyecto están diseñadas para cubrir mejor que otra vuelta de este review.

| Severidad | Cantidad |
|---|---|
| 🔴 Bloqueante | 0 |
| 🟠 Alta | 0 |
| 🟡 Media | 0 |
| 🔵 Baja | 1 |

## Seguimiento de la 5ª pasada

| Hallazgo previo | Estado | Nota |
|---|---|---|
| H-12 (🟠 override de `50ms` a nivel de perfil completo, riesgo de `504` espurios en el resto de la suite) | ✅ Resuelto | Restricciones (spec:171-188) y `## Parametrización` (spec:219) acotan el override a `@TestPropertySource` sobre la clase de test puntual; el resto de la suite queda con el `3s` de producción. Razonamiento explícito de por qué un override global sería riesgoso (spec:180-185). |

## Hallazgos nuevos de esta pasada

### H-13 🔵 Baja — "La clase de test" (singular) asume que los 2 escenarios de timeout viven en el mismo archivo de test

- **Tipo:** Ambigüedad / precisión de redacción
- **Requisitos afectados:** — (detalle de organización de tests, no un requisito del PDF ni un riesgo funcional)
- **Evidencia:**
  - Spec, Restricciones (línea 171-173): *"el valor ... se overridea ... **solo en la clase de test** que cubre los 2 escenarios de timeout (captura y listado del Equipo Activo)"* — en singular, como si un solo archivo de test cubriera ambos.
  - Spec, tabla de endpoints (línea 79-83): captura es `POST /trainers/{trainerId}/pokemon` y listar Equipo Activo es `GET /trainers/{trainerId}/pokemon?location=team` — dos endpoints distintos, sin que la spec declare en ningún lado que sus tests vivan en la misma clase.
- **Por qué es un problema:** es un detalle menor, pero si al implementar los dos escenarios terminan en clases de test separadas (lo más probable, dado que son dos endpoints distintos), la instrucción literal ("la clase", singular) podría leerse como que alcanza con anotar una sola cuando en realidad hacen falta dos `@TestPropertySource` — un caso de duplicación olvidada bastante menor y fácil de notar en el momento, no un bloqueo real.
- **Recomendación:** cambiar "en la clase de test que cubre los 2 escenarios" por "en la o las clases de test que cubren esos escenarios" — un ajuste de una palabra, sin impacto en el diseño.
- **Confianza:** baja (es una imprecisión de lenguaje, no algo que vaya a producir un bug; probablemente se resuelve solo al escribir el primer test).

## Lo que está bien (nuevo en esta pasada)

### ✔ H-12 se resolvió explicando el "por qué no" del enfoque descartado, no solo el "qué sí"
- **Evidencia:** Restricciones (línea 180-185) no se limita a decir "override acotado a la clase" — explica explícitamente por qué un override a nivel de perfil sería riesgoso (*"con riesgo real de 504 espurios bajo lentitud de CI"*), dejando el razonamiento a la vista para quien lea la spec más adelante.
- **Por qué suma:** mismo patrón que ya funcionó bien en D1/D3 de `docs/DESIGN.md` (documentar la alternativa descartada, no solo la elegida) — acá se aplicó también a un bullet de Restricciones, no solo a las decisiones formales de `DESIGN.md`.

## Recomendación sobre continuar el ciclo de reviews

Doce hallazgos en seis pasadas, con severidad decreciente y cada vez más alejada del enunciado del PDF (los últimos tres — H-10, H-11, H-12 — son puramente de viabilidad de testing, no de fidelidad PDF→PRD→spec). Ya no encuentro hallazgos de severidad Media o superior sobre la relación PDF↔PRD↔spec en sí. Antes de correr una 7ª pasada, probablemente rinda más avanzar a `sdd-implement`: los huecos que quedan (como H-13) son del tipo que se resuelve solo, escribiendo el primer test rojo del escenario correspondiente, y `sdd-verify` al cierre de la spec va a cubrir cualquier divergencia real entre lo implementado y lo documentado con más precisión que otra vuelta de review adversarial sobre el papel.

## Preguntas probables en la defensa (sin cambios)

No hay preguntas nuevas de esta pasada — las últimas dos pendientes (H-11/H-12, sobre timeouts de test) ya están resueltas. La lista acumulada de las pasadas 1 a 5 sigue vigente sin cambios.

## Nota metodológica

No se repiten las secciones de requisitos del PDF ni la matriz de trazabilidad: no cambiaron desde la 1ª pasada. H-13, igual que H-11 y H-12, no corresponde a ningún requisito del PDF.
