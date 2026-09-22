# Review adversarial — Desafío Técnico Betwarrior Java Developer

**Fecha:** 2026-09-21
**Documentos revisados:**
- Enunciado: `.claude/information/Desafío Técnico Betwarrior Java Developer.pdf`
- PRD: `docs/PRD.md` (working tree, modificado sin commitear)
- Spec: `docs/specs/001-equipo-activo-y-baul.md` (working tree, sin trackear — spec aún no implementada)
- Repo: working tree sobre rama `chore/pre-baseline-setup`, también se consultaron `docs/PRODUCT.md`, `docs/DESIGN.md` y `docs/AS-IS.md` como contexto de decisiones ya tomadas, y el código fuente (`pom.xml`, `skaro.pokeapi.client.*`) para verificar supuestos técnicos.

## Resumen ejecutivo

El PRD es una extracción fiel y muy completa del PDF — no encontré requisitos perdidos ni inventados en esa traducción, y las ambigüedades quedaron correctamente marcadas. `PRODUCT.md` resuelve esas ambigüedades con justificación explícita en vez de asunciones silenciosas, que es exactamente lo que un evaluador escéptico busca. El punto débil está un nivel más abajo: la spec 001 traduce bien el diseño de alto nivel (endpoints, capas, aislamiento de bloqueo), pero dos huecos concretos en su tabla de errores vs. sus escenarios Gherkin significan que, bajo el propio método TDD del proyecto, esas rutas de error corren riesgo real de no implementarse. También hay una condición de carrera no contemplada en la validación de límites y una contradicción interna menor (R2DBC vs. JPA) que quedó de una revisión a medio actualizar. Nada de esto es bloqueante para el enunciado en sí — es implementable tal como está — pero sí son preguntas altamente probables en la defensa técnica que hoy no tienen respuesta preparada.

| Severidad | Cantidad |
|---|---|
| 🔴 Bloqueante | 0 |
| 🟠 Alta | 2 |
| 🟡 Media | 3 |
| 🔵 Baja | 2 |

## Requisitos extraídos del enunciado

