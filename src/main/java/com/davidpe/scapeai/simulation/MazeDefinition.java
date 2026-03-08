package com.davidpe.scapeai.simulation;

public final class MazeDefinition {

  private final int rows;
  private final int cols;
  private final boolean[][] walls;
  private final GridPosition exit;

  public MazeDefinition(int rows, int cols, boolean[][] walls, GridPosition exit) {
    this.rows = rows;
    this.cols = cols;
    this.walls = copyWalls(rows, cols, walls);
    this.exit = exit;
  }

  public boolean isInside(GridPosition position) {
    return position.row() >= 0 && position.row() < rows && position.col() >= 0 && position.col() < cols;
  }

  public boolean isWall(GridPosition position) {
    return walls[position.row()][position.col()];
  }

  public boolean isExit(GridPosition position) {
    return exit.equals(position);
  }

  private static boolean[][] copyWalls(int rows, int cols, boolean[][] source) {
    boolean[][] result = new boolean[rows][cols];
    for (int row = 0; row < rows; row++) {
      System.arraycopy(source[row], 0, result[row], 0, cols);
    }
    return result;
  }
}
