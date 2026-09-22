# Diseño

Decisiones técnicas no obvias tomadas durante la implementación de una
spec — con las alternativas descartadas y el motivo concreto del descarte,
para no volver a proponerlas en el futuro sin saber que ya se evaluaron.
Fase F4 del método SDD (`~/.claude/sdd/SDD-METHOD.md`). Solo entran acá
decisiones con más de un camino razonable; si solo hay una forma sensata
de hacer algo, no se documenta — sería ruido.

## D1 — Persistencia de Spec 001: JPA + `Schedulers.boundedElastic()`, no R2DBC end-to-end

Se eligió JPA (Hibernate) sobre H2 en modo archivo, con las llamadas
bloqueantes aisladas dentro de `skaro.trainer.persistence`: cada método
del repositorio envuelve la llamada JPA en
`Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())` y expone
`Mono`/`Flux` hacia `domain` — el resto de la app, incluido todo
`skaro.pokeapi`, no se entera de que ahí adentro hay una llamada
bloqueante. El modelo de `PokemonInstance` necesita objetos embebidos
(IVs y EVs, 6 stats cada uno), una colección (hasta 4 movimientos), varios
enums (naturaleza de 25 valores, género, location) y una relación
`Trainer`→`PokemonInstance` (aggregate root, uno a muchos) — todo con
soporte directo en JPA (`@Embeddable`, `@ElementCollection`,
`@Enumerated`, `@OneToMany`, `ddl-auto`). Con el plazo de entrega acotado
del challenge, se priorizó velocidad y menos superficie de error sobre
pureza reactiva end-to-end.

Descartado: R2DBC end-to-end (resuelve el dolor P7 de `docs/AS-IS.md`,
coherente con el resto del stack reactivo ya existente) — Spring Data
R2DBC no soporta objetos embebidos ni colecciones de elementos, y no
genera schema automáticamente. El mismo modelo de datos hubiera
requerido mapeo manual fila a fila y un `schema.sql` escrito a mano, con
más tiempo y más riesgo del que permite el plazo de entrega acotado del
challenge.

Revisa la decisión "tentativa" de `docs/PRODUCT.md` (fila "Modelo de
concurrencia (A5)") — ver esa tabla, sección `## Cambios`, para el
registro cronológico completo.

La enumeración de campos de arriba es ilustrativa (para justificar JPA
sobre R2DBC), no exhaustiva — el listado completo del payload de captura,
incluyendo Shiny, Held Item y los datos de origen (OT/Pokéball/fecha/nivel
inicial/ubicación), está en
`docs/specs/001-equipo-activo-y-baul.md` § "Valores por defecto del
payload de captura" (hallazgo H-06 de
`docs/reviews/adversarial-review-2026-09-21.md`).

## D2 — Constantes de mecánica Pokémon (IV/EV/movimientos/naturaleza) hardcodeadas, no configurables

Quedan como constantes de dominio en el código — no como propiedades de
`TrainerConfigurationProperties` — el rango de IV (0-31), los topes de EV
(510 total / 252 por stat), el tamaño del set de movimientos (4) y el
modificador de naturaleza (±10%). Son la definición misma de esos
conceptos tal como los fija `docs/PRD.md` (BR-002 a BR-005, BR-008) — no
aparecen en la lista de ambigüedades A1-A12, están dados como regla fija
en el enunciado del challenge, no como una política de negocio de esta
entrega. Afecta a varios validadores (`skaro.trainer.domain`), por eso se
registra acá y no solo como comentario puntual — ver
`docs/specs/001-equipo-activo-y-baul.md` § Parametrización para la tabla
completa de valores y sus justificaciones una por una.

Descartado: exponerlos como propiedades configurables junto a
`team-limit`/`box-limit` (aplicar la regla de "sin hardcode" de forma
literal a todo número del dominio) — se descarta porque cambiarlos en
runtime no representaría ninguna variación de negocio válida, solo un
sistema que ya no implementa las reglas que el PRD pide. Es la misma
categoría de excepción que da `~/.claude/CLAUDE.md` como ejemplo legítimo
("un enum que define un contrato del dominio").

## D3 — Exclusión de escrituras concurrentes: lock pesimista sobre `Trainer`, no lock optimista ni constraint de base

El invariante de capacidad (6 en Equipo Activo, 300 en Baúl) se valida
contando ejemplares antes de insertar/mover — un check-then-act clásico.
Bajo escrituras concurrentes contra el mismo `Trainer`, dos requests
podrían leer el mismo conteo y ambas insertar, superando el límite
(hallazgo H-03 de `docs/reviews/adversarial-review-2026-09-21.md`). Se
eligió que `TrainerRosterService` tome un lock pesimista sobre la fila del
`Trainer` (`@Lock(LockModeType.PESSIMISTIC_WRITE)` al leerlo) dentro de la
misma transacción que cuenta, decide destino e inserta/actualiza — todo el
ciclo check→insert queda serializado a nivel de fila para ese entrenador.

Descartado:
- **Lock optimista** (`@Version` en `Trainer` + reintento en el service
  ante `OptimisticLockException`) — con un único `Trainer` en esta
  entrega, toda escritura concurrente colisiona contra la misma fila, así
  que la tasa de reintento esperable es alta; suma complejidad de
  reintento sin beneficio de concurrencia real (no hay múltiples
  entrenadores entre los que repartir la contención).
- **Constraint o trigger a nivel de base** (H2) que valide el conteo — es
  la opción más correcta en teoría (invariante garantizado por la propia
  base), pero el costo de escribir y mantener un trigger en H2 no se
  justifica frente al lock pesimista, dado el plazo de entrega acotado del
  challenge y que el modelo ya usa JPA con transacciones explícitas.

Con múltiples entrenadores reales (fuera de esta entrega, ver A9 en
`docs/PRODUCT.md`), el lock pesimista sigue siendo válido: cada `Trainer`
tiene su propia fila, así que la serialización es por entrenador, no
global.
