# Método SDD — Spec-Driven Development

> Copia congelada el 2026-09-20 del método de trabajo usado en este repo.
> Vive en la raíz para que cualquiera que abra el proyecto (incluido un
> evaluador sin acceso a configuración personal) entienda el proceso sin
> depender de nada externo. Si algo de acá se contradice con lo que hacemos
> en la práctica, gana este documento o lo actualizamos explícitamente —no
> dejamos la divergencia en silencio.

Este documento es la versión generalizada de una convención que nació para
un refactor de proyecto heredado puntual. La lógica del método (spec antes
que código, tests antes que implementación, verificación al cierre) es la
misma para cualquier trabajo de software; lo único que cambia es si hace
falta o no una fase previa de arqueología.

Dos convenciones fijas del método, sin excepción:

- **Los criterios de aceptación se escriben en Gherkin** (`Dado / Cuando /
  Entonces`), no como prosa o checkboxes sueltos. Un criterio que no se
  puede escribir como escenario Gherkin es un criterio ambiguo.
- **La implementación es TDD real**, ciclo rojo-verde-refactor por
  escenario — no "escribo todos los tests y después todo el código". Ver
  §8 F5/F6.

---

## 1. Principio

La fuente de verdad es la spec, no el código. El código es una derivación.

En un **proyecto nuevo** el flujo es directo: no hay código todavía, así que
se especifica y después se construye. En un **proyecto heredado** arranca al
revés: el código ya existe y no hay spec. Por eso hay una fase previa de
arqueología, donde el código es la fuente y la spec es el producto. Recién
cuando lo existente está documentado y congelado con tests, se invierte la
relación y se vuelve al SDD normal.

Regla que sostiene todo el método:

> **Cambio de comportamiento → spec primero, siempre.**
> Si el código se adelanta a la spec tres veces, la spec es ficción y
> volviste a desarrollar como siempre pero con más archivos markdown.

## 2. Dos modos de entrada

| | Modo A — Proyecto nuevo | Modo B — Proyecto heredado |
|---|---|---|
| Punto de partida | No hay código relevante todavía | Ya existe código sin spec |
| Fases previas | Ninguna | F1 Arqueología + F2 Baseline |
| Primera spec | `001-<nombre>.md` | `000-baseline.md`, luego `001-<nombre>.md` |

Este proyecto es **Modo B**: `pokeapi-reactor` es código heredado sin spec.

A partir de F3 el ciclo es idéntico en los dos modos.

## 3. Estructura de archivos

```
proyecto/
├── CLAUDE.md                      # reglas de la casa de ESTE proyecto (estable)
├── SDD-CONVENTION.md              # este documento, congelado al iniciar
├── docs/
│   ├── PRD.md                     # el encargo, pasado en limpio (congelado)
│   ├── PRODUCT.md                 # qué es esto y para quién, decisiones tomadas (estable)
│   ├── AS-IS.md                   # lo heredado, tal cual está hoy (solo modo B)
│   ├── DESIGN.md                  # decisiones técnicas y por qué (crece)
│   ├── AI-WORKFLOW.md             # registro de prompts usados y correcciones
│   └── specs/
│       ├── 000-baseline.md        # comportamiento heredado observable (solo modo B, congelado)
│       ├── 001-<nombre>.md        # primera spec
│       └── 002-<nombre>.md
└── src/
```

### Qué información va en cada archivo

| Pregunta que contesta | Archivo |
|---|---|
| ¿Qué nos pidieron exactamente? | `docs/PRD.md` |
| ¿Qué comando corre los tests? | `CLAUDE.md` |
| ¿Para qué existe este sistema? | `docs/PRODUCT.md` |
| ¿Cómo está hecho hoy y qué duele? | `docs/AS-IS.md` |
| ¿Por qué elegimos X sobre Y? | `docs/DESIGN.md` |
| ¿Qué hace la feature Z, verificable? | `docs/specs/NNN-*.md` |

Criterio rápido: si lo vas a repetir en cada prompt → `CLAUDE.md`.
Si es un **por qué** → `DESIGN.md`. Si es un **qué hace** verificable → spec.

**PRD vs PRODUCT.md:** el PRD es lo que pidieron, congelado, con la
ambigüedad del enunciado explicitada. `PRODUCT.md` es la interpretación
operativa, ya con las decisiones tomadas. El PRD no se toca; `PRODUCT.md`
evoluciona.

## 4. El flujo