| ID | Tipo | Requisito | Ubicación en PDF |
|---|---|---|---|
| R-01 | Funcional | Diseñar e implementar endpoints y capa de persistencia para gestionar Pokémon individuales de un entrenador | p.1, "Diseñar e implementar los endpoints y la capa de persistencia necesaria..." |
| R-02 | Funcional | Diferenciar Equipo Activo del Baúl de Almacenamiento | p.1 |
| R-03 | Funcional | IVs: 6 estadísticas arrancando en 1, + genética 0-31 por estadística | p.1-2, "Valores Individuales (IVs)" |
| R-04 | Funcional | EVs: máx. 510 puntos totales, máx. 252 por estadística individual | p.2, "Valores de Esfuerzo (EVs)" |
| R-05 | Funcional | Naturaleza: 1 de 25, incrementa una stat 10% y reduce otra 10% | p.2, "Naturaleza" |
| R-06 | Funcional | Habilidad entre las que la especie posee en PokéAPI | p.2, "Habilidad" |
| R-07 | Funcional | Flag Shiny + Género (Macho/Hembra/Sin Género) | p.2, "Atributos Visuales y Género" |
| R-08 | Funcional | Datos de origen: OT, Pokéball, fecha de captura, nivel inicial, ubicación | p.2, "Datos de Origen (Metadata de Captura)" |
| R-09 | Funcional | Hasta 4 movimientos | p.2, "Set de Movimientos" |
| R-10 | Funcional | Máximo 1 objeto sostenido (Held Item) | p.2, "Objeto Equipado" |
| R-11 | Regla de negocio (valor no dado) | Equipo Activo con máximo de ejemplares | p.2, "Cada entrenador puede llevar un máximo de ejemplares..." |
| R-12 | Regla de negocio (valor no dado) | Baúl con límite máximo | p.2, "Espacio de almacenamiento secundario con un límite máximo" |
| R-13 | Funcional | Endpoints: crear/capturar, listar equipo, listar baúl, mover | p.2, "Gestión de Estado" |
| R-14 | Funcional | Captura asigna automáticamente a equipo o baúl según disponibilidad | p.2, "Captura / Creación de Ejemplar" |
| R-15 | Funcional | Detalle de ejemplar por id: metadata técnica, genética y de origen | p.2, "Detalle de Ejemplar" |
| R-16 | Funcional | Consulta de Equipo Activo: vista compuesta (metadata propia + atributos estáticos de especie desde PokéAPI) | p.2, "Consulta de Equipo Activo" |
| R-17 | Funcional | Consulta del Baúl: lista de ejemplares almacenados | p.2, "Consulta del Baúl (PC Box)" |
| R-18 | Funcional | Traslado equipo↔baúl con validaciones de límite | p.2, "Traslado de Ejemplares" |
| R-19 (implícito) | No funcional | Manejo adecuado de errores/códigos HTTP en todos los casos de negocio, no solo el feliz | Justificación: "Se evalúa... el manejo adecuado de códigos de respuesta HTTP y errores" (p.2) es un criterio explícito, no un caso puntual |
| R-20 (bonus) | Funcional | Endpoint de evolución (ID + especie destino) | p.3 |
| R-21 (bonus) | Regla | Validar línea evolutiva válida y directa, sin requisitos situacionales | p.3 |
| R-22 (bonus) | Regla | Actualizar habilidad al evolucionar | p.3 |
| R-23 (bonus) | Regla | Preservar identidad/genética/objetos/historial/slot exacto | p.3 |
| R-24 (bonus) | Regla | Soportar líneas evolutivas simples y ramificadas | p.3 |
| R-25 | Entregable | Repo público de GitHub, proyecto refactorizado + funcionalidad | p.3, "Criterios de Entrega" |
| R-26 | Entregable | README.md modificado: trabajo realizado, decisiones, uso y mantenimiento | p.3 |
| R-27 | No funcional | Mantenibilidad, escalabilidad, fácil incorporación de nuevos integrantes | p.1 |
| R-28 | Criterio de evaluación | Diseño RESTful, estructura de payloads, códigos HTTP/errores | p.2 |
| R-29 | Criterio de evaluación | Decisiones de arquitectura, alternativas evaluadas, herramientas usadas, defensa técnica | p.3 |
| R-30 (implícito) | No funcional | Manejo de fallas de la dependencia externa PokéAPI (timeout/5xx), no solo "no encontrado" | Justificación: el sistema depende de PokéAPI para resolver Habilidad y para la vista compuesta (R-16); un evaluador razonable espera que una dependencia externa que puede fallar esté contemplada, sobre todo cuando "manejo adecuado de errores" ya es criterio explícito (R-19/R-28) |

## Matriz de trazabilidad

