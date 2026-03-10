# Scape AI - Functional Feature Log

## Iteration 2026-03-09

### SCAPE-0000 - Bootstrap inicial del proyecto
- Objetivo funcional: establecer la base minima ejecutable de Scape AI en Java.
- Alcance introducido:
- Estructura inicial por capas para UI, aplicacion, simulacion, IA y persistencia.
- Arranque base compatible con Spring Boot + JavaFX.
- Regla de trabajo inicial sobre rama `features-nightly-20260309`.

### SCAPE-0001 - Esqueleto DDD y arranque unificado
- Objetivo funcional: habilitar un slice tecnico DDD que conecte dominio, aplicacion e infraestructura.
- Alcance introducido:
- Contratos iniciales para contexto de maze, simulacion y politica IA.
- Preparacion de bootstrap para servicios de aplicacion y vista principal.
- Convencion de navegacion JavaFX sin logica de dominio en controladores UI.

### SCAPE-0002 - Shell JavaFX estilo control panel
- Objetivo funcional: entregar la pantalla principal con paneles funcionales base.
- Alcance introducido:
- Layout principal con panel de controles, panel de laberinto y panel de metricas.
- Tema visual oscuro con acentos neon.
- Eventos base de controles (iniciar, pausar, reset) hacia la capa de aplicacion.

### SCAPE-0003 - Nucleo de simulacion de paso unico
- Objetivo funcional: ejecutar un paso determinista del agente en laberinto 2D.
- Alcance introducido:
- Validacion de movimiento con deteccion de colision.
- Registro de intentos invalidos y celdas visitadas.
- Evaluacion de condicion de salida alcanzada por iteracion.

### SCAPE-0004 - Politica IA base y contrato de recompensas
- Objetivo funcional: definir contratos de decision de movimiento y reward.
- Alcance introducido:
- Interfaz de politica para consumir contexto espacial y proponer direccion.
- Evaluador de recompensas con senales positivas, negativas y muy negativas.
- Integracion desacoplada del motor de simulacion respecto a libreria IA concreta.

### SCAPE-0005 - Persistencia inicial de laberintos y corridas
- Objetivo funcional: introducir persistencia minima para continuidad de aprendizaje.
- Alcance introducido:
- Entidades base de laberinto y corrida de entrenamiento.
- Registro de resultado resumido por corrida (exito, pasos, tiempo, recompensa).
- Consulta de historial basico de corridas por laberinto.

### SCAPE-0006 - Renderizado de laberinto en viewport JavaFX
- Objetivo funcional: visualizar el laberinto 2D operativo dentro del shell JavaFX.
- Alcance introducido:
- Renderizado de grilla con diferenciacion de muros, celdas transitables, inicio y salida.
- Refresco del viewport al cambiar el laberinto seleccionado sin reiniciar aplicacion.
- Base visual para integrar animacion del agente sobre el estado del motor.
- Implementacion tecnica: `MazeCatalogService` provee mazes predefinidos y `MazeViewportRenderer` dibuja la grilla en JavaFX con selector reactivo en `MainWindow`.

### SCAPE-0007 - Orquestador de episodio con limite temporal
- Objetivo funcional: cerrar episodios de simulacion por exito o timeout estandar.
- Alcance introducido:
- Coordinacion del bucle de pasos sobre `SingleStepSimulationEngine` hasta estado terminal.
- Politica de timeout configurable con valor por defecto de 5 minutos.
- Resultado unificado por episodio con exito, pasos, duracion y motivo de finalizacion.
- Implementacion tecnica: `SimulationEpisodeOrchestrator` ejecuta `SimulationStepFlow` hasta `EXIT_REACHED` o `TIMEOUT` y retorna `SimulationEpisodeResult`.

### SCAPE-0008 - Politica heuristica baseline enchufable
- Objetivo funcional: disponer de una politica de movimiento ejecutable sin dependencia de DJL.
- Alcance introducido:
- Implementacion baseline de `MovementPolicy` con heuristica espacial determinista.
- Estrategia anti-bucle inmediata basada en historial reciente de celdas.
- Seleccion de politica por configuracion para pruebas extremo a extremo del flujo actual.
- Implementacion tecnica: `MovementPolicyConfiguration` selecciona `SimpleMovementPolicy` via `scape.ai.policy=heuristic-baseline`.

### SCAPE-0009 - Registro persistente de metricas por episodio
- Objetivo funcional: guardar metricas detalladas de aprendizaje por episodio.
- Alcance introducido:
- Persistencia de recompensa acumulada, colisiones, nuevas celdas y distancia final a salida.
- Consulta cronologica de episodios recientes por laberinto.
- Compatibilidad del modelo de datos con `TrainingRunEntity` ya introducida.
- Implementacion tecnica: `training_runs` agrega columnas de metricas y `TrainingRunRepository` incorpora `findRecentByMazeId(mazeId, limit)`.

### SCAPE-0010 - Panel UI de metricas en tiempo real
- Objetivo funcional: exponer telemetria operativa del episodio en ejecucion dentro de la UI.
- Alcance introducido:
- Visualizacion en vivo de pasos, colisiones, recompensa acumulada y tiempo transcurrido.
- Actualizacion no bloqueante del panel durante ejecucion de simulacion.
- Reinicio limpio de metricas al comenzar un nuevo episodio.
- Implementacion tecnica: `InMemoryLiveMetricsService` emite snapshots periodicos y `MainWindow` aplica actualizaciones con `Platform.runLater`.

### SCAPE-0011 - Entrenador iterativo de episodios
- Objetivo funcional: ejecutar entrenamientos multi-episodio con metricas agregadas por corrida.
- Alcance introducido:
- Servicio de entrenamiento que encadena N episodios sobre el orquestador existente.
- Resumen agregado de aprendizaje con tasa de exito, recompensa media y colisiones medias.
- Cancelacion controlada desde capa de aplicacion sin bloqueo del hilo JavaFX.
- Implementacion tecnica: `DefaultIterativeEpisodeTrainingService` agrega estadisticas por corrida y `ApplicationTrainingExecutionService` ejecuta/cancela entrenamientos asincronos.

