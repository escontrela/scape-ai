package com.davidpe.scapeai.ai;

import com.davidpe.scapeai.simulation.GridPosition;
import com.davidpe.scapeai.simulation.MoveDirection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class SimpleMovementPolicy implements MovementPolicy {

  private static final List<MoveDirection> DIRECTION_PRIORITY =
      List.of(MoveDirection.UP, MoveDirection.RIGHT, MoveDirection.DOWN, MoveDirection.LEFT);
  private final int recentHistorySize;

  public SimpleMovementPolicy() {
    this(6);
  }

  public SimpleMovementPolicy(int recentHistorySize) {
    this.recentHistorySize = recentHistorySize;
  }

  @Override
  public MoveDirection chooseNextMove(SpatialContext context) {
    var maze = context.maze();
    var state = context.simulationState();
    var current = state.agentPosition();
    var recentVisited = recentVisited(state.visitedCells());

    List<MoveDirection> validMoves =
        DIRECTION_PRIORITY.stream()
            .filter(
                direction -> {
                  var next = current.move(direction);
                  return maze.isInside(next) && !maze.isWall(next);
                })
            .toList();

    if (validMoves.isEmpty()) {
      return MoveDirection.UP;
    }

    List<MoveDirection> preferred =
        validMoves.stream()
            .filter(direction -> !recentVisited.contains(current.move(direction)))
            .toList();

    List<MoveDirection> evaluationSet = preferred.isEmpty() ? validMoves : preferred;
    return evaluationSet.stream()
        .min(
            Comparator.comparingInt(
                direction -> distanceToExit(current.move(direction), maze.exit())))
        .orElse(MoveDirection.UP);
  }

  private Set<GridPosition> recentVisited(Set<GridPosition> visitedCells) {
    List<GridPosition> visited = new ArrayList<>(visitedCells);
    int fromIndex = Math.max(0, visited.size() - recentHistorySize);
    return Set.copyOf(visited.subList(fromIndex, visited.size()));
  }

  private int distanceToExit(GridPosition position, GridPosition exit) {
    return Math.abs(position.row() - exit.row()) + Math.abs(position.col() - exit.col());
  }
}
