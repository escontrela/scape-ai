package com.davidpe.scapeai.ui;

import com.davidpe.scapeai.application.LiveEpisodeMetrics;
import com.davidpe.scapeai.application.LiveMetricsService;
import com.davidpe.scapeai.application.MovementPolicyOption;
import com.davidpe.scapeai.application.RecentRunComparisonRow;
import com.davidpe.scapeai.application.RecentRunsComparisonService;
import com.davidpe.scapeai.application.RecentRunsSortOption;
import com.davidpe.scapeai.application.StartTrainingSessionCommand;
import com.davidpe.scapeai.application.StartTrainingSessionResult;
import com.davidpe.scapeai.application.StartTrainingSessionUseCase;
import com.davidpe.scapeai.application.SimulationSpeed;
import com.davidpe.scapeai.application.SimulationControlService;
import com.davidpe.scapeai.application.TrainingTargetDifficulty;
import com.davidpe.scapeai.application.TrainingLifecycleEvent;
import com.davidpe.scapeai.application.TrainingLifecycleEventBus;
import com.davidpe.scapeai.application.TrainingTimelineEntry;
import com.davidpe.scapeai.application.TrainingTimelineStatus;
import com.davidpe.scapeai.application.TrainingPresetOption;
import com.davidpe.scapeai.simulation.GridPosition;
import com.davidpe.scapeai.simulation.MazeDefinition;
import com.davidpe.scapeai.simulation.MoveDirection;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

@Component
public final class MainWindow {

  private final SimulationControlService controlService;
  private final StartTrainingSessionUseCase startTrainingSessionUseCase;
  private final LiveMetricsService liveMetricsService;
  private final RecentRunsComparisonService recentRunsComparisonService;
  private final MazeCatalogService mazeCatalogService;
  private final MazeViewportRenderer mazeViewportRenderer;
  private final TrainingLifecycleEventBus trainingLifecycleEventBus;
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
  private Label activePolicyValue;
  private Label activePresetValue;
  private Label activeSpeedValue;
  private Label systemStatusValue;
  private VBox timelineEntriesBox;
  private VBox recentRunsEntriesBox;
  private StackPane mazeViewport;
  private MazeDefinition selectedMaze;
  private String selectedMazeName;
  private volatile RecentRunsSortOption selectedRecentRunsSort = RecentRunsSortOption.BY_DATE;
  private GridPosition trajectoryCurrent;
  private ScheduledFuture<?> trajectoryTicker;
  private volatile boolean trajectoryRunning;
  private TrainingLifecycleEventBus.Subscription lifecycleSubscription;

  public MainWindow(
      SimulationControlService controlService,
      StartTrainingSessionUseCase startTrainingSessionUseCase,
      LiveMetricsService liveMetricsService,
      RecentRunsComparisonService recentRunsComparisonService,
      MazeCatalogService mazeCatalogService,
      MazeViewportRenderer mazeViewportRenderer,
      TrainingLifecycleEventBus trainingLifecycleEventBus) {
    this.controlService = controlService;
    this.startTrainingSessionUseCase = startTrainingSessionUseCase;
    this.liveMetricsService = liveMetricsService;
    this.recentRunsComparisonService = recentRunsComparisonService;
    this.mazeCatalogService = mazeCatalogService;
    this.mazeViewportRenderer = mazeViewportRenderer;
    this.trainingLifecycleEventBus = trainingLifecycleEventBus;
  }

