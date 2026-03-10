package com.davidpe.scapeai.application;

import com.davidpe.scapeai.simulation.GridPosition;
import com.davidpe.scapeai.simulation.MazeDefinition;
import java.util.Set;

public record MazeQuadrantCoverage(
    double mazeCoverageRatio,
    double q1Coverage,
    double q2Coverage,
    double q3Coverage,
    double q4Coverage,
    double leftSideCoverage,
    double rightSideCoverage) {

  static MazeQuadrantCoverage from(MazeDefinition maze, Set<GridPosition> visitedCells) {
    int midRow = maze.rows() / 2;
    int midCol = maze.cols() / 2;
    int q1Total = 0;
    int q2Total = 0;
    int q3Total = 0;
    int q4Total = 0;
    int q1Visited = 0;
    int q2Visited = 0;
    int q3Visited = 0;
    int q4Visited = 0;
    int traversableTotal = 0;
    int traversableVisited = 0;
    for (int row = 0; row < maze.rows(); row++) {
      for (int col = 0; col < maze.cols(); col++) {
        GridPosition position = new GridPosition(row, col);
        if (maze.isWall(position)) {
          continue;
        }
        traversableTotal++;
        if (visitedCells.contains(position)) {
          traversableVisited++;
        }
        boolean top = row < midRow;
        boolean left = col < midCol;
        if (top && left) {
          q1Total++;
          if (visitedCells.contains(position)) {
            q1Visited++;
          }
        } else if (top) {
          q2Total++;
          if (visitedCells.contains(position)) {
            q2Visited++;
          }
        } else if (left) {
          q3Total++;
          if (visitedCells.contains(position)) {
            q3Visited++;
          }
        } else {
          q4Total++;
          if (visitedCells.contains(position)) {
            q4Visited++;
          }
        }
      }
    }
    double q1 = ratio(q1Visited, q1Total);
    double q2 = ratio(q2Visited, q2Total);
    double q3 = ratio(q3Visited, q3Total);
    double q4 = ratio(q4Visited, q4Total);
    double left = ratio(q1Visited + q3Visited, q1Total + q3Total);
    double right = ratio(q2Visited + q4Visited, q2Total + q4Total);
    double mazeCoverageRatio = ratio(traversableVisited, traversableTotal);
    return new MazeQuadrantCoverage(mazeCoverageRatio, q1, q2, q3, q4, left, right);
  }

  private static double ratio(int value, int total) {
    if (total <= 0) {
      return 0.0;
    }
    return (double) value / (double) total;
  }
}
