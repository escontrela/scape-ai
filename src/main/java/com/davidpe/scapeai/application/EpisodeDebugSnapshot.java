package com.davidpe.scapeai.application;

import com.davidpe.scapeai.simulation.GridPosition;

public record EpisodeDebugSnapshot(
    String milestone,
    GridPosition position,
    int steps,
    double totalReward,
    int collisions,
    int loopEvents,
    int uniqueCellsVisited,
    long elapsedMillis,
    long remainingMillis,
    long effectiveSeed) {

  public EpisodeDebugSnapshot {
    milestone = milestone == null ? "UNKNOWN" : milestone;
    position = java.util.Objects.requireNonNull(position, "position must not be null");
    elapsedMillis = Math.max(0L, elapsedMillis);
    remainingMillis = Math.max(0L, remainingMillis);
  }
}
