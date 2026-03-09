package com.davidpe.scapeai.ui;

import com.davidpe.scapeai.simulation.GridPosition;
import com.davidpe.scapeai.simulation.MazeDefinition;
import java.util.List;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.springframework.stereotype.Component;

@Component
public class MazeViewportRenderer {

  private static final double CELL_SIZE = 32.0;
  private static final String WALKABLE_STYLE =
      "-fx-fill: #0b1831; -fx-stroke: #1b2b52; -fx-stroke-width: 0.8;";
  private static final String WALL_STYLE =
      "-fx-fill: #142247; -fx-stroke: #3be2ff; -fx-stroke-width: 1.0;";
  private static final String START_STYLE =
      "-fx-fill: #1f4f35; -fx-stroke: #63ffb1; -fx-stroke-width: 1.4;";
  private static final String EXIT_STYLE =
      "-fx-fill: #4b2f17; -fx-stroke: #ffcf57; -fx-stroke-width: 1.4;";
  private Pane trajectoryLayer;
  private MazeDefinition activeMaze;

  public void renderInto(StackPane container, MazeDefinition maze) {
    activeMaze = maze;
    GridPane grid = new GridPane();

    for (int row = 0; row < maze.rows(); row++) {
      for (int col = 0; col < maze.cols(); col++) {
        GridPosition position = new GridPosition(row, col);
        Rectangle cell = new Rectangle(CELL_SIZE, CELL_SIZE);
        cell.setStyle(styleForCell(maze, position));
        grid.add(cell, col, row);
      }
    }

    trajectoryLayer = new Pane();
    trajectoryLayer.setManaged(false);
    trajectoryLayer.setMouseTransparent(true);
    trajectoryLayer.setPrefSize(maze.cols() * CELL_SIZE, maze.rows() * CELL_SIZE);

    container.getChildren().setAll(grid, trajectoryLayer);
  }

  public void renderTrajectory(List<GridPosition> trajectory) {
    if (trajectoryLayer == null || activeMaze == null) {
      return;
    }
    trajectoryLayer.getChildren().clear();
    int total = trajectory.size();
    for (int index = 0; index < total; index++) {
      GridPosition position = trajectory.get(index);
      if (!activeMaze.isInside(position)) {
        continue;
      }
      Rectangle marker = new Rectangle(CELL_SIZE * 0.55, CELL_SIZE * 0.55);
      marker.setArcWidth(8);
      marker.setArcHeight(8);
      double intensity = (index + 1.0) / Math.max(1, total);
      marker.setFill(Color.color(0.92, 0.38 + (0.52 * intensity), 0.87, 0.22 + (0.58 * intensity)));
      marker.setStroke(Color.color(1.0, 0.78, 0.25, 0.25 + (0.55 * intensity)));
      marker.setStrokeWidth(0.8);
      marker.setLayoutX(position.col() * CELL_SIZE + (CELL_SIZE - marker.getWidth()) / 2.0);
      marker.setLayoutY(position.row() * CELL_SIZE + (CELL_SIZE - marker.getHeight()) / 2.0);
      trajectoryLayer.getChildren().add(marker);
    }
  }

  public void clearTrajectory() {
    if (trajectoryLayer != null) {
      trajectoryLayer.getChildren().clear();
    }
  }

  private String styleForCell(MazeDefinition maze, GridPosition position) {
    if (maze.isWall(position)) {
      return WALL_STYLE;
    }
    if (maze.start().equals(position)) {
      return START_STYLE;
    }
    if (maze.exit().equals(position)) {
      return EXIT_STYLE;
    }
    return WALKABLE_STYLE;
  }
}