```
        ┌─────────────────────────────┐
  F0    │  PRD                        │   encargo → docs/PRD.md
        │  qué nos pidieron           │   ← se congela, no se toca más
        └──────────────┬──────────────┘
                       ↓
        ┌─────────────────────────────┐
  F1    │  ARQUEOLOGÍA (solo modo B)  │   código → AS-IS.md
        │  entender lo heredado       │
        └──────────────┬──────────────┘
                       ↓
        ┌─────────────────────────────┐
  F2    │  BASELINE (solo modo B)     │   spec 000 + characterization tests
        │  congelar el comportamiento │   ← red de seguridad, quedan VERDES
        └──────────────┬──────────────┘
                       ↓
        ┌─────────────────────────────┐
  F3    │  SPECIFICATION              │   docs/specs/NNN-*.md
        │  qué queremos que haga      │   incluye criterios de aceptación
        └──────────────┬──────────────┘
                       ↓
        ┌─────────────────────────────┐
  F4    │  DESIGN                     │   entrada nueva en DESIGN.md
        │  cómo y por qué             │   con alternativas descartadas
        └──────────────┬──────────────┘
                       ↓
        ┌─────────────────────────────┐
 F5+F6  │  TDD — rojo/verde/refactor  │   un escenario Gherkin a la vez
        │  por escenario Gherkin      │   (baseline sigue verde todo el ciclo)
        └──────────────┬──────────────┘
                       ↓
        ┌─────────────────────────────┐
  F7    │  VERIFICATION               │   spec ↔ código, escenario por escenario
        │  ¿qué quedó desalineado?    │
        └──────────────┬──────────────┘
                       │
        ┌──────────────┴──────────────┐
        ↓                             ↓
  todo alineado               hay divergencia
   → siguiente spec (F3)       → volver a F3
                                 (actualizar spec, DESPUÉS código)
```

F0, F1 y F2 se corren una sola vez. F3 a F7 es el ciclo que se repite por
spec.

**La flecha de vuelta no es opcional.** Implementar siempre enseña algo que
la spec no previó. Que F7 devuelva trabajo a F3 es el proceso funcionando,
no fallando.

**Los tests van antes de la implementación.** Si se implementa y después se
escriben los tests, se escriben contra el código que ya existe, no contra la
spec: si el código interpretó mal un requisito, el test hereda el mismo
error y da verde igual. Por eso F5 y F6 no son dos pasos separados en el
tiempo (todos los tests, después todo el código) sino un único ciclo TDD
que se repite escenario por escenario — ver §8.

## 5. Fase 0 — PRD

Objetivo: pasar en limpio el encargo tal como se dio, separando lo que dice
de lo que se está asumiendo. Salida: `docs/PRD.md`.

No es un resumen del enunciado. Es una traducción a criterios verificables,
con la ambigüedad **marcada en vez de resuelta**. Lo que el enunciado no
dice es información tan importante como lo que dice: es donde se define el
alcance real de la entrega.

**Gate:** el PRD se congela antes de abrir el código. Si después aparece
algo que lo contradice, no se edita en silencio: se agrega una sección
`## Cambios` al final con fecha y motivo.

## 6. Fase 1 — Arqueología (solo modo B)

Objetivo: entender lo heredado sin tocar nada. Salida: `docs/AS-IS.md`.

No es un resumen bonito del código. Es un mapa con lo que duele marcado.
Conviene hacerlo en dos pasadas: reconocimiento primero, redacción después.
De un tiro sale un resumen genérico.

Regla de oro de esta fase: **todo lo que se afirme lleva archivo y línea.**
Si no se verificó leyendo código, no se escribe.

## 7. Fase 2 — Baseline (solo modo B)

Antes de mover una sola línea, se congela el comportamiento actual.

**Spec 000:** describe el comportamiento observable heredado, sin juicio de
valor. Es el contrato que el trabajo posterior **no puede romper**. Se
escribe una vez y no se toca: si se quiere cambiar comportamiento, va en
una spec nueva, no acá.

**Characterization tests:** documentan lo que el sistema hace hoy,
incluyendo los bugs. Nacen verdes y tienen que seguir verdes durante todo
el trabajo posterior. Son la red: si uno se pone rojo, algo se rompió.

Criterio de alcance: cubrir los entrypoints y los caminos que se van a
tocar. No apuntar al 100%, apuntar a **dormir tranquilo**.

**Gate:** no se pasa a F3 hasta que la suite de baseline esté verde y
commiteada.

## 8. Fases 3 a 7 — El ciclo (aplica en los dos modos)

Se repite una vez por spec. Una spec = un cambio acotado, implementable en
una sesión.

### F3 — Specification

Se redacta conversando, no de un tirón. Orden:

1. Se vuelca el problema en bruto. *"No escribas la spec todavía."*
2. **Interrogatorio:** *"Hacé las preguntas necesarias para que la spec no
   tenga ambigüedades. De a una, priorizando las que más impactan en el
   diseño."* Es la parte de mayor valor del método: son las preguntas que
   se iban a descubrir a mitad de la implementación, cuando corregirlas
   cuesta caro.
3. Recorte de alcance.
4. Redacción.

