package com.davidpe.scapeai.ui;

import com.davidpe.scapeai.application.CellVisitFrequency;
import com.davidpe.scapeai.application.LiveEpisodeMetrics;
import com.davidpe.scapeai.application.LiveMetricsService;
import com.davidpe.scapeai.application.MazeCoverageSummaryRow;
import com.davidpe.scapeai.application.MazeCoverageSummaryService;
import com.davidpe.scapeai.application.MovementPolicyOption;
import com.davidpe.scapeai.application.PersistentMazeHeatmapService;
import com.davidpe.scapeai.application.PersistedAssetCatalogItem;
import com.davidpe.scapeai.application.PersistedAssetCatalogService;
import com.davidpe.scapeai.application.PersistedAssetCleanupService;
import com.davidpe.scapeai.application.PersistedAssetDeletionCandidate;
import com.davidpe.scapeai.application.PersistedAssetPreview;
import com.davidpe.scapeai.application.PersistedAssetPreviewService;
import com.davidpe.scapeai.application.PersistedAssetRef;
import com.davidpe.scapeai.application.PersistedAssetType;
import com.davidpe.scapeai.application.RecentRunComparisonRow;
import com.davidpe.scapeai.application.RecentRunsComparisonService;
import com.davidpe.scapeai.application.RecentRunsSortOption;
import com.davidpe.scapeai.application.SimulationControlService;
import com.davidpe.scapeai.application.SimulationSpeed;
import com.davidpe.scapeai.application.StartTrainingSessionCommand;
import com.davidpe.scapeai.application.StartTrainingSessionResult;
import com.davidpe.scapeai.application.StartTrainingSessionUseCase;
import com.davidpe.scapeai.application.SuccessfulEpisodeReplay;
import com.davidpe.scapeai.application.SuccessfulEpisodeResumeService;
import com.davidpe.scapeai.application.TrainingBudget;
import com.davidpe.scapeai.application.TrainingExecutionService;
import com.davidpe.scapeai.application.TrainingLifecycleEvent;
import com.davidpe.scapeai.application.TrainingLifecycleEventType;
import com.davidpe.scapeai.application.TrainingLifecycleSubscriberRouter;
import com.davidpe.scapeai.application.TrainingPreset;
import com.davidpe.scapeai.application.TrainingPresetOption;
import com.davidpe.scapeai.application.TrainingPresetService;
import com.davidpe.scapeai.application.TrainingSessionAsciiTrendRenderer;
import com.davidpe.scapeai.application.TrainingSessionConfig;
import com.davidpe.scapeai.application.TrainingSessionSummary;
import com.davidpe.scapeai.application.TrainingSessionSummaryService;
import com.davidpe.scapeai.application.TrainingTargetDifficulty;
import com.davidpe.scapeai.application.TrainingEpisodeDetail;
import com.davidpe.scapeai.application.TrainingEpisodeDetailService;
import com.davidpe.scapeai.application.TrainingTimelineEntry;
import com.davidpe.scapeai.persistence.TrainingRunEntity;
import com.davidpe.scapeai.persistence.TrainingSessionEntity;
import com.davidpe.scapeai.persistence.repository.TrainingRunRepository;
import com.davidpe.scapeai.persistence.repository.TrainingSessionRepository;
import com.davidpe.scapeai.simulation.GridPosition;
import com.davidpe.scapeai.simulation.MazeDefinition;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Alert;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class MainWindow {
  private static final String UI_FONT_FAMILY = "Consolas";
  private static final double FONT_SIZE_PANEL_TITLE = 18.0;
  private static final double FONT_SIZE_SECTION_LABEL = 12.0;
  private static final double FONT_SIZE_METRIC_LABEL = 12.0;
  private static final double FONT_SIZE_METRIC_VALUE = 13.0;
  private static final double FONT_SIZE_METRIC_VALUE_PRIORITY = 15.0;

  private final SimulationControlService controlService;
  private final StartTrainingSessionUseCase startTrainingSessionUseCase;
  private final TrainingExecutionService trainingExecutionService;
  private final TrainingPresetService trainingPresetService;
  private final LiveMetricsService liveMetricsService;
  private final RecentRunsComparisonService recentRunsComparisonService;
  private final PersistentMazeHeatmapService persistentMazeHeatmapService;
  private final PersistedAssetCatalogService persistedAssetCatalogService;
  private final PersistedAssetPreviewService persistedAssetPreviewService;
  private final PersistedAssetCleanupService persistedAssetCleanupService;
  private final MazeCoverageSummaryService mazeCoverageSummaryService;
  private final TrainingSessionRepository trainingSessionRepository;
  private final TrainingRunRepository trainingRunRepository;
  private final TrainingSessionSummaryService trainingSessionSummaryService;
  private final TrainingSessionAsciiTrendRenderer trainingSessionAsciiTrendRenderer;
  private final TrainingEpisodeDetailService trainingEpisodeDetailService;
  private final SuccessfulEpisodeResumeService successfulEpisodeResumeService;
  private final MazeCatalogService mazeCatalogService;
  private final MazeViewportRenderer mazeViewportRenderer;
  private final ScheduledExecutorService trajectoryScheduler =
      Executors.newSingleThreadScheduledExecutor(
          runnable -> {
            Thread thread = new Thread(runnable, "maze-trajectory-overlay");
            thread.setDaemon(true);
            return thread;
          });
  private final ExecutorService recentRunsExecutor =
      Executors.newSingleThreadExecutor(
          runnable -> {
            Thread thread = new Thread(runnable, "recent-runs-loader");
            thread.setDaemon(true);
            return thread;
          });
  private final List<GridPosition> trajectoryCells = new ArrayList<>();
  private final Object trajectoryLock = new Object();
  private Label stepsValue;
  private Label collisionsValue;
  private Label rewardValue;
  private Label elapsedValue;
  private Label remainingValue;
  private Label sideCoverageValue;
  private Label diagnosticTerminationValue;
  private Label diagnosticCoverageValue;
  private Label diagnosticAlertValue;
  private Label activePolicyValue;
  private Label activePresetValue;
  private Label activeSpeedValue;
  private Label effectiveMazeValue;
  private Label effectivePolicyValue;
  private Label effectiveSeedCardValue;
  private Label effectiveTimeoutValue;
  private Label effectiveDifficultyValue;
  private Label systemStatusValue;
  private Label sessionSeedValue;
  private Label executionModeValue;
  private Label reviewSessionMetaValue;
  private Label reviewSessionSummaryValue;
  private Label reviewEpisodeMetaValue;
  private Label episodeSeedDetailValue;
  private Label episodePolicyDetailValue;
  private Label episodeStepDetailValue;
  private Label episodeRewardDetailValue;
  private ComboBox<TrainingSessionEntity> reviewSessionsSelector;
  private VBox timelineEntriesBox;
  private VBox recentRunsEntriesBox;
  private VBox coverageEntriesBox;
  private VBox reviewEpisodesEntriesBox;
  private GridPane miniHeatmapGrid;
  private TextArea reviewAsciiArea;
  private TextArea reviewTrajectoryArea;
  private StackPane workspaceStack;
  private BorderPane dashboardPane;
  private VBox controlPanel;
  private VBox mazePanel;
  private VBox metricsPanel;
  private VBox reviewPanel;
  private VBox assetsPanel;
  private ComboBox<PersistedAssetType> assetTypeFilter;
  private ComboBox<String> assetStateFilter;
  private ListView<PersistedAssetCatalogItem> assetListView;
  private TextArea assetPreviewArea;
  private Label assetsStatusValue;
  private List<PersistedAssetCatalogItem> assetCatalogCache = List.of();
  private StackPane mazeViewport;
  private MazeDefinition selectedMaze;
  private String selectedMazeName;
  private volatile RecentRunsSortOption selectedRecentRunsSort = RecentRunsSortOption.BY_DATE;
  private GridPosition trajectoryCurrent;
  private ScheduledFuture<?> trajectoryTicker;
  private volatile boolean trajectoryRunning;
  private volatile boolean unexploredOverlayEnabled;
  private volatile boolean miniHeatmapEnabled = true;
  private volatile boolean episodeDetailsCollapsed;
  private volatile boolean startActionProcessing;
  private volatile boolean focusModeEnabled;
  private volatile boolean detailsCollapsedBeforeFocus;
  private volatile boolean replayModeActive;
  private volatile ViewportMode viewportMode = ViewportMode.LIVE;
  private volatile boolean sessionConfigLocked;
  private volatile TrainingTargetDifficulty selectedTargetDifficulty =
      TrainingTargetDifficulty.MEDIUM;
  private volatile HeatmapComparisonMode heatmapComparisonMode = HeatmapComparisonMode.SUPERPOSED;
  private volatile List<CellVisitFrequency> accumulatedHeatmapFrequencies = List.of();
  private volatile List<SuccessfulEpisodeReplay> replayEpisodes = List.of();
  private volatile LiveEpisodeMetrics lastLiveMetrics;
  private volatile String selectedReviewSessionId;
  private volatile Long selectedReviewRunId;
  private volatile int replayEpisodeIndex;
  private volatile int replayTrajectoryIndex;
  private volatile ScheduledFuture<?> replayTicker;
  private Label replayStatusValue;
  private ComboBox<ViewportMode> viewportModeSelector;
  private Button startButton;
  private Button pauseButton;
  private Button resetButton;
  private Button episodeDetailsToggleButton;
  private Button focusModeToggleButton;
  private HBox focusModeHud;
  private Label focusModeStatusValue;
  private Label focusModeElapsedValue;
  private Label focusModeRewardValue;
  private Label focusModeCollisionsValue;
  private Label contextModeValue;
  private Label contextSummaryValue;
  private Label contextDetailValue;
  private Label contextSignalValue;
  private VBox notificationLayer;
  private final Map<String, Long> notificationDedupe = new ConcurrentHashMap<>();
  private final double coverageAlertThreshold;
  private final int persistentHeatmapRuns;
  private final long notificationDurationMillis;
  private final long notificationDedupWindowMillis;
  private volatile Long activeSessionSeed;

  public MainWindow(
      SimulationControlService controlService,
      StartTrainingSessionUseCase startTrainingSessionUseCase,
      TrainingExecutionService trainingExecutionService,
      TrainingPresetService trainingPresetService,
      LiveMetricsService liveMetricsService,
      RecentRunsComparisonService recentRunsComparisonService,
      PersistentMazeHeatmapService persistentMazeHeatmapService,
      PersistedAssetCatalogService persistedAssetCatalogService,
      PersistedAssetPreviewService persistedAssetPreviewService,
      PersistedAssetCleanupService persistedAssetCleanupService,
      MazeCoverageSummaryService mazeCoverageSummaryService,
      TrainingSessionRepository trainingSessionRepository,
      TrainingRunRepository trainingRunRepository,
      TrainingSessionSummaryService trainingSessionSummaryService,
      TrainingSessionAsciiTrendRenderer trainingSessionAsciiTrendRenderer,
      TrainingEpisodeDetailService trainingEpisodeDetailService,
      SuccessfulEpisodeResumeService successfulEpisodeResumeService,
      MazeCatalogService mazeCatalogService,
      MazeViewportRenderer mazeViewportRenderer,
      TrainingLifecycleSubscriberRouter trainingLifecycleSubscriberRouter,
      @Value("${scape.ui.coverage-alert-threshold:0.35}") double coverageAlertThreshold,
      @Value("${scape.ui.persistent-heatmap-runs:12}") int persistentHeatmapRuns,
      @Value("${scape.ui.notification-duration-ms:2800}") long notificationDurationMillis,
      @Value("${scape.ui.notification-dedup-window-ms:900}") long notificationDedupWindowMillis) {
    this.controlService = controlService;
    this.startTrainingSessionUseCase = startTrainingSessionUseCase;
    this.trainingExecutionService = trainingExecutionService;
    this.trainingPresetService = trainingPresetService;
    this.liveMetricsService = liveMetricsService;
    this.recentRunsComparisonService = recentRunsComparisonService;
    this.persistentMazeHeatmapService = persistentMazeHeatmapService;
    this.persistedAssetCatalogService = persistedAssetCatalogService;
    this.persistedAssetPreviewService = persistedAssetPreviewService;
    this.persistedAssetCleanupService = persistedAssetCleanupService;
    this.mazeCoverageSummaryService = mazeCoverageSummaryService;
    this.trainingSessionRepository = trainingSessionRepository;
    this.trainingRunRepository = trainingRunRepository;
    this.trainingSessionSummaryService = trainingSessionSummaryService;
    this.trainingSessionAsciiTrendRenderer = trainingSessionAsciiTrendRenderer;
    this.trainingEpisodeDetailService = trainingEpisodeDetailService;
    this.successfulEpisodeResumeService = successfulEpisodeResumeService;
    this.mazeCatalogService = mazeCatalogService;
    this.mazeViewportRenderer = mazeViewportRenderer;
    this.coverageAlertThreshold = Math.max(0.0, Math.min(1.0, coverageAlertThreshold));
    this.persistentHeatmapRuns = Math.max(1, persistentHeatmapRuns);
    this.notificationDurationMillis = Math.max(900L, notificationDurationMillis);
    this.notificationDedupWindowMillis = Math.max(200L, notificationDedupWindowMillis);
    trainingLifecycleSubscriberRouter.register(
        "main-window",
        EnumSet.allOf(TrainingLifecycleEventType.class),
        this::onTrainingLifecycleEvent);
  }

  public void show(Stage stage) {
    BorderPane content = new BorderPane();
    content.setPadding(new Insets(20));
    content.setStyle("-fx-background-color: linear-gradient(to bottom right, #050812, #0d1122);");

    content.setTop(buildHeader());
    controlPanel = buildControlPanel();
    content.setLeft(controlPanel);
    dashboardPane = new BorderPane();
    mazePanel = buildMazePanel();
    metricsPanel = buildMetricsPanel();
    dashboardPane.setCenter(mazePanel);
    applyEpisodeDetailsPanelState();
    reviewPanel = buildReviewPanel();
    reviewPanel.setVisible(false);
    reviewPanel.setManaged(false);
    assetsPanel = buildAssetsPanel();
    assetsPanel.setVisible(false);
    assetsPanel.setManaged(false);
    workspaceStack = new StackPane(dashboardPane, reviewPanel, assetsPanel);
    content.setCenter(workspaceStack);

    ScrollPane mainScroll = new ScrollPane(content);
    mainScroll.getStyleClass().add("neon-scroll-pane");
    mainScroll.setFitToWidth(true);
    mainScroll.setFitToHeight(false);
    mainScroll.setPannable(true);
    mainScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    mainScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
    mainScroll.setStyle(
        "-fx-background-color: transparent;"
            + "-fx-control-inner-background: transparent;"
            + "-fx-background-insets: 0;"
            + "-fx-padding: 0;");
    refreshMiniHeatmap();
    liveMetricsService.subscribe(this::applyMetrics);
    liveMetricsService.subscribeTimeline(this::applyTimeline);
    refreshRecentRunsAsync();
    refreshCoverageSummaryAsync();
    refreshReviewSessionsAsync();
    refreshAssetCatalogAsync();

    notificationLayer = buildNotificationLayer();
    StackPane root = new StackPane(mainScroll, notificationLayer);
    StackPane.setAlignment(notificationLayer, Pos.TOP_RIGHT);
    StackPane.setMargin(notificationLayer, new Insets(16));

    Scene scene = new Scene(root, 1200, 760);
    installKeyboardNavigation(scene);
    installResponsiveLayout(scene);
    var neonScrollCss =
        getClass().getResource("/styles/neon-scroll.css");
    if (neonScrollCss != null) {
      scene.getStylesheets().add(neonScrollCss.toExternalForm());
    }
    stage.setTitle("Scape AI Control Panel");
    stage.setScene(scene);
    stage.show();
  }

  private HBox buildHeader() {
    Label title = new Label("SCAPE AI // CONTROL CONSOLE");
    title.setFont(Font.font("Consolas", 26));
    title.setTextFill(Color.web("#7ef9ff"));

    systemStatusValue = new Label("SYSTEM READY");
    systemStatusValue.setFont(Font.font("Consolas", 15));
    systemStatusValue.setTextFill(Color.web("#89ff9a"));

    sessionSeedValue = new Label("SEED: -");
    sessionSeedValue.setFont(Font.font("Consolas", 13));
    sessionSeedValue.setTextFill(Color.web("#9db2ff"));

    executionModeValue = new Label("MODE: VISUAL");
    executionModeValue.setFont(Font.font("Consolas", 13));
    executionModeValue.setTextFill(Color.web("#9db2ff"));

    Button dashboardButton = neonButton("Dashboard", "#7ef9ff", this::showDashboardMode);
    dashboardButton.setMinWidth(110);
    Button reviewButton = neonButton("Review", "#ffd166", this::showReviewMode);
    reviewButton.setMinWidth(110);
    Button assetsButton = neonButton("Assets", "#ffb86b", this::showAssetsMode);
    assetsButton.setMinWidth(110);

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    HBox header =
        new HBox(
            12,
            title,
            spacer,
            dashboardButton,
            reviewButton,
            assetsButton,
            sessionSeedValue,
            executionModeValue,
            systemStatusValue);
    header.setAlignment(Pos.CENTER_LEFT);
    header.setPadding(new Insets(0, 0, 18, 0));
    return header;
  }

  private VBox buildControlPanel() {
    Label title = panelTitle("Controls");
    Label algorithmLabel = new Label("ALGORITHM");
    algorithmLabel.setTextFill(Color.web("#9db2ff"));
    algorithmLabel.setFont(Font.font("Consolas", 12));
    ComboBox<MovementPolicyOption> algorithmSelector =
        new ComboBox<>(
            FXCollections.observableArrayList(controlService.availableMovementPolicies()));
    algorithmSelector.setMaxWidth(Double.MAX_VALUE);
    algorithmSelector.setStyle(
        "-fx-background-color: #101938;"
            + "-fx-text-fill: #c6d7ff;"
            + "-fx-border-color: #2cf1ff;"
            + "-fx-border-radius: 6;"
            + "-fx-background-radius: 6;");
    algorithmSelector.setCellFactory(
        ignored ->
            new javafx.scene.control.ListCell<>() {
              @Override
              protected void updateItem(MovementPolicyOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.label());
              }
            });
    algorithmSelector.setButtonCell(
        new javafx.scene.control.ListCell<>() {
          @Override
          protected void updateItem(MovementPolicyOption item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : item.label());
          }
        });
    selectActiveAlgorithm(algorithmSelector);
    installFocusStyle(algorithmSelector, "#7ef9ff");
    algorithmSelector
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (ignored, oldSelection, selected) -> {
              if (selected == null || selected.equals(oldSelection)) {
                return;
              }
              controlService.selectMovementPolicy(selected.id());
              updateActivePolicyLabel();
              refreshSessionConfigCardPreview();
              emitNotification(
                  "control.algorithm",
                  "Algorithm changed to " + selected.label() + ".",
                  "#7ef9ff");
            });

    activePolicyValue = new Label();
    activePolicyValue.setTextFill(Color.web("#89ff9a"));
    activePolicyValue.setFont(Font.font("Consolas", 12));
    updateActivePolicyLabel();

    Label presetLabel = new Label("TRAINING PRESET");
    presetLabel.setTextFill(Color.web("#9db2ff"));
    presetLabel.setFont(Font.font("Consolas", 12));
    ComboBox<TrainingPresetOption> presetSelector =
        new ComboBox<>(
            FXCollections.observableArrayList(controlService.availableTrainingPresets()));
    presetSelector.setMaxWidth(Double.MAX_VALUE);
    presetSelector.setStyle(
        "-fx-background-color: #101938;"
            + "-fx-text-fill: #c6d7ff;"
            + "-fx-border-color: #2cf1ff;"
            + "-fx-border-radius: 6;"
            + "-fx-background-radius: 6;");
    presetSelector.setCellFactory(
        ignored ->
            new javafx.scene.control.ListCell<>() {
              @Override
              protected void updateItem(TrainingPresetOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.label());
              }
            });
    presetSelector.setButtonCell(
        new javafx.scene.control.ListCell<>() {
          @Override
          protected void updateItem(TrainingPresetOption item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : item.label());
          }
        });
    selectActivePreset(presetSelector);
    presetSelector
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (ignored, oldSelection, selected) -> {
              if (selected == null || selected.equals(oldSelection)) {
                return;
              }
              controlService.applyTrainingPreset(selected.id());
              updateActivePolicyLabel();
              updateActivePresetLabel();
              selectActiveAlgorithm(algorithmSelector);
              refreshSessionConfigCardPreview();
            });

    activePresetValue = new Label();
    activePresetValue.setTextFill(Color.web("#89ff9a"));
    activePresetValue.setFont(Font.font("Consolas", 12));
    updateActivePresetLabel();

    Label speedLabel = new Label("SIMULATION SPEED");
    speedLabel.setTextFill(Color.web("#9db2ff"));
    speedLabel.setFont(Font.font("Consolas", 12));
    ComboBox<SimulationSpeed> speedSelector =
        new ComboBox<>(FXCollections.observableArrayList(SimulationSpeed.values()));
    speedSelector.setMaxWidth(Double.MAX_VALUE);
    speedSelector.setStyle(
        "-fx-background-color: #101938;"
            + "-fx-text-fill: #c6d7ff;"
            + "-fx-border-color: #2cf1ff;"
            + "-fx-border-radius: 6;"
            + "-fx-background-radius: 6;");
    speedSelector.setCellFactory(
        ignored ->
            new javafx.scene.control.ListCell<>() {
              @Override
              protected void updateItem(SimulationSpeed item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.label());
              }
            });
    speedSelector.setButtonCell(
        new javafx.scene.control.ListCell<>() {
          @Override
          protected void updateItem(SimulationSpeed item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : item.label());
          }
        });
    installFocusStyle(speedSelector, "#7ef9ff");
    speedSelector.getSelectionModel().select(liveMetricsService.simulationSpeed());
    speedSelector
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (ignored, oldSelection, selected) -> {
              if (selected == null || selected == oldSelection) {
                return;
              }
              liveMetricsService.setSimulationSpeed(selected);
              updateActiveSpeedLabel();
            });

    activeSpeedValue = new Label();
    activeSpeedValue.setTextFill(Color.web("#89ff9a"));
    activeSpeedValue.setFont(Font.font("Consolas", 12));
    updateActiveSpeedLabel();

    Label targetDifficultyLabel = new Label("TARGET DIFFICULTY");
    targetDifficultyLabel.setTextFill(Color.web("#9db2ff"));
    targetDifficultyLabel.setFont(Font.font("Consolas", 12));
    ComboBox<TrainingTargetDifficulty> targetDifficultySelector =
        new ComboBox<>(FXCollections.observableArrayList(TrainingTargetDifficulty.values()));
    targetDifficultySelector.getSelectionModel().select(selectedTargetDifficulty);
    targetDifficultySelector.setMaxWidth(Double.MAX_VALUE);
    targetDifficultySelector.setStyle(
        "-fx-background-color: #101938;"
            + "-fx-text-fill: #c6d7ff;"
            + "-fx-border-color: #2cf1ff;"
            + "-fx-border-radius: 6;"
            + "-fx-background-radius: 6;");
    targetDifficultySelector.setCellFactory(
        ignored ->
            new javafx.scene.control.ListCell<>() {
              @Override
              protected void updateItem(TrainingTargetDifficulty item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.label());
              }
            });
    targetDifficultySelector.setButtonCell(
        new javafx.scene.control.ListCell<>() {
          @Override
          protected void updateItem(TrainingTargetDifficulty item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : item.label());
          }
        });
    installFocusStyle(targetDifficultySelector, "#7ef9ff");
    targetDifficultySelector
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (ignored, oldSelection, selected) -> {
              if (selected == null || selected == oldSelection) {
                return;
              }
              selectedTargetDifficulty = selected;
              refreshSessionConfigCardPreview();
            });
    Label batchLabel = new Label("BATCHES");
    batchLabel.setTextFill(Color.web("#9db2ff"));
    batchLabel.setFont(Font.font("Consolas", 12));
    ComboBox<Integer> batchSelector =
        new ComboBox<>(FXCollections.observableArrayList(1, 2, 3, 4, 5));
    batchSelector.getSelectionModel().select(Integer.valueOf(1));
    batchSelector.setMaxWidth(Double.MAX_VALUE);
    batchSelector.setStyle(
        "-fx-background-color: #101938;"
            + "-fx-text-fill: #c6d7ff;"
            + "-fx-border-color: #2cf1ff;"
            + "-fx-border-radius: 6;"
            + "-fx-background-radius: 6;");
    installFocusStyle(batchSelector, "#7ef9ff");

    startButton =
        neonButton(
            "Start",
            "#22e6ff",
            () -> {
              startActionProcessing = true;
              startButton.setText("Starting...");
              updateControlAvailability();
              updateSystemStatus("START REQUESTED", UiSemanticState.IDLE);
              TrainingPresetOption selectedPreset = presetSelector.getValue();
              Long selectedPresetId = selectedPreset == null ? null : selectedPreset.id();
              TrainingTargetDifficulty targetDifficulty = targetDifficultySelector.getValue();
              var activePresetBeforeStart = trainingPresetService.activePreset();
              if (activePresetBeforeStart.isEmpty()) {
                startActionProcessing = false;
                startButton.setText("Start");
                updateControlAvailability();
                updateSystemStatus(
                    "Select a training preset before starting batches.",
                    UiSemanticState.VALIDATION_ERROR);
                emitNotification(
                    "validation.preset.missing",
                    "Validation error: select a training preset before starting.",
                    "#ff6b8a");
                return;
              }
              TrainingSessionConfig sessionConfig =
                  TrainingSessionConfig.v1(
                      selectedMazeName,
                      controlService.activeMovementPolicy(),
                      activePresetBeforeStart.get().timeout(),
                      null,
                      true,
                      targetDifficulty);
              StartTrainingSessionResult startResult =
                  startTrainingSessionUseCase.start(
                      new StartTrainingSessionCommand(
                          selectedMaze, selectedPresetId, sessionConfig));
              if (!startResult.started()) {
                startActionProcessing = false;
                startButton.setText("Start");
                updateControlAvailability();
                updateSystemStatus(startResult.message(), UiSemanticState.VALIDATION_ERROR);
                emitNotification("validation.start.denied", startResult.message(), "#ff6b8a");
                return;
              }
              if (startResult.maze() != null) {
                selectedMaze = startResult.maze();
                mazeViewportRenderer.renderInto(mazeViewport, startResult.maze());
                liveMetricsService.setActiveMaze(startResult.maze());
              }
              updateSystemStatus(startResult.message(), UiSemanticState.RUNNING);
              emitNotification("training.start", "Training started.", "#89ff9a");
              updateSessionHud(startResult.effectiveSeed(), "visual");
              lockSessionConfigCard(startResult.effectiveSeed(), sessionConfig);
              updateActivePolicyLabel();
              updateActivePresetLabel();
              var activePreset = trainingPresetService.activePreset();
              if (activePreset.isEmpty()) {
                startActionProcessing = false;
                startButton.setText("Start");
                updateControlAvailability();
                updateSystemStatus(
                    "Select a training preset before starting batches.",
                    UiSemanticState.VALIDATION_ERROR);
                emitNotification(
                    "validation.preset.missing",
                    "Validation error: select a training preset before starting.",
                    "#ff6b8a");
                return;
              }
              int batches =
                  batchSelector.getValue() == null ? 1 : Math.max(1, batchSelector.getValue());
              int episodesPerBatch = Math.max(1, activePreset.get().episodes());
              TrainingBudget budget =
                  new TrainingBudget(episodesPerBatch * batches, sessionConfig.timeout());
              trainingExecutionService
                  .startBatchTraining(
                      selectedMaze, episodesPerBatch, batches, sessionConfig.timeout(), budget)
                  .whenComplete(
                      (summary, error) ->
                          Platform.runLater(
                              () -> {
                                startActionProcessing = false;
                                startButton.setText("Start");
                                updateControlAvailability();
                                if (error != null) {
                                  updateSystemStatus("BATCH TRAINING CANCELLED", UiSemanticState.PAUSED);
                                  return;
                                }
                                if (summary.cancelled()) {
                                  updateSystemStatus("BATCH TRAINING CANCELLED", UiSemanticState.PAUSED);
                                } else {
                                  updateSystemStatus("BATCH TRAINING FINISHED", UiSemanticState.SUCCESS);
                                }
                                refreshRecentRunsAsync();
                                refreshCoverageSummaryAsync();
                              }));
            });
    pauseButton =
        neonButton(
            "Pause",
            "#ffd166",
            () -> {
              updateSystemStatus("PAUSE REQUESTED", UiSemanticState.PAUSED);
              emitNotification("control.pause", "Pause requested.", "#ffd166");
              controlService.pause();
            });
    resetButton =
        neonButton(
            "Reset",
            "#ff6b8a",
            () -> {
              updateSystemStatus("RESET REQUESTED", UiSemanticState.TIMEOUT);
              emitNotification("control.reset", "Reset requested.", "#ff6b8a");
              trainingExecutionService.cancelTraining();
              controlService.reset();
            });
    Button overlayToggle = neonButton("Unexplored Overlay: OFF", "#8fd8ff", () -> {});
    overlayToggle.setOnAction(
        event -> {
          unexploredOverlayEnabled = !unexploredOverlayEnabled;
          overlayToggle.setText(
              unexploredOverlayEnabled ? "Unexplored Overlay: ON" : "Unexplored Overlay: OFF");
          refreshUnexploredOverlay();
        });
    Button heatmapToggle = neonButton("Mini Heatmap: ON", "#9bff9f", () -> {});
    heatmapToggle.setOnAction(
        event -> {
          miniHeatmapEnabled = !miniHeatmapEnabled;
          heatmapToggle.setText(miniHeatmapEnabled ? "Mini Heatmap: ON" : "Mini Heatmap: OFF");
          refreshMiniHeatmap();
        });

    VBox panel =
        new VBox(
            10,
            title,
            algorithmLabel,
            algorithmSelector,
            activePolicyValue,
            presetLabel,
            presetSelector,
            activePresetValue,
            speedLabel,
            speedSelector,
            activeSpeedValue,
            targetDifficultyLabel,
            targetDifficultySelector,
            batchLabel,
            batchSelector,
            overlayToggle,
            heatmapToggle,
            startButton,
            pauseButton,
            resetButton);
    panel.setPadding(new Insets(16));
    panel.setMinWidth(220);
    panel.setStyle(panelStyle());
    installFocusStyle(startButton, "#22e6ff");
    installFocusStyle(pauseButton, "#ffd166");
    installFocusStyle(resetButton, "#ff6b8a");
    updateControlAvailability();
    refreshSessionConfigCardPreview();
    return panel;
  }

  private VBox buildMazePanel() {
    Label title = panelTitle("Maze");
    Label modeLabel = new Label("VIEW MODE");
    modeLabel.setTextFill(Color.web("#9db2ff"));
    modeLabel.setFont(Font.font("Consolas", 12));
    viewportModeSelector =
        new ComboBox<>(FXCollections.observableArrayList(ViewportMode.values()));
    viewportModeSelector.getSelectionModel().select(viewportMode);
    viewportModeSelector.setMaxWidth(Double.MAX_VALUE);
    viewportModeSelector.setStyle(
        "-fx-background-color: #101938;"
            + "-fx-text-fill: #c6d7ff;"
            + "-fx-border-color: #2cf1ff;"
            + "-fx-border-radius: 6;"
            + "-fx-background-radius: 6;");
    viewportModeSelector.setCellFactory(
        ignored ->
            new javafx.scene.control.ListCell<>() {
              @Override
              protected void updateItem(ViewportMode item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.label());
              }
            });
    viewportModeSelector.setButtonCell(
        new javafx.scene.control.ListCell<>() {
          @Override
          protected void updateItem(ViewportMode item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : item.label());
          }
        });
    installFocusStyle(viewportModeSelector, "#7ef9ff");
    viewportModeSelector
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (ignored, oldValue, selected) -> {
              if (selected == null || selected == oldValue) {
                return;
              }
              setViewportMode(selected);
            });

    Label sortLabel = new Label("SORT BY DIFFICULTY");
    sortLabel.setTextFill(Color.web("#9db2ff"));
    sortLabel.setFont(Font.font("Consolas", 12));
    ComboBox<String> sortSelector =
        new ComboBox<>(FXCollections.observableArrayList("Ascending", "Descending"));
    sortSelector.getSelectionModel().selectFirst();
    sortSelector.setMaxWidth(Double.MAX_VALUE);
    sortSelector.setStyle(
        "-fx-background-color: #101938;"
            + "-fx-text-fill: #c6d7ff;"
            + "-fx-border-color: #2cf1ff;"
            + "-fx-border-radius: 6;"
            + "-fx-background-radius: 6;");

    ComboBox<String> mazeSelector =
        new ComboBox<>(
            FXCollections.observableArrayList(mazeCatalogService.namesByDifficulty(true)));
    mazeSelector.setMaxWidth(Double.MAX_VALUE);
    mazeSelector.setStyle(
        "-fx-background-color: #101938;"
            + "-fx-text-fill: #c6d7ff;"
            + "-fx-border-color: #2cf1ff;"
            + "-fx-border-radius: 6;"
            + "-fx-background-radius: 6;");
    sortSelector
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (ignored, oldSort, selectedSort) -> {
              if (selectedSort == null || selectedSort.equals(oldSort)) {
                return;
              }
              refreshMazeSelector(mazeSelector, "Ascending".equals(selectedSort));
            });

    mazeViewport = new StackPane();
    mazeViewport.setAlignment(Pos.CENTER);
    mazeViewport.setMinHeight(520);
    mazeViewport.setStyle(
        "-fx-background-color: #0a1329;"
            + "-fx-border-color: #2cf1ff;"
            + "-fx-border-width: 1;"
            + "-fx-border-radius: 8;"
            + "-fx-background-radius: 8;");

    mazeSelector
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (ignored, oldValue, selectedName) -> {
              if (selectedName == null || selectedName.equals(oldValue)) {
                return;
              }
              MazeDefinition maze = mazeCatalogService.byName(selectedName);
              if (maze != null) {
                selectedMazeName = selectedName;
                selectedMaze = maze;
                accumulatedHeatmapFrequencies = List.of();
                mazeViewportRenderer.renderInto(mazeViewport, maze);
                liveMetricsService.setActiveMaze(maze);
                resetTrajectoryEpisode();
                refreshUnexploredOverlay();
                refreshMiniHeatmap();
                refreshPersistentHeatmapAsync();
                refreshRecentRunsAsync();
                refreshSessionConfigCardPreview();
              }
            });

    if (!mazeSelector.getItems().isEmpty()) {
      mazeSelector.getSelectionModel().selectFirst();
      MazeDefinition firstMaze = mazeCatalogService.byName(mazeSelector.getValue());
      if (firstMaze != null) {
        selectedMazeName = mazeSelector.getValue();
        selectedMaze = firstMaze;
        accumulatedHeatmapFrequencies = List.of();
        mazeViewportRenderer.renderInto(mazeViewport, firstMaze);
        liveMetricsService.setActiveMaze(firstMaze);
        refreshUnexploredOverlay();
        refreshMiniHeatmap();
        refreshPersistentHeatmapAsync();
        refreshRecentRunsAsync();
        refreshSessionConfigCardPreview();
      }
    }

    Label replayTitle = new Label("SUCCESS REPLAY");
    replayTitle.setTextFill(Color.web("#9db2ff"));
    replayTitle.setFont(Font.font("Consolas", 12));
    replayStatusValue = timelinePlaceholder("Replay idle.");
    Button replayPlay = neonButton("Play", "#7ef9ff", this::playReplay);
    Button replayPause = neonButton("Pause", "#ffd166", this::pauseReplay);
    Button replayRestart = neonButton("Restart", "#89ff9a", this::restartReplay);
    Button replayNext = neonButton("Next Success", "#ffb86b", this::nextReplayEpisode);
    episodeDetailsToggleButton =
        neonButton("Episode Details: ON", "#9db2ff", this::toggleEpisodeDetailsPanel);
    focusModeToggleButton = neonButton("Focus Mode: OFF", "#7ef9ff", this::toggleFocusMode);
    focusModeHud = buildFocusModeHud();
    HBox replayControls =
        new HBox(
            8,
            replayPlay,
            replayPause,
            replayRestart,
            replayNext,
            episodeDetailsToggleButton,
            focusModeToggleButton);

    VBox panel =
        new VBox(
            12,
            title,
            modeLabel,
            viewportModeSelector,
            sortLabel,
            sortSelector,
            mazeSelector,
            replayTitle,
            replayStatusValue,
            replayControls,
            focusModeHud,
            mazeViewport);
    panel.setPadding(new Insets(16));
    panel.setStyle(panelStyle());
    BorderPane.setMargin(panel, new Insets(0, 16, 0, 16));
    applyEpisodeDetailsPanelState();
    refreshFocusModeHud();
    refreshReplayEpisodesAsync();
    return panel;
  }

  private HBox buildFocusModeHud() {
    focusModeStatusValue = timelinePlaceholder("Episode: IDLE");
    focusModeElapsedValue = timelinePlaceholder("Elapsed: --:--");
    focusModeRewardValue = timelinePlaceholder("Reward: 0.0");
    focusModeCollisionsValue = timelinePlaceholder("Collisions: 0");
    HBox hud =
        new HBox(
            12,
            focusModeStatusValue,
            focusModeElapsedValue,
            focusModeRewardValue,
            focusModeCollisionsValue);
    hud.setPadding(new Insets(8, 10, 8, 10));
    hud.setAlignment(Pos.CENTER_LEFT);
    hud.setStyle(
        "-fx-background-color: rgba(8, 17, 36, 0.96);"
            + "-fx-border-color: #2cf1ff;"
            + "-fx-border-radius: 6;"
            + "-fx-background-radius: 6;");
    hud.setVisible(false);
    hud.setManaged(false);
    return hud;
  }

  private void refreshMazeSelector(ComboBox<String> mazeSelector, boolean ascendingDifficulty) {
    String previousSelection = mazeSelector.getValue();
    List<String> orderedNames = mazeCatalogService.namesByDifficulty(ascendingDifficulty);
    mazeSelector.getItems().setAll(orderedNames);
    if (previousSelection != null && orderedNames.contains(previousSelection)) {
      mazeSelector.getSelectionModel().select(previousSelection);
      return;
    }
    if (!orderedNames.isEmpty()) {
      mazeSelector.getSelectionModel().selectFirst();
    }
  }

  private VBox buildMetricsPanel() {
    Label title = panelTitle("Metrics");
    VBox contextualPanel = buildContextualStatusPanel();
    VBox episodeDetailsCard = buildEpisodeDetailsCard();
    VBox metrics =
        new VBox(
            10,
            metricLine("Steps", "--"),
            metricLine("Collisions", "--"),
            metricLine("Reward", "--"),
            metricLine("Elapsed", "--:--"),
            metricLine("Remaining", "--:--"),
            metricLine("Coverage L/R", "-- / --"));
    VBox diagnostics =
        new VBox(
            10,
            metricLine("Termination", "LOADING"),
            metricLine("Maze Coverage", "--"),
            metricLine("Alert", "WAITING"));
    VBox effectiveSessionCard = buildEffectiveSessionCard();

    Label timelineTitle = new Label("RECENT EPISODES");
    timelineTitle.setTextFill(Color.web("#9db2ff"));
    timelineTitle.setFont(Font.font("Consolas", 12));

    timelineEntriesBox = new VBox(6);
    timelineEntriesBox.getChildren().add(timelinePlaceholder("Loading episode timeline..."));

    Label comparisonTitle = new Label("LAST 10 RUNS");
    comparisonTitle.setTextFill(Color.web("#9db2ff"));
    comparisonTitle.setFont(Font.font("Consolas", 12));

    ComboBox<RecentRunsSortOption> comparisonSortSelector =
        new ComboBox<>(FXCollections.observableArrayList(RecentRunsSortOption.values()));
    comparisonSortSelector.getSelectionModel().select(RecentRunsSortOption.BY_DATE);
    comparisonSortSelector.setMaxWidth(Double.MAX_VALUE);
    comparisonSortSelector.setStyle(
        "-fx-background-color: #101938;"
            + "-fx-text-fill: #c6d7ff;"
            + "-fx-border-color: #2cf1ff;"
            + "-fx-border-radius: 6;"
            + "-fx-background-radius: 6;");
    comparisonSortSelector.setCellFactory(
        ignored ->
            new javafx.scene.control.ListCell<>() {
              @Override
              protected void updateItem(RecentRunsSortOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.label());
              }
            });
    comparisonSortSelector.setButtonCell(
        new javafx.scene.control.ListCell<>() {
          @Override
          protected void updateItem(RecentRunsSortOption item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : item.label());
          }
        });
    comparisonSortSelector
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (ignored, oldSelection, selected) -> {
              if (selected == null || selected == oldSelection) {
                return;
              }
              selectedRecentRunsSort = selected;
              refreshRecentRunsAsync();
            });

    recentRunsEntriesBox = new VBox(6);
    recentRunsEntriesBox.getChildren().add(timelinePlaceholder("Loading recent run catalog..."));

    Label coverageTitle = new Label("PENDING COVERAGE");
    coverageTitle.setTextFill(Color.web("#9db2ff"));
    coverageTitle.setFont(Font.font("Consolas", 12));

    coverageEntriesBox = new VBox(6);
    coverageEntriesBox.getChildren().add(timelinePlaceholder("Loading coverage catalog..."));

    Label heatmapTitle = new Label("VISIT HEATMAP");
    heatmapTitle.setTextFill(Color.web("#9db2ff"));
    heatmapTitle.setFont(Font.font("Consolas", 12));
    ComboBox<HeatmapComparisonMode> heatmapModeSelector =
        new ComboBox<>(FXCollections.observableArrayList(HeatmapComparisonMode.values()));
    heatmapModeSelector.getSelectionModel().select(heatmapComparisonMode);
    heatmapModeSelector.setMaxWidth(Double.MAX_VALUE);
    heatmapModeSelector.setStyle(
        "-fx-background-color: #101938;"
            + "-fx-text-fill: #c6d7ff;"
            + "-fx-border-color: #2cf1ff;"
            + "-fx-border-radius: 6;"
            + "-fx-background-radius: 6;");
    heatmapModeSelector.setCellFactory(
        ignored ->
            new javafx.scene.control.ListCell<>() {
              @Override
              protected void updateItem(HeatmapComparisonMode item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.label());
              }
            });
    heatmapModeSelector.setButtonCell(
        new javafx.scene.control.ListCell<>() {
          @Override
          protected void updateItem(HeatmapComparisonMode item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : item.label());
          }
        });
    heatmapModeSelector
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (ignored, oldSelection, selected) -> {
              if (selected == null || selected == oldSelection) {
                return;
              }
              heatmapComparisonMode = selected;
              refreshMiniHeatmap();
            });
    miniHeatmapGrid = new GridPane();
    miniHeatmapGrid.setHgap(1.2);
    miniHeatmapGrid.setVgap(1.2);
    miniHeatmapGrid.setStyle(
        "-fx-padding: 6;"
            + "-fx-background-color: #081124;"
            + "-fx-border-color: #2cf1ff;"
            + "-fx-border-radius: 6;"
            + "-fx-background-radius: 6;");
    HBox heatmapLegend = buildHeatmapLegend();

    VBox panel =
        new VBox(
            12,
            title,
            contextualPanel,
            episodeDetailsCard,
            metrics,
            diagnostics,
            effectiveSessionCard,
            timelineTitle,
            timelineEntriesBox,
            comparisonTitle,
            comparisonSortSelector,
            recentRunsEntriesBox,
            coverageTitle,
            coverageEntriesBox,
            heatmapTitle,
            heatmapModeSelector,
            heatmapLegend,
            miniHeatmapGrid);
    panel.setPadding(new Insets(16));
    panel.setMinWidth(240);
    panel.setStyle(panelStyle());
    refreshSessionConfigCardPreview();
    refreshContextualStatusPanel();
    refreshEpisodeDetailsCard();
    return panel;
  }

  private VBox buildEpisodeDetailsCard() {
    Label title = new Label("EPISODE DETAILS");
    title.setTextFill(Color.web("#9db2ff"));
    title.setFont(Font.font(UI_FONT_FAMILY, FONT_SIZE_SECTION_LABEL));
    episodeSeedDetailValue = timelinePlaceholder("Seed: AUTO");
    episodePolicyDetailValue = timelinePlaceholder("Policy: -");
    episodeStepDetailValue = timelinePlaceholder("Step: 0");
    episodeRewardDetailValue = timelinePlaceholder("Last reward: 0.0");
    VBox card =
        new VBox(
            4,
            title,
            episodeSeedDetailValue,
            episodePolicyDetailValue,
            episodeStepDetailValue,
            episodeRewardDetailValue);
    card.setPadding(new Insets(10));
    card.setStyle(
        "-fx-background-color: #081124;"
            + "-fx-border-color: #2cf1ff;"
            + "-fx-border-radius: 6;"
            + "-fx-background-radius: 6;");
    return card;
  }

  private VBox buildContextualStatusPanel() {
    Label title = new Label("MODE CONTEXT");
    title.setTextFill(Color.web("#9db2ff"));
    title.setFont(Font.font("Consolas", 12));

    contextModeValue = timelinePlaceholder("Mode: LIVE");
    contextSummaryValue = timelinePlaceholder("Awaiting episode telemetry.");
    contextDetailValue = timelinePlaceholder("Algorithm/speed pending.");
    contextSignalValue = timelinePlaceholder("Signal: IDLE");
    contextSignalValue.setTextFill(Color.web(UiSemanticState.PAUSED.hex()));

    VBox panel =
        new VBox(6, title, contextModeValue, contextSummaryValue, contextDetailValue, contextSignalValue);
    panel.setPadding(new Insets(10));
    panel.setStyle(
        "-fx-background-color: #081124;"
            + "-fx-border-color: #2cf1ff;"
            + "-fx-border-radius: 6;"
            + "-fx-background-radius: 6;");
    return panel;
  }

  private VBox buildReviewPanel() {
    Label title = panelTitle("Training Review");
    Label sessionsTitle = new Label("SESSIONS");
    sessionsTitle.setTextFill(Color.web("#9db2ff"));
    sessionsTitle.setFont(Font.font("Consolas", 12));

    reviewSessionsSelector = new ComboBox<>();
    reviewSessionsSelector.setMaxWidth(Double.MAX_VALUE);
    reviewSessionsSelector.setStyle(
        "-fx-background-color: #101938;"
            + "-fx-text-fill: #c6d7ff;"
            + "-fx-border-color: #ffd166;"
            + "-fx-border-radius: 6;"
            + "-fx-background-radius: 6;");
    reviewSessionsSelector.setCellFactory(
        ignored ->
            new javafx.scene.control.ListCell<>() {
              @Override
              protected void updateItem(TrainingSessionEntity item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatSessionLabel(item));
              }
            });
    reviewSessionsSelector.setButtonCell(
        new javafx.scene.control.ListCell<>() {
          @Override
          protected void updateItem(TrainingSessionEntity item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : formatSessionLabel(item));
          }
        });
    reviewSessionsSelector
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (ignored, oldSession, session) -> {
              if (session == null || session.equals(oldSession)) {
                return;
              }
              selectedReviewSessionId = session.id();
              selectedReviewRunId = null;
              refreshReviewSessionDetailAsync(session.id());
            });

    reviewSessionMetaValue = timelinePlaceholder("Select a session.");
    reviewSessionSummaryValue = timelinePlaceholder("Summary will appear here.");
    reviewEpisodeMetaValue = timelinePlaceholder("Select an episode to inspect details.");

    Label asciiTitle = new Label("ASCII TREND");
    asciiTitle.setTextFill(Color.web("#9db2ff"));
    asciiTitle.setFont(Font.font("Consolas", 12));
    reviewAsciiArea = readonlyArea("No session selected.");
    reviewAsciiArea.setPrefRowCount(8);

    Label episodesTitle = new Label("EPISODES");
    episodesTitle.setTextFill(Color.web("#9db2ff"));
    episodesTitle.setFont(Font.font("Consolas", 12));
    reviewEpisodesEntriesBox = new VBox(6);
    reviewEpisodesEntriesBox.getChildren().setAll(timelinePlaceholder("No session selected."));
    ScrollPane episodesScroll = new ScrollPane(reviewEpisodesEntriesBox);
    episodesScroll.setFitToWidth(true);
    episodesScroll.setPrefHeight(220);
    episodesScroll.setStyle(
        "-fx-background: #081124;"
            + "-fx-border-color: #2cf1ff;"
            + "-fx-border-radius: 6;"
            + "-fx-background-radius: 6;");

    Label trajectoryTitle = new Label("EPISODE PATH");
    trajectoryTitle.setTextFill(Color.web("#9db2ff"));
    trajectoryTitle.setFont(Font.font("Consolas", 12));
    reviewTrajectoryArea = readonlyArea("No trajectory loaded.");
    reviewTrajectoryArea.setPrefRowCount(7);

    VBox panel =
        new VBox(
            10,
            title,
            sessionsTitle,
            reviewSessionsSelector,
            reviewSessionMetaValue,
            reviewSessionSummaryValue,
            asciiTitle,
            reviewAsciiArea,
            episodesTitle,
            episodesScroll,
            reviewEpisodeMetaValue,
            trajectoryTitle,
            reviewTrajectoryArea);
    panel.setPadding(new Insets(16));
    panel.setStyle(panelStyle());
    panel.setFillWidth(true);
    return panel;
  }

  private VBox buildAssetsPanel() {
    Label title = panelTitle("Assets Review");
    Label filtersTitle = new Label("FILTERS");
    filtersTitle.setTextFill(Color.web("#9db2ff"));
    filtersTitle.setFont(Font.font("Consolas", 12));

    assetTypeFilter =
        new ComboBox<>(
            FXCollections.observableArrayList(
                (PersistedAssetType) null,
                PersistedAssetType.MAZE,
                PersistedAssetType.TRAINING_RUN,
                PersistedAssetType.TRAINING_PRESET,
                PersistedAssetType.TRAINING_SESSION,
                PersistedAssetType.EXPERIENCE_TRANSITION,
                PersistedAssetType.MAZE_POLICY_COVERAGE,
                PersistedAssetType.EXPLORATION_BUDGET));
    assetTypeFilter.setMaxWidth(Double.MAX_VALUE);
    assetTypeFilter.setStyle(
        "-fx-background-color: #101938;"
            + "-fx-text-fill: #c6d7ff;"
            + "-fx-border-color: #ffb86b;"
            + "-fx-border-radius: 6;"
            + "-fx-background-radius: 6;");
    assetTypeFilter.setCellFactory(
        ignored ->
            new javafx.scene.control.ListCell<>() {
              @Override
              protected void updateItem(PersistedAssetType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item == null ? "All types" : item.name());
              }
            });
    assetTypeFilter.setButtonCell(
        new javafx.scene.control.ListCell<>() {
          @Override
          protected void updateItem(PersistedAssetType item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty ? null : item == null ? "All types" : item.name());
          }
        });
    assetTypeFilter.getSelectionModel().selectFirst();
    assetTypeFilter
        .getSelectionModel()
        .selectedItemProperty()
        .addListener((ignored, oldType, newType) -> applyAssetFilters());

    assetStateFilter = new ComboBox<>(FXCollections.observableArrayList("All states", "Deletable", "Blocked"));
    assetStateFilter.getSelectionModel().selectFirst();
    assetStateFilter.setMaxWidth(Double.MAX_VALUE);
    assetStateFilter.setStyle(
        "-fx-background-color: #101938;"
            + "-fx-text-fill: #c6d7ff;"
            + "-fx-border-color: #ffb86b;"
            + "-fx-border-radius: 6;"
            + "-fx-background-radius: 6;");
    assetStateFilter
        .getSelectionModel()
        .selectedItemProperty()
        .addListener((ignored, oldValue, newValue) -> applyAssetFilters());

    assetListView = new ListView<>();
    assetListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    assetListView.setPrefHeight(360);
    assetListView.setStyle(
        "-fx-background-color: #081124;"
            + "-fx-control-inner-background: #081124;"
            + "-fx-text-fill: #c6d7ff;"
            + "-fx-border-color: #ffb86b;"
            + "-fx-border-radius: 6;"
            + "-fx-background-radius: 6;");
    assetListView.setCellFactory(
        ignored ->
            new javafx.scene.control.ListCell<>() {
              @Override
              protected void updateItem(PersistedAssetCatalogItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                  setText(null);
                  return;
                }
                String updated =
                    item.updatedAtEpochMillis() == null
                        ? "-"
                        : Long.toString(item.updatedAtEpochMillis());
                setText(
                    item.type()
                        + " #"
                        + item.assetId()
                        + " | updated="
                        + updated
                        + " | size~"
                        + item.estimatedSizeBytes()
                        + "B");
              }
            });
    assetListView
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (ignored, oldItem, selectedItem) -> {
              if (selectedItem == null) {
                assetPreviewArea.setText("Select an asset to inspect preview.");
                return;
              }
              refreshAssetPreviewAsync(selectedItem);
            });

    assetPreviewArea = readonlyArea("Loading assets...");
    assetPreviewArea.setPrefRowCount(14);
    assetPreviewArea.setFont(Font.font("Consolas", 12));

    assetsStatusValue = timelinePlaceholder("Loading persisted assets...");
    Button refreshButton = neonButton("Refresh Catalog", "#ffb86b", this::refreshAssetCatalogAsync);
    Button deleteSelectedButton = neonButton("Delete Selected", "#ff6b8a", this::requestAssetDeletion);

    VBox panel =
        new VBox(
            10,
            title,
            filtersTitle,
            assetTypeFilter,
            assetStateFilter,
            refreshButton,
            deleteSelectedButton,
            assetsStatusValue,
            assetListView,
            new Label("TEXT PREVIEW"),
            assetPreviewArea);
    panel.setPadding(new Insets(16));
    panel.setStyle(panelStyle());
    panel.setFillWidth(true);
    return panel;
  }

  private void refreshAssetCatalogAsync() {
    recentRunsExecutor.execute(
        () -> {
          List<PersistedAssetCatalogItem> items = persistedAssetCatalogService.listAll();
          Platform.runLater(
              () -> {
                assetCatalogCache = items;
                applyAssetFilters();
                if (assetsStatusValue != null) {
                  assetsStatusValue.setText("Assets loaded: " + items.size());
                }
              });
        });
  }

  private void applyAssetFilters() {
    if (assetListView == null) {
      return;
    }
    PersistedAssetType selectedType = assetTypeFilter == null ? null : assetTypeFilter.getValue();
    String stateFilter = assetStateFilter == null ? "All states" : assetStateFilter.getValue();
    List<PersistedAssetCatalogItem> filtered =
        assetCatalogCache.stream()
            .filter(item -> selectedType == null || item.type() == selectedType)
            .filter(
                item -> {
                  if (stateFilter == null || "All states".equals(stateFilter)) {
                    return true;
                  }
                  List<PersistedAssetDeletionCandidate> evaluated =
                      persistedAssetCleanupService.evaluate(
                          List.of(new PersistedAssetRef(item.type(), item.assetId())));
                  boolean deletable =
                      !evaluated.isEmpty() && evaluated.get(0) != null && evaluated.get(0).eligible();
                  return "Deletable".equals(stateFilter) ? deletable : !deletable;
                })
            .toList();
    assetListView.getItems().setAll(filtered);
    if (filtered.isEmpty()) {
      assetPreviewArea.setText("No assets for current filters.");
    }
  }

  private void refreshAssetPreviewAsync(PersistedAssetCatalogItem item) {
    recentRunsExecutor.execute(
        () -> {
          String preview =
              persistedAssetPreviewService
                  .render(item.type(), item.assetId())
                  .map(PersistedAssetPreview::content)
                  .orElse("No preview available for this asset.");
          Platform.runLater(() -> assetPreviewArea.setText(preview));
        });
  }

  private void requestAssetDeletion() {
    if (assetListView == null) {
      return;
    }
    List<PersistedAssetCatalogItem> selectedItems = List.copyOf(assetListView.getSelectionModel().getSelectedItems());
    if (selectedItems.isEmpty()) {
      assetsStatusValue.setText("Select one or more assets before deleting.");
      return;
    }
    List<PersistedAssetRef> refs =
        selectedItems.stream().map(item -> new PersistedAssetRef(item.type(), item.assetId())).toList();
    List<PersistedAssetDeletionCandidate> evaluation = persistedAssetCleanupService.evaluate(refs);
    String summary = buildDeletionSummary(selectedItems, evaluation);

    Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
    confirmation.setTitle("Confirm Cleanup");
    confirmation.setHeaderText("Delete selected assets?");
    confirmation.setContentText(summary);
    confirmation.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
    confirmation.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

    var result = confirmation.showAndWait();
    if (result.isEmpty() || result.get() != ButtonType.OK) {
      assetsStatusValue.setText("Cleanup cancelled.");
      return;
    }

    assetsStatusValue.setText("Executing cleanup...");
    recentRunsExecutor.execute(
        () -> {
          var cleanupResult = persistedAssetCleanupService.delete(refs);
          Platform.runLater(
              () -> {
                assetsStatusValue.setText(
                    "Cleanup result: deleted="
                        + cleanupResult.deleted().size()
                        + ", blocked="
                        + cleanupResult.blocked().size()
                        + ", failed="
                        + cleanupResult.failed().size());
                refreshAssetCatalogAsync();
              });
        });
  }

  private String buildDeletionSummary(
      List<PersistedAssetCatalogItem> selectedItems,
      List<PersistedAssetDeletionCandidate> evaluation) {
    StringBuilder builder = new StringBuilder();
    builder.append("Selected: ").append(selectedItems.size()).append('\n');
    int index = 0;
    for (PersistedAssetCatalogItem item : selectedItems) {
      builder.append("- ").append(item.type()).append(" #").append(item.assetId()).append('\n');
      index++;
      if (index >= 8 && selectedItems.size() > index) {
        builder.append("... +").append(selectedItems.size() - index).append(" more").append('\n');
        break;
      }
    }
    long blocked = evaluation.stream().filter(candidate -> !candidate.eligible()).count();
    if (blocked > 0) {
      builder.append('\n').append("Warnings:").append('\n');
      int warningIndex = 0;
      for (PersistedAssetDeletionCandidate candidate : evaluation) {
        if (!candidate.eligible()) {
          builder
              .append("* ")
              .append(candidate.ref().type())
              .append(" #")
              .append(candidate.ref().assetId())
              .append(" -> ")
              .append(candidate.validationMessage())
              .append('\n');
          warningIndex++;
          if (warningIndex >= 4 && blocked > warningIndex) {
            builder.append("* ... +").append(blocked - warningIndex).append(" more blocked").append('\n');
            break;
          }
        }
      }
    } else {
      builder.append('\n').append("No dependency warnings.");
    }
    return builder.toString();
  }

  private void showDashboardMode() {
    if (reviewPanel != null) {
      reviewPanel.setVisible(false);
      reviewPanel.setManaged(false);
    }
    if (assetsPanel != null) {
      assetsPanel.setVisible(false);
      assetsPanel.setManaged(false);
    }
  }

  private void showReviewMode() {
    if (reviewPanel != null) {
      reviewPanel.setVisible(true);
      reviewPanel.setManaged(true);
      refreshReviewSessionsAsync();
    }
    if (assetsPanel != null) {
      assetsPanel.setVisible(false);
      assetsPanel.setManaged(false);
    }
  }

  private void showAssetsMode() {
    if (assetsPanel != null) {
      assetsPanel.setVisible(true);
      assetsPanel.setManaged(true);
      refreshAssetCatalogAsync();
    }
    if (reviewPanel != null) {
      reviewPanel.setVisible(false);
      reviewPanel.setManaged(false);
    }
  }

  private TextArea readonlyArea(String text) {
    TextArea area = new TextArea(text);
    area.setEditable(false);
    area.setWrapText(false);
    area.setFont(Font.font(UI_FONT_FAMILY, 11));
    area.setStyle(
        "-fx-control-inner-background: #081124;"
            + "-fx-text-fill: #c6d7ff;"
            + "-fx-highlight-fill: #2cf1ff;"
            + "-fx-highlight-text-fill: #081124;"
            + "-fx-border-color: #2cf1ff;"
            + "-fx-border-radius: 6;"
            + "-fx-background-radius: 6;");
    return area;
  }

  private void refreshReviewSessionsAsync() {
    recentRunsExecutor.execute(
        () -> {
          List<TrainingSessionEntity> sessions = trainingSessionRepository.findRecent(40);
          Platform.runLater(() -> renderReviewSessions(sessions));
        });
  }

  private void renderReviewSessions(List<TrainingSessionEntity> sessions) {
    if (reviewPanel == null || reviewSessionsSelector == null) {
      return;
    }
    reviewSessionsSelector.getItems().setAll(sessions);
    if (sessions.isEmpty()) {
      reviewSessionMetaValue.setText("No training sessions stored yet.");
      reviewSessionSummaryValue.setText("Run training to populate review history.");
      reviewAsciiArea.setText("No session selected.");
      reviewEpisodesEntriesBox.getChildren().setAll(timelinePlaceholder("No session selected."));
      reviewTrajectoryArea.setText("No trajectory loaded.");
      return;
    }
    TrainingSessionEntity preferred =
        sessions.stream()
            .filter(session -> session.id().equals(selectedReviewSessionId))
            .findFirst()
            .orElse(sessions.get(0));
    reviewSessionsSelector.getSelectionModel().select(preferred);
    selectedReviewSessionId = preferred.id();
    refreshReviewSessionDetailAsync(preferred.id());
  }

  private void refreshReviewSessionDetailAsync(String sessionId) {
    if (sessionId == null || sessionId.isBlank()) {
      return;
    }
    recentRunsExecutor.execute(
        () -> {
          TrainingSessionSummary summary = trainingSessionSummaryService.summarize(sessionId);
          String ascii = trainingSessionAsciiTrendRenderer.renderForSession(sessionId);
          List<TrainingRunEntity> runs = trainingRunRepository.findByTrainingSessionId(sessionId);
          Platform.runLater(() -> renderReviewSessionDetail(summary, ascii, runs));
        });
  }

  private void renderReviewSessionDetail(
      TrainingSessionSummary summary, String ascii, List<TrainingRunEntity> runs) {
    if (summary == null) {
      return;
    }
    reviewSessionMetaValue.setText(
        "Session " + summary.trainingSessionId() + " episodes=" + summary.episodesTotal());
    reviewSessionSummaryValue.setText(
        String.format(
            Locale.US,
            "Success %.0f%% | Reward %.2f | Collisions %.2f | Coverage %.0f%% | Duration %s",
            summary.successRate() * 100.0,
            summary.averageReward(),
            summary.averageCollisions(),
            summary.averageCoverage() * 100.0,
            formatElapsed(summary.totalDurationMillis())));
    reviewAsciiArea.setText(ascii == null ? "" : ascii);
    reviewEpisodesEntriesBox.getChildren().clear();
    if (runs == null || runs.isEmpty()) {
      reviewEpisodesEntriesBox.getChildren().setAll(timelinePlaceholder("No episodes in session."));
      reviewTrajectoryArea.setText("No trajectory loaded.");
      return;
    }
    for (TrainingRunEntity run : runs) {
      reviewEpisodesEntriesBox.getChildren().add(reviewEpisodeRow(run));
    }
    selectedReviewRunId = runs.get(runs.size() - 1).id();
    refreshEpisodeDetailAsync(selectedReviewRunId);
  }

  private HBox reviewEpisodeRow(TrainingRunEntity run) {
    Button pick = neonButton("#" + run.id(), "#2cf1ff", () -> refreshEpisodeDetailAsync(run.id()));
    pick.setMinWidth(76);
    Label terminal = new Label(formatTerminalReason(run.terminalReason()));
    terminal.setFont(Font.font("Consolas", 11));
    terminal.setTextFill(Color.web(terminalReasonColor(run.terminalReason())));
    Label reward = new Label(String.format(Locale.US, "R %.1f", run.totalReward()));
    reward.setFont(Font.font("Consolas", 11));
    reward.setTextFill(Color.web("#b8ffcb"));
    Label coverage =
        new Label(String.format(Locale.US, "C %.0f%%", run.mazeCoverageRatio() * 100.0));
    coverage.setFont(Font.font("Consolas", 11));
    coverage.setTextFill(Color.web("#9db2ff"));
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    return new HBox(8, pick, terminal, reward, coverage, spacer, new Label(formatElapsed(run.elapsedMillis())));
  }

  private void refreshEpisodeDetailAsync(Long trainingRunId) {
    if (trainingRunId == null) {
      return;
    }
    selectedReviewRunId = trainingRunId;
    recentRunsExecutor.execute(
        () -> {
          var detail = trainingEpisodeDetailService.findByTrainingRunId(trainingRunId);
          Platform.runLater(() -> renderEpisodeDetail(trainingRunId, detail.orElse(null)));
        });
  }

  private void renderEpisodeDetail(long trainingRunId, TrainingEpisodeDetail detail) {
    if (detail == null) {
      reviewEpisodeMetaValue.setText("Episode #" + trainingRunId + " not found.");
      reviewTrajectoryArea.setText("No trajectory available.");
      return;
    }
    reviewEpisodeMetaValue.setText(
        "Episode #"
            + detail.trainingRunId()
            + " final="
            + detail.finalPosition().row()
            + ","
            + detail.finalPosition().col());
    StringBuilder path = new StringBuilder();
    path.append("Path length=").append(detail.trajectory().size()).append('\n');
    int index = 0;
    for (GridPosition position : detail.trajectory()) {
      if (index > 0) {
        path.append(" -> ");
      }
      path.append('(').append(position.row()).append(',').append(position.col()).append(')');
      index++;
      if (index % 8 == 0) {
        path.append('\n');
      }
      if (index >= 120) {
        path.append(" ...");
        break;
      }
    }
    path.append('\n')
        .append("Replay metadata: ")
        .append(detail.replayMetadata() == null ? "{}" : detail.replayMetadata());
    reviewTrajectoryArea.setText(path.toString());
  }

  private String formatSessionLabel(TrainingSessionEntity session) {
    String maze = session.mazeRef() == null || session.mazeRef().isBlank() ? "maze?" : session.mazeRef();
    String policy = session.policyId() == null || session.policyId().isBlank() ? "policy?" : session.policyId();
    return session.id() + " :: " + maze + " :: " + policy;
  }

  private VBox buildEffectiveSessionCard() {
    Label title = new Label("EFFECTIVE SESSION");
    title.setTextFill(Color.web("#9db2ff"));
    title.setFont(Font.font("Consolas", 12));

    effectiveMazeValue = sessionCardValue("Maze", "-");
    effectivePolicyValue = sessionCardValue("Policy", "-");
    effectiveSeedCardValue = sessionCardValue("Seed", "AUTO");
    effectiveTimeoutValue = sessionCardValue("Timeout", "-");
    effectiveDifficultyValue = sessionCardValue("Difficulty", selectedTargetDifficulty.label());

    VBox card =
        new VBox(
            6,
            title,
            effectiveMazeValue,
            effectivePolicyValue,
            effectiveSeedCardValue,
            effectiveTimeoutValue,
            effectiveDifficultyValue);
    card.setPadding(new Insets(8));
    card.setStyle(
        "-fx-background-color: rgba(11, 20, 42, 0.75);"
            + "-fx-border-color: #2c3f73;"
            + "-fx-border-width: 1;"
            + "-fx-border-radius: 6;"
            + "-fx-background-radius: 6;");
    return card;
  }

  private HBox buildHeatmapLegend() {
    Label low = new Label("LOW");
    low.setTextFill(Color.web("#9db2ff"));
    low.setFont(Font.font("Consolas", 10));
    Label high = new Label("HIGH");
    high.setTextFill(Color.web("#9db2ff"));
    high.setFont(Font.font("Consolas", 10));
    HBox swatches = new HBox(2);
    for (int index = 0; index < 6; index++) {
      double ratio = index / 5.0;
      Rectangle swatch = new Rectangle(12, 8);
      swatch.setArcWidth(3);
      swatch.setArcHeight(3);
      swatch.setFill(
          Color.color(
              0.22 + (0.70 * ratio),
              0.32 + (0.52 * ratio),
              0.78 - (0.58 * ratio),
              0.30 + (0.55 * ratio)));
      swatch.setStroke(Color.color(0.30, 0.90, 1.0, 0.35));
      swatch.setStrokeWidth(0.3);
      swatches.getChildren().add(swatch);
    }
    Label title = new Label("Shared intensity scale");
    title.setTextFill(Color.web("#6fd6ff"));
    title.setFont(Font.font("Consolas", 10));
    HBox legend = new HBox(6, title, low, swatches, high);
    legend.setAlignment(Pos.CENTER_LEFT);
    return legend;
  }

  private Label sessionCardValue(String label, String value) {
    Label row = new Label(label + ": " + value);
    row.setTextFill(Color.web("#c6d7ff"));
    row.setFont(Font.font("Consolas", 11));
    return row;
  }

  private HBox metricLine(String name, String value) {
    Label left = new Label(name);
    left.setTextFill(Color.web("#9db2ff"));
    left.setFont(Font.font(UI_FONT_FAMILY, FONT_SIZE_METRIC_LABEL));

    Label right = new Label(value);
    right.setTextFill(Color.web("#b8ffcb"));
    right.setFont(Font.font(UI_FONT_FAMILY, FONT_SIZE_METRIC_VALUE));
    bindMetricLabel(name, right);

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    return new HBox(8, left, spacer, right);
  }

  private Label panelTitle(String text) {
    Label title = new Label(text.toUpperCase());
    title.setTextFill(Color.web("#7ef9ff"));
    title.setFont(Font.font(UI_FONT_FAMILY, FONT_SIZE_PANEL_TITLE));
    return title;
  }

  private Button neonButton(String label, String accent, Runnable action) {
    Button button = new Button(label);
    button.setMaxWidth(Double.MAX_VALUE);
    button.setFont(Font.font(UI_FONT_FAMILY, 14));
    button.setStyle(neonButtonStyle(accent, "normal"));
    button.setOnAction(event -> action.run());
    button.hoverProperty()
        .addListener(
            (ignored, wasHover, isHover) -> {
              if (button.isDisabled() || button.isPressed()) {
                return;
              }
              button.setStyle(neonButtonStyle(accent, isHover ? "hover" : "normal"));
            });
    button.pressedProperty()
        .addListener(
            (ignored, wasPressed, isPressed) -> {
              if (button.isDisabled()) {
                return;
              }
              if (isPressed) {
                button.setStyle(neonButtonStyle(accent, "active"));
              } else {
                button.setStyle(neonButtonStyle(accent, button.isHover() ? "hover" : "normal"));
              }
            });
    button.disabledProperty()
        .addListener(
            (ignored, wasDisabled, isDisabled) ->
                button.setStyle(neonButtonStyle(accent, isDisabled ? "disabled" : "normal")));
    return button;
  }

  private String neonButtonStyle(String accent, String state) {
    String background;
    String textColor = accent;
    String border = accent;
    switch (state) {
      case "hover" -> background = "#1a2647";
      case "active" -> background = "#22335f";
      case "disabled" -> {
        background = "#0f1528";
        textColor = "#6f7fa8";
        border = "#2d3756";
      }
      default -> background = "#11182f";
    }
    return "-fx-background-color: "
        + background
        + ";"
        + "-fx-text-fill: "
        + textColor
        + ";"
        + "-fx-border-color: "
        + border
        + ";"
        + "-fx-border-width: 1;"
        + "-fx-background-radius: 6;"
        + "-fx-border-radius: 6;";
  }

  private String panelStyle() {
    return "-fx-background-color: rgba(7, 13, 28, 0.92);"
        + "-fx-border-color: #233056;"
        + "-fx-border-width: 1;"
        + "-fx-border-radius: 10;"
        + "-fx-background-radius: 10;";
  }

  private void installKeyboardNavigation(Scene scene) {
    if (scene == null) {
      return;
    }
    scene.addEventHandler(
        KeyEvent.KEY_PRESSED,
        event -> {
          if (event.getTarget() instanceof TextArea) {
            return;
          }
          if (event.getCode() == KeyCode.SPACE) {
            if (trajectoryRunning) {
              controlService.pause();
              updateSystemStatus("SHORTCUT: PAUSE", UiSemanticState.PAUSED);
            } else {
              controlService.start();
              updateSystemStatus("SHORTCUT: RESUME", UiSemanticState.RUNNING);
            }
            event.consume();
            return;
          }
          if (event.getCode() == KeyCode.R) {
            trainingExecutionService.cancelTraining();
            controlService.reset();
            updateSystemStatus("SHORTCUT: RESET", UiSemanticState.TIMEOUT);
            event.consume();
          }
        });
  }

  private void installResponsiveLayout(Scene scene) {
    if (scene == null) {
      return;
    }
    scene.widthProperty().addListener((ignored, oldWidth, newWidth) -> applyResponsiveLayout(newWidth.doubleValue()));
    scene.heightProperty().addListener((ignored, oldHeight, newHeight) -> applyResponsiveLayout(scene.getWidth()));
    applyResponsiveLayout(scene.getWidth());
  }

  private void applyResponsiveLayout(double width) {
    double safeWidth = Math.max(1024.0, width);
    double controlWidth;
    double metricsWidth;
    double viewportHeight;
    if (safeWidth <= 1440.0) {
      controlWidth = 240.0;
      metricsWidth = 270.0;
      viewportHeight = 440.0;
    } else if (safeWidth <= 2200.0) {
      controlWidth = 280.0;
      metricsWidth = 320.0;
      viewportHeight = 520.0;
    } else {
      controlWidth = 340.0;
      metricsWidth = 390.0;
      viewportHeight = 620.0;
    }
    if (controlPanel != null) {
      controlPanel.setMinWidth(controlWidth);
      controlPanel.setPrefWidth(controlWidth);
      controlPanel.setMaxWidth(controlWidth);
    }
    if (metricsPanel != null) {
      metricsPanel.setMinWidth(metricsWidth);
      metricsPanel.setPrefWidth(metricsWidth);
      metricsPanel.setMaxWidth(metricsWidth);
    }
    if (mazeViewport != null) {
      mazeViewport.setMinHeight(viewportHeight);
      mazeViewport.setPrefHeight(viewportHeight);
    }
  }

  private void installFocusStyle(Control control, String accent) {
    if (control == null) {
      return;
    }
    String baseStyle = control.getStyle() == null ? "" : control.getStyle();
    control.setFocusTraversable(true);
    control.focusedProperty()
        .addListener(
            (ignored, oldFocused, focused) -> {
              if (focused) {
                control.setStyle(
                    baseStyle
                        + "-fx-border-color: "
                        + accent
                        + ";"
                        + "-fx-border-width: 2;"
                        + "-fx-effect: dropshadow(gaussian, "
                        + accent
                        + "66, 12, 0.4, 0, 0);");
              } else {
                control.setStyle(baseStyle);
              }
            });
  }

  private VBox buildNotificationLayer() {
    VBox layer = new VBox(8);
    layer.setAlignment(Pos.TOP_RIGHT);
    layer.setPickOnBounds(false);
    layer.setMouseTransparent(true);
    layer.setMaxWidth(340);
    return layer;
  }

  private void emitNotification(String key, String message, String accentHex) {
    if (notificationLayer == null || message == null || message.isBlank()) {
      return;
    }
    String safeKey = key == null || key.isBlank() ? message.trim() : key.trim();
    long now = System.currentTimeMillis();
    Long previous = notificationDedupe.get(safeKey);
    if (previous != null && now - previous < notificationDedupWindowMillis) {
      return;
    }
    notificationDedupe.put(safeKey, now);

    Label toast = new Label(message);
    toast.setWrapText(true);
    toast.setFont(Font.font("Consolas", 12));
    toast.setTextFill(Color.web("#dce8ff"));
    toast.setPadding(new Insets(10, 12, 10, 12));
    toast.setMaxWidth(320);
    toast.setStyle(
        "-fx-background-color: rgba(7, 13, 28, 0.96);"
            + "-fx-border-color: "
            + accentHex
            + ";"
            + "-fx-border-width: 1.4;"
            + "-fx-border-radius: 8;"
            + "-fx-background-radius: 8;");
    toast.setMouseTransparent(true);
    notificationLayer.getChildren().add(0, toast);
    while (notificationLayer.getChildren().size() > 4) {
      notificationLayer.getChildren().remove(notificationLayer.getChildren().size() - 1);
    }

    PauseTransition dismiss = new PauseTransition(Duration.millis(notificationDurationMillis));
    dismiss.setOnFinished(event -> notificationLayer.getChildren().remove(toast));
    dismiss.play();
  }

  private void bindMetricLabel(String metricName, Label label) {
    switch (metricName) {
      case "Steps" -> stepsValue = label;
      case "Collisions" -> {
        collisionsValue = label;
        emphasizePriorityMetric(label);
      }
      case "Reward" -> {
        rewardValue = label;
        emphasizePriorityMetric(label);
      }
      case "Elapsed" -> {
        elapsedValue = label;
        emphasizePriorityMetric(label);
      }
      case "Remaining" -> remainingValue = label;
      case "Coverage L/R" -> sideCoverageValue = label;
      case "Termination" -> {
        diagnosticTerminationValue = label;
        emphasizePriorityMetric(label);
      }
      case "Maze Coverage" -> diagnosticCoverageValue = label;
      case "Alert" -> diagnosticAlertValue = label;
      default -> {}
    }
  }

  private void applyMetrics(LiveEpisodeMetrics metrics) {
    lastLiveMetrics = metrics;
    Platform.runLater(
        () -> {
          if (stepsValue != null) {
            stepsValue.setText(Integer.toString(metrics.steps()));
          }
          if (collisionsValue != null) {
            collisionsValue.setText(Integer.toString(metrics.collisions()));
          }
          if (rewardValue != null) {
            rewardValue.setText(String.format(Locale.US, "%.1f", metrics.accumulatedReward()));
          }
          if (elapsedValue != null) {
            elapsedValue.setText(formatElapsed(metrics.elapsedMillis()));
          }
          if (remainingValue != null) {
            remainingValue.setText(formatElapsed(metrics.remainingMillis()));
          }
          if (diagnosticTerminationValue != null) {
            diagnosticTerminationValue.setText(metrics.terminationReason());
          }
          if (diagnosticCoverageValue != null) {
            diagnosticCoverageValue.setText(
                String.format(Locale.US, "%.0f%%", metrics.mazeCoverageRatio() * 100.0));
          }
          if (diagnosticAlertValue != null) {
            boolean timeoutAlert =
                metrics.remainingMillis() == 0L
                    && !"EXIT_REACHED".equals(metrics.terminationReason());
            boolean lowCoverageAlert =
                "IN_PROGRESS".equals(metrics.terminationReason())
                    && metrics.mazeCoverageRatio() < coverageAlertThreshold;
            if (timeoutAlert) {
              diagnosticAlertValue.setText("TIMEOUT RISK");
              diagnosticAlertValue.setTextFill(Color.web(UiSemanticState.TIMEOUT.hex()));
            } else if (lowCoverageAlert) {
              diagnosticAlertValue.setText("LOW COVERAGE");
              diagnosticAlertValue.setTextFill(Color.web(UiSemanticState.PAUSED.hex()));
            } else {
              diagnosticAlertValue.setText("NOMINAL");
              diagnosticAlertValue.setTextFill(Color.web(UiSemanticState.RUNNING.hex()));
            }
          }
          if (sideCoverageValue != null) {
            sideCoverageValue.setText(
                String.format(
                    Locale.US,
                    "%.0f%% / %.0f%%",
                    metrics.leftSideCoverage() * 100.0,
                    metrics.rightSideCoverage() * 100.0));
          }
          refreshEpisodeDetailsCard();
          refreshFocusModeHud();
          refreshContextualStatusPanel();
          renderLiveViewport(metrics);
        });
  }

  private void renderLiveViewport(LiveEpisodeMetrics metrics) {
    if (viewportMode == ViewportMode.RESUME || replayModeActive) {
      return;
    }
    if (metrics == null) {
      return;
    }
    synchronized (trajectoryLock) {
      trajectoryCells.clear();
      trajectoryCells.addAll(metrics.trajectory());
      trajectoryCurrent = metrics.currentPosition();
    }
    mazeViewportRenderer.renderTrajectory(metrics.trajectory());
    if (unexploredOverlayEnabled) {
      mazeViewportRenderer.renderUnexploredOverlay(metrics.trajectory());
    }
    if (miniHeatmapEnabled) {
      renderMiniHeatmap(metrics.trajectory(), metrics.currentPosition());
    }
  }

  private void applyTimeline(List<TrainingTimelineEntry> entries) {
    Platform.runLater(
        () -> {
          if (timelineEntriesBox == null) {
            return;
          }
          timelineEntriesBox.getChildren().clear();
          if (entries.isEmpty()) {
            timelineEntriesBox
                .getChildren()
                .add(timelinePlaceholder("No episodes completed yet. Start training to build timeline."));
            return;
          }
          for (TrainingTimelineEntry entry : entries) {
            timelineEntriesBox.getChildren().add(timelineRow(entry));
          }
        });
  }

  private void refreshRecentRunsAsync() {
    String mazeName = selectedMazeName;
    RecentRunsSortOption sort = selectedRecentRunsSort;
    Platform.runLater(
        () -> {
          if (recentRunsEntriesBox != null) {
            recentRunsEntriesBox.getChildren().setAll(timelinePlaceholder("Loading recent run catalog..."));
          }
        });
    if (mazeName == null || mazeName.isBlank()) {
      Platform.runLater(
          () -> {
            if (recentRunsEntriesBox != null) {
              recentRunsEntriesBox
                  .getChildren()
                  .setAll(timelinePlaceholder("No run catalog yet. Select a maze and launch training."));
            }
          });
      return;
    }
    recentRunsExecutor.execute(
        () -> {
          List<RecentRunComparisonRow> rows =
              recentRunsComparisonService.recentRuns(mazeName, sort);
          Platform.runLater(() -> renderRecentRuns(mazeName, sort, rows));
        });
  }

  private void refreshCoverageSummaryAsync() {
    Platform.runLater(
        () -> {
          if (coverageEntriesBox != null) {
            coverageEntriesBox.getChildren().setAll(timelinePlaceholder("Loading coverage catalog..."));
          }
        });
    recentRunsExecutor.execute(
        () -> {
          List<MazeCoverageSummaryRow> rows = mazeCoverageSummaryService.pendingCoverage();
          Platform.runLater(() -> renderCoverageSummary(rows));
        });
  }

  private void refreshPersistentHeatmapAsync() {
    String mazeName = selectedMazeName;
    if (mazeName == null || mazeName.isBlank()) {
      Platform.runLater(mazeViewportRenderer::clearPersistentHeatmap);
      return;
    }
    recentRunsExecutor.execute(
        () -> {
          List<CellVisitFrequency> frequencies =
              persistentMazeHeatmapService.loadAccumulatedHeatmap(mazeName, persistentHeatmapRuns);
          Platform.runLater(() -> renderPersistentHeatmap(mazeName, frequencies));
        });
  }

  private void renderPersistentHeatmap(String mazeName, List<CellVisitFrequency> frequencies) {
    if (!mazeName.equals(selectedMazeName)) {
      return;
    }
    accumulatedHeatmapFrequencies = frequencies == null ? List.of() : List.copyOf(frequencies);
    if (frequencies == null || frequencies.isEmpty()) {
      mazeViewportRenderer.clearPersistentHeatmap();
      refreshMiniHeatmap();
      return;
    }
    mazeViewportRenderer.renderPersistentHeatmap(frequencies);
    refreshMiniHeatmap();
  }

  private void renderRecentRuns(
      String mazeName, RecentRunsSortOption sort, List<RecentRunComparisonRow> rows) {
    if (recentRunsEntriesBox == null) {
      return;
    }
    if (!mazeName.equals(selectedMazeName) || sort != selectedRecentRunsSort) {
      return;
    }
    recentRunsEntriesBox.getChildren().clear();
    if (rows.isEmpty()) {
      recentRunsEntriesBox
          .getChildren()
          .add(timelinePlaceholder("No training runs stored yet. Launch a batch from Controls."));
      return;
    }
    for (RecentRunComparisonRow row : rows) {
      recentRunsEntriesBox.getChildren().add(recentRunRow(row));
    }
  }

  private void renderCoverageSummary(List<MazeCoverageSummaryRow> rows) {
    if (coverageEntriesBox == null) {
      return;
    }
    coverageEntriesBox.getChildren().clear();
    if (rows.isEmpty()) {
      coverageEntriesBox.getChildren().add(timelinePlaceholder("No pending mazes. Coverage is up to date."));
      return;
    }
    int shown = 0;
    for (MazeCoverageSummaryRow row : rows) {
      coverageEntriesBox
          .getChildren()
          .add(
              timelinePlaceholder(
                  row.mazeName() + " -> pending policies: " + row.pendingPolicies()));
      shown++;
      if (shown >= 5) {
        break;
      }
    }
  }

  private HBox recentRunRow(RecentRunComparisonRow row) {
    Label status = new Label(formatTerminalReason(row.terminalReason()));
    status.setFont(Font.font("Consolas", 11));
    status.setTextFill(Color.web(terminalReasonColor(row.terminalReason())));

    Label reward = new Label(String.format(Locale.US, "R %.1f", row.reward()));
    reward.setFont(Font.font("Consolas", 11));
    reward.setTextFill(Color.web("#b8ffcb"));

    Label collisions = new Label("C " + row.collisions());
    collisions.setFont(Font.font("Consolas", 11));
    collisions.setTextFill(Color.web("#ffd166"));

    Label netProgress = new Label(String.format(Locale.US, "NP %.1f", row.netProgress()));
    netProgress.setFont(Font.font("Consolas", 11));
    netProgress.setTextFill(Color.web("#7ef9ff"));

    Label sideCoverage =
        new Label(
            String.format(
                Locale.US,
                "L/R %.0f%%/%.0f%%",
                row.leftSideCoverage() * 100.0,
                row.rightSideCoverage() * 100.0));
    sideCoverage.setFont(Font.font("Consolas", 11));
    sideCoverage.setTextFill(Color.web("#9db2ff"));

    Label entropy =
        new Label(
            String.format(
                Locale.US, "H %.2f%s", row.pathEntropy(), row.lowEntropyAlert() ? " !" : ""));
    entropy.setFont(Font.font("Consolas", 11));
    entropy.setTextFill(Color.web(row.lowEntropyAlert() ? "#ff6b8a" : "#7ef9ff"));

    Label health =
        new Label(
            String.format(
                Locale.US,
                "THI %.1f%s",
                row.trainingHealthIndex(),
                row.healthRegression() ? " !" : ""));
    health.setFont(Font.font("Consolas", 11));
    health.setTextFill(Color.web(row.healthRegression() ? "#ff6b8a" : "#89ff9a"));

    Label rewardVersion =
        new Label(
            "RV "
                + (row.rewardVersion() == null || row.rewardVersion().isBlank()
                    ? "v1"
                    : row.rewardVersion()));
    rewardVersion.setFont(Font.font("Consolas", 11));
    rewardVersion.setTextFill(Color.web("#7ef9ff"));

    Label elapsed = new Label(formatElapsed(row.elapsedMillis()));
    elapsed.setFont(Font.font("Consolas", 11));
    elapsed.setTextFill(Color.web("#9db2ff"));

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    return new HBox(
        8,
        status,
        reward,
        collisions,
        netProgress,
        sideCoverage,
        entropy,
        health,
        rewardVersion,
        spacer,
        elapsed);
  }

  private HBox timelineRow(TrainingTimelineEntry entry) {
    Label status = new Label(formatTerminalReason(entry.terminalReason()));
    status.setFont(Font.font("Consolas", 12));
    status.setTextFill(Color.web(terminalReasonColor(entry.terminalReason())));

    Label reward = new Label(String.format(Locale.US, "R %.1f", entry.reward()));
    reward.setFont(Font.font("Consolas", 12));
    reward.setTextFill(Color.web("#b8ffcb"));

    Label elapsed = new Label(formatElapsed(entry.durationMillis()));
    elapsed.setFont(Font.font("Consolas", 12));
    elapsed.setTextFill(Color.web("#9db2ff"));

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    return new HBox(8, status, spacer, reward, elapsed);
  }

  private Label timelinePlaceholder(String text) {
    Label label = new Label(text);
    label.setFont(Font.font(UI_FONT_FAMILY, FONT_SIZE_SECTION_LABEL));
    label.setTextFill(Color.web("#5e719f"));
    return label;
  }

  private void emphasizePriorityMetric(Label label) {
    if (label == null) {
      return;
    }
    label.setFont(Font.font(UI_FONT_FAMILY, FONT_SIZE_METRIC_VALUE_PRIORITY));
    label.setTextFill(Color.web("#d9fff5"));
  }

  private String terminalReasonColor(String terminalReason) {
    if (terminalReason == null || terminalReason.isBlank()) {
      return "#5e719f";
    }
    return switch (terminalReason.trim().toUpperCase(Locale.ROOT)) {
      case "EXIT_REACHED" -> UiSemanticState.RUNNING.hex();
      case "TIMEOUT" -> UiSemanticState.PAUSED.hex();
      case "DEAD_END" -> "#ff9f43";
      case "ABORTED", "ERROR" -> UiSemanticState.TIMEOUT.hex();
      default -> "#5e719f";
    };
  }

  private String formatTerminalReason(String terminalReason) {
    if (terminalReason == null || terminalReason.isBlank()) {
      return "UNKNOWN";
    }
    return switch (terminalReason.trim().toUpperCase(Locale.ROOT)) {
      case "EXIT_REACHED" -> "EXIT";
      case "TIMEOUT" -> "TIMEOUT";
      case "DEAD_END" -> "DEAD_END";
      case "ABORTED" -> "ABORTED";
      case "ERROR" -> "ERROR";
      default -> terminalReason.toUpperCase(Locale.ROOT);
    };
  }

  private void selectActiveAlgorithm(ComboBox<MovementPolicyOption> selector) {
    String activePolicy = controlService.activeMovementPolicy();
    for (MovementPolicyOption option : selector.getItems()) {
      if (option.id().equals(activePolicy)) {
        selector.getSelectionModel().select(option);
        return;
      }
    }
    if (!selector.getItems().isEmpty()) {
      selector.getSelectionModel().selectFirst();
      controlService.selectMovementPolicy(selector.getValue().id());
    }
  }

  private void updateActivePolicyLabel() {
    if (activePolicyValue == null) {
      return;
    }
    activePolicyValue.setText(
        "ACTIVE ALGORITHM: " + controlService.activeMovementPolicy().toUpperCase(Locale.ROOT));
    refreshEpisodeDetailsCard();
    refreshContextualStatusPanel();
  }

  private void selectActivePreset(ComboBox<TrainingPresetOption> selector) {
    Long activePresetId = controlService.activeTrainingPresetId();
    if (activePresetId != null) {
      for (TrainingPresetOption option : selector.getItems()) {
        if (option.id() == activePresetId.longValue()) {
          selector.getSelectionModel().select(option);
          return;
        }
      }
    }
    if (!selector.getItems().isEmpty()) {
      selector.getSelectionModel().selectFirst();
      controlService.applyTrainingPreset(selector.getValue().id());
    }
  }

  private void updateActivePresetLabel() {
    if (activePresetValue == null) {
      return;
    }
    Long activePresetId = controlService.activeTrainingPresetId();
    String text =
        activePresetId == null ? "ACTIVE PRESET: NONE" : "ACTIVE PRESET: #" + activePresetId;
    activePresetValue.setText(text);
  }

  private void updateActiveSpeedLabel() {
    if (activeSpeedValue == null) {
      return;
    }
    activeSpeedValue.setText(
        "ACTIVE SPEED: " + liveMetricsService.simulationSpeed().name().toUpperCase(Locale.ROOT));
    refreshContextualStatusPanel();
  }

  private void refreshSessionConfigCardPreview() {
    updateControlAvailability();
    if (sessionConfigLocked) {
      return;
    }
    applySessionConfigCard(
        selectedMazeName == null ? "-" : selectedMazeName,
        controlService.activeMovementPolicy(),
        null,
        trainingPresetService.activePreset().map(TrainingPreset::timeout).orElse(null),
        selectedTargetDifficulty);
  }

  private void lockSessionConfigCard(Long effectiveSeed, TrainingSessionConfig sessionConfig) {
    sessionConfigLocked = true;
    applySessionConfigCard(
        sessionConfig == null || sessionConfig.mazeId() == null || sessionConfig.mazeId().isBlank()
            ? selectedMazeName
            : sessionConfig.mazeId(),
        sessionConfig == null ? controlService.activeMovementPolicy() : sessionConfig.policyId(),
        effectiveSeed,
        sessionConfig == null ? null : sessionConfig.timeout(),
        sessionConfig == null ? selectedTargetDifficulty : sessionConfig.difficultyTarget());
  }

  private void applySessionConfigCard(
      String mazeName,
      String policy,
      Long seed,
      java.time.Duration timeout,
      TrainingTargetDifficulty difficulty) {
    if (effectiveMazeValue != null) {
      effectiveMazeValue.setText(
          "Maze: " + (mazeName == null || mazeName.isBlank() ? "-" : mazeName));
    }
    if (effectivePolicyValue != null) {
      effectivePolicyValue.setText(
          "Policy: " + (policy == null || policy.isBlank() ? "-" : policy));
    }
    if (effectiveSeedCardValue != null) {
      effectiveSeedCardValue.setText(seed == null ? "Seed: AUTO" : "Seed: " + seed);
    }
    if (effectiveTimeoutValue != null) {
      effectiveTimeoutValue.setText("Timeout: " + formatTimeout(timeout));
    }
    if (effectiveDifficultyValue != null) {
      TrainingTargetDifficulty currentDifficulty =
          difficulty == null ? TrainingTargetDifficulty.MEDIUM : difficulty;
      effectiveDifficultyValue.setText("Difficulty: " + currentDifficulty.label());
    }
  }

  private String formatTimeout(java.time.Duration timeout) {
    if (timeout == null) {
      return "-";
    }
    long seconds = Math.max(0L, timeout.toSeconds());
    long minutes = seconds / 60L;
    long remainingSeconds = seconds % 60L;
    if (remainingSeconds == 0L) {
      return minutes + "m";
    }
    return minutes + "m " + remainingSeconds + "s";
  }

  private void updateSystemStatus(String text, String color) {
    if (systemStatusValue == null) {
      return;
    }
    systemStatusValue.setText(text);
    systemStatusValue.setTextFill(Color.web(color));
  }

  private void updateSystemStatus(String text, UiSemanticState state) {
    updateSystemStatus(text, state.hex());
  }

  private void applySignalState(Label label, String text, UiSemanticState state) {
    if (label == null) {
      return;
    }
    label.setText(text);
    label.setTextFill(Color.web(state.hex()));
  }

  private void updateSessionHud(Long seed, String mode) {
    activeSessionSeed = seed;
    if (sessionSeedValue != null) {
      sessionSeedValue.setText(seed == null ? "SEED: -" : "SEED: " + seed);
    }
    if (executionModeValue != null) {
      executionModeValue.setText(
          "MODE: " + (mode == null ? "VISUAL" : mode.toUpperCase(Locale.ROOT)));
    }
    refreshEpisodeDetailsCard();
  }

  private String formatElapsed(long elapsedMillis) {
    long totalSeconds = elapsedMillis / 1_000;
    long minutes = totalSeconds / 60;
    long seconds = totalSeconds % 60;
    return String.format("%02d:%02d", minutes, seconds);
  }

  private void updateControlAvailability() {
    if (startButton == null || pauseButton == null || resetButton == null) {
      return;
    }
    boolean missingPreset = trainingPresetService.activePreset().isEmpty();
    boolean missingMaze = selectedMaze == null;
    boolean running = trajectoryRunning;

    String startDisabledReason = null;
    if (startActionProcessing) {
      startDisabledReason = "Start disabled while training is being started.";
    } else if (running) {
      startDisabledReason = "Start disabled while a training run is active.";
    } else if (missingPreset) {
      startDisabledReason = "Start disabled: select a training preset first.";
    } else if (missingMaze) {
      startDisabledReason = "Start disabled: select a maze first.";
    }
    applyDisabledReason(startButton, startDisabledReason);

    String pauseDisabledReason = running ? null : "Pause disabled because no run is active.";
    applyDisabledReason(pauseButton, pauseDisabledReason);

    String resetDisabledReason = running ? null : "Reset disabled because no run is active.";
    applyDisabledReason(resetButton, resetDisabledReason);
  }

  private void applyDisabledReason(Button button, String disabledReason) {
    if (button == null) {
      return;
    }
    boolean disabled = disabledReason != null && !disabledReason.isBlank();
    button.setDisable(disabled);
    button.setTooltip(new Tooltip(disabled ? disabledReason : button.getText()));
  }

  private void toggleEpisodeDetailsPanel() {
    if (focusModeEnabled) {
      updateSystemStatus(
          "Episode details stay collapsed while focus mode is active.", UiSemanticState.PAUSED);
      return;
    }
    episodeDetailsCollapsed = !episodeDetailsCollapsed;
    applyEpisodeDetailsPanelState();
  }

  private void toggleFocusMode() {
    setFocusModeEnabled(!focusModeEnabled);
  }

  private void setFocusModeEnabled(boolean enabled) {
    focusModeEnabled = enabled;
    if (focusModeEnabled) {
      detailsCollapsedBeforeFocus = episodeDetailsCollapsed;
      episodeDetailsCollapsed = true;
    } else {
      episodeDetailsCollapsed = detailsCollapsedBeforeFocus;
    }
    applyEpisodeDetailsPanelState();
    if (controlPanel != null) {
      controlPanel.setVisible(!focusModeEnabled);
      controlPanel.setManaged(!focusModeEnabled);
    }
    if (focusModeHud != null) {
      focusModeHud.setVisible(focusModeEnabled);
      focusModeHud.setManaged(focusModeEnabled);
    }
    if (focusModeToggleButton != null) {
      focusModeToggleButton.setText(focusModeEnabled ? "Focus Mode: ON" : "Focus Mode: OFF");
    }
    updateSystemStatus(
        focusModeEnabled ? "FOCUS MODE ENABLED" : "FOCUS MODE DISABLED",
        focusModeEnabled ? UiSemanticState.RUNNING : UiSemanticState.IDLE);
    emitNotification(
        focusModeEnabled ? "focus.on" : "focus.off",
        focusModeEnabled ? "Focus mode enabled." : "Focus mode disabled.",
        focusModeEnabled ? UiSemanticState.RUNNING.hex() : UiSemanticState.IDLE.hex());
    refreshFocusModeHud();
  }

  private void applyEpisodeDetailsPanelState() {
    if (dashboardPane != null && metricsPanel != null) {
      dashboardPane.setRight(episodeDetailsCollapsed ? null : metricsPanel);
    }
    if (episodeDetailsToggleButton != null) {
      episodeDetailsToggleButton.setText(
          episodeDetailsCollapsed ? "Episode Details: OFF" : "Episode Details: ON");
      episodeDetailsToggleButton.setDisable(focusModeEnabled);
      episodeDetailsToggleButton.setTooltip(
          new Tooltip(
              focusModeEnabled
                  ? "Episode details stay collapsed while focus mode is active."
                  : "Toggle episode details side panel."));
    }
  }

  private void refreshEpisodeDetailsCard() {
    if (episodeSeedDetailValue == null
        || episodePolicyDetailValue == null
        || episodeStepDetailValue == null
        || episodeRewardDetailValue == null) {
      return;
    }
    episodeSeedDetailValue.setText(
        activeSessionSeed == null ? "Seed: AUTO" : "Seed: " + activeSessionSeed);
    String activePolicy =
        controlService.activeMovementPolicy() == null
            ? "-"
            : controlService.activeMovementPolicy().toUpperCase(Locale.ROOT);
    episodePolicyDetailValue.setText("Policy: " + activePolicy);
    int step = lastLiveMetrics == null ? 0 : Math.max(0, lastLiveMetrics.steps());
    double lastReward = lastLiveMetrics == null ? 0.0 : lastLiveMetrics.accumulatedReward();
    episodeStepDetailValue.setText("Step: " + step);
    episodeRewardDetailValue.setText(String.format(Locale.US, "Last reward: %.1f", lastReward));
  }

  private void refreshFocusModeHud() {
    if (focusModeStatusValue == null
        || focusModeElapsedValue == null
        || focusModeRewardValue == null
        || focusModeCollisionsValue == null) {
      return;
    }
    LiveEpisodeMetrics metrics = lastLiveMetrics;
    String episodeState =
        metrics == null
            ? "IDLE"
            : formatTerminalReason(metrics.terminationReason()).toUpperCase(Locale.ROOT);
    String elapsed = metrics == null ? "--:--" : formatElapsed(metrics.elapsedMillis());
    String reward =
        metrics == null
            ? "0.0"
            : String.format(Locale.US, "%.1f", metrics.accumulatedReward());
    String collisions = metrics == null ? "0" : Integer.toString(metrics.collisions());
    focusModeStatusValue.setText("Episode: " + episodeState);
    focusModeElapsedValue.setText("Elapsed: " + elapsed);
    focusModeRewardValue.setText("Reward: " + reward);
    focusModeCollisionsValue.setText("Collisions: " + collisions);
  }

  private void startTrajectoryEpisode() {
    if (selectedMaze == null || mazeViewport == null) {
      return;
    }
    liveMetricsService.setActiveMaze(selectedMaze);
    synchronized (trajectoryLock) {
      trajectoryCells.clear();
      trajectoryCurrent = selectedMaze.start();
      trajectoryCells.add(trajectoryCurrent);
    }
    Platform.runLater(() -> mazeViewportRenderer.renderTrajectory(List.copyOf(trajectoryCells)));
    refreshUnexploredOverlay();
    refreshMiniHeatmap();
    trajectoryRunning = true;
  }

  private void resetTrajectoryEpisode() {
    trajectoryRunning = false;
    synchronized (trajectoryLock) {
      trajectoryCells.clear();
      trajectoryCurrent = null;
    }
    Platform.runLater(mazeViewportRenderer::clearTrajectory);
    Platform.runLater(mazeViewportRenderer::clearUnexploredOverlay);
    refreshMiniHeatmap();
  }

  private synchronized void stopTrajectoryTicker() {
    if (trajectoryTicker != null) {
      trajectoryTicker.cancel(false);
      trajectoryTicker = null;
    }
  }

  private synchronized void restartTrajectoryTicker() {
    // Live trajectory now comes from LiveMetricsService updates.
  }

  private void refreshReplayEpisodesAsync() {
    Platform.runLater(
        () -> {
          if (replayStatusValue != null) {
            replayStatusValue.setText("Loading successful episodes...");
          }
        });
    recentRunsExecutor.execute(
        () -> {
          List<TrainingSessionEntity> sessions = trainingSessionRepository.findRecent(1);
          if (sessions.isEmpty()) {
            Platform.runLater(
                () -> {
                  replayEpisodes = List.of();
                  replayEpisodeIndex = 0;
                  replayTrajectoryIndex = 0;
                  if (replayStatusValue != null) {
                    replayStatusValue.setText("No successful episodes yet. Run training to generate successes.");
                  }
                  if (viewportMode == ViewportMode.RESUME) {
                    mazeViewportRenderer.clearTrajectory();
                    refreshMiniHeatmap();
                  }
                });
            return;
          }
          String sessionId = sessions.get(0).id();
          List<SuccessfulEpisodeReplay> replays =
              successfulEpisodeResumeService.listByTrainingSession(sessionId, 40);
          Platform.runLater(() -> applyReplayEpisodes(sessionId, replays));
        });
  }

  private void applyReplayEpisodes(String sessionId, List<SuccessfulEpisodeReplay> replays) {
    replayEpisodes = replays == null ? List.of() : List.copyOf(replays);
    replayEpisodeIndex = 0;
    replayTrajectoryIndex = 0;
    if (replayEpisodes.isEmpty()) {
      if (replayStatusValue != null) {
        replayStatusValue.setText(
            "Session "
                + sessionId
                + ": no EXIT_REACHED episodes. Run additional batches to populate resume.");
      }
      if (viewportMode == ViewportMode.RESUME) {
        mazeViewportRenderer.clearTrajectory();
        refreshMiniHeatmap();
      }
      refreshContextualStatusPanel();
      return;
    }
    if (replayStatusValue != null) {
      replayStatusValue.setText(
          "Session " + sessionId + ": loaded " + replayEpisodes.size() + " successful replay(s).");
    }
    if (viewportMode == ViewportMode.RESUME) {
      renderReplayFrame();
    }
    refreshContextualStatusPanel();
  }

  private void playReplay() {
    if (viewportMode != ViewportMode.RESUME) {
      setViewportMode(ViewportMode.RESUME);
    }
    if (replayEpisodes.isEmpty()) {
      if (replayStatusValue != null) {
        replayStatusValue.setText("No successful episodes available to replay.");
      }
      return;
    }
    replayModeActive = true;
    stopReplayTicker();
    long period = Math.max(40L, liveMetricsService.simulationSpeed().trajectoryTickMillis());
    replayTicker =
        trajectoryScheduler.scheduleAtFixedRate(
            this::advanceReplayFrame, period, period, TimeUnit.MILLISECONDS);
    if (replayStatusValue != null) {
      replayStatusValue.setText("Replay playing episode #" + activeReplay().trainingRunId());
    }
    refreshContextualStatusPanel();
  }

  private void pauseReplay() {
    stopReplayTicker();
    replayModeActive = false;
    if (replayStatusValue != null && !replayEpisodes.isEmpty()) {
      replayStatusValue.setText(
          "Replay paused at step " + replayTrajectoryIndex + " of #" + activeReplay().trainingRunId());
    }
    refreshContextualStatusPanel();
  }

  private void restartReplay() {
    if (viewportMode != ViewportMode.RESUME) {
      setViewportMode(ViewportMode.RESUME);
    }
    if (replayEpisodes.isEmpty()) {
      return;
    }
    replayModeActive = true;
    replayTrajectoryIndex = 0;
    renderReplayFrame();
    if (replayStatusValue != null) {
      replayStatusValue.setText("Replay restarted for #" + activeReplay().trainingRunId());
    }
    refreshContextualStatusPanel();
  }

  private void nextReplayEpisode() {
    if (viewportMode != ViewportMode.RESUME) {
      setViewportMode(ViewportMode.RESUME);
    }
    if (replayEpisodes.isEmpty()) {
      return;
    }
    replayModeActive = true;
    replayEpisodeIndex = (replayEpisodeIndex + 1) % replayEpisodes.size();
    replayTrajectoryIndex = 0;
    renderReplayFrame();
    if (replayStatusValue != null) {
      replayStatusValue.setText("Replay switched to #" + activeReplay().trainingRunId());
    }
    refreshContextualStatusPanel();
  }

  private void advanceReplayFrame() {
    if (!replayModeActive || replayEpisodes.isEmpty()) {
      return;
    }
    SuccessfulEpisodeReplay replay = activeReplay();
    if (replay.trajectory().isEmpty()) {
      stopReplayTicker();
      return;
    }
    replayTrajectoryIndex = Math.min(replayTrajectoryIndex + 1, replay.trajectory().size());
    Platform.runLater(this::renderReplayFrame);
    if (replayTrajectoryIndex >= replay.trajectory().size()) {
      stopReplayTicker();
      replayModeActive = false;
      Platform.runLater(
          () -> {
            if (replayStatusValue != null) {
              replayStatusValue.setText("Replay finished for #" + replay.trainingRunId());
            }
            refreshContextualStatusPanel();
          });
    }
  }

  private void renderReplayFrame() {
    if (replayEpisodes.isEmpty()) {
      return;
    }
    SuccessfulEpisodeReplay replay = activeReplay();
    List<GridPosition> trajectory = replay.trajectory();
    if (trajectory.isEmpty()) {
      mazeViewportRenderer.clearTrajectory();
      return;
    }
    int endExclusive = Math.max(1, Math.min(replayTrajectoryIndex + 1, trajectory.size()));
    List<GridPosition> frame = trajectory.subList(0, endExclusive);
    mazeViewportRenderer.renderTrajectory(frame);
    if (miniHeatmapEnabled) {
      renderMiniHeatmap(frame, frame.get(frame.size() - 1));
    }
    if (unexploredOverlayEnabled) {
      mazeViewportRenderer.renderUnexploredOverlay(frame);
    }
  }

  private SuccessfulEpisodeReplay activeReplay() {
    int safeIndex = Math.max(0, Math.min(replayEpisodeIndex, replayEpisodes.size() - 1));
    replayEpisodeIndex = safeIndex;
    return replayEpisodes.get(safeIndex);
  }

  private synchronized void stopReplayTicker() {
    if (replayTicker != null) {
      replayTicker.cancel(false);
      replayTicker = null;
    }
  }

  private void setViewportMode(ViewportMode mode) {
    viewportMode = mode == null ? ViewportMode.LIVE : mode;
    if (viewportMode == ViewportMode.LIVE) {
      replayModeActive = false;
      stopReplayTicker();
      if (lastLiveMetrics != null) {
        renderLiveViewport(lastLiveMetrics);
      }
      if (replayStatusValue != null) {
        replayStatusValue.setText("Live mode active.");
      }
      refreshContextualStatusPanel();
      return;
    }
    refreshReplayEpisodesAsync();
    if (!replayEpisodes.isEmpty()) {
      renderReplayFrame();
    } else if (replayStatusValue != null) {
      replayStatusValue.setText("Resume mode: no successful episodes yet.");
    }
    refreshContextualStatusPanel();
  }

  private void refreshContextualStatusPanel() {
    if (contextModeValue == null
        || contextSummaryValue == null
        || contextDetailValue == null
        || contextSignalValue == null) {
      return;
    }
    if (viewportMode == ViewportMode.LIVE) {
      applyLiveContextSummary();
      return;
    }
    applyResumeContextSummary();
  }

  private void applyLiveContextSummary() {
    LiveEpisodeMetrics metrics = lastLiveMetrics;
    String policy = controlService.activeMovementPolicy().toUpperCase(Locale.ROOT);
    String speed = liveMetricsService.simulationSpeed().name().toUpperCase(Locale.ROOT);
    boolean active =
        metrics != null
            && "IN_PROGRESS".equalsIgnoreCase(metrics.terminationReason())
            && !metrics.trajectory().isEmpty();
    String termination = metrics == null ? "IDLE" : formatTerminalReason(metrics.terminationReason());
    int steps = metrics == null ? 0 : metrics.steps();

    contextModeValue.setText("Mode: LIVE");
    contextSummaryValue.setText("Episode: " + termination + " | Steps: " + steps);
    contextDetailValue.setText("Algorithm: " + policy + " | Speed: " + speed);
    applySignalState(
        contextSignalValue,
        active ? "Signal: STREAMING" : "Signal: IDLE",
        active ? UiSemanticState.RUNNING : UiSemanticState.PAUSED);
  }

  private void applyResumeContextSummary() {
    contextModeValue.setText("Mode: RESUME");
    if (replayEpisodes.isEmpty()) {
      contextSummaryValue.setText("No successful episodes available.");
      contextDetailValue.setText("Select/complete successful runs to inspect context.");
      applySignalState(contextSignalValue, "Signal: EMPTY", UiSemanticState.PAUSED);
      return;
    }
    SuccessfulEpisodeReplay replay = activeReplay();
    String terminalReason = formatTerminalReason(replay.terminalReason());
    String duration = formatElapsed(replay.elapsedMillis());
    String reward = String.format(Locale.US, "%.1f", replay.totalReward());
    contextSummaryValue.setText(
        "Success #" + replay.trainingRunId() + " | Terminal: " + terminalReason);
    contextDetailValue.setText("Duration: " + duration + " | Reward: " + reward);
    applySignalState(
        contextSignalValue,
        replayModeActive ? "Signal: PLAYING" : "Signal: READY",
        replayModeActive ? UiSemanticState.RUNNING : UiSemanticState.IDLE);
  }

  private void refreshUnexploredOverlay() {
    if (!unexploredOverlayEnabled) {
      Platform.runLater(mazeViewportRenderer::clearUnexploredOverlay);
      return;
    }
    List<GridPosition> snapshot = trajectorySnapshot();
    Platform.runLater(() -> mazeViewportRenderer.renderUnexploredOverlay(snapshot));
  }

  private void refreshMiniHeatmap() {
    MiniHeatmapSnapshot snapshot = miniHeatmapSnapshot();
    Platform.runLater(() -> renderMiniHeatmap(snapshot.trajectory(), snapshot.currentPosition()));
  }

  private void renderMiniHeatmap(List<GridPosition> trajectory, GridPosition currentPosition) {
    if (miniHeatmapGrid == null || selectedMaze == null) {
      return;
    }
    miniHeatmapGrid.getChildren().clear();
    if (!miniHeatmapEnabled) {
      return;
    }
    java.util.Map<GridPosition, Integer> activeVisits = new java.util.HashMap<>();
    for (GridPosition position : trajectory) {
      if (!selectedMaze.isInside(position) || selectedMaze.isWall(position)) {
        continue;
      }
      int value = activeVisits.getOrDefault(position, 0) + 1;
      activeVisits.put(position, value);
    }
    java.util.Map<GridPosition, Integer> accumulatedVisits = new java.util.HashMap<>();
    for (CellVisitFrequency frequency : accumulatedHeatmapFrequencies) {
      if (!selectedMaze.isInside(frequency.position())
          || selectedMaze.isWall(frequency.position())) {
        continue;
      }
      accumulatedVisits.put(frequency.position(), Math.max(0, frequency.visits()));
    }
    int maxVisits = 1;
    for (int count : activeVisits.values()) {
      maxVisits = Math.max(maxVisits, count);
    }
    for (int count : accumulatedVisits.values()) {
      maxVisits = Math.max(maxVisits, count);
    }
    for (int row = 0; row < selectedMaze.rows(); row++) {
      for (int col = 0; col < selectedMaze.cols(); col++) {
        GridPosition position = new GridPosition(row, col);
        Rectangle cell = new Rectangle(7.0, 7.0);
        double strokeWidth = 0.3;
        if (selectedMaze.isWall(position)) {
          cell.setFill(Color.color(0.08, 0.12, 0.22, 0.95));
          cell.setStroke(Color.color(0.16, 0.22, 0.36, 0.8));
        } else {
          int activeCount = activeVisits.getOrDefault(position, 0);
          int accumulatedCount = accumulatedVisits.getOrDefault(position, 0);
          double activeIntensity =
              activeCount <= 0 ? 0.0 : (double) activeCount / (double) maxVisits;
          double accumulatedIntensity =
              accumulatedCount <= 0 ? 0.0 : (double) accumulatedCount / (double) maxVisits;
          boolean leftHalf = col < (selectedMaze.cols() / 2);
          double intensity;
          double red;
          double green;
          double blue;
          double alpha;
          switch (heatmapComparisonMode) {
            case ACTIVE_ONLY -> {
              intensity = activeIntensity;
              red = 0.18 + (0.18 * intensity);
              green = 0.68 + (0.28 * intensity);
              blue = 0.70 + (0.24 * intensity);
              alpha = 0.20 + (0.70 * intensity);
            }
            case ACCUMULATED_ONLY -> {
              intensity = accumulatedIntensity;
              red = 0.35 + (0.65 * intensity);
              green = 0.24 + (0.58 * intensity);
              blue = 0.12 + (0.38 * intensity);
              alpha = 0.20 + (0.70 * intensity);
            }
            case SPLIT -> {
              intensity = leftHalf ? activeIntensity : accumulatedIntensity;
              if (leftHalf) {
                red = 0.18 + (0.18 * intensity);
                green = 0.68 + (0.28 * intensity);
                blue = 0.70 + (0.24 * intensity);
              } else {
                red = 0.35 + (0.65 * intensity);
                green = 0.24 + (0.58 * intensity);
                blue = 0.12 + (0.38 * intensity);
              }
              alpha = 0.20 + (0.70 * intensity);
            }
            case SUPERPOSED -> {
              double combined = Math.max(activeIntensity, accumulatedIntensity);
              red = 0.22 + (0.65 * accumulatedIntensity);
              green = 0.26 + (0.56 * activeIntensity);
              blue = 0.75 - (0.40 * accumulatedIntensity) + (0.18 * activeIntensity);
              alpha = 0.24 + (0.70 * combined);
            }
            default -> {
              red = 0.18;
              green = 0.28;
              blue = 0.72;
              alpha = 0.25;
            }
          }
          cell.setFill(Color.color(red, green, blue, alpha));
          cell.setStroke(
              Color.color(
                  0.26,
                  0.88,
                  1.0,
                  0.16 + (0.35 * Math.max(activeIntensity, accumulatedIntensity))));
          if (position.equals(selectedMaze.exit())) {
            cell.setFill(Color.color(1.0, 0.78, 0.24, 0.95));
            cell.setStroke(Color.color(1.0, 0.93, 0.55, 0.95));
            strokeWidth = 1.2;
          }
          if (currentPosition != null && position.equals(currentPosition)) {
            cell.setFill(Color.color(0.30, 0.97, 1.0, 0.98));
            cell.setStroke(Color.color(0.90, 1.0, 1.0, 1.0));
            strokeWidth = 1.5;
          }
        }
        cell.setStrokeWidth(strokeWidth);
        miniHeatmapGrid.add(cell, col, row);
      }
    }
  }

  private MiniHeatmapSnapshot miniHeatmapSnapshot() {
    synchronized (trajectoryLock) {
      return new MiniHeatmapSnapshot(List.copyOf(trajectoryCells), trajectoryCurrent);
    }
  }

  private List<GridPosition> trajectorySnapshot() {
    synchronized (trajectoryLock) {
      return List.copyOf(trajectoryCells);
    }
  }

  private record MiniHeatmapSnapshot(List<GridPosition> trajectory, GridPosition currentPosition) {}

  private enum UiSemanticState {
    RUNNING("#89ff9a"),
    PAUSED("#ffd166"),
    SUCCESS("#7ef9ff"),
    TIMEOUT("#ff6b8a"),
    VALIDATION_ERROR("#ff6b8a"),
    IDLE("#7ef9ff");

    private final String hex;

    UiSemanticState(String hex) {
      this.hex = hex;
    }

    String hex() {
      return hex;
    }
  }

  private enum ViewportMode {
    LIVE("LIVE"),
    RESUME("RESUME");

    private final String label;

    ViewportMode(String label) {
      this.label = label;
    }

    String label() {
      return label;
    }
  }

  private enum HeatmapComparisonMode {
    ACTIVE_ONLY("Active"),
    ACCUMULATED_ONLY("Accumulated"),
    SUPERPOSED("Superposed"),
    SPLIT("Split");

    private final String label;

    HeatmapComparisonMode(String label) {
      this.label = label;
    }

    String label() {
      return label;
    }
  }

  private void onTrainingLifecycleEvent(TrainingLifecycleEvent event) {
    Platform.runLater(
        () -> {
          switch (event.type()) {
            case STARTED -> {
              stopReplayTicker();
              replayModeActive = false;
              startTrajectoryEpisode();
              if (diagnosticAlertValue != null) {
                diagnosticAlertValue.setText("NOMINAL");
                diagnosticAlertValue.setTextFill(Color.web(UiSemanticState.RUNNING.hex()));
              }
              updateSystemStatus("TRAINING RUNNING", UiSemanticState.RUNNING);
              emitNotification("event.started", "Training running.", "#89ff9a");
            }
            case PAUSED -> {
              trajectoryRunning = false;
              updateSystemStatus("TRAINING PAUSED", UiSemanticState.PAUSED);
              emitNotification("event.paused", "Training paused.", "#ffd166");
            }
            case RESUMED -> {
              trajectoryRunning = true;
              updateSystemStatus("TRAINING RESUMED", UiSemanticState.RUNNING);
            }
            case FINISHED -> {
              startActionProcessing = false;
              if (startButton != null) {
                startButton.setText("Start");
              }
              resetTrajectoryEpisode();
              refreshPersistentHeatmapAsync();
              refreshReplayEpisodesAsync();
              if (event.detail() != null && event.detail().contains("RESET")) {
                updateSessionHud(null, "visual");
              }
              updateSystemStatus("TRAINING FINISHED", UiSemanticState.SUCCESS);
              if (lastLiveMetrics != null
                  && "EXIT_REACHED".equalsIgnoreCase(lastLiveMetrics.terminationReason())) {
                emitNotification("event.success", "Episode success reached exit.", "#89ff9a");
              }
            }
            case TIMED_OUT -> {
              startActionProcessing = false;
              if (startButton != null) {
                startButton.setText("Start");
              }
              trajectoryRunning = false;
              refreshPersistentHeatmapAsync();
              updateSystemStatus("TRAINING TIMEOUT", UiSemanticState.TIMEOUT);
              emitNotification("event.timeout", "Episode timeout reached.", "#ff6b8a");
            }
          }
          updateControlAvailability();
          refreshFocusModeHud();
          refreshContextualStatusPanel();
        });
  }

  @PreDestroy
  public synchronized void shutdownTrajectoryOverlay() {
    stopTrajectoryTicker();
    stopReplayTicker();
    trajectoryScheduler.shutdownNow();
    recentRunsExecutor.shutdownNow();
  }
}
