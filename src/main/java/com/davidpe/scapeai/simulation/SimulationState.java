package com.davidpe.scapeai.simulation;

import java.util.LinkedHashSet;
import java.util.Set;

public record SimulationState(
    GridPosition agentPosition, Set<GridPosition> visitedCells, int invalidAttempts, boolean exitReached) {

  public static SimulationState initial(GridPosition start) {
    Set<GridPosition> visited = new LinkedHashSet<>();
    visited.add(start);
    return new SimulationState(start, Set.copyOf(visited), 0, false);
  }
}
