# Code review — cierre del hallazgo #9 (`chore/pre-baseline-setup`)

**Fecha:** 2026-09-22
**Skill aplicada:** `java21-reactive-code-review`
**Relación con las pasadas anteriores:** cierra el hallazgo #9 de
`code-review-2026-09-22-r2.md` (`Nature.valueOf`/`Gender.valueOf` sin
manejar en `TrainerRosterService.toEntity()`). No edita ni reemplaza los
reportes anteriores — son historial.
**Modo:** fix aplicado con TDD (rojo/verde por escenario, no todos los
tests seguidos de todo el código), como pide el método SDD del proyecto.

---

## Qué se resolvió

### Nature — BR-005 (ya espec'd, no implementado)

`docs/specs/001-equipo-activo-y-baul.md` ya tenía el escenario Gherkin
"Captura con una naturaleza inválida" (→ `422 Unprocessable Entity`,
BR-005) y la fila correspondiente en la tabla de errores, pero el código
nunca lo implementaba ni lo testeaba: `Nature.valueOf(...)` corría sin
capturar dentro de `toEntity()`, después de la llamada a PokéAPI. Ahora:

- `TrainerRosterService.resolveNature(String)` valida temprano, junto con
  EVs/IVs/moves, **antes** de llamar a PokéAPI — un payload con
  naturaleza inválida falla rápido en vez de gastar una llamada externa
  para un request que iba a rechazarse igual.
- Lanza `CaptureValidationException("BR-005: ...")`, mapeado a 422 por
  `TrainerExceptionHandler` (sin cambios ahí, ya cubría el tipo de
  excepción).
- Cobertura: test unitario
  (`TrainerRosterServiceTest.unaNaturalezaInvalidaRechaza`) + el
  escenario de integración que faltaba
  (`PokemonCaptureIntegrationTest.capturaConUnaNaturalezaInvalida`),
  redactado calcando el Gherkin de la spec.

### Gender — mismo bug estructural, sin BR formal en la spec

`Gender.valueOf(...)` tenía el mismo problema tres líneas más abajo, pero
a diferencia de Nature no tiene escenario Gherkin ni código BR en
`docs/specs/001-equipo-activo-y-baul.md`. Consultado con la persona
usuaria (ver alcance abajo), se decidió tratarlo igual que Nature por
consistencia — es el mismo campo de tipo enum en el mismo payload, con el
mismo bug — pero **sin inventar un código BR que no está en la spec**:
`resolveGender` lanza `CaptureValidationException` con mensaje
descriptivo, sin prefijo `BR-XXX`, y queda comentado en el código que
falta formalizarlo si se decide sumarlo a la spec 001. Cobertura: test
unitario (`TrainerRosterServiceTest.unGeneroInvalidoRechaza`); sin test
de integración porque no hay escenario Gherkin que calcar.

### `toEntity()` como mapeo puro

Efecto secundario limpio del fix: `toEntity()` ya no parsea strings ni
puede lanzar — recibe `Nature`/`Gender` ya resueltos por
`resolveNature`/`resolveGender` y solo mapea. Alineado con el punto de la
skill sobre funciones puras ("mappers que... tienen efectos" — acá ya no
los tiene).

---

## Alcance de la decisión sobre Gender

Antes de tocar código se le preguntó a la persona usuaria cómo tratar el
caso de Gender, dado que `~/.claude/CLAUDE.md` pide no arreglar bugs
fuera de la spec activa sin anotarlos aparte. Opciones ofrecidas: (a)
arreglar igual que Nature con nota de que falta el BR formal, (b) dejar
Gender sin tocar y anotado aparte, (c) actualizar la spec primero. Se
eligió (a) — código y tests reflejan esa decisión; queda pendiente, si
se quiere, sumar el BR y el escenario Gherkin correspondiente a
`docs/specs/001-equipo-activo-y-baul.md` para que el código y la spec
queden alineados también en el papel.

---

## Archivos tocados

- `src/main/java/skaro/trainer/domain/TrainerRosterService.java` —
  `resolveNature`/`resolveGender` nuevos, `capture()` los llama temprano,
  `toEntity()` recibe los valores ya resueltos en vez de parsear.
- `src/test/java/skaro/trainer/domain/TrainerRosterServiceTest.java` —
  `unaNaturalezaInvalidaRechaza`, `unGeneroInvalidoRechaza`.
- `src/test/java/skaro/trainer/api/PokemonCaptureIntegrationTest.java` —
  `capturaConUnaNaturalezaInvalida` (cierra el gap con el Gherkin de la
  spec).

## Verificación

`./mvnw test`: 50/50 verdes (47 previos + 3 nuevos: 2 unitarios + 1 de
integración). Ciclo TDD real: cada test se corrió en rojo contra el
código sin el fix antes de implementarlo.

## Resumen

| # | Estado | Archivo:línea | Resumen |
|---|---|---|---|
| 9 | Resuelto | `TrainerRosterService.java` (resolveNature/resolveGender) | Nature (BR-005, spec'd) y Gender (sin BR, por consistencia) ahora se validan temprano y rechazan con 422 en vez de `IllegalArgumentException` sin manejar |
