# Code review — segunda pasada (`chore/pre-baseline-setup`)

**Fecha:** 2026-09-22
**Skill aplicada:** `java21-reactive-code-review`
**Relación con la primera pasada:** este archivo complementa a
`code-review-2026-09-22.md`, no lo reemplaza ni lo edita (los reportes de
`docs/reviews/` son historial, no documentos vivos).
**Alcance de esta pasada:**
1. Verificación del fix aplicado para el hallazgo #3 del reporte anterior
   (`@Valid` → `@Validated`, ver detalle abajo).
2. Revisión de calidad del código agregado como parte de esa sesión de
   fixes: `TrainerConfiguration.java`, `TrainerConfigurationProperties.java`
   y el nuevo `TrainerRosterServiceTest.java` (hallazgo #6).
3. Segunda lectura completa del resto del diff (`git diff HEAD`) buscando
   lo que la primera pasada no haya cubierto.

**Modo:** mixto — dos limpiezas triviales y sin riesgo sobre código
escrito en la sesión anterior (orden de imports, comentarios que
referenciaban el archivo de reporte por nombre) se corrigieron directo;
el hallazgo nuevo de fondo (#9) se deja documentado, no aplicado, para
que la persona usuaria decida el fix como con el resto. `./mvnw test`:
47/47 verdes después de los ajustes.

---

## Verificación del hallazgo #3 (reporte anterior)

Confirmado correcto, con una corrección respecto al intento inicial: el
primer fix puso `@Validated` en el método `@Bean` de
`TrainerConfiguration` — se probó con un test descartable
(`SpringApplicationBuilder` + `--skaro.trainer.team-limit=0`) y el
contexto arrancaba igual, sin validar nada. Moviendo `@Validated` a la
propia clase `TrainerConfigurationProperties` (línea 19 ahora), el mismo
test sí falla con `BindValidationException` real ("rejected value [0]...
must be greater than or equal to 1"). Es el estado actual del código
(`src/main/java/skaro/trainer/config/TrainerConfigurationProperties.java:19-20`).
Test de verificación borrado tras confirmar — no se deja como test
permanente porque tira abajo el contexto completo de la app a propósito,
no es el tipo de test que corresponde en la suite normal.

---

## Hallazgo nuevo

### 9. [Media-Alta] `Nature.valueOf`/`Gender.valueOf` sin manejar en `TrainerRosterService.toEntity()`

Mismo patrón que el hallazgo #5 del reporte anterior (`Location.valueOf`
sin capturar en `PokemonController.move()`), pero acá en el dominio y con
más superficie de exposición: cualquier captura con un `nature` o
`gender` que no matchee exactamente uno de los valores del enum
(typo, mayúscula/minúscula ya cubierta por `toUpperCase()`, pero un
valor inexistente como `"nature": "cool"`) dispara una
`IllegalArgumentException` sin capturar.

`src/main/java/skaro/trainer/domain/TrainerRosterService.java:130,133-134`:
```java
instance.setNature(request.nature() != null ? Nature.valueOf(request.nature().toUpperCase()) : Nature.HARDY);
...
instance.setGender(
        request.gender() != null ? Gender.valueOf(request.gender().toUpperCase()) : Gender.GENDERLESS);
```

A diferencia del hallazgo #5 (en el controller, antes de tocar el
service), acá el `valueOf` corre **dentro** de `toEntity`, que se invoca
como el `instanceFactory` que `PokemonCaptureTransaction.execute()`
ejecuta ya con el lock pesimista tomado y el conteo de capacidad
resuelto (`PokemonCaptureTransaction.java:31-46`). El fallo llega igual
como señal de error hacia el controller (vía `Mono.fromCallable`,
correcto en cuanto al modelo reactivo), pero:

- No hay ningún `@ExceptionHandler` para `IllegalArgumentException` en
  `TrainerExceptionHandler` → termina en el manejador por defecto de
  Spring, probablemente un 500 en vez de un 422 semánticamente correcto
  (mismo tratamiento que ya reciben BR-002 a BR-008 vía
  `CaptureValidationException`).
- No hay ningún test — ni integration ni el nuevo
  `TrainerRosterServiceTest` — que ejercite un `nature`/`gender`
  inválido.
- Al pasar por el lock pesimista antes de fallar, un payload con
  `nature`/`gender` inválido *sí* toma y libera el lock de fila del
  `Trainer` para nada — no es un bug de concurrencia (la transacción
  hace rollback), pero es trabajo evitable si se valida antes, como ya
  se hace con EVs/IVs/moves/ability.

**Antes** (`TrainerRosterService.java:34-46`, validaciones actuales):
```java
public Mono<PokemonInstance> capture(Long trainerId, CaptureRequest request) {
    return Mono.defer(() -> {
        validateEvs(request.evs());
        validateIvs(request.ivs());
        validateMoves(request.moves());
        return resolveAbility(request)
                .flatMap(ability -> repository.capture(trainerId, properties.getTeamLimit(),
                        properties.getBoxLimit(), (trainer, location) -> toEntity(trainer, request, location, ability)));
    });
}
```

**Después** (agregar `validateNature`/`validateGender` al mismo grupo,
fallando rápido antes incluso de llamar a PokéAPI):
```java
public Mono<PokemonInstance> capture(Long trainerId, CaptureRequest request) {
    return Mono.defer(() -> {
        validateEvs(request.evs());
        validateIvs(request.ivs());
        validateMoves(request.moves());
        Nature nature = resolveNature(request.nature());
        Gender gender = resolveGender(request.gender());
        return resolveAbility(request)
                .flatMap(ability -> repository.capture(trainerId, properties.getTeamLimit(),
                        properties.getBoxLimit(),
                        (trainer, location) -> toEntity(trainer, request, location, ability, nature, gender)));
    });
}

private Nature resolveNature(String nature) {
    if (nature == null) {
        return Nature.HARDY;
    }
    try {
        return Nature.valueOf(nature.toUpperCase());
    } catch (IllegalArgumentException ex) {
        throw new CaptureValidationException("BR-XXX: la naturaleza no es válida");
    }
}

private Gender resolveGender(String gender) {
    if (gender == null) {
        return Gender.GENDERLESS;
    }
    try {
        return Gender.valueOf(gender.toUpperCase());
    } catch (IllegalArgumentException ex) {
        throw new CaptureValidationException("BR-XXX: el género no es válido");
    }
}
```
(`toEntity` pasa a recibir `Nature`/`Gender` ya resueltos en vez de
parsear strings — el `BR-XXX` es un placeholder: la spec 001 no tiene
hoy un código de regla para esto, habría que sumarlo o reusar un código
genérico de validación si `docs/specs/001-equipo-activo-y-baul.md` ya
previó uno.)

---

## Limpiezas aplicadas directo (código de la sesión anterior, sin riesgo)

Dos ajustes de higiene sobre código escrito en esta misma sesión de
fixes — no hallazgos nuevos sobre código de terceros, corregidos sin
pedir triage porque son mecánicos y no cambian comportamiento:

- **Orden de imports** en
  `TrainerConfigurationProperties.java`: el import de
  `org.springframework.validation.annotation.Validated` estaba agrupado
  junto a los `jakarta.validation.constraints.*` sin la convención de
  grupos que usa el resto del proyecto (`java.*` → blank → `org.springframework.*`
  → blank → el resto). Reordenado.
- **Comentarios que referenciaban el archivo de reporte por nombre**
  (`TrainerConfigurationProperties.java` y
  `TrainerRosterServiceTest.java`, ambos agregados en la sesión de fixes
  posterior a la primera pasada): citaban
  "hallazgo #3/#6 de docs/reviews/code-review-2026-09-22.md" en vez de
  explicar el motivo directamente en el propio comentario — mismo
  anti-patrón que "referenciar la tarea o el fix actual" en vez de la
  razón subyacente, agravado acá porque el archivo referenciado es un
  documento histórico inmutable, no algo que vaya a seguir existiendo
  con ese contenido como fuente de verdad del código. Reescritos para
  explicar el WHY sin la referencia.

---

## Resto del diff (segunda lectura)

Sin hallazgos nuevos sobre los archivos que ya cubrió la primera pasada
más allá del #9 de arriba. Se revisaron especialmente en busca de lo que
una primera pasada suele pasar por alto: duplicación entre
`validateEvs`/`validateIvs` (similar pero con reglas distintas — no
amerita abstraer, ya evaluado y descartado en la pasada anterior),
manejo de `null` en mappers, uso de `Optional`, y otros `valueOf`/parseo
de enums sin capturar — que es exactamente donde apareció el hallazgo
#9.

## Resumen

| # | Severidad | Estado | Archivo:línea | Resumen |
|---|---|---|---|---|
| 9 | Media-Alta | Nuevo, sin aplicar | `TrainerRosterService.java:130,133-134` | `Nature.valueOf`/`Gender.valueOf` sin manejar — mismo patrón que hallazgo #5, en el dominio |
| — | — | Verificado | `TrainerConfigurationProperties.java:19` | Fix del hallazgo #3 confirmado correcto (con corrección de ubicación de `@Validated`) |
| — | — | Limpiado | `TrainerConfigurationProperties.java`, `TrainerRosterServiceTest.java` | Orden de imports y comentarios que referenciaban el reporte por nombre |

`./mvnw test`: 47/47 verdes.
