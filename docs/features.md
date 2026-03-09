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

### SCAPE-0012 - Selector de algoritmo en panel de control
- Objetivo funcional: permitir seleccionar la politica de movimiento activa antes de simular.
- Alcance introducido:
- Selector UI de algoritmo con opciones baseline heuristica y aleatoria controlada.
- Propagacion de seleccion a capa de aplicacion para definir politica del siguiente episodio.
- Indicador visible del algoritmo activo durante la ejecucion.

### SCAPE-0013 - Adaptador IA DJL con contrato MovementPolicy
- Objetivo funcional: habilitar integracion inicial con DJL manteniendo el desacoplamiento por contratos.
- Alcance introducido:
- Implementacion de `MovementPolicy` mediante adaptador DJL encapsulado en infraestructura IA.
- Traduccion de contexto espacial a prediccion de movimiento con fallback controlado.
- Frontera estable para evolucion futura del modelo sin romper simulacion ni UI.

### SCAPE-0014 - Persistencia de configuraciones de entrenamiento
- Objetivo funcional: guardar y reutilizar presets de entrenamiento reproducibles.
- Alcance introducido:
- Entidad `TrainingPreset` con episodios, timeout, politica y semilla opcional.
- Casos de uso para guardar, listar y cargar presets desde capa de aplicacion.
- Aplicacion de preset al iniciar nuevas corridas de entrenamiento.

### SCAPE-0015 - Cargador de laberintos desde recursos JSON
- Objetivo funcional: importar catalogos de laberintos versionables para pruebas repetibles.
- Alcance introducido:
- Parser JSON con validacion estructural (dimensiones, inicio, salida).
- Mapeo de archivos a modelo de dominio reutilizable por motor y UI.
- Manejo de errores de formato con mensajes claros sin detener toda la aplicacion.

## Estado operativo actual
- WIP objetivo: 1 ticket en `in_progress`.
- Backlog objetivo: al menos 5 tickets listos.
- Siguiente foco tecnico: completar `SCAPE-0001` y ejecutar backlog en este orden sugerido `SCAPE-0011` -> `SCAPE-0012` -> `SCAPE-0013` -> `SCAPE-0014` -> `SCAPE-0015`.
- Implementacion tecnica: bootstrap JavaFX con ciclo de vida de contexto Spring Boot y `MainWindow` gestionada como componente Spring.
- Implementacion tecnica: `MainWindow` con panel de control, viewport de laberinto y panel de metricas; botones `Start/Pause/Reset` publican comandos a la capa de aplicacion.
- Implementacion tecnica: motor `SingleStepSimulationEngine` con validacion de colisiones, conteo de intentos invalidos, seguimiento de celdas visitadas y deteccion de salida.
- Implementacion tecnica: contratos `MovementPolicy` y `RewardEvaluator` con senales `POSITIVE`, `NEGATIVE` y `VERY_NEGATIVE`, consumidos por `SimulationStepFlow` desacoplado de librerias IA concretas.
- Implementacion tecnica: entidades `MazeEntity` y `TrainingRunEntity` con repositorios JDBC para guardar corridas resumidas y listar historial por laberinto.