### SCAPE-0012 - Selector de algoritmo en panel de control
- Objetivo funcional: permitir seleccionar la politica de movimiento activa antes de simular.
- Alcance introducido:
- Selector UI de algoritmo con opciones baseline heuristica y aleatoria controlada.
- Propagacion de seleccion a capa de aplicacion para definir politica del siguiente episodio.
- Indicador visible del algoritmo activo durante la ejecucion.
- Implementacion tecnica: `ActiveMovementPolicyService` mantiene la politica activa y `MainWindow` incorpora selector + indicador `ACTIVE ALGORITHM`.

### SCAPE-0013 - Adaptador IA DJL con contrato MovementPolicy
- Objetivo funcional: habilitar integracion inicial con DJL manteniendo el desacoplamiento por contratos.
- Alcance introducido:
- Implementacion de `MovementPolicy` mediante adaptador DJL encapsulado en infraestructura IA.
- Traduccion de contexto espacial a prediccion de movimiento con fallback controlado.
- Frontera estable para evolucion futura del modelo sin romper simulacion ni UI.
- Implementacion tecnica: `DjlMovementPolicyAdapter` delega en `DjlDirectionPredictor` y aplica fallback heuristico ante predicciones invalidas o errores de inferencia.

### SCAPE-0014 - Persistencia de configuraciones de entrenamiento
- Objetivo funcional: guardar y reutilizar presets de entrenamiento reproducibles.
- Alcance introducido:
- Entidad `TrainingPreset` con episodios, timeout, politica y semilla opcional.
- Casos de uso para guardar, listar y cargar presets desde capa de aplicacion.
- Aplicacion de preset al iniciar nuevas corridas de entrenamiento.
- Implementacion tecnica: `JdbcTrainingPresetRepository` persiste `training_presets` y `MainWindow` aplica preset seleccionado en `Start`.

### SCAPE-0015 - Cargador de laberintos desde recursos JSON
- Objetivo funcional: importar catalogos de laberintos versionables para pruebas repetibles.
- Alcance introducido:
- Parser JSON con validacion estructural (dimensiones, inicio, salida).
- Mapeo de archivos a modelo de dominio reutilizable por motor y UI.
- Manejo de errores de formato con mensajes claros sin detener toda la aplicacion.
- Implementacion tecnica: `MazeJsonResourceLoader` carga `classpath:mazes/*.json` y `MazeCatalogService` expone `loadErrors()` para reportar fallos de parseo sin bloquear UI.

### SCAPE-0016 - Overlay de trayectoria del agente en el viewport
- Objetivo funcional: visualizar el recorrido del agente durante el episodio activo para interpretar su estrategia.
- Alcance introducido:
- Capa visual adicional en el viewport con celdas visitadas en orden temporal.
- Actualizacion en tiempo real de la trayectoria sin bloqueo del hilo JavaFX.
- Reinicio limpio del overlay al comenzar un nuevo episodio.
- Implementacion tecnica: `MazeViewportRenderer` agrega capa `trajectoryLayer` superpuesta y `MainWindow` coordina un ticker dedicado de trayectoria con actualizaciones via `Platform.runLater`.

### SCAPE-0017 - Control de velocidad de simulacion en UI
- Objetivo funcional: permitir ajustar la velocidad de ejecucion visual del episodio desde el panel de control.
- Alcance introducido:
- Control UI con niveles de velocidad (lento, normal, rapido) aplicables en tiempo real.
- Propagacion del factor de velocidad hacia la ejecucion en curso sin reiniciar el episodio.
- Indicador del nivel activo para mejorar trazabilidad operativa durante pruebas.
- Implementacion tecnica: `SimulationSpeed` centraliza perfiles de velocidad y `LiveMetricsService` + ticker de trayectoria en `MainWindow` aplican cambios en caliente.

### SCAPE-0018 - Detector de bucles con senal de penalizacion
- Objetivo funcional: detectar ciclos de movimiento repetitivo para penalizar exploracion improductiva.
- Alcance introducido:
- Deteccion de bucles cortos dentro de una ventana configurable de posiciones visitadas.
- Emision de senal negativa adicional consumible por el evaluador de recompensas.
- Exposicion de contador de eventos de bucle en el resultado de episodio.
- Implementacion tecnica: `SimulationEpisodeOrchestrator` mantiene ventana deslizante (`scape.simulation.loop-window`) y propaga `loopDetected` en `RewardContext`, persistiendo `loopEvents` en `SimulationEpisodeResult`.

### SCAPE-0019 - Estado contextual ampliado para politicas IA
- Objetivo funcional: enriquecer el contexto de decision para politicas de movimiento.
- Alcance introducido:
- Contrato de contexto extendido con vecindad local, direccion previa y racha sin progreso.
- Compatibilidad de politicas existentes mediante adaptacion o fallback de contrato.
- Pruebas de contrato para validar consistencia y estabilidad del nuevo contexto.
- Implementacion tecnica: `SpatialContext` expone `localNeighborhood`, `previousDirection` y `noProgressStreak`; el orquestador calcula estos campos en cada paso y `SpatialContextTest` valida el contrato.

### SCAPE-0020 - Persistencia de snapshot de politica por corrida
- Objetivo funcional: registrar la configuracion efectiva de politica por training run para reproducibilidad.
- Alcance introducido:
- Persistencia de identificador de politica y parametros efectivos serializados por corrida.
- Consulta de snapshots recientes vinculables con metricas historicas por laberinto.
- Compatibilidad retroactiva con corridas historicas ya persistidas.
- Implementacion tecnica: `training_runs` incorpora `policy_id` y `policy_snapshot`; `TrainingRunEntity`/`JdbcTrainingRunRepository` leen-escriben snapshot y el esquema incluye migracion idempotente para bases existentes.

### SCAPE-0021 - Caso de uso unificado Start Training Session
- Objetivo funcional: centralizar la preparacion y validacion del inicio de entrenamiento en capa de aplicacion.
- Alcance introducido:
- Caso de uso `StartTrainingSession` que valida maze, preset y politica activa antes de arrancar.
- Delegacion de `MainWindow` hacia este caso de uso para reducir logica distribuida en UI.
- Resultado de validacion tipado para presentar errores operativos de forma consistente.
- Implementacion tecnica: `ApplicationStartTrainingSessionUseCase` retorna `StartTrainingSessionResult` tipado y `MainWindow` consume ese resultado para arrancar o mostrar error en el estado del header.

