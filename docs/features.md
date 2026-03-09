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
- Implementacion tecnica: `TrainingLifecycleEventBus` publica `TrainingLifecycleEvent` y `MainWindow`/servicios de aplicacion consumen eventos via handlers registrados.

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

## Estado operativo actual
- WIP objetivo: 1 ticket en `in_progress`.
- Backlog objetivo: al menos 5 tickets listos.
- Ticket activo actual: `SCAPE-0022`.
- Siguiente foco tecnico de backlog: `SCAPE-0028` -> `SCAPE-0029` -> `SCAPE-0030` -> `SCAPE-0031` -> `SCAPE-0032`.
- Validacion Tasker (2026-03-09): `in_progress=1` (`SCAPE-0022`) y `backlog=5` (`SCAPE-0028`..`SCAPE-0032`).
- Validacion de repositorio (2026-03-09): sin commits nuevos que demuestren cierre funcional de `SCAPE-0022`; no se aplican transiciones de estado en esta iteracion.
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