| ID | PRD | Spec | Estado | Nota |
|---|---|---|---|---|
| R-01 | FR-001 | "Alcance del cambio" | ✅ | |
| R-02 | FR-002 | Tabla de endpoints | ✅ | |
| R-03 | FR-003, BR-001/002 | Escenario "Captura con un IV fuera de rango" | ✅ | |
| R-04 | FR-004, BR-003/004 | Escenarios EVs total/individual | ✅ | |
| R-05 | FR-005, BR-005 | Tabla de errores (422 "naturaleza inválida") | ⚠️ Parcial | Está en la tabla de errores pero sin escenario Gherkin — ver H-01 |
| R-06 | FR-006, BR-006 | Escenario "Habilidad que no pertenece a la especie" | ✅ | |
| R-07 | FR-007/008 | No aparece en Alcance ni en Gherkin | ⚠️ Parcial | Campo estructural (enum/boolean), sin validación de negocio que amerite escenario — ver H-06 |
| R-08 | FR-009 | Restricciones (OT como FK al Trainer fijo); Pokéball/fecha/nivel/ubicación resueltos en PRODUCT.md A10/A11 pero no citados en spec 001 | ⚠️ Parcial | Ver H-06 |
| R-09 | FR-010, BR-008 | Escenario "Captura con más de 4 movimientos" | ✅ | |
| R-10 | FR-011, BR-007 | No aparece explícitamente | ⚠️ Parcial | Campo estructural (máx. 1 = un solo campo), riesgo bajo — ver H-06 |
| R-11 | FR-012, BR-009 | Restricciones: "Equipo Activo = 6" (PRODUCT.md A1) | ✅ | Decisión propia, declarada y justificada |
| R-12 | FR-013, BR-010 | Restricciones: "Baúl = 300" (PRODUCT.md A2) | ✅ | Ídem |
| R-13 | FR-014-017 | Tabla de endpoints | ✅ | |
| R-14 | FR-019, BR-011 | Escenarios de captura (team/box) | ✅ | |
| R-15 | FR-020 | Escenarios "Detalle de un ejemplar" | ✅ | |
| R-16 | FR-021 | Escenario "Listar el Equipo Activo devuelve vista compuesta" | ✅ | |
| R-17 | FR-022 | Escenario "Listar el Baúl" | ✅ | |
| R-18 | FR-017/023 | Escenarios de traslado | ⚠️ Parcial | Falta el caso simétrico "mover de Equipo a Baúl sin espacio" — ver H-05 |
| R-19 (implícito) | NFR-004 | Tabla de errores HTTP | ⚠️ Parcial | 2 de 6 causas del 422 y el 400 no tienen escenario Gherkin — ver H-01 |
| R-20 a R-24 (bonus) | FR-024-028 | "Fuera de alcance" | ❌ No implementado (declarado) | Decisión explícita y justificada en PRODUCT.md A12; el PRD ya marca como abierto el peso real del bonus en la nota final (Q7/A12) |
| R-25/R-26 | §8 Entregables | No aplica a spec 001 (nivel proyecto/README) | — | Fuera del alcance de esta spec puntual, no es un hallazgo de esta spec |
| R-27 | O2-O4 | Motivación (separación en capas `domain`/`persistence`/`api`) | ✅ | Cualitativo, no verificable por diseño (el propio PRD ya lo marca como ambiguo, NFR-002) |
| R-28 | NFR-004/005 | Tabla de endpoints + tabla de errores | ✅ con huecos puntuales | Ver H-01 |
| R-29 | §10 | `PRODUCT.md` + `DESIGN.md` | ✅ | Fuerte — ver "Lo que está bien" |
| R-30 (implícito) | — | Restricciones (solo valida Habilidad contra PokéAPI) | ❌ Faltante | Ver H-02 |

### Elementos sin respaldo en el enunciado

| Elemento | Dónde aparece | ¿Declarado como decisión propia? | Evaluación |
|---|---|---|---|
| Convención REST "sin verbos, `location` como query param, contrato en inglés" | Spec, "Cambia" | Sí, con justificación (evita ambigüedad de ruteo) | Sin problema — decisión técnica razonable y declarada, no atribuida al PDF |
| `trainerId` en la URL pese a Entrenador único/fijo | Spec, "Cambia" y "Restricciones" | Sí, con justificación (contrato estable a futuro) | Sin problema |


No se identificó scope creep real (funcionalidad no pedida y presentada como si lo fuera).

## Hallazgos

### H-01 🟠 Alta — Tabla de errores promete más de lo que los escenarios Gherkin garantizan implementar