### SCAPE-0022 - Bus de eventos de ciclo de entrenamiento
- Objetivo funcional: desacoplar la coordinacion del ciclo de entrenamiento mediante eventos de aplicacion tipados.
- Alcance introducido:
- Contratos de eventos para inicio, pausa, reanudacion, finalizacion y timeout de sesion.
- Suscripcion de UI y persistencia al flujo de eventos sin dependencias directas entre modulos.
- Base para telemetria y automatizacion de acciones post-episodio sin tocar el caso de uso principal.
- Implementacion tecnica: `TrainingLifecycleEventBus` publica `TrainingLifecycleEvent` tipado (`STARTED`, `PAUSED`, `RESUMED`, `FINISHED`, `TIMED_OUT`) y `MainWindow` consume el flujo para estado visual/overlay sin acoplarse a infraestructura.
- Integraciones desacopladas: `TrainingLifecycleTelemetrySubscriber` y `TrainingLifecyclePersistenceSubscriber` se registran al bus para reaccionar a ciclo sin cambiar `ApplicationStartTrainingSessionUseCase`.

### SCAPE-0023 - Reanudar episodio con checkpoint determinista
- Objetivo funcional: permitir pausar y continuar episodios sin perder consistencia del estado de simulacion.
- Alcance introducido:
- Snapshot minimo del episodio activo (posicion, trayectoria, metricas y tiempo restante).
- Restauracion determinista del estado para continuar el episodio en caliente.
- Validaciones de invariantes para evitar drift entre pausa y reanudacion.
- Implementacion tecnica: `EpisodeCheckpoint` encapsula estado serializable y `SimulationEpisodeOrchestrator` incorpora `pause()/resume(checkpoint)`.

### SCAPE-0024 - Trazas de inferencia DJL por decision
- Objetivo funcional: mejorar observabilidad de decisiones de politica DJL en ejecucion.
- Alcance introducido:
- Emision opcional de confianza, latencia y razon de fallback por decision.
- Contrato de trazas compatible con politicas no-DJL sin romper el flujo actual.
- Vinculacion de trazas al resultado de episodio para analisis posterior.
- Implementacion tecnica: `PolicyInferenceTrace` se agrega al pipeline de `DjlMovementPolicyAdapter` y se agrega en `SimulationEpisodeResult`.

### SCAPE-0025 - Timeline visual de entrenamiento en panel UI
- Objetivo funcional: visualizar episodios recientes en una linea temporal operativa dentro del dashboard.
- Alcance introducido:
- Lista cronologica de episodios con estado final, recompensa y duracion.
- Refresco incremental al cierre de cada episodio sin bloqueo del hilo JavaFX.
- Codificacion visual diferenciada para exito, timeout y cierre no exitoso.
- Implementacion tecnica: `TrainingTimelineViewModel` alimenta un componente timeline en `MainWindow` con snapshots de `LiveMetricsService`.

### SCAPE-0026 - Score de dificultad de laberinto persistente
- Objetivo funcional: priorizar escenarios de entrenamiento en funcion de complejidad estimada.
- Alcance introducido:
- Calculo de score de dificultad por maze usando dimensiones, densidad de muros y distancia minima a salida.
- Persistencia del score en catalogo para reuso entre ejecuciones.
- Ordenacion de selector de laberintos por dificultad ascendente o descendente.
- Implementacion tecnica: `MazeDifficultyScorer` calcula score y `MazeCatalogService` expone ordenacion por `difficultyScore`.

### SCAPE-0027 - Buffer de experiencia persistente para entrenamiento
- Objetivo funcional: almacenar transiciones SARSA para habilitar replay en iteraciones de IA futuras.
- Alcance introducido:
- Modelo persistente para transicion estado-accion-recompensa-estado_siguiente.
- Registro incremental durante episodios sin bloquear simulacion.
- Consulta paginada de transiciones recientes para entrenadores iterativos.
- Implementacion tecnica: `ExperienceTransitionEntity` y `ExperienceReplayRepository` soportan escritura append-only y lectura paginada.

### SCAPE-0028 - Selector de objetivo de entrenamiento por dificultad
- Objetivo funcional: definir dificultad objetivo para seleccionar automaticamente mazes acordes durante el inicio de entrenamiento.
- Alcance introducido:
- Selector UI con niveles `baja`, `media` y `alta` vinculado al caso de uso de inicio de sesion.
- Filtrado de mazes por `difficultyScore` persistido para elegir candidato valido por nivel.
- Mensaje operativo en UI cuando no existen mazes disponibles para el objetivo seleccionado.
- Implementacion tecnica propuesta: extender `StartTrainingSession` con `targetDifficulty` y resolver maze desde `MazeCatalogService` con estrategia de fallback vacio.

### SCAPE-0029 - Contrato de semilla reproducible por sesion
- Objetivo funcional: garantizar reproducibilidad de corridas usando una semilla efectiva comun para simulacion y politicas.
- Alcance introducido:
- Parametro de semilla opcional en configuracion de sesion con autogeneracion cuando no se informa.
- Fuente de aleatoriedad compartida entre motor de simulacion y politicas para eliminar divergencias.
- Exposicion de semilla efectiva en el resumen final de sesion para trazabilidad tecnica.
- Implementacion tecnica propuesta: introducir `TrainingSessionSeedContext` inmutable y propagarlo por `ApplicationTrainingExecutionService`.

### SCAPE-0030 - Modo headless de simulacion por lotes
- Objetivo funcional: ejecutar lotes de episodios sin inicializar JavaFX para acelerar validaciones de entrenamiento.
- Alcance introducido:
- Caso de uso batch con N episodios y resumen agregado reutilizando contratos actuales del dominio.
- Ejecucion sin renderizado ni dependencias de hilo de UI.
- Salida agregada con tasa de exito, recompensa media, colisiones medias y tiempo total.
- Implementacion tecnica propuesta: `HeadlessBatchTrainingUseCase` montado sobre `SimulationEpisodeOrchestrator` y `IterativeEpisodeTrainingService`.

