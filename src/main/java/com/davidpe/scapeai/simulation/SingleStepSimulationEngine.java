package com.davidpe.scapeai.simulation;

import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SingleStepSimulationEngine {

  public SimulationStepResult step(
      SimulationState currentState, MazeDefinition maze, MoveDirection proposedDirection) {
    GridPosition targetPosition = currentState.agentPosition().move(proposedDirection);

    if (!maze.isInside(targetPosition) || maze.isWall(targetPosition)) {
      SimulationState collidedState =
          new SimulationState(
              currentState.agentPosition(),
              currentState.visitedCells(),
              currentState.invalidAttempts() + 1,
              currentState.exitReached());
      return new SimulationStepResult(collidedState, false, true);
    }

    Set<GridPosition> visited = new LinkedHashSet<>(currentState.visitedCells());
    visited.add(targetPosition);

    SimulationState movedState =
        new SimulationState(
            targetPosition,
            Set.copyOf(visited),
            currentState.invalidAttempts(),
            currentState.exitReached() || maze.isExit(targetPosition));

    return new SimulationStepResult(movedState, true, false);
  }
}