- **Tipo:** Hueco
- **Requisitos afectados:** R-05, R-19, R-28
- **Evidencia:**
  - Spec (`docs/specs/001-equipo-activo-y-baul.md:105`): la fila 422 lista seis causas: *"IV fuera de 0-31, EVs sobre 510 total o 252 individual, naturaleza inválida, habilidad que no pertenece a la especie, más de 4 movimientos, especie inexistente en PokéAPI"*.
  - Spec, sección "Criterios de aceptación" (líneas 153-249): hay escenario Gherkin para IV, EVs (total e individual), habilidad y más de 4 movimientos — pero **no** para "naturaleza inválida" ni para "especie inexistente en PokéAPI".
  - Tampoco hay escenario para el `400 Bad Request` ("Payload malformado o con campos obligatorios faltantes", línea 104), pese a que está en la misma tabla.
  - CLAUDE.md del proyecto (sección "Método de trabajo"): *"Implementación con TDD real: ciclo rojo/verde/refactor, un escenario a la vez"* — el método de este proyecto implementa exactamente lo que está en un escenario Gherkin, no lo que está solo mencionado en prosa.
- **Por qué es un problema:** bajo el propio método SDD del proyecto, un caso de error que solo aparece en la tabla de prosa y no en un escenario Gherkin corre el riesgo concreto de no implementarse (nadie escribe un test rojo para él, así que nadie escribe el código verde). Si eso pasa, un request con naturaleza inválida o especie inexistente probablemente termine en un `500` sin traducir — justo el escenario que el criterio de evaluación explícito del PDF (R-28, "manejo adecuado de códigos de respuesta HTTP y errores") va a mirar.
- **Recomendación:** agregar 3 escenarios Gherkin a la spec: captura con naturaleza inválida (422), captura con especie inexistente en PokéAPI (422), y captura con payload malformado/campo obligatorio faltante (400). Si alguno se decide posponer, que sea una decisión explícita en "Fuera de alcance", no una omisión silenciosa.
- **Confianza:** alta.

### H-02 🟠 Alta — La spec dice resolver P8 pero solo traduce un caso; el resto de fallas de PokéAPI queda sin contrato de error

- **Tipo:** Hueco
- **Requisitos afectados:** R-30
- **Evidencia:**
  - Spec, "Motivación" (línea 6): *"Resuelve... P8 (sin traducción de errores HTTP a errores de dominio) de `docs/AS-IS.md`"*.
  - `docs/AS-IS.md:103`: P8 está descripto como *"Sin traducción de errores HTTP a errores de dominio"*, con riesgo explícito anotado: *"riesgo de códigos de respuesta inconsistentes hacia afuera si la nueva capa no envuelve esto (afecta NFR-004 del PRD)"*.
  - `docs/specs/000-baseline.md:91-96`: characteriza que hoy, ante un 404 de PokéAPI, *"el Mono termina en error con la excepción default de WebClient (`WebClientResponseException.NotFound`)"* — sin traducir. Ese comportamiento no cambia con la spec 001 (`skaro.pokeapi.client` no se toca, según "Alcance del cambio", línea 59).
  - La única traducción que la spec 001 sí define es "especie inexistente en PokéAPI → 422" (tabla de errores, línea 105). No hay fila para timeout, conexión rechazada o 5xx de PokéAPI durante la captura (resolviendo Habilidad) ni durante la vista compuesta de FR-021 (que llama a PokéAPI por cada ejemplar listado).
  - Verificado en código: `PokeApiClient` (interfaz, `src/main/java/skaro/pokeapi/client/PokeApiClient.java`) no tiene manejo de errores propio, y no se encontró ningún `onErrorResume`/`onStatus`/`WebClientResponseException` en las implementaciones (`ReactiveNonCachingPokeApiClient`, `ReactiveCachingPokeApiClient`, `WebClientEntityFactory`) — confirma que el error crudo de WebClient se propaga tal cual hoy.
