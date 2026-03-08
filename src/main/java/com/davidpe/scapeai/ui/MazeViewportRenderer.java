package com.davidpe.scapeai.ui;

import com.davidpe.scapeai.simulation.GridPosition;
import com.davidpe.scapeai.simulation.MazeDefinition;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
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

  public void renderInto(StackPane container, MazeDefinition maze) {
    GridPane grid = new GridPane();

    for (int row = 0; row < maze.rows(); row++) {
      for (int col = 0; col < maze.cols(); col++) {
        GridPosition position = new GridPosition(row, col);
        Rectangle cell = new Rectangle(CELL_SIZE, CELL_SIZE);
        cell.setStyle(styleForCell(maze, position));
        grid.add(cell, col, row);
      }
    }

    container.getChildren().setAll(grid);
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
