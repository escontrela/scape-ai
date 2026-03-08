package com.davidpe.scapeai.simulation;

public record GridPosition(int row, int col) {

  public GridPosition move(MoveDirection direction) {
    return new GridPosition(row + direction.deltaRow(), col + direction.deltaCol());
  }
}