- **Por qué es un problema:** la spec ahora depende de PokéAPI no solo para lectura (como el cliente original) sino para *validar* una regla de negocio de escritura (Habilidad) y para componer una respuesta de lectura (FR-021). Si PokéAPI está caída o lenta — algo perfectamente posible durante la demo en vivo de la charla técnica — capturar un Pokémon o listar el Equipo Activo probablemente devuelva un 500 con traza cruda de WebClient, exactamente el "código de respuesta inconsistente" que P8 ya anticipaba. Afirmar que la spec "resuelve P8" sin cubrir este caso es un desvío entre lo que dice la Motivación y lo que realmente cubre el diseño.
- **Recomendación:** agregar al menos una fila a la tabla de errores para fallas de PokéAPI (timeout/5xx/conexión rechazada) con un código explícito (`502 Bad Gateway` o `504 Gateway Timeout` son razonables) y, si el tiempo no alcanza para implementarlo, mover ese caso a "Fuera de alcance" con nota explícita — no dejarlo implícito bajo un "resuelve P8" que en los hechos es parcial.
- **Confianza:** alta.

### H-03 🟡 Media — Validación de límites (6/300) vulnerable a condición de carrera bajo escrituras concurrentes

- **Tipo:** Hueco (no funcional / concurrencia)
- **Requisitos afectados:** R-11, R-12, R-14, R-18, R-27 (escalabilidad)
- **Evidencia:**
  - Spec, "Restricciones" (línea 128-132): persistencia JPA con llamadas aisladas vía `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())` — múltiples requests concurrentes pueden ejecutar el ciclo "contar ejemplares → decidir si hay espacio → insertar" en threads distintos del pool `boundedElastic`, sin que la spec declare ningún mecanismo de exclusión (lock pesimista, constraint de base, transacción con aislamiento adecuado).
  - Ni la spec ni `docs/DESIGN.md` (D1) mencionan cómo se evita que dos capturas simultáneas, cuando queda exactamente 1 slot libre en el Equipo Activo, terminen ambas insertando y superando el límite de 6.
- **Por qué es un problema:** es un check-then-act clásico. El PDF no lo pide explícitamente, pero el PRD sí eleva "escalabilidad" a objetivo explícito (O3/NFR-002), y romper el invariante "máximo 6 en Equipo Activo" bajo concurrencia es exactamente el tipo de bug que una defensa técnica ("¿qué pasa si dos requests llegan al mismo tiempo?") va a exponer.
- **Recomendación:** documentar la estrategia elegida (por ejemplo, `@Version`/lock optimista + reintento, o una constraint `CHECK`/trigger a nivel de base, o serializar por entrenador) en `docs/DESIGN.md`, aunque sea como decisión de "no lo resolvemos hoy, documentado como deuda" — mejor eso que silencio.
- **Confianza:** media (depende de que el volumen de escritura concurrente real sea bajo en la demo, pero el riesgo de diseño es real independientemente de la probabilidad de disparo).

### H-04 🟡 Media — Contradicción interna: la spec no está actualizada consigo misma sobre R2DBC vs. JPA

- **Tipo:** Contradicción
- **Requisitos afectados:** — (calidad interna de la spec, no un requisito del PDF)
- **Evidencia:**
  - Spec, "Alcance del cambio" (línea 20-23): *"se agregan dependencias de persistencia (R2DBC + driver H2, o JPA + H2 si se activa el fallback — ver Restricciones)"* — redactado como si JPA fuera todavía el fallback tentativo.
  - Spec, "Restricciones" (línea 128-130), en el mismo documento: *"Persistencia: H2 en modo archivo, con **JPA (Hibernate) + `Schedulers.boundedElastic()`** — decisión definitiva, ya no tentativa"*.
  - `docs/PRODUCT.md:77-100` y `docs/DESIGN.md:10-36` confirman que la revisión a JPA definitivo se hizo el 2026-09-21 y quedó bien documentada — el problema es solo que la sección "Alcance del cambio" de la spec no se actualizó con el mismo lenguaje.