### SCAPE-0031 - Estrategia epsilon-greedy sobre MovementPolicy
- Objetivo funcional: equilibrar exploracion y explotacion aplicando epsilon configurable sobre politicas existentes.
- Alcance introducido:
- Decorador de `MovementPolicy` que alterna entre decision base y movimiento exploratorio controlado.
- Contadores de decisiones por modo (`exploration`/`exploitation`) incorporados al resultado de episodio.
- Validacion centralizada del parametro epsilon en rango `[0,1]`.
- Implementacion tecnica propuesta: `EpsilonGreedyMovementPolicyDecorator` configurable desde `TrainingPreset` y `StartTrainingSession`.

### SCAPE-0032 - Vista comparativa de ultimas corridas
- Objetivo funcional: comparar rapidamente las ultimas corridas para evaluar tendencia reciente del entrenamiento.
- Alcance introducido:
- Tabla compacta en UI con las ultimas 10 corridas y columnas de exito, recompensa, colisiones y duracion.
- Ordenacion por fecha y recompensa sin bloqueo del hilo JavaFX.
- Reutilizacion de persistencia de metricas existente sin crear almacenamiento duplicado.
- Implementacion tecnica propuesta: `RecentRunsComparisonViewModel` alimentado por `TrainingRunRepository.findRecentByMazeId`.

### SCAPE-0033 - Router de suscriptores para eventos de entrenamiento
- Objetivo funcional: reducir acoplamiento entre UI, aplicacion y persistencia en la suscripcion a eventos de ciclo de entrenamiento.
- Alcance introducido:
- Registro centralizado de suscriptores por tipo de `TrainingLifecycleEvent`.
- Enlace de listeners de UI y persistencia mediante router, sin wiring directo modulo-a-modulo.
- Validacion de suscriptor duplicado para prevenir configuraciones ambiguas.
- Implementacion tecnica: `TrainingLifecycleSubscriberRouter` enruta eventos por tipo y aplica validacion fail-fast para suscriptor duplicado por evento.

### SCAPE-0034 - Indicador HUD de semilla y modo de ejecucion
- Objetivo funcional: aumentar trazabilidad operativa mostrando en UI la semilla efectiva y el modo activo de ejecucion.
- Alcance introducido:
- Header con metadatos de sesion (`seed` y modo `visual/headless`) visibles durante la corrida.
- Actualizacion de metadatos al iniciar sesion y limpieza al reset.
- Integracion no bloqueante con el hilo JavaFX durante Start/Pause/Reset.
- Implementacion tecnica: `MainWindow` expone etiquetas HUD `SEED` y `MODE`, actualizadas en `StartTrainingSessionResult` y limpiadas al evento `FINISHED` originado por reset.

### SCAPE-0035 - Metrica de progreso neto por episodio
- Objetivo funcional: complementar exito/fallo con una metrica cuantitativa de avance hacia la salida.
- Alcance introducido:
- Calculo de `netProgress` a partir de distancia inicial/final y mejoras acumuladas por episodio.
- Exposicion de `netProgress` en `SimulationEpisodeResult` y persistencia de corridas.
- Ordenacion de comparativas de corridas por `netProgress` sin recalculo pesado en cliente.
- Implementacion tecnica: `SimulationEpisodeOrchestrator` calcula `netProgress`, `training_runs` lo persiste en `net_progress` y `RecentRunsComparisonService` habilita orden `BY_NET_PROGRESS`.

### SCAPE-0036 - Muestreador balanceado para experience replay
- Objetivo funcional: preparar iteraciones de IA con lotes de replay menos sesgados por tipo de resultado.
- Alcance introducido:
- Servicio de aplicacion para extraer lotes balanceados entre exito, timeout y colision.
- Reutilizacion del almacenamiento append-only actual sin romper contratos persistentes.
- Pruebas unitarias sobre datasets controlados para verificar balance minimo por categoria.
- Implementacion tecnica: `BalancedExperienceReplaySampler` clasifica `SUCCESS/TIMEOUT/COLLISION` y arma lotes balanceados reutilizando `ExperienceReplayRepository.findRecent(page,size)`.

### SCAPE-0037 - Inventario de mazes con estado de cobertura
- Objetivo funcional: visualizar cobertura de resolucion por maze y politica para priorizar backlog de entrenamiento.
- Alcance introducido:
- Persistencia de estado `unsolved/solved` por combinacion de maze + policyId.
- Actualizacion automatica al cerrar episodios exitosos sin duplicados.
- Consulta de resumen de cobertura para destacar mazes pendientes en UI.
- Implementacion tecnica: `maze_policy_coverage` + `JdbcMazeCoverageRepository` persisten cobertura por `maze_id+policy_id`; `JdbcTrainingRunRepository` hace upsert automatico y `MainWindow` consulta `MazeCoverageSummaryService` para resaltar pendientes.

## Estado operativo actual
- WIP objetivo: 1 ticket en `in_progress`.
- Backlog objetivo: al menos 5 tickets listos.
- Ticket activo actual: `SCAPE-0022`.
- Siguiente foco tecnico de backlog: `SCAPE-0033` -> `SCAPE-0034` -> `SCAPE-0035` -> `SCAPE-0036` -> `SCAPE-0037`.
- Validacion Tasker (2026-03-09): `in_progress=1` (`SCAPE-0022`) y `backlog=5` (`SCAPE-0033`..`SCAPE-0037`).
- Validacion de repositorio (2026-03-09): no se detectan commits nuevos que demuestren cierre funcional adicional de `SCAPE-0022`; no se aplican transiciones de estado en esta iteracion.
- Implementacion tecnica: bootstrap JavaFX con ciclo de vida de contexto Spring Boot y `MainWindow` gestionada como componente Spring.
- Implementacion tecnica: `MainWindow` con panel de control, viewport de laberinto y panel de metricas; botones `Start/Pause/Reset` publican comandos a la capa de aplicacion.
- Implementacion tecnica: motor `SingleStepSimulationEngine` con validacion de colisiones, conteo de intentos invalidos, seguimiento de celdas visitadas y deteccion de salida.
- Implementacion tecnica: contratos `MovementPolicy` y `RewardEvaluator` con senales `POSITIVE`, `NEGATIVE` y `VERY_NEGATIVE`, consumidos por `SimulationStepFlow` desacoplado de librerias IA concretas.
- Implementacion tecnica: entidades `MazeEntity` y `TrainingRunEntity` con repositorios JDBC para guardar corridas resumidas y listar historial por laberinto.

