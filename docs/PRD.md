# PRD — Desafío Técnico Betwarrior Java Developer

## 1. Fuente

- **Origen:** archivo adjunto `Desafío Técnico Betwarrior Java Developer.pdf` (ubicado en `.claude/information/`).
- **Fecha:** No especificado en la fuente.
- **Documentos/materiales utilizados:** únicamente el PDF mencionado. El documento incluye un hipervínculo al texto "pokeapi-reactor" (presumiblemente al repositorio base a descargar), pero la URL no está disponible como texto plano en el material extraído.
- No hay registro de intercambios adicionales (mail, llamada) más allá de este documento. Todo lo que no esté en este PDF no existe como requisito.

La fuente original es la autoridad de este PRD.

## 2. El encargo en una frase

Diseñar e implementar, sobre el proyecto heredado `pokeapi-reactor`, los endpoints y la capa de persistencia necesarios para gestionar Pokémon individuales de un entrenador (con genética y metadata propias de cada ejemplar), diferenciando el Equipo Activo del Baúl de Almacenamiento.

## 3. Contexto

**Hechos (provistos por el evaluador):**
- El punto de partida es un proyecto heredado, `pokeapi-reactor`, que consume la API pública de PokéAPI.
- El sistema "requiere una evolución estructural importante" (sin especificar en qué consiste esa evolución más allá de la funcionalidad pedida).
- El desarrollo con asistencia de IA está "totalmente permitido y recomendado".
- El evaluador explicita que, dados los límites de tiempo, prioriza "la calidad del diseño por sobre la completitud": valora claridad arquitectónica, buenas prácticas, decisiones y trade-offs evaluados, y la visión de evolución del sistema, por encima de un proyecto 100% ejecutable.

