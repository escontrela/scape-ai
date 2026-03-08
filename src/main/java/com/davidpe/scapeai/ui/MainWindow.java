package com.davidpe.scapeai.ui;

import com.davidpe.scapeai.application.SimulationControlService;
import com.davidpe.scapeai.simulation.MazeDefinition;
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
import java.util.ArrayList;
import org.springframework.stereotype.Component;

@Component
public final class MainWindow {

  private final SimulationControlService controlService;
  private final MazeCatalogService mazeCatalogService;
  private final MazeViewportRenderer mazeViewportRenderer;

  public MainWindow(
      SimulationControlService controlService,
      MazeCatalogService mazeCatalogService,
      MazeViewportRenderer mazeViewportRenderer) {
    this.controlService = controlService;
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
    Button start = neonButton("Start", "#22e6ff", controlService::start);
    Button pause = neonButton("Pause", "#ffd166", controlService::pause);
    Button reset = neonButton("Reset", "#ff6b8a", controlService::reset);

    VBox panel = new VBox(12, title, start, pause, reset);
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

    StackPane viewport = new StackPane();
    viewport.setAlignment(Pos.CENTER);
    viewport.setMinHeight(520);
    viewport.setStyle(
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
                mazeViewportRenderer.renderInto(viewport, maze);
              }
            });

    if (!mazeSelector.getItems().isEmpty()) {
      mazeSelector.getSelectionModel().selectFirst();
      MazeDefinition firstMaze = mazeCatalogService.byName(mazeSelector.getValue());
      if (firstMaze != null) {
        mazeViewportRenderer.renderInto(viewport, firstMaze);
      }
    }

    VBox panel = new VBox(12, title, mazeSelector, viewport);
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
            metricLine("Iterations", "0"),
            metricLine("Score", "0"),
            metricLine("Best Score", "0"),
            metricLine("Elapsed", "00:00"),
            metricLine("Escapes", "0"));

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
}