## Ejecucion implementador 2026-03-09 (nightly)

### Tickets cerrados en esta ejecucion
- SCAPE-0023: motor con `EpisodeCheckpoint` para pausa/reanudacion determinista, incluyendo trayectoria, contadores y tiempo restante con pruebas de invariantes.
- SCAPE-0024: trazas opcionales de inferencia (`PolicyInferenceTrace`) para politicas DJL, integradas por paso y agregadas a `SimulationEpisodeResult`.
- SCAPE-0025: timeline visual de episodios recientes en UI con estado, recompensa y duracion, actualizada al cerrar episodio sin bloquear JavaFX.
- SCAPE-0026: score persistente de dificultad de laberinto (`difficulty_score`) calculado por `MazeDifficultyScorer` y ordenacion asc/desc en selector UI.
- SCAPE-0027: buffer persistente de transiciones SARSA (`experience_transitions`) con escritura asincrona desde orquestador y consulta paginada.

### Estado MCP tras la ejecucion
- projectId=5, userId=1.
- Tickets movidos a `done`: `SCAPE-0023`, `SCAPE-0024`, `SCAPE-0025`, `SCAPE-0026`, `SCAPE-0027`.

## Iteracion PO 2026-03-09 (seguimiento)

### Validacion MCP de la iteracion
- projectId=5, userId=1.
- `in_progress=1`: `SCAPE-0022`.
- `backlog=5`: `SCAPE-0028`, `SCAPE-0029`, `SCAPE-0030`, `SCAPE-0031`, `SCAPE-0032`.
- `done` confirmado para `SCAPE-0000` y `SCAPE-0001`..`SCAPE-0021`, `SCAPE-0023`..`SCAPE-0027`.

### Validacion de repositorio local
- Rama activa obligatoria verificada: `features-nightly-20260309`.
- HEAD actual: `1437c59` (`docs: record SCAPE-0028 to SCAPE-0032 backlog scope`).
- No se detectan commits nuevos que evidencien cierre funcional adicional de `SCAPE-0022`; no se aplican transiciones de estado en esta iteracion.

## Ejecucion implementador 2026-03-09 (automation cycle 2)

### Tickets cerrados en esta ejecucion
- SCAPE-0028: selector UI de dificultad objetivo (`baja/media/alta`) en panel de control y filtro de maze candidato por terciles de `difficultyScore` desde `MazeCatalogService`, con error accionable cuando no hay candidatos.
- SCAPE-0029: `StartTrainingSession` acepta semilla opcional, genera semilla efectiva cuando falta, la publica en `StartTrainingSessionResult` y sincroniza fuente aleatoria compartida (`SessionRandomSource`) consumida por simulacion y politicas.
- SCAPE-0030: nuevo caso de uso `HeadlessBatchTrainingUseCase` para ejecutar N episodios sin JavaFX y devolver agregado (`successRate`, `averageReward`, `averageCollisions`, `totalDurationMillis`).
- SCAPE-0031: decorador `EpsilonGreedyMovementPolicyDecorator` con epsilon configurable por `application.properties`, validacion de rango `[0,1]` y contadores de exploracion/explotacion en `SimulationEpisodeResult`.
- SCAPE-0032: vista compacta en UI de ultimas 10 corridas (`RecentRunsComparisonService`) con orden por fecha o recompensa y carga asíncrona (`recentRunsExecutor`) sin bloqueo del hilo JavaFX.

### Estado MCP tras la ejecucion
- projectId=5, userId=1.
- Tickets movidos a `done`: `SCAPE-0028`, `SCAPE-0029`, `SCAPE-0030`, `SCAPE-0031`, `SCAPE-0032`.

## Iteracion PO 2026-03-09 (automation cycle 3)

### Validacion MCP de la iteracion
- projectId=5, userId=1.
- `in_progress=1`: `SCAPE-0022`.
- `backlog=0` detectado al iniciar iteracion.
- Se crean tickets de backlog: `SCAPE-0033`, `SCAPE-0034`, `SCAPE-0035`, `SCAPE-0036`, `SCAPE-0037`.
- Estado final MCP tras creacion: `backlog=5` y `in_progress=1` (WIP objetivo mantenido).

### Validacion de repositorio local
- Rama activa obligatoria verificada: `features-nightly-20260309`.
- Sin evidencia en commits locales de cierre funcional adicional de `SCAPE-0022`; no se aplica transicion de estado.

### Resultado de gestion PO
- Backlog restaurado con 5 tickets verticales y variados (arquitectura, UI, simulacion/metrica, IA/replay, persistencia/cobertura).
- No se cambian alcances de tickets existentes ni se crean ramas.

## Ejecucion implementador 2026-03-10 (automation cycle 4)

