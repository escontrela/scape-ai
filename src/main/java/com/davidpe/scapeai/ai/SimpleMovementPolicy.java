package com.davidpe.scapeai.ai;

import com.davidpe.scapeai.simulation.GridPosition;
import com.davidpe.scapeai.simulation.MoveDirection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SimpleMovementPolicy implements MovementPolicy, ExplorationBudgetAwarePolicy {

  private static final List<MoveDirection> DIRECTION_PRIORITY =
      List.of(MoveDirection.UP, MoveDirection.RIGHT, MoveDirection.DOWN, MoveDirection.LEFT);
  private final int recentHistorySize;
  private volatile int remainingExplorationBudget = Integer.MAX_VALUE;
  private volatile int consumePerEpisode = 0;

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
    var recentVisited = recentVisited(context.recentPositions());
    var visitedCells = state.visitedCells();
    int midCol = maze.cols() / 2;
    int leftVisited = countVisitedBySide(visitedCells, midCol, true);
    int rightVisited = countVisitedBySide(visitedCells, midCol, false);
    boolean stagnated = context.noProgressStreak() >= 2;

    List<MoveDirection> validMoves =
        DIRECTION_PRIORITY.stream()
            .filter(context::canMove)
            .toList();

    if (validMoves.isEmpty()) {
      return MoveDirection.UP;
    }

    MoveDirection bestDirection = validMoves.get(0);
    double bestScore = Double.NEGATIVE_INFINITY;
    for (MoveDirection direction : validMoves) {
      GridPosition next = current.move(direction);
      if (maze.isExit(next)) {
        return direction;
      }
      double score = 0.0;
      if (!visitedCells.contains(next)) {
        score += 3.0;
        if (remainingExplorationBudget <= 0) {
          score -= 0.8;
        } else if (consumePerEpisode > 0) {
          score += 0.25;
        }
      }
      score -= recentVisited.getOrDefault(next, 0) * 2.5;
      if (createsShortLoop(current, next, context.recentPositions())) {
        score -= 2.2;
      }
      score -= distanceToExit(next, maze.exit()) * 0.3;
      if (stagnated) {
        score += coveragePotential(next, midCol, leftVisited, rightVisited) * 1.6;
        if (isImmediateBacktrack(direction, context.previousDirection())) {
          score -= 1.5;
        }
      }
      if (score > bestScore) {
        bestScore = score;
        bestDirection = direction;
      }
    }
    return bestDirection;
  }

  private Map<GridPosition, Integer> recentVisited(List<GridPosition> recentPositions) {
    List<GridPosition> source = recentPositions == null ? List.of() : recentPositions;
    int fromIndex = Math.max(0, source.size() - recentHistorySize);
    Map<GridPosition, Integer> counts = new HashMap<>();
    for (GridPosition position : new ArrayList<>(source).subList(fromIndex, source.size())) {
      counts.merge(position, 1, Integer::sum);
    }
    return counts;
  }

  private int countVisitedBySide(Set<GridPosition> visitedCells, int midCol, boolean left) {
    int count = 0;
    for (GridPosition visited : visitedCells) {
      boolean isLeft = visited.col() < midCol;
      if (left == isLeft) {
        count++;
      }
    }
    return count;
  }

  private double coveragePotential(
      GridPosition candidate, int midCol, int leftVisited, int rightVisited) {
    boolean targetLeft = candidate.col() < midCol;
    int imbalance = leftVisited - rightVisited;
    if (targetLeft) {
      return imbalance > 0 ? -1.0 : 1.0;
    }
    return imbalance < 0 ? -1.0 : 1.0;
  }

  private boolean isImmediateBacktrack(MoveDirection candidate, MoveDirection previousDirection) {
    if (previousDirection == null) {
      return false;
    }
    return opposite(previousDirection) == candidate;
  }

  private boolean createsShortLoop(
      GridPosition current, GridPosition candidate, List<GridPosition> recentPositions) {
    if (recentPositions == null || recentPositions.size() < 3) {
      return false;
    }
    int last = recentPositions.size() - 1;
    GridPosition previous = recentPositions.get(last - 1);
    GridPosition twoStepsBack = recentPositions.get(last - 2);
    return twoStepsBack.equals(current) && previous.equals(candidate);
  }

  private MoveDirection opposite(MoveDirection direction) {
    return switch (direction) {
      case UP -> MoveDirection.DOWN;
      case DOWN -> MoveDirection.UP;
      case LEFT -> MoveDirection.RIGHT;
      case RIGHT -> MoveDirection.LEFT;
    };
  }

  private int distanceToExit(GridPosition position, GridPosition exit) {
    return Math.abs(position.row() - exit.row()) + Math.abs(position.col() - exit.col());
  }

  @Override
  public void applyExplorationBudget(int remainingBudget, int consumePerEpisode) {
    this.remainingExplorationBudget = Math.max(0, remainingBudget);
    this.consumePerEpisode = Math.max(0, consumePerEpisode);
  }
}
