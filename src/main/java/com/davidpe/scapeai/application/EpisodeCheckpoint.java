package com.davidpe.scapeai.application;

import com.davidpe.scapeai.simulation.GridPosition;
import com.davidpe.scapeai.simulation.MoveDirection;
import com.davidpe.scapeai.simulation.SimulationState;
import java.util.List;
import java.util.Objects;

public record EpisodeCheckpoint(
    SimulationState currentState,
    MoveDirection previousDirection,
    int noProgressStreak,
    int totalSteps,
    int collisions,
    int loopEvents,
    double totalReward,
    long elapsedMillis,
    long remainingMillis,
    List<GridPosition> recentPositions,
    List<GridPosition> trajectory) {

  public EpisodeCheckpoint {
    currentState = Objects.requireNonNull(currentState, "currentState must not be null");
    noProgressStreak = Math.max(0, noProgressStreak);
    totalSteps = Math.max(0, totalSteps);
    collisions = Math.max(0, collisions);
    loopEvents = Math.max(0, loopEvents);
    elapsedMillis = Math.max(0L, elapsedMillis);
    remainingMillis = Math.max(0L, remainingMillis);
    recentPositions =
        recentPositions == null || recentPositions.isEmpty()
            ? List.of(currentState.agentPosition())
            : List.copyOf(recentPositions);
    trajectory =
        trajectory == null || trajectory.isEmpty()
            ? List.of(currentState.agentPosition())
            : List.copyOf(trajectory);
  }
}