### Tickets cerrados en esta ejecucion
- SCAPE-0039: telemetria de cobertura por cuadrantes (`Q1..Q4`) calculada por episodio en `SimulationEpisodeOrchestrator`, con exposicion adicional de cobertura izquierda/derecha en `SimulationEpisodeResult`.
- SCAPE-0039: panel de metricas UI ampliado con indicador en vivo `Coverage L/R`, actualizado por `LiveMetricsService` sin bloqueo del hilo JavaFX.
- SCAPE-0039: persistencia por corrida de cobertura de cuadrantes y lados (`q1_coverage`..`right_side_coverage`) en `training_runs`, incluyendo migracion idempotente y consumo en comparativa reciente.
- SCAPE-0040: `SimpleMovementPolicy` ahora usa ventana deslizante de posiciones recientes (`SpatialContext.recentPositions`) para castigar repeticiones locales y romper bucles cortos.
- SCAPE-0040: ante estancamiento (`noProgressStreak`), la politica prioriza movimientos con mayor potencial de cobertura nueva segun balance de lados.
- SCAPE-0040: benchmark de regresion agregado en `SimulationEpisodeOrchestratorTest` que compara contra baseline legado y valida mejora en `rightSideCoverage` y no incremento de `loopEvents`.
- SCAPE-0041: `RewardContext` se amplia con señales de cobertura (`discoveredNewCell`, `movedToUnderExploredSide`) y repeticion de transicion (`transitionRepeatCount`).
- SCAPE-0041: `DefaultRewardEvaluator` aplica reward shaping espacial con bono por nuevas celdas en zonas poco exploradas y penalizacion incremental por bucles repetidos.
- SCAPE-0041: pruebas de regresion agregadas para validar mejora de reward orientado a cobertura sin degradar `successRate` base.
- SCAPE-0042: nueva suite `CoverageRegressionSuiteTest` headless con semillas fijas (`20260310..20260313`) y ejecucion reproducible.
- SCAPE-0042: validacion automatica de umbrales minimos de `rightSideCoverage` y maximos de `loopEvents` por episodio.
- SCAPE-0042: al ejecutarse en `mvn test` (Surefire por defecto), cualquier regresion de cobertura rompe build local/proyecto y bloquea merge.
- SCAPE-0043: calculo de `pathEntropy` por episodio en `SimulationEpisodeOrchestrator` combinando distribucion de movimientos y frecuencia de celdas visitadas.
- SCAPE-0043: persistencia de `path_entropy` en `training_runs` (schema + migrador + repositorio JDBC) junto a metricas de corrida.
- SCAPE-0043: comparativa reciente expone entropia por corrida con alerta visual de baja entropia segun umbral configurable `scape.metrics.path-entropy-alert-threshold`.
- SCAPE-0044: se incorpora `mazeCoverageRatio` como metrica global por episodio (`MazeQuadrantCoverage`) y se propaga a `SimulationEpisodeResult`.
- SCAPE-0044: persistencia de `maze_coverage_ratio` en `training_runs` (schema + migrador + JDBC) manteniendo desglose por cuadrantes y lados.
- SCAPE-0044: `SimpleMovementPolicy` agrega guardia anti-oscilacion de trayectoria corta (patron `A->B->A->B`) y la regresion fija por semillas valida umbral minimo de cobertura derecha por episodio.
- SCAPE-0045: cierre de episodio por timeout endurecido en `SimulationEpisodeOrchestrator`, acotando `elapsedMillis` al limite configurado para evitar deriva bajo carga/reloj grueso.
- SCAPE-0045: `LiveMetricsService` extiende contrato con timeout activo para publicar `elapsed/remaining` consistentes y cierre forzado `TIMEOUT` en telemetria UI.
- SCAPE-0045: panel UI de metricas agrega `Remaining` y la suite de tests incluye regresion reproducible del bug de expiracion y validacion de timeline `TIMEOUT`.
- SCAPE-0046: se agrega `EpisodeTerminationResolver` como validador central para garantizar exclusividad entre `EXIT_REACHED`, `TIMEOUT` y `ABORTED`.
- SCAPE-0046: `SimulationEpisodeResult` pasa a exponer `terminationReason` y `terminatedAtEpochMillis` (con compatibilidad via `endReason()`), dejando trazabilidad terminal unica.
- SCAPE-0046: pruebas de borde cubren `timeout` en tick limite y salida en tick limite sin terminalidad doble ni ambigua.
- SCAPE-0047: `MazeViewportRenderer` incorpora capa opcional `unexploredOverlay` con intensidad por aislamiento/distancia respecto a celdas visitadas.
- SCAPE-0047: `MainWindow` agrega toggle `Unexplored Overlay` en panel de control y aplica activacion/desactivacion inmediata sin reiniciar sesion.
- SCAPE-0047: la superposicion se refresca en vivo junto al ticker de trayectoria, manteniendo el trabajo en hilo JavaFX via `Platform.runLater`.
- SCAPE-0048: nuevo modelo persistente `exploration_budgets` (`preset_id + policy_id`) con `initial_budget`, `consume_per_episode` y `remaining_budget`.
- SCAPE-0048: `ApplicationStartTrainingSessionUseCase` carga presupuesto activo por sesion y lo propaga a politicas compatibles via `ExplorationBudgetAwarePolicy` sin acoplar UI.
- SCAPE-0048: `ExplorationBudgetLifecycleSubscriber` consume/persiste remanente al cierre de episodio (`TIMED_OUT`/`FINISHED`) para continuidad entre sesiones del mismo preset/algoritmo.

## Iteracion PO 2026-03-10 (automation cycle 7)

### SCAPE-0038 - Diagnostico reproducible de bucle y sesgo de cobertura
- Objetivo funcional: aislar con reproducibilidad el sesgo de exploracion para guiar fixes sin ruido experimental.
- Alcance introducido:
- Escenario de referencia con semilla fija para reproducir cobertura desbalanceada.
- Telemetria minima exigida por episodio: `coverageRatio`, `rightSideCoverage`, `loopEvents`, `uniqueCellsVisited`.
- Salida documental con hipotesis tecnica principal basada en evidencia del runtime.
- Implementacion tecnica: `CoverageRegressionSuiteTest#shouldProvideDeterministicLoopAndCoverageBiasDiagnosticFromSeededRandomPolicy` recorre un catalogo fijo de semillas (`20260310..20260410`) sobre el maze de referencia y selecciona la primera semilla reproducible con sesgo y bucle.
- Registro por episodio consolidado en `SimulationEpisodeResult`: `mazeCoverageRatio` (equivalente operativo de `coverageRatio`), `rightSideCoverage`, `loopEvents` y `uniqueCellsVisited`.
- Hipotesis tecnica principal (evidencia runtime): la politica aleatoria controlada reinstancia el generador pseudoaleatorio en cada decision cuando se fija semilla por construccion, repitiendo patrones locales y provocando sesgo de cobertura hacia la mitad izquierda (`rightSideCoverage < leftSideCoverage` + `loopEvents > 0` en semilla fija del catalogo diagnostico).

