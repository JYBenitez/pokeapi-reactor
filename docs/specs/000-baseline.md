# Spec 000 — Baseline

## Motivación
Congela el comportamiento observable heredado, documentado en
`docs/AS-IS.md` § Comportamiento observable, antes de construir la feature
nueva de Equipo Activo / Baúl (`docs/PRD.md`). Es el contrato que las specs
`001` en adelante no pueden romper sin una decisión explícita.

## Alcance del cambio
Cubre el comportamiento observable del cliente PokéAPI: resolución de
recursos por id/nombre, listados (paginados o no), seguimiento de links
(`NamedApiResource`), logging de requests salientes, deserialización
(snake_case y manejo de propiedades desconocidas), degradación de cache, y
propagación de errores HTTP.

No apunta a cubrir el 100% del repo — los ~90 DTOs sin lógica bajo
`skaro.pokeapi.resource.**` quedan fuera (ver `docs/AS-IS.md` § Zonas sin
cobertura). Se prioriza lo que las specs `001+` vayan a tocar.

Se construye **por etapas, un módulo/clase a la vez**; cada etapa es su
propio commit `test(baseline): ...`. El gate de F2 (no se pasa a `sdd-spec`
hasta cerrarlo) se cumple recién cuando la suite cubre todos los caminos
relevantes para el trabajo siguiente y está 100% verde.

## Comportamiento
### Se preserva
Todo lo listado en "Criterios de aceptación" más abajo. No hay sección
"Cambia": esta spec no introduce comportamiento nuevo, solo documenta el
que ya existe — bugs heredados incluidos, marcados como tales.

## Restricciones
Los characterization tests nacen en verde: documentan lo que el código
hace **hoy**. Si alguno naciera en rojo, es señal de que `docs/AS-IS.md`
describió mal ese comportamiento — se vuelve a arqueología para esa línea
puntual, no se fuerza el test.

## Fuera de alcance
Corregir cualquier comportamiento aquí descripto. Un cambio de
comportamiento va en una spec nueva (`001` en adelante) — esta spec no se
edita una vez cerrada.

## Criterios de aceptación

### `WebClientEntityFactory` (`skaro.pokeapi.client`)

#### Escenario: obtener un recurso por id o nombre
```gherkin
Dado un tipo de recurso T registrado con endpoint "/pokemon"
Cuando se pide getResource(T.class, "Mienfoobar")
Entonces se hace un GET a "/pokemon/Mienfoobar"
Y el Mono resuelve con el recurso deserializado del body de la respuesta
```
Cubierto por: `WebClientEntityFactoryTest#testGetResource`.

#### Escenario: obtener el listado completo de un tipo de recurso
```gherkin
Dado un tipo de recurso T registrado con endpoint "/move"
Cuando se pide getBaseResource(T.class) sin PageQuery
Entonces se hace un GET a "/move" sin parámetros de paginación
Y el Mono resuelve con la lista de NamedApiResource<T> del body
```
Cubierto por: `WebClientEntityFactoryTest#testGetBaseResource`.

#### Escenario: obtener el listado paginado de un tipo de recurso
```gherkin
Dado un tipo de recurso T registrado con endpoint "ability" y un PageQuery(limit=5, offset=10)
Cuando se pide getBaseResource(T.class, query)
Entonces se hace un GET a "ability?limit=5&offset=10"
Y el Mono resuelve con la lista de NamedApiResource<T> del body
```
Cubierto por: `WebClientEntityFactoryTest#testGetBaseResourceWithQuery`.

#### Escenario: seguir el link de un recurso y obtener el recurso completo
```gherkin
Dado un NamedApiResource<T> con una url absoluta a "/item/Leftovers"
Cuando se pide getNamedResource(resource, T.class)
Entonces se hace un GET directo a esa url, sin pasar por el registro de endpoints
Y el Mono resuelve con el recurso deserializado del body
```
Cubierto por: `WebClientEntityFactoryTest#getNamedResourceTest`.

#### Escenario: seguir una lista de links y obtener los recursos completos
```gherkin
Dado una lista de NamedApiResource<T> con urls absolutas a "/stat/attack" y "/stat/defense"
Cuando se pide getNamedResources(resources, T.class)
Entonces se hace un GET por cada url
Y el Flux emite cada recurso deserializado, sin orden garantizado (Flux.merge)
```
Cubierto por: `WebClientEntityFactoryTest#getNamedResourcesTest`.

#### Escenario: un error HTTP de PokéAPI se propaga sin traducir a error de dominio
```gherkin
Dado que PokéAPI responde 404 Not Found a un GET de recurso
Cuando se pide getResource(T.class, "inexistente")
Entonces el Mono termina en error con la excepción default de WebClient (WebClientResponseException.NotFound)
# No hay traducción a un error de dominio propio (dolor P8 de docs/AS-IS.md).
# Se preserva hasta que una spec lo cambie a propósito.
```
Cubierto por: `WebClientEntityFactoryTest#testGetResourcePropagatesHttpErrorWithoutTranslation`
(nuevo — no existía cobertura para este camino).