Los criterios de aceptación se escriben como **escenarios Gherkin**
(`Dado / Cuando / Entonces`), uno por caso — incluyendo bordes y casos
negativos, no solo el camino feliz. Un escenario Gherkin bien escrito **es**
el test: el Dado es el setup, el Cuando es la acción bajo prueba, el
Entonces es la aserción.

```gherkin
Escenario: cancelar una orden que todavía no fue despachada
  Dado una orden en estado PENDIENTE
  Cuando se solicita su cancelación
  Entonces la orden pasa a estado CANCELADA
  Y se emite el evento OrdenCancelada

Escenario: intentar cancelar una orden ya despachada
  Dado una orden en estado DESPACHADA
  Cuando se solicita su cancelación
  Entonces la operación se rechaza con "orden no cancelable"
  Y el estado de la orden no cambia
```

### F4 — Design

Solo si la spec implica una decisión no obvia. Una entrada por decisión en
`DESIGN.md`. La parte que más rinde es **"Descartado"**: evita que dos
semanas después se proponga lo mismo que ya se rechazó.

### F5 + F6 — TDD, rojo/verde/refactor

No son dos pasos separados: es un único ciclo que se repite **un escenario
Gherkin a la vez**, solo para los escenarios de la sección "Cambia" de la
spec (los de "Se preserva" ya están cubiertos por baseline).

1. **Rojo.** Se toma un escenario Gherkin — el más simple o el que más
   condiciona el diseño, no todos a la vez. Se traduce literalmente a un
   test: Dado → setup, Cuando → acción, Entonces → aserción. Se corre la
   suite y se confirma que el test falla **por la razón correcta** (el
   comportamiento todavía no existe).
2. **Verde.** Se escribe el mínimo código de producción para pasar ese
   test. Se corre la suite completa. Si un test de baseline se pone rojo,
   se **para y reporta** antes de seguir.
3. **Refactor (si hace falta).** Con el test en verde como red, se limpia
   lo que quedó sucio del paso anterior. Se corre la suite de nuevo.
4. Se repite desde 1 con el siguiente escenario.

### F7 — Verification

Dos preguntas, siempre las mismas:

```
Andá escenario por escenario de la spec NNN. Para cada uno: qué test lo
cubre y si realmente verifica el Entonces, no solo ejercita el Cuando. Si
alguno no está cubierto o el test no lo verifica de verdad, decilo en vez
de justificar.
```

```
¿Hay algo en el código que no esté en la spec, o algo en la spec que el
código resuelva distinto? Listá las diferencias.
```

Ante divergencia hay dos salidas legítimas — corregir el código, o
actualizar la spec porque la realidad enseñó algo. Dejarlos
desincronizados no es una opción.

## 9. Convenciones operativas

### Numeración

`000` baseline. `001` en adelante, orden de ejecución, sin renumerar nunca.
Nombre: `NNN-verbo-objeto.md`.

### Commits

Un tipo por commit, nunca mezclados:

```
docs(prd): encargo pasado en limpio
docs(spec): 003 extraer pricing service
test(baseline): characterization de OrderController
feat(003): agregar endpoint de cancelación de orden
refactor(003): mover cálculo de descuentos a PricingService
docs(design): D3 servicio de dominio para pricing
```

Regla dura: **un commit no cambia comportamiento y estructura a la vez.**

### Dónde vive el registro de trabajo

`docs/AI-WORKFLOW.md` — qué prompt/skill se usó en cada fase, qué devolvió,
qué se corrigió a mano. Es lo que demuestra criterio ante quien revise el
repo.

## 10. Errores frecuentes

| Error | Síntoma | Corrección |
|---|---|---|
| Arrancar sin PRD | a mitad del trabajo no se sabe si algo estaba pedido | F0 antes de abrir el código |
| Resolver ambigüedad en silencio | asunciones que solo viven en la cabeza de quien las tomó | van en PRD § Ambigüedades |
| Modificar sin baseline (modo B) | "creo que no rompí nada" | F2 es un gate, no una sugerencia |
| Specs demasiado grandes | plan con más de 6 pasos | partir la spec |
| Spec que describe implementación | dice "usar un HashMap" | eso va en DESIGN.md |
| Arreglar bugs "de paso" | commit que mezcla cambios no relacionados | spec aparte |
| Saltear F7 | specs viejas que nadie lee, código y spec divergen | son documentación, no specs |
| Criterios de aceptación en prosa suelta | no dice qué verificar | reescribir como escenario Gherkin |
| Escribir todos los tests y después todo el código | no es TDD real, el diseño ya quedó fijado antes del primer verde | ciclo rojo/verde/refactor por escenario |
| Refactor que cambia un test para que pase | el refactor "arregla" el test en vez del código | si un test se pone rojo en refactor, el bug está en el código nuevo |
