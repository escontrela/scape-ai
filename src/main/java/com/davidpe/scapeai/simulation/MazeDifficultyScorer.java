package com.davidpe.scapeai.simulation;

import java.util.ArrayDeque;
import java.util.Queue;
import org.springframework.stereotype.Component;

@Component
public class MazeDifficultyScorer {

  public double score(MazeDefinition maze) {
    int rows = maze.rows();
    int cols = maze.cols();
    int totalCells = Math.max(1, rows * cols);
    int walls = countWalls(maze);
    double wallDensity = walls / (double) totalCells;
    int shortestPath = shortestPathLength(maze);
    double distanceFactor = shortestPath < 0 ? (rows + cols) * 1.5 : shortestPath;

    return (totalCells * 0.35) + (wallDensity * 100.0 * 0.4) + (distanceFactor * 0.25);
  }

  private int countWalls(MazeDefinition maze) {
    int walls = 0;
    for (int row = 0; row < maze.rows(); row++) {
      for (int col = 0; col < maze.cols(); col++) {
        if (maze.isWall(new GridPosition(row, col))) {
          walls++;
        }
      }
    }
    return walls;
  }

  private int shortestPathLength(MazeDefinition maze) {
    GridPosition start = maze.start();
    GridPosition exit = maze.exit();
    if (start.equals(exit)) {
      return 0;
    }

    boolean[][] visited = new boolean[maze.rows()][maze.cols()];
    Queue<PositionDistance> queue = new ArrayDeque<>();
    queue.add(new PositionDistance(start, 0));
    visited[start.row()][start.col()] = true;

    while (!queue.isEmpty()) {
      PositionDistance current = queue.remove();
      for (MoveDirection direction : MoveDirection.values()) {
        GridPosition next = current.position().move(direction);
        if (!maze.isInside(next) || maze.isWall(next) || visited[next.row()][next.col()]) {
          continue;
        }
        if (next.equals(exit)) {
          return current.distance() + 1;
        }
        visited[next.row()][next.col()] = true;
        queue.add(new PositionDistance(next, current.distance() + 1));
      }
    }
    return -1;
  }

  private record PositionDistance(GridPosition position, int distance) {}
}