- **Por qué es un problema:** es menor en sustancia (la decisión real está clara y bien justificada en PRODUCT/DESIGN), pero dentro de la propia spec un lector que solo lea "Alcance del cambio" se queda con la impresión de que R2DBC sigue siendo el camino primario. En una charla técnica, que dos secciones del mismo documento entregado no coincidan es una pregunta fácil de que te hagan y no debería sorprender.
- **Recomendación:** actualizar la frase de "Alcance del cambio" a algo como *"se agregan dependencias de persistencia JPA + driver H2 (ver Restricciones y `docs/DESIGN.md` D1 para el porqué)"`, quitando la mención a R2DBC como opción activa.
- **Confianza:** alta.

### H-05 🔵 Baja — Falta el escenario Gherkin simétrico de traslado Equipo→Baúl sin espacio

- **Tipo:** Hueco
- **Requisitos afectados:** R-18
- **Evidencia:**
  - Spec, "Criterios de aceptación": existe "Mover del Baúl al Equipo Activo sin espacio disponible" (líneas 239-244, 409), pero no el caso inverso, "mover del Equipo al Baúl sin espacio disponible", pese a que la tabla de errores (línea 107) dice en general *"destino lleno al mover"* sin distinguir dirección.
- **Por qué es un problema:** con el Baúl en 300 de capacidad es un caso de borde poco probable de disparar en la demo, pero sigue siendo una rama de código documentada en la tabla de errores sin test que la respalde — mismo patrón de riesgo que H-01, a menor escala.
- **Recomendación:** agregar el escenario simétrico, aunque sea trivial de escribir dado que la lógica ya existe para la otra dirección.
- **Confianza:** alta.

### H-06 🔵 Baja — El modelo de datos descripto en DESIGN.md D1 no menciona Shiny, Held Item ni los campos de origen

- **Tipo:** Hueco (posible, a confirmar)
- **Requisitos afectados:** R-07, R-08, R-10
- **Evidencia:**
  - `docs/DESIGN.md:18-21` describe el modelo de `PokemonInstance` que motiva elegir JPA: *"objetos embebidos (IVs y EVs...), una colección (hasta 4 movimientos), varios enums (naturaleza de 25 valores, género, location)"* — no menciona Shiny (boolean), Held Item, ni el bloque de datos de origen (OT/Pokéball/fecha/nivel inicial/ubicación), aunque PRODUCT.md sí resuelve varios de esos campos por separado (A10, A11).
- **Por qué es un problema:** probablemente D1 es solo una enumeración ilustrativa (para justificar la elección de JPA sobre R2DBC) y no un contrato de campos — en ese caso no hay problema real. Pero como ningún documento de esta spec enumera el payload completo de captura, no hay forma de confirmar desde la documentación que estos campos efectivamente estén contemplados en el modelo antes de que se escriba el código.
- **Recomendación:** si se quiere cerrar la duda antes de implementar, alcanza con una lista corta de campos en la spec o el DESIGN.md — no hace falta un escenario Gherkin para cada uno, son estructurales sin regla de negocio asociada.
- **Confianza:** baja (es más una observación de completitud documental que un hallazgo firme).

## Lo que está bien

### ✔ Trazabilidad explícita de tres niveles (AS-IS → PRD → spec)
- **Evidencia:** Spec, "Motivación" (líneas 4-14): cada línea de motivación cita el dolor puntual del AS-IS que resuelve (P1, P2, P3, P8) y el objetivo puntual del PRD que sirve (O1-O6), con número de FR exacto (FR-001 a FR-023).
- **Por qué suma:** es exactamente la clase de "decisiones tomadas en base a la arquitectura... en base a las reglas de negocio" que el PDF pide evaluar explícitamente (Criterios de Evaluación, p.3) — y hace la defensa técnica mucho más fácil porque cada decisión ya tiene su cadena de justificación escrita.

### ✔ Las 12 ambigüedades del PRD se resolvieron con justificación, no en silencio
- **Evidencia:** `docs/PRODUCT.md`, tabla "Decisiones" (líneas 26-41): cada fila referencia el ID de ambigüedad del PRD (A1-A12) y da un motivo concreto, incluyendo por qué "naturaleza — tabla completa" (A7) en realidad no era una ambigüedad real (verificable, dominio público).
- **Por qué suma:** el PDF advierte que "priorizamos la calidad del diseño... las decisiones y trade-offs que evaluaste" (p.1) — este documento es prueba escrita de ese trade-off evaluado, en vez de asunciones implícitas que solo saldrían a la luz si alguien pregunta.

### ✔ Aislamiento correcto del bloqueo JPA dentro de una capa reactiva
- **Evidencia:** Spec, "Restricciones" (línea 128-132) y `docs/DESIGN.md` D1: las llamadas JPA quedan encapsuladas en `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())` dentro de `skaro.trainer.persistence`; `domain` y `api` (y todo `skaro.pokeapi`) solo ven `Mono`/`Flux`.
- **Por qué suma:** es la respuesta correcta al riesgo #1 del checklist Reactor/WebFlux (bloqueo del event loop) — y está declarado explícitamente, no es un accidente que haya que descubrir leyendo el código.

### ✔ Trade-off R2DBC vs. JPA documentado con motivo técnico verificable, no solo preferencia
- **Evidencia:** `docs/DESIGN.md` D1 (líneas 27-32): *"Spring Data R2DBC no soporta objetos embebidos ni colecciones de elementos, y no genera schema automáticamente"* — motivo técnico concreto, no una preferencia de estilo.
- **Por qué suma:** responde directamente al criterio explícito del PDF "Alternativas posibles a la solución propuesta" (p.3) con una alternativa real, evaluada y descartada por una razón verificable, no post-hoc.

### ✔ Modelo de errores HTTP semánticamente diferenciado (400 / 404 / 409 / 422)
- **Evidencia:** Spec, tabla de errores (líneas 100-107): usa 409 específicamente para conflicto de estado del recurso (límites llenos) en vez de reusar 400/422, y 422 para violación de regla de negocio con payload bien formado, distinto de 400 para payload malformado.
- **Por qué suma:** es una separación semánticamente correcta según la especificación HTTP, y responde directo al criterio explícito "manejo adecuado de códigos de respuesta HTTP" (p.2) — con la salvedad de cobertura de tests señalada en H-01.

## Preguntas probables en la defensa

| # | Pregunta | ¿Los documentos la responden? | Dónde / qué falta preparar |
|---|---|---|---|
| 1 | ¿Por qué JPA bloqueante en un proyecto que es reactivo de punta a punta? | Sí | `docs/DESIGN.md` D1 |
| 2 | ¿Por qué H2 en archivo y no una base real? | Sí | `docs/PRODUCT.md`, fila "Motor de persistencia" |
| 3 | ¿Qué pasa si PokéAPI está caída durante la demo? | No | Preparar respuesta — ver H-02 |
| 4 | ¿Cómo evitás que dos capturas simultáneas exceedan el límite del Equipo Activo? | No | Preparar respuesta — ver H-03 |
| 5 | ¿Por qué el Entrenador es fijo/único si el modelo ya lo soporta como entidad propia? | Sí | `docs/PRODUCT.md` A9, Spec "Restricciones" |
| 6 | ¿Por qué no hay autenticación? | Sí | `docs/PRODUCT.md`, fila "Autenticación/autorización" |
| 7 | ¿Por qué 6 y 300 como límites, si el PDF no los da? | Sí | `docs/PRODUCT.md` A1/A2 |
| 8 | ¿Por qué la tabla de errores menciona "naturaleza inválida" y "especie inexistente" pero no hay tests para esos casos? | No | Preparar respuesta o cerrar el hueco — ver H-01 |
| 9 | ¿Cómo evolucionaría el bonus de evolución sin tocar el dominio actual? | Parcial | Spec, "Alcance del cambio" menciona `skaro.evolution` como paquete hermano futuro, sin más detalle — alcanza para la pregunta pero es escueto |
| 10 | ¿Por qué elegiste modelar el traslado como `PATCH` y no como un endpoint de acción (`/pokemon/{id}/mover`)? | Sí | Spec, "Cambia" (línea 85-91) |

## Seguimiento de reviews anteriores

No aplica — es el primer review adversarial sobre estos documentos.
