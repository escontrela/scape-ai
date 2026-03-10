package com.davidpe.scapeai.ai;

import com.davidpe.scapeai.simulation.GridPosition;
import com.davidpe.scapeai.simulation.MazeDefinition;
import com.davidpe.scapeai.simulation.MoveDirection;
import com.davidpe.scapeai.simulation.SimulationState;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record SpatialContext(
    MazeDefinition maze,
    SimulationState simulationState,
    Map<MoveDirection, NeighborCell> localNeighborhood,
    Map<MoveDirection, Boolean> validActionMask,
    List<GridPosition> recentPositions,
    MoveDirection previousDirection,
    int noProgressStreak) {

  public SpatialContext(MazeDefinition maze, SimulationState simulationState) {
    this(maze, simulationState, null, 0);
  }

  public SpatialContext(
      MazeDefinition maze,
      SimulationState simulationState,
      MoveDirection previousDirection,
      int noProgressStreak) {
    this(maze, simulationState, List.of(), previousDirection, noProgressStreak);
  }

  public SpatialContext(
      MazeDefinition maze,
      SimulationState simulationState,
      List<GridPosition> recentPositions,
      MoveDirection previousDirection,
      int noProgressStreak) {
    this(
        maze,
        simulationState,
        resolveLocalNeighborhood(maze, simulationState.agentPosition()),
        resolveValidActionMask(maze, simulationState.agentPosition()),
        recentPositions,
        previousDirection,
        noProgressStreak);
  }

  public SpatialContext {
    maze = Objects.requireNonNull(maze, "maze must not be null");
    simulationState = Objects.requireNonNull(simulationState, "simulationState must not be null");
    localNeighborhood = Map.copyOf(Objects.requireNonNull(localNeighborhood, "localNeighborhood must not be null"));
    validActionMask = Map.copyOf(Objects.requireNonNull(validActionMask, "validActionMask must not be null"));
    recentPositions = List.copyOf(Objects.requireNonNull(recentPositions, "recentPositions must not be null"));
    noProgressStreak = Math.max(0, noProgressStreak);
  }

  public boolean canMove(MoveDirection direction) {
    return validActionMask.getOrDefault(direction, false);
  }

  public static Map<MoveDirection, NeighborCell> resolveLocalNeighborhood(
      MazeDefinition maze, GridPosition center) {
    EnumMap<MoveDirection, NeighborCell> neighborhood = new EnumMap<>(MoveDirection.class);
    for (MoveDirection direction : MoveDirection.values()) {
      GridPosition target = center.move(direction);
      if (!maze.isInside(target) || maze.isWall(target)) {
        neighborhood.put(direction, NeighborCell.WALL);
      } else if (maze.isExit(target)) {
        neighborhood.put(direction, NeighborCell.EXIT);
      } else {
        neighborhood.put(direction, NeighborCell.OPEN);
      }
    }
    return Map.copyOf(neighborhood);
  }

  public static Map<MoveDirection, Boolean> resolveValidActionMask(
      MazeDefinition maze, GridPosition center) {
    EnumMap<MoveDirection, Boolean> mask = new EnumMap<>(MoveDirection.class);
    for (MoveDirection direction : MoveDirection.values()) {
      GridPosition target = center.move(direction);
      mask.put(direction, maze.isInside(target) && !maze.isWall(target));
    }
    return Map.copyOf(mask);
  }

  public enum NeighborCell {
    WALL,
    OPEN,
    EXIT
  }
}