**Inferencias:** ninguna en esta sección — ver [§13 Información no especificada](#13-información-no-especificada) para lo que el contexto no cubre.

## 4. Objetivos

| # | Objetivo | Tipo | Evidencia |
|---|---|---|---|
| O1 | Diseñar e implementar una nueva funcionalidad del dominio de negocio sobre el proyecto heredado | Explícito | "Tu objetivo es diseñar e implementar una nueva funcionalidad del dominio de negocio." |
| O2 | Garantizar la mantenibilidad del sistema | Explícito | "Es necesario garantizar la mantenibilidad, escalabilidad y la fácil incorporación de nuevos integrantes al desarrollo del proyecto." |
| O3 | Garantizar la escalabilidad del sistema | Explícito | Misma cita que O2. |
| O4 | Garantizar la fácil incorporación de nuevos integrantes al desarrollo | Explícito | Misma cita que O2. |
| O5 | Priorizar la calidad del diseño por sobre la completitud de la implementación | Explícito | "priorizamos la calidad del diseño por sobre la completitud." |
| O6 | Evidenciar claridad arquitectónica, buenas prácticas, decisiones/trade-offs evaluados y visión de evolución del sistema | Explícito | "valoramos la claridad arquitectónica, las buenas prácticas, las decisiones y trade-offs que evaluaste, y tu visión sobre cómo debería evolucionar el sistema." |

No se agregan objetivos adicionales por considerarse buenas prácticas generales no mencionadas en la fuente.

## 5. Requisitos funcionales

| ID | Requisito | Tipo | Evidencia |
|---|---|---|---|
| FR-001 | Diseñar e implementar endpoints y capa de persistencia para gestionar los Pokémon individuales de un entrenador | Explícito | "Diseñar e implementar los endpoints y la capa de persistencia necesaria para gestionar los Pokémon individuales de un entrenador..." |
| FR-002 | Diferenciar el Equipo Activo del Baúl de Almacenamiento | Explícito | "...diferenciando el Equipo Activo del Baúl de Almacenamiento." |
| FR-003 | Cada ejemplar debe soportar Valores Individuales (IVs): 6 estadísticas (Salud, Ataque, Defensa, Ataque Especial, Defensa Especial, Velocidad) que arrancan desde el valor 1, más un valor de "genética" adicional de entre 0 y 31 por estadística | Explícito | Sección "Datos relevantes para guardar", punto IVs. |
| FR-004 | Cada ejemplar debe soportar Valores de Esfuerzo (EVs) distribuidos entre las 6 estadísticas, con máximo 510 puntos totales y máximo 252 puntos en una sola estadística | Explícito | Punto "Valores de Esfuerzo (EVs)". |
| FR-005 | Cada ejemplar debe tener asignada una Naturaleza (1 de 25 posibles) que modifica estadísticas incrementando una en 10% y reduciendo otra en 10% | Explícito | Punto "Naturaleza". |
| FR-006 | Cada ejemplar debe tener asignada una Habilidad entre las que la especie posee en PokéAPI | Explícito | Punto "Habilidad". |
| FR-007 | Cada ejemplar debe soportar un flag de Variocolor (Shiny) | Explícito | Punto "Atributos Visuales y Género". |
| FR-008 | Cada ejemplar debe tener asignado un Género (Macho, Hembra o Sin Género) | Explícito | Punto "Atributos Visuales y Género". |
| FR-009 | Cada ejemplar debe registrar datos de origen/captura: ID del Entrenador Original (OT), Pokéball utilizada, fecha de captura, nivel inicial y ubicación de origen | Explícito | Punto "Datos de Origen (Metadata de Captura)". |
| FR-010 | Cada ejemplar debe soportar un set de hasta 4 movimientos seleccionados/aprendidos | Explícito | Punto "Set de Movimientos". |
| FR-011 | Cada ejemplar puede sostener como máximo 1 objeto (Held Item) | Explícito | Punto "Objeto Equipado". |
| FR-012 | El Equipo Activo de un entrenador tiene un número máximo de ejemplares simultáneos | Explícito (valor no especificado) | "Cada entrenador puede llevar un máximo de ejemplares en su equipo activo al mismo tiempo." |
| FR-013 | El Baúl (PC Box) es un espacio de almacenamiento secundario con un límite máximo | Explícito (valor no especificado) | "Baúl (PC Box): Espacio de almacenamiento secundario con un límite máximo." |
| FR-014 | Debe existir un endpoint para crear/capturar nuevos Pokémon | Explícito | "Endpoints para crear/capturar nuevos Pokémon..." |
| FR-015 | Debe existir un endpoint para listar el Equipo Activo | Explícito | "...listar el equipo activo y el baúl..." |
| FR-016 | Debe existir un endpoint para listar el Baúl | Explícito | Misma cita que FR-015. |
| FR-017 | Debe existir uno o más endpoints para mover Pokémon entre Equipo Activo y Baúl | Explícito | "...y mover Pokémon entre el equipo activo y el baúl." |
| FR-018 | El endpoint de Captura/Creación debe permitir especificar los parámetros individuales del ejemplar (especie, IVs, EVs, naturaleza, movimientos, etc.) | Explícito | "Endpoint para dar de alta un nuevo Pokémon especificando sus parámetros individuales (especie, IVs, EVs, naturaleza, movimientos, etc.)..." |
| FR-019 | El endpoint de Captura/Creación debe asignar automáticamente el nuevo ejemplar al Equipo Activo o al Baúl según disponibilidad de espacio | Explícito | "...y asignándolo automáticamente al equipo activo o al baúl según la disponibilidad de espacio." |
| FR-020 | Debe existir un endpoint de Detalle de Ejemplar que consulte toda la metadata técnica, genética y de origen de un Pokémon particular mediante su identificador único | Explícito | "Endpoint para consultar toda la metadata técnica, genética y de origen de un Pokémon en particular mediante su identificador único." |
| FR-021 | El endpoint de Consulta de Equipo Activo debe retornar una vista compuesta que combine la metadata única del ejemplar (genética, origen, estado) con los atributos estáticos de la especie provenientes de PokéAPI | Explícito | "Debe retornar una vista compuesta que combine la metadata única del ejemplar almacenado (genética, origen, estado) con los atributos estáticos de la especie provenientes de PokéAPI." |
| FR-022 | Debe existir un endpoint de Consulta del Baúl que liste los ejemplares almacenados en el baúl del entrenador | Explícito | "Endpoint para listar los ejemplares almacenados en el baúl del entrenador." |
| FR-023 | El/los endpoint(s) de Traslado deben aplicar las validaciones de límite correspondientes al mover un ejemplar entre Equipo Activo y Baúl | Explícito | "...aplicando las validaciones de límite correspondiente." |
| FR-024 *(bonus opcional)* | Debe existir un endpoint de Evolución que reciba el ID del Pokémon a evolucionar y la especie destino deseada | Explícito | "Endpoint de Evolución: Crear un endpoint especificando el ID del Pokémon a evolucionar y la especie destino deseada." |
| FR-025 *(bonus opcional)* | El sistema debe validar que la especie destino sea una evolución válida y directa para el ejemplar, sin validar requisitos situacionales (nivel, objetos, felicidad) | Explícito | "Validación de Línea Evolutiva..." |
| FR-026 *(bonus opcional)* | Al evolucionar, el ejemplar debe reemplazar su habilidad previa por una de las habilidades permitidas para la nueva especie | Explícito | "Actualización de Habilidad..." |
| FR-027 *(bonus opcional)* | El ejemplar evolucionado debe preservar identidad, genética (IVs, EVs, naturaleza), objetos, historial y slot exacto dentro del Equipo Activo | Explícito | "Preservación de Estado y Lugar..." |
| FR-028 *(bonus opcional)* | Debe soportarse evolución tanto en líneas simples como ramificadas (ej. Eevee) | Explícito | "...contemplando tanto líneas de evolución simples como ramificadas (ej. Bulbasaur → Ivysaur → Venusaur frente a las múltiples ramas de Eevee)." |

## 6. Requisitos no funcionales

| ID | Requisito | Categoría | Tipo | Evidencia |
|---|---|---|---|---|
| NFR-001 | El sistema debe ser mantenible | Maintainability | Explícito | "Es necesario garantizar la mantenibilidad..." |
| NFR-002 | El sistema debe ser escalable | Scalability | Explícito | "...escalabilidad..." (sin métricas ni escenarios de carga especificados) |
| NFR-003 | El diseño debe facilitar la incorporación de nuevos integrantes al desarrollo del proyecto | Maintainability | Explícito | "...la fácil incorporación de nuevos integrantes al desarrollo del proyecto." |
| NFR-004 | Los endpoints deben manejar adecuadamente los códigos de respuesta HTTP y errores | Reliability | Explícito | "Se evalúa el diseño RESTful, la estructura de los payloads y el manejo adecuado de códigos de respuesta HTTP y errores." |
| NFR-005 | Los endpoints deben seguir un diseño RESTful | Maintainability | Explícito | Misma cita que NFR-004. |

No especificado en la fuente: valores concretos de performance (latencia, throughput), disponibilidad (SLA), seguridad (autenticación/autorización), ni métricas específicas de escalabilidad.

## 7. Reglas de negocio

| ID | Regla | Tipo | Evidencia |
|---|---|---|---|
| BR-001 | Las 6 estadísticas de un ejemplar arrancan desde el valor 1 | Explícito | "...arrancando desde el valor 1." |
| BR-002 | El valor de "genética" (IV) por estadística está entre 0 y 31 y se suma a la estadística individual | Explícito | "...puntos adicionales a las estadísticas individuales de entre 0 y 31." |
| BR-003 | Los EVs no pueden superar 510 puntos totales distribuidos entre las 6 estadísticas | Explícito | "Máximo 510 puntos totales a distribuir entre las 6 estadísticas..." |
| BR-004 | Los EVs no pueden superar 252 puntos en una sola estadística | Explícito | "...con un límite máximo de 252 puntos en una sola estadística." |
| BR-005 | La Naturaleza asignada incrementa una estadística en 10% y reduce otra en 10% | Explícito | "modifica estadísticas incrementando una en un 10% y reduciendo otra en un 10%." |
| BR-006 | La Habilidad asignada debe pertenecer al conjunto de habilidades que la especie posee en PokéAPI | Explícito | "1 habilidad asignada entre las posibles que la especie posee en PokéAPI." |
| BR-007 | Un ejemplar puede sostener como máximo 1 objeto (Held Item) a la vez | Explícito | "Capacidad de sostener como máximo 1 objeto (Held Item)." |
| BR-008 | Un ejemplar puede tener hasta 4 movimientos en su set | Explícito | "Lista de hasta 4 movimientos seleccionados/aprendidos." |
| BR-009 | El Equipo Activo tiene un número máximo de ejemplares (valor no especificado) | Explícito (valor no especificado) | "Cada entrenador puede llevar un máximo de ejemplares en su equipo activo al mismo tiempo." |
| BR-010 | El Baúl tiene un límite máximo de ejemplares (valor no especificado) | Explícito (valor no especificado) | "Espacio de almacenamiento secundario con un límite máximo." |
| BR-011 | Al capturar/crear un ejemplar, se asigna automáticamente a Equipo Activo o Baúl según disponibilidad de espacio | Explícito | "...asignándolo automáticamente al equipo activo o al baúl según la disponibilidad de espacio." |
| BR-012 *(bonus)* | La especie destino de una evolución debe ser una evolución válida y directa del ejemplar | Explícito | "Verificar que la especie elegida sea una evolución válida y directa para el ejemplar." |
| BR-013 *(bonus)* | No es necesario validar requisitos situacionales de evolución (nivel, objetos, felicidad) | Explícito | "No es necesario validar requisitos situacionales (nivel, objetos o felicidad), únicamente la validez del camino evolutivo." |
| BR-014 *(bonus)* | Al evolucionar, la habilidad previa se reemplaza por una habilidad permitida de la nueva especie | Explícito | "...el ejemplar debe reemplazar su habilidad previa por una de las habilidades permitidas para la nueva especie." |
| BR-015 *(bonus)* | El ejemplar evolucionado conserva identidad, genética (IVs, EVs, naturaleza), objetos, historial y slot exacto en el Equipo Activo | Explícito | "El Pokémon evolucionado debe mantener su identidad, genética (IVs, EVs, naturaleza), objetos, historial y slot exacto dentro del Equipo Activo." |
| BR-016 *(bonus)* | El soporte de evolución debe contemplar tanto líneas simples como ramificadas | Explícito | "...contemplando tanto líneas de evolución simples como ramificadas..." |

## 8. Entregables

- **Código fuente:** enlace a un repositorio **público** de GitHub con el proyecto refactorizado y la nueva funcionalidad implementada.
- **README.md:** modificación del README original del repositorio clonado para describir el trabajo realizado y las decisiones tomadas, incluyendo documentación del proyecto para su uso y mantenimiento.

No especificado en la fuente: canal concreto de envío del enlace (email, formulario, plataforma de reclutamiento), ni formato adicional de entrega (ej. presentación separada, video).

## 9. Restricciones

- El punto de partida obligatorio es la base de código de `pokeapi-reactor` ("Descargar la base de código de pokeapi-reactor").
- El repositorio de entrega debe ser público en GitHub.
- El desarrollo con asistencia de IA está explícitamente permitido y recomendado (no es una restricción negativa, sino una condición habilitante explícita del proceso).
- Existen límites de tiempo reconocidos por el evaluador, pero no se especifica un plazo concreto (fecha u horas) en la fuente.

No especificado en la fuente: stack tecnológico obligatorio a mantener o reemplazar, motor de persistencia requerido, necesidad de autenticación/autorización, tecnologías prohibidas.

## 10. Criterios de evaluación

### Explícitos
- Diseño RESTful de los endpoints, estructura de los payloads y manejo adecuado de códigos de respuesta HTTP y errores.
- Decisiones tomadas en base a la arquitectura del código y del sistema, en relación a las reglas de negocio y buenas prácticas.
- Mantenibilidad de las modificaciones realizadas.
- Alternativas posibles a la solución propuesta.
- Herramientas utilizadas.
- El trabajo será revisado por el equipo de desarrollo y **defendido en una charla técnica**.
- Claridad arquitectónica, buenas prácticas, decisiones/trade-offs evaluados, y visión de evolución del sistema (sección "Aclaración sobre el alcance").

### Inferidos
- Legibilidad y organización del código — *Inferido*. Evidencia: se desprende de la mención explícita de "buenas prácticas" y "mantenibilidad" como criterios, sin que el documento defina qué constituye código mantenible.
- Cobertura de tests automatizados sobre la nueva funcionalidad — *Inferido*. Evidencia: el proyecto heredado ya incluye suite de tests y reporte de cobertura (jacoco) como parte de su configuración base, y la mantenibilidad es un criterio explícito; sin embargo, el challenge no menciona tests como entregable ni criterio de forma explícita.
- Coherencia entre el diseño nuevo y el código heredado existente (no romper lo que ya funciona) — *Inferido*. Evidencia: el proyecto se describe como "heredado" que "requiere una evolución estructural importante", lo que sugiere continuidad, pero el documento no dice explícitamente que deba preservarse el comportamiento existente.

## 11. Ambigüedades

| ID | Ambigüedad | Posibles interpretaciones | Riesgo |
|---|---|---|---|
| A1 | No se especifica el número máximo de Pokémon del Equipo Activo | (a) valor canónico de los juegos oficiales (6); (b) valor de configuración arbitrario/parametrizable; (c) valor a definir libremente por el candidato | Elegir un valor no alineado con lo que el evaluador espera, o no dejarlo configurable cuando sí se esperaba |
| A2 | No se especifica el límite máximo del Baúl (PC Box) | Mismas interpretaciones que A1 | Mismo riesgo que A1 |
| A3 | No se especifica qué destino (Equipo Activo o Baúl) tiene prioridad al capturar un ejemplar cuando ambos tienen espacio disponible | (a) prioriza Equipo Activo hasta llenarlo; (b) prioriza Baúl; (c) es indistinto/configurable | Comportamiento del endpoint de captura no coincide con lo esperado |
| A4 | No se especifica el comportamiento (código HTTP, mensaje) cuando se intenta capturar o trasladar un ejemplar y no hay espacio disponible en ningún destino | (a) error 4xx con mensaje; (b) rechazo silencioso; (c) queda pendiente sin asignar | Justamente el manejo de errores es un criterio de evaluación explícito; un mal manejo afecta directamente ese criterio |
| A5 | No se especifica si el stack tecnológico actual (Java 11, Spring Boot 2.4.3, WebFlux, Maven) debe mantenerse o puede reemplazarse como parte de la "evolución estructural importante" | (a) mantener el stack y evolucionar sobre él; (b) migrar libremente a otro stack/versión | Condiciona toda la arquitectura de la solución |
| A6 | No se especifica el tipo de persistencia esperada | (a) base de datos relacional; (b) NoSQL; (c) almacenamiento en memoria | Afecta directamente la "capa de persistencia" pedida explícitamente |
| A7 | No se detallan las 25 naturalezas ni su efecto específico por estadística, ni si existen naturalezas neutras (sin efecto) | (a) las 25 modifican siempre una stat +10%/-10%; (b) existen naturalezas neutras como en los juegos oficiales, no mencionadas en la fuente | La tabla de naturalezas implementada podría no coincidir con la expectativa del evaluador |
| A8 | No queda claro si "arrancando desde el valor 1" (IVs) se refiere a la estadística base de la especie (dato de PokéAPI) o a un valor que el sistema debe generar/almacenar por ejemplar | (a) es la estadística base de especie ya provista por PokéAPI; (b) es un valor inicial propio del ejemplar que el sistema define | Afecta el modelo de datos de "Valores Individuales" |
| A9 | No se especifica si el "Entrenador" (OT) debe modelarse como una entidad de dominio propia con persistencia, o si es solo un identificador/campo de referencia | (a) entidad Entrenador completa (con equipo, baúl, etc. asociados); (b) simple campo identificador sin entidad propia | Afecta el alcance del modelo de dominio a diseñar |
| A10 | No se especifica el formato de "Ubicación de origen" en los datos de captura | (a) texto libre; (b) referencia a un recurso de tipo Location existente en PokéAPI | Afecta el diseño del payload de captura |
| A11 | No se especifica si la "fecha de captura" es provista por el cliente en el payload o generada automáticamente por el sistema | (a) provista por el cliente; (b) timestamp generado por el sistema al crear el registro | Afecta el contrato del endpoint de creación |
| A12 | No se especifica el peso o impacto del "Desafío Opcional" (bonus de evolución) en la evaluación final | (a) es puramente adicional, sin impacto si no se hace; (b) su ausencia resta puntos aunque se llame "opcional" | Afecta la priorización del esfuerzo del candidato |

## 12. Contradicciones

No se identificaron contradicciones internas en la fuente (no hay dos afirmaciones del documento que se opongan entre sí sobre un mismo hecho).

## 13. Información no especificada

- Valor numérico del límite máximo del Equipo Activo.
- Valor numérico del límite máximo del Baúl.
- Tabla completa de las 25 naturalezas y su efecto exacto por estadística; existencia o no de naturalezas neutras.
- Canal y formato concreto de entrega del enlace al repositorio (a quién se envía, por qué medio).
- Fecha límite concreta de entrega.
- Si el "Entrenador" requiere modelado como entidad de dominio propia, más allá del campo OT.
- Formato exacto del campo "Ubicación de origen" (texto libre vs. referencia a datos de PokéAPI).
- Si la "fecha de captura" la provee el cliente o la genera el sistema.
- Métricas o escenarios concretos de performance, disponibilidad y escalabilidad.
- Necesidad (o no) de autenticación/autorización de entrenadores en los endpoints.
- Motor o tecnología de persistencia esperada.
- Si el stack tecnológico actual (versión de Java/Spring Boot) debe conservarse o puede actualizarse.

## 14. Decisiones pendientes

- Definir valores concretos (o mecanismo de configuración) para los límites de Equipo Activo y Baúl.
- Elegir el motor/tipo de persistencia a utilizar.
- Definir si "Entrenador" es una entidad de dominio propia o un identificador simple, y su modelo de datos asociado.
- Definir la tabla completa de naturalezas y sus efectos sobre estadísticas.
- Definir si se implementa algún mecanismo de autenticación/autorización.
- Definir el esquema de payloads y el catálogo de respuestas/errores HTTP.
- Decidir si actualizar el stack tecnológico (versión de Java, Spring Boot) como parte de la evolución estructural, y hasta qué punto.
- Priorizar si se implementa el bonus de evolución y con qué nivel de profundidad, dado el límite de tiempo.

## 15. Fuera de alcance explícito

- Validación de requisitos situacionales de evolución (nivel, objetos o felicidad) al verificar la validez del camino evolutivo — aplica únicamente al desafío opcional de evolución: "No es necesario validar requisitos situacionales (nivel, objetos o felicidad), únicamente la validez del camino evolutivo."

No hay otras declaraciones explícitas de exclusión de alcance en la fuente. Cualquier otro tema no mencionado (ver §13) no debe interpretarse como "fuera de alcance", sino como no especificado.

## 16. Preguntas para el evaluador

| ID | Pregunta | Ambigüedad relacionada | Impacto |
|---|---|---|---|
| Q1 | ¿Cuál es el número máximo de Pokémon permitido en el Equipo Activo, y cuál es el límite máximo del Baúl? | A1, A2 | Alto — regla de negocio central y validaciones de todos los endpoints de gestión |
| Q2 | ¿Se espera algún mecanismo de autenticación/autorización para los endpoints, o pueden operar sin control de acceso? | — (no cubierta por ninguna ambigüedad de contenido, es una omisión total) | Alto — afecta diseño de API y modelo de seguridad |
| Q3 | ¿Debe mantenerse el stack tecnológico actual (Java 11, Spring Boot 2.4.3, WebFlux) o se puede migrar/actualizar como parte de la evolución estructural? | A5 | Alto — condiciona toda la arquitectura de la solución |
| Q4 | ¿Qué tecnología de persistencia se espera o se prefiere (relacional, NoSQL, en memoria)? | A6 | Alto — afecta directamente la capa de persistencia pedida |
| Q5 | ¿El Entrenador debe modelarse como entidad propia del dominio, o el OT es solo un identificador de referencia? | A9 | Medio-alto — define el alcance del modelo de dominio |
| Q6 | ¿Existe una fecha límite concreta de entrega? | — | Medio — afecta planificación del alcance dado el tiempo disponible |
| Q7 | ¿El bonus de evolución impacta en la evaluación si no se implementa, o es estrictamente adicional? | A12 | Medio — afecta priorización del esfuerzo |
| Q8 | ¿Cuál es el efecto específico de cada una de las 25 naturalezas sobre las estadísticas? | A7 | Medio — necesario para implementar la regla de negocio de Naturaleza con fidelidad |

## 17. Glosario

| Término | Significado |
|---|---|
| Ejemplar | Instancia individual de un Pokémon perteneciente a un entrenador, diferenciada de otras de la misma especie por su genética y metadata propia. |
| IVs (Valores Individuales) | Puntos de "genética" entre 0 y 31 que se suman a cada una de las 6 estadísticas base de un ejemplar. |
| EVs (Valores de Esfuerzo) | Puntos de entrenamiento distribuibles entre las 6 estadísticas; máximo 510 en total y 252 por estadística. |
| Naturaleza | Uno de 25 rasgos posibles de un ejemplar que incrementa una estadística en 10% y reduce otra en 10%. Efecto detallado por naturaleza: significado no especificado. |
| Equipo Activo | Conjunto de Pokémon que un entrenador lleva consigo activamente, con un máximo de ejemplares: valor no especificado. |
| Baúl / PC Box | Espacio de almacenamiento secundario de Pokémon de un entrenador, con un límite máximo: valor no especificado. |
| OT (Original Trainer / Entrenador Original) | Identificador del entrenador que capturó/creó originalmente el ejemplar. Si corresponde a una entidad de dominio propia: significado no especificado. |
| Held Item (Objeto Equipado) | Objeto que un ejemplar puede sostener; máximo 1 por ejemplar. |
| Shiny (Variocolor) | Flag visual especial de un ejemplar. Efecto o implicancia más allá del flag: significado no especificado. |
| Línea evolutiva | Secuencia de especies relacionadas por evolución; puede ser simple (lineal) o ramificada (múltiples destinos posibles desde una misma especie). |

---

## Resumen de cobertura

- Requisitos funcionales encontrados: 28
- Requisitos no funcionales encontrados: 5
- Reglas de negocio encontradas: 16
- Restricciones encontradas: 4 (más 4 no especificadas)
- Ambigüedades encontradas: 12
- Contradicciones encontradas: 0
- Elementos relevantes no especificados: 12