### SCAPE-0044 - Garantizar exploracion completa del laberinto
- Objetivo funcional: elevar cobertura global y reducir estancamiento ciclico del agente.
- Alcance introducido:
- Metrica persistente `mazeCoverageRatio` con desglose por lado/cuadrante.
- Guardia anti-bucle validada sobre benchmark de regresion.
- Umbral minimo de cobertura del lado derecho en escenario de referencia con semilla fija.

### SCAPE-0045 - Corregir expiracion de timeout por episodio
- Objetivo funcional: asegurar cierre por `TIMEOUT` al alcanzar el limite configurado, incluso en condiciones de carga.
- Alcance introducido:
- Regla de cierre robusta por limite temporal en orquestacion de episodio.
- Prueba automatizada reproducible que captura el bug de no expiracion.
- Consistencia de `elapsed/remaining` entre runtime UI y persistencia al cerrar por timeout.

### SCAPE-0046 - Contrato determinista de cierre de episodio
- Objetivo funcional: eliminar estados terminales ambiguos y dejar trazabilidad unica del cierre por episodio.
- Alcance introducido:
- Validador central de exclusividad entre `EXIT_REACHED`, `TIMEOUT` y `ABORTED`.
- Persistencia de un unico `terminationReason` y marca temporal terminal coherente.
- Pruebas de borde para empate temporal entre salida y timeout sin doble terminalidad.

### SCAPE-0047 - Overlay de celdas no exploradas en viewport
- Objetivo funcional: hacer visible en UI las zonas no cubiertas para diagnostico rapido de sesgo espacial.
- Alcance introducido:
- Capa opcional `unexploredOverlay` en viewport JavaFX.
- Actualizacion en vivo por episodio sin bloqueo del hilo UI.
- Toggle inmediato desde panel de control para activar/desactivar la superposicion.

### SCAPE-0048 - Presupuesto de exploracion persistente por sesion
- Objetivo funcional: controlar exploracion inter-episodio mediante presupuesto persistente reutilizable por preset/algoritmo.
- Alcance introducido:
- Modelo de `explorationBudget` con valor inicial, consumo y remanente.
- Carga y propagacion desde `StartTrainingSession` hacia politicas compatibles.
- Persistencia de remanente al cierre de episodio para continuidad entre sesiones.

### Validacion MCP de la iteracion
- projectId=5, userId=1.
- `in_progress=1`: `SCAPE-0038` (WIP mantenido).
- `backlog=5`: `SCAPE-0044`, `SCAPE-0045`, `SCAPE-0046`, `SCAPE-0047`, `SCAPE-0048`.
- No se aplican transiciones de estado en esta iteracion porque el WIP ya cumple objetivo.

## Iteracion PO 2026-03-10 (automation cycle 8)

### SCAPE-0049 - Scheduler de entrenamiento por tandas con guardrails de UI
- Objetivo funcional: ejecutar tandas consecutivas reutilizando entrenamiento actual sin bloquear UI.
- Alcance introducido:
- `TrainingExecutionService` incorpora `startBatchTraining(...)` y `ApplicationTrainingExecutionService` implementa scheduler secuencial de tandas sobre `IterativeEpisodeTrainingService`.
- Cada tanda publica eventos `STARTED`/`FINISHED` en `TrainingLifecycleEventBus` con detalle `BATCH i/N`, habilitando observabilidad en timeline sin nuevos tipos de evento.
- `MainWindow` agrega selector de tandas (`1..5`) y dispara ejecución batch usando preset activo; `Reset` cancela entrenamiento en curso antes de emitir comando de reseteo para evitar sesiones huérfanas.

### SCAPE-0050 - Mini mapa de calor de celdas visitadas
- Objetivo funcional: visualizar densidad de visitas por celda de forma compacta durante la sesión activa.
- Alcance introducido:
- `MainWindow` incorpora bloque `VISIT HEATMAP` en panel de métricas con grilla compacta de celdas.
- El heatmap se recalcula incrementalmente desde el snapshot de trayectoria en cada tick del episodio sobre `Platform.runLater`, evitando bloqueo del hilo JavaFX.
- Se agrega toggle `Mini Heatmap: ON/OFF` en controles para ocultar/mostrar la visualización sin reiniciar sesión.

### SCAPE-0051 - Curriculum de dificultad adaptativa por tasa de éxito
- Objetivo funcional: ajustar automáticamente dificultad objetivo en función de éxito reciente de sesiones.
- Alcance introducido:
- Nuevo `AdaptiveDifficultyService` con ventana móvil configurable (`window-size`) y umbrales de promoción/degradación (`promote-threshold`, `demote-threshold`).
- La decisión respeta límites (`LOW..HIGH`) y se traza por sesión en el mensaje de inicio (`ADAPT X->Y`, `SR`, `n`), dejando evidencia operativa del ajuste.
- Configuración `scape.adaptive-difficulty.enabled` permite desactivar la estrategia y mantener comportamiento fijo.

### SCAPE-0052 - Snapshots de estado del episodio para replay debug
- Objetivo funcional: capturar snapshots compactos de diagnóstico para reconstrucción determinista de episodios problemáticos.
- Alcance introducido:
- `SimulationEpisodeOrchestrator` captura hitos `START`, `MIDPOINT`, `PRE_TIMEOUT` y `FINAL` con posición, contadores principales y semilla efectiva.
- `SimulationEpisodeResult` expone `debugSnapshots`, `replayMetadata` y serialización compacta JSON para persistencia (`debugSnapshotsJson`, `replayMetadataJson`).
- Persistencia preparada en `training_runs` con columnas `episode_debug_snapshots` y `replay_debug_metadata` (schema + migrador + JDBC).

### SCAPE-0053 - Índice de salud de entrenamiento persistente
- Objetivo funcional: consolidar una métrica única de salud por corrida para ordenar resultados y detectar regresiones.
- Alcance introducido:
- Fórmula versionada `TrainingHealthIndexFormula` (`v1.0.0`) basada en éxito, cobertura, entropía y `timeoutRatio` reciente.
- Persistencia de `training_health_index`, `health_index_formula_version` y `timeout_reached` en `training_runs` (schema + migrador + JDBC).
- Comparativa reciente (`RecentRunsComparisonService`) soporta orden por `BY_HEALTH_INDEX` y marca regresiones frente a ventana histórica previa.

