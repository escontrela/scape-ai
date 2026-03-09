package com.davidpe.scapeai.ui;

import com.davidpe.scapeai.application.LiveEpisodeMetrics;
import com.davidpe.scapeai.application.LiveMetricsService;
import com.davidpe.scapeai.application.MovementPolicyOption;
import com.davidpe.scapeai.application.SimulationControlService;
import com.davidpe.scapeai.application.TrainingPresetOption;
import com.davidpe.scapeai.simulation.GridPosition;
import com.davidpe.scapeai.simulation.MazeDefinition;
import com.davidpe.scapeai.simulation.MoveDirection;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
  private final LiveMetricsService liveMetricsService;
  private final MazeCatalogService mazeCatalogService;
  private final MazeViewportRenderer mazeViewportRenderer;
  private final ScheduledExecutorService trajectoryScheduler =
      Executors.newSingleThreadScheduledExecutor(
          runnable -> {
            Thread thread = new Thread(runnable, "maze-trajectory-overlay");
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
  private StackPane mazeViewport;
  private MazeDefinition selectedMaze;
  private GridPosition trajectoryCurrent;
  private ScheduledFuture<?> trajectoryTicker;

  public MainWindow(
      SimulationControlService controlService,
      LiveMetricsService liveMetricsService,
      MazeCatalogService mazeCatalogService,
      MazeViewportRenderer mazeViewportRenderer) {
    this.controlService = controlService;
    this.liveMetricsService = liveMetricsService;
    this.mazeCatalogService = mazeCatalogService;
    this.mazeViewportRenderer = mazeViewportRenderer;
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

    Scene scene = new Scene(root, 1200, 760);
    stage.setTitle("Scape AI Control Panel");
    stage.setScene(scene);
    stage.show();
  }

  private HBox buildHeader() {
    Label title = new Label("SCAPE AI // CONTROL CONSOLE");
    title.setFont(Font.font("Consolas", 26));
    title.setTextFill(Color.web("#7ef9ff"));

    Label status = new Label("SYSTEM READY");
    status.setFont(Font.font("Consolas", 15));
    status.setTextFill(Color.web("#89ff9a"));

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    HBox header = new HBox(12, title, spacer, status);
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

    Button start =
        neonButton(
            "Start",
            "#22e6ff",
            () -> {
              TrainingPresetOption selectedPreset = presetSelector.getValue();
              if (selectedPreset != null) {
                controlService.applyTrainingPreset(selectedPreset.id());
              }
              controlService.start();
              liveMetricsService.startEpisode();
              startTrajectoryEpisode();
              updateActivePolicyLabel();
              updateActivePresetLabel();
            });
    Button pause =
        neonButton(
            "Pause",
            "#ffd166",
            () -> {
              controlService.pause();
              liveMetricsService.pauseEpisode();
              stopTrajectoryTicker();
            });
    Button reset =
        neonButton(
            "Reset",
            "#ff6b8a",
            () -> {
              controlService.reset();
              liveMetricsService.resetEpisode();
              resetTrajectoryEpisode();
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
    ComboBox<String> mazeSelector =
        new ComboBox<>(FXCollections.observableArrayList(new ArrayList<>(mazeCatalogService.names())));
    mazeSelector.setMaxWidth(Double.MAX_VALUE);
    mazeSelector.setStyle(
        "-fx-background-color: #101938;"
            + "-fx-text-fill: #c6d7ff;"
            + "-fx-border-color: #2cf1ff;"
            + "-fx-border-radius: 6;"
            + "-fx-background-radius: 6;");

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
                selectedMaze = maze;
                mazeViewportRenderer.renderInto(mazeViewport, maze);
                resetTrajectoryEpisode();
              }
            });

    if (!mazeSelector.getItems().isEmpty()) {
      mazeSelector.getSelectionModel().selectFirst();
      MazeDefinition firstMaze = mazeCatalogService.byName(mazeSelector.getValue());
      if (firstMaze != null) {
        selectedMaze = firstMaze;
        mazeViewportRenderer.renderInto(mazeViewport, firstMaze);
      }
    }

    VBox panel = new VBox(12, title, mazeSelector, mazeViewport);
    panel.setPadding(new Insets(18));
    panel.setStyle(panelStyle());
    BorderPane.setMargin(panel, new Insets(0, 16, 0, 16));
    return panel;
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

    VBox panel = new VBox(14, title, metrics);
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
    stopTrajectoryTicker();
    trajectoryTicker =
        trajectoryScheduler.scheduleAtFixedRate(this::advanceTrajectoryOverlay, 120, 120, TimeUnit.MILLISECONDS);
  }

  private void resetTrajectoryEpisode() {
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

  @PreDestroy
  public synchronized void shutdownTrajectoryOverlay() {
    stopTrajectoryTicker();
    trajectoryScheduler.shutdownNow();
  }
}