  public void show(Stage stage) {
    BorderPane root = new BorderPane();
    root.setPadding(new Insets(20));
    root.setStyle("-fx-background-color: linear-gradient(to bottom right, #050812, #0d1122);");

    root.setTop(buildHeader());
    root.setLeft(buildControlPanel());
    root.setCenter(buildMazePanel());
    root.setRight(buildMetricsPanel());
    liveMetricsService.subscribe(this::applyMetrics);
    liveMetricsService.subscribeTimeline(this::applyTimeline);
    if (lifecycleSubscription != null) {
      lifecycleSubscription.unsubscribe();
    }
    lifecycleSubscription = trainingLifecycleEventBus.subscribe(this::onTrainingLifecycleEvent);
    refreshRecentRunsAsync();

    Scene scene = new Scene(root, 1200, 760);
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

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    HBox header = new HBox(12, title, spacer, systemStatusValue);
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
        new ComboBox<>(FXCollections.observableArrayList(controlService.availableMovementPolicies()));
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
            });

    activePolicyValue = new Label();
    activePolicyValue.setTextFill(Color.web("#89ff9a"));
    activePolicyValue.setFont(Font.font("Consolas", 12));
    updateActivePolicyLabel();

    Label presetLabel = new Label("TRAINING PRESET");
    presetLabel.setTextFill(Color.web("#9db2ff"));
    presetLabel.setFont(Font.font("Consolas", 12));
    ComboBox<TrainingPresetOption> presetSelector =
        new ComboBox<>(FXCollections.observableArrayList(controlService.availableTrainingPresets()));
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
              if (trajectoryRunning) {
                restartTrajectoryTicker();
              }
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
    targetDifficultySelector.getSelectionModel().select(TrainingTargetDifficulty.MEDIUM);
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

    Button start =
        neonButton(
            "Start",
            "#22e6ff",
            () -> {
              TrainingPresetOption selectedPreset = presetSelector.getValue();
              Long selectedPresetId = selectedPreset == null ? null : selectedPreset.id();
              TrainingTargetDifficulty targetDifficulty = targetDifficultySelector.getValue();
              StartTrainingSessionResult startResult =
                  startTrainingSessionUseCase.start(
                      new StartTrainingSessionCommand(
                          selectedMaze, selectedPresetId, targetDifficulty, null));
              if (!startResult.started()) {
                updateSystemStatus(startResult.message(), "#ff6b8a");
                return;
              }
              if (startResult.maze() != null) {
                selectedMaze = startResult.maze();
                mazeViewportRenderer.renderInto(mazeViewport, startResult.maze());
              }
              updateSystemStatus(startResult.message(), "#89ff9a");
              updateActivePolicyLabel();
              updateActivePresetLabel();
            });
    Button pause =
        neonButton(
            "Pause",
            "#ffd166",
            () -> {
              controlService.pause();
            });
    Button reset =
        neonButton(
            "Reset",
            "#ff6b8a",
            () -> {
              controlService.reset();
            });

    VBox panel =
        new VBox(
            12,
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
            start,
            pause,
            reset);
    panel.setPadding(new Insets(18));
    panel.setMinWidth(220);
    panel.setStyle(panelStyle());
    return panel;
  }

  private VBox buildMazePanel() {
    Label title = panelTitle("Maze");
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
        new ComboBox<>(FXCollections.observableArrayList(mazeCatalogService.namesByDifficulty(true)));
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
                mazeViewportRenderer.renderInto(mazeViewport, maze);
                resetTrajectoryEpisode();
                refreshRecentRunsAsync();
              }
            });

    if (!mazeSelector.getItems().isEmpty()) {
      mazeSelector.getSelectionModel().selectFirst();
      MazeDefinition firstMaze = mazeCatalogService.byName(mazeSelector.getValue());
      if (firstMaze != null) {
        selectedMazeName = mazeSelector.getValue();
        selectedMaze = firstMaze;
        mazeViewportRenderer.renderInto(mazeViewport, firstMaze);
        refreshRecentRunsAsync();
      }
    }

    VBox panel = new VBox(12, title, sortLabel, sortSelector, mazeSelector, mazeViewport);
    panel.setPadding(new Insets(18));
    panel.setStyle(panelStyle());
    BorderPane.setMargin(panel, new Insets(0, 16, 0, 16));
    return panel;
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
    VBox metrics =
        new VBox(
            10,
            metricLine("Steps", "0"),
            metricLine("Collisions", "0"),
            metricLine("Reward", "0.0"),
            metricLine("Elapsed", "00:00"));

    Label timelineTitle = new Label("RECENT EPISODES");
    timelineTitle.setTextFill(Color.web("#9db2ff"));
    timelineTitle.setFont(Font.font("Consolas", 12));

    timelineEntriesBox = new VBox(6);
    timelineEntriesBox
        .getChildren()
        .add(
            timelinePlaceholder("No episodes completed yet."));

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
    recentRunsEntriesBox.getChildren().add(timelinePlaceholder("No training runs stored yet."));

    VBox panel =
        new VBox(
            14,
            title,
            metrics,
            timelineTitle,
            timelineEntriesBox,
            comparisonTitle,
            comparisonSortSelector,
            recentRunsEntriesBox);
    panel.setPadding(new Insets(18));
    panel.setMinWidth(240);
    panel.setStyle(panelStyle());
    return panel;
  }

  private HBox metricLine(String name, String value) {
    Label left = new Label(name);
    left.setTextFill(Color.web("#9db2ff"));
    left.setFont(Font.font("Consolas", 14));

    Label right = new Label(value);
    right.setTextFill(Color.web("#b8ffcb"));
    right.setFont(Font.font("Consolas", 14));
    bindMetricLabel(name, right);

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    return new HBox(8, left, spacer, right);
  }

  private Label panelTitle(String text) {
    Label title = new Label(text.toUpperCase());
    title.setTextFill(Color.web("#7ef9ff"));
    title.setFont(Font.font("Consolas", 18));
    return title;
  }

  private Button neonButton(String label, String accent, Runnable action) {
    Button button = new Button(label);
    button.setMaxWidth(Double.MAX_VALUE);
    button.setFont(Font.font("Consolas", 15));
    button.setStyle(
        "-fx-background-color: #11182f;"
            + "-fx-text-fill: "
            + accent
            + ";"
            + "-fx-border-color: "
            + accent
            + ";"
            + "-fx-border-width: 1;"
            + "-fx-background-radius: 6;"
            + "-fx-border-radius: 6;");
    button.setOnAction(event -> action.run());
    return button;
  }

  private String panelStyle() {
    return "-fx-background-color: rgba(7, 13, 28, 0.92);"
        + "-fx-border-color: #233056;"
        + "-fx-border-width: 1;"
        + "-fx-border-radius: 10;"
        + "-fx-background-radius: 10;";
  }

  private void bindMetricLabel(String metricName, Label label) {
    switch (metricName) {
      case "Steps" -> stepsValue = label;
      case "Collisions" -> collisionsValue = label;
      case "Reward" -> rewardValue = label;
      case "Elapsed" -> elapsedValue = label;
      default -> {
      }
    }
  }

  private void applyMetrics(LiveEpisodeMetrics metrics) {
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
        });
  }

  private void applyTimeline(List<TrainingTimelineEntry> entries) {
    Platform.runLater(
        () -> {
          if (timelineEntriesBox == null) {
            return;
          }
          timelineEntriesBox.getChildren().clear();
          if (entries.isEmpty()) {
            timelineEntriesBox.getChildren().add(timelinePlaceholder("No episodes completed yet."));
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
    if (mazeName == null || mazeName.isBlank()) {
      Platform.runLater(
          () -> {
            if (recentRunsEntriesBox != null) {
              recentRunsEntriesBox.getChildren().setAll(timelinePlaceholder("Select a maze to compare runs."));
            }
          });
      return;
    }
    recentRunsExecutor.execute(
        () -> {
          List<RecentRunComparisonRow> rows = recentRunsComparisonService.recentRuns(mazeName, sort);
          Platform.runLater(() -> renderRecentRuns(mazeName, sort, rows));
        });
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
      recentRunsEntriesBox.getChildren().add(timelinePlaceholder("No training runs stored yet."));
      return;
    }
    for (RecentRunComparisonRow row : rows) {
      recentRunsEntriesBox.getChildren().add(recentRunRow(row));
    }
  }

  private HBox recentRunRow(RecentRunComparisonRow row) {
    Label status = new Label(row.success() ? "OK" : "FAIL");
    status.setFont(Font.font("Consolas", 11));
    status.setTextFill(Color.web(row.success() ? "#89ff9a" : "#ff6b8a"));

    Label reward = new Label(String.format(Locale.US, "R %.1f", row.reward()));
    reward.setFont(Font.font("Consolas", 11));
    reward.setTextFill(Color.web("#b8ffcb"));

    Label collisions = new Label("C " + row.collisions());
    collisions.setFont(Font.font("Consolas", 11));
    collisions.setTextFill(Color.web("#ffd166"));

    Label elapsed = new Label(formatElapsed(row.elapsedMillis()));
    elapsed.setFont(Font.font("Consolas", 11));
    elapsed.setTextFill(Color.web("#9db2ff"));

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    return new HBox(8, status, reward, collisions, spacer, elapsed);
  }

  private HBox timelineRow(TrainingTimelineEntry entry) {
    Label status = new Label(entry.status().name());
    status.setFont(Font.font("Consolas", 12));
    status.setTextFill(Color.web(statusColor(entry.status())));

    Label reward =
        new Label(String.format(Locale.US, "R %.1f", entry.reward()));
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
    label.setFont(Font.font("Consolas", 12));
    label.setTextFill(Color.web("#5e719f"));
    return label;
  }

  private String statusColor(TrainingTimelineStatus status) {
    return switch (status) {
      case SUCCESS -> "#89ff9a";
      case TIMEOUT -> "#ffd166";
      case COLLISION_STALL -> "#ff6b8a";
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
    activePolicyValue.setText("ACTIVE ALGORITHM: " + controlService.activeMovementPolicy().toUpperCase(Locale.ROOT));
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
    String text = activePresetId == null ? "ACTIVE PRESET: NONE" : "ACTIVE PRESET: #" + activePresetId;
    activePresetValue.setText(text);
  }

  private void updateActiveSpeedLabel() {
    if (activeSpeedValue == null) {
      return;
    }
    activeSpeedValue.setText(
        "ACTIVE SPEED: " + liveMetricsService.simulationSpeed().name().toUpperCase(Locale.ROOT));
  }

  private void updateSystemStatus(String text, String color) {
    if (systemStatusValue == null) {
      return;
    }
    systemStatusValue.setText(text);
    systemStatusValue.setTextFill(Color.web(color));
  }

  private String formatElapsed(long elapsedMillis) {
    long totalSeconds = elapsedMillis / 1_000;
    long minutes = totalSeconds / 60;
    long seconds = totalSeconds % 60;
    return String.format("%02d:%02d", minutes, seconds);
  }

  private void startTrajectoryEpisode() {
    if (selectedMaze == null || mazeViewport == null) {
      return;
    }
    synchronized (trajectoryLock) {
      trajectoryCells.clear();
      trajectoryCurrent = selectedMaze.start();
      trajectoryCells.add(trajectoryCurrent);
    }
    Platform.runLater(() -> mazeViewportRenderer.renderTrajectory(List.copyOf(trajectoryCells)));
    trajectoryRunning = true;
    restartTrajectoryTicker();
  }

  private void resetTrajectoryEpisode() {
    trajectoryRunning = false;
    stopTrajectoryTicker();
    synchronized (trajectoryLock) {
      trajectoryCells.clear();
      trajectoryCurrent = null;
    }
    Platform.runLater(mazeViewportRenderer::clearTrajectory);
  }

  private void advanceTrajectoryOverlay() {
    MazeDefinition maze = selectedMaze;
    if (maze == null) {
      return;
    }

    List<GridPosition> snapshot;
    synchronized (trajectoryLock) {
      GridPosition current = trajectoryCurrent == null ? maze.start() : trajectoryCurrent;
      GridPosition next = chooseNextPosition(current, maze);
      trajectoryCurrent = next;
      trajectoryCells.add(next);
      if (trajectoryCells.size() > 600) {
        trajectoryCells.remove(0);
      }
      snapshot = List.copyOf(trajectoryCells);
    }
    Platform.runLater(() -> mazeViewportRenderer.renderTrajectory(snapshot));
  }

  private GridPosition chooseNextPosition(GridPosition current, MazeDefinition maze) {
    GridPosition revisitCandidate = null;
    for (MoveDirection direction : MoveDirection.values()) {
      GridPosition candidate = current.move(direction);
      if (!maze.isInside(candidate) || maze.isWall(candidate)) {
        continue;
      }
      if (recentlyVisited(candidate)) {
        if (revisitCandidate == null) {
          revisitCandidate = candidate;
        }
        continue;
      }
      return candidate;
    }
    return revisitCandidate == null ? current : revisitCandidate;
  }

  private boolean recentlyVisited(GridPosition candidate) {
    int size = trajectoryCells.size();
    int start = Math.max(0, size - 6);
    for (int index = start; index < size; index++) {
      if (trajectoryCells.get(index).equals(candidate)) {
        return true;
      }
    }
    return false;
  }

  private synchronized void stopTrajectoryTicker() {
    if (trajectoryTicker != null) {
      trajectoryTicker.cancel(false);
      trajectoryTicker = null;
    }
  }

  private synchronized void restartTrajectoryTicker() {
    stopTrajectoryTicker();
    long period = liveMetricsService.simulationSpeed().trajectoryTickMillis();
    trajectoryTicker = trajectoryScheduler.scheduleAtFixedRate(this::advanceTrajectoryOverlay, period, period, TimeUnit.MILLISECONDS);
  }

  private void onTrainingLifecycleEvent(TrainingLifecycleEvent event) {
    Platform.runLater(
        () -> {
          switch (event.type()) {
            case STARTED -> {
              startTrajectoryEpisode();
              updateSystemStatus("TRAINING RUNNING", "#89ff9a");
            }
            case PAUSED -> {
              trajectoryRunning = false;
              stopTrajectoryTicker();
              updateSystemStatus("TRAINING PAUSED", "#ffd166");
            }
            case RESUMED -> {
              trajectoryRunning = true;
              restartTrajectoryTicker();
              updateSystemStatus("TRAINING RESUMED", "#89ff9a");
            }
            case FINISHED -> {
              resetTrajectoryEpisode();
              updateSystemStatus("TRAINING FINISHED", "#7ef9ff");
            }
            case TIMED_OUT -> {
              trajectoryRunning = false;
              stopTrajectoryTicker();
              updateSystemStatus("TRAINING TIMEOUT", "#ff6b8a");
            }
          }
        });
  }

  @PreDestroy
  public synchronized void shutdownTrajectoryOverlay() {
    if (lifecycleSubscription != null) {
      lifecycleSubscription.unsubscribe();
      lifecycleSubscription = null;
    }
    stopTrajectoryTicker();
    trajectoryScheduler.shutdownNow();
    recentRunsExecutor.shutdownNow();
  }
}