### SCAPE-0052 - Snapshots de estado del episodio para replay debug
- Objetivo funcional: persistir snapshots ligeros en hitos del episodio para reproducir diagnosticos sin reejecutar el entrenamiento completo.
- Alcance introducido:
- Captura compacta por hitos (inicio, mitad, pre-timeout, final) con posicion del agente, metrica clave y semilla efectiva.
- Reconstruccion determinista de replay de diagnostico a partir de snapshot final + metadatos asociados.
- Restriccion de impacto: el guardado no debe degradar perceptiblemente la ejecucion estandar.

### SCAPE-0053 - Indice de salud de entrenamiento persistente
- Objetivo funcional: consolidar una metrica unica de salud para priorizar decisiones de producto y tecnica por evidencia.
- Alcance introducido:
- Calculo y persistencia de `trainingHealthIndex` combinando exito, cobertura, entropia y ratio de timeout.
- Comparativa reciente con orden por indice y señalizacion de regresiones frente a ventana anterior.
- Versionado explicito de formula para evolucion controlada sin romper historicos.

### SCAPE-0054 - Contrato de metadatos de replay entre sesiones
- Objetivo funcional: normalizar y versionar metadatos de replay para comparabilidad entre corridas y sesiones.
- Alcance introducido:
- Contrato estable de metadatos (`maze`, `policy`, `seed`, `terminationReason`, `mazeCoverageRatio`, `loopEvents`).
- Persistencia/consulta por `trainingRunId` para recuperar ultimos diagnosticos reproducibles.
- Compatibilidad futura garantizada mediante campo de version de contrato.

### SCAPE-0055 - Panel UI de diagnostico de timeout y cobertura
- Objetivo funcional: concentrar en una sola vista operativa las señales de cierre y cobertura del episodio en curso.
- Alcance introducido:
- Panel en tiempo real con `terminationReason`, `elapsed`, `remaining` y `mazeCoverageRatio`.
- Indicadores de alerta cuando hay timeout sin salida o cobertura por debajo de umbral configurable.
- Limpieza y reinicio del panel al comenzar una nueva sesion sin bloquear JavaFX.

### SCAPE-0056 - Smoke-run determinista previo al entrenamiento largo
- Objetivo funcional: cortar corridas largas defectuosas antes de consumir tiempo de entrenamiento.
- Alcance introducido:
- Ejecucion obligatoria de smoke-run con semilla fija y umbrales minimos de timeout/cobertura.
- Bloqueo del entrenamiento principal y registro de causa cuando el smoke-run falla.
- Continuidad del flujo normal cuando el smoke-run cumple criterios.

### SCAPE-0057 - Politica compuesta con fallback DJL->heuristica
- Objetivo funcional: mejorar estabilidad de decisiones con fallback controlado ante baja confianza o fallo de inferencia DJL.
- Alcance introducido:
- `CompositeMovementPolicy` con DJL como primario y politica heuristica determinista como fallback.
- Trazabilidad por decision indicando si se uso rama primaria o fallback.
- Pruebas reproducibles que validan reduccion de colisiones/bucles frente a fallo directo de inferencia.

## Iteracion PO 2026-03-10 (automation cycle 9)

### Validacion MCP de la iteracion
- projectId=5, userId=1.
- Estado inicial consolidado: `in_progress=1` (`SCAPE-0052`) y `backlog=2` (`SCAPE-0053` + ticket adicional previo), con correccion de lectura parcial inicial.
- Se crean tickets de backlog: `SCAPE-0054`, `SCAPE-0055`, `SCAPE-0056`, `SCAPE-0057`.
- Ajuste de WIP aplicado: `SCAPE-0054` se movio temporalmente a `in_progress` y se retorno a `backlog` para respetar `WIP=1` al detectarse `SCAPE-0052` activo.
- Estado final MCP confirmado: `in_progress=1` (`SCAPE-0052`) y `backlog=5` (`SCAPE-0053`, `SCAPE-0054`, `SCAPE-0055`, `SCAPE-0056`, `SCAPE-0057`).

### Validacion de repositorio local
- Repositorio operativo unico usado: `/Users/davidpe/dev/projects/scape-ai` (sin worktrees).
- Rama obligatoria confirmada: `features-nightly-20260309`.
- Actualizacion acumulativa aplicada en `docs/features.md` para mantener trazabilidad funcional del backlog vigente.

## Iteracion PO 2026-03-10 (automation cycle 10)

### SCAPE-0058 - Contrato unificado de configuracion de sesion
- Objetivo funcional: unificar la configuracion de sesion para evitar divergencias entre arranque UI y ejecucion headless.
- Alcance introducido:
- Definicion de `TrainingSessionConfig` versionado con campos minimos (maze, policy, timeout, seed, smoke-run, dificultad objetivo).
- Consumo obligatorio del mismo contrato por `StartTrainingSession`, flujo UI y modo headless.
- Validaciones centralizadas con errores tipados reutilizables en panel UI y logs operativos.

### SCAPE-0059 - Minimap de cobertura acumulada en runtime
- Objetivo funcional: exponer en tiempo real zonas ciegas de exploracion mediante un minimapa de cobertura acumulada.
- Alcance introducido:
- Componente UI de minimapa 2D con intensidad por frecuencia de visita durante episodio activo.
- Actualizacion no bloqueante en JavaFX y reinicio limpio al iniciar nueva sesion.
- Resaltado de salida y posicion actual del agente sobre el minimapa para diagnostico inmediato.

### Validacion MCP de la iteracion
- projectId=5, userId=1.
- Se crearon tickets en backlog: `SCAPE-0058` y `SCAPE-0059`.
- Transicion aplicada: `SCAPE-0056` -> `in_progress` para restaurar WIP operativo.
- Estado objetivo esperado tras la transicion: `in_progress=1` y `backlog=5`.
