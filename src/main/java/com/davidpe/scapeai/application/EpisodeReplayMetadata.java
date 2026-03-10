package com.davidpe.scapeai.application;

import com.davidpe.scapeai.simulation.GridPosition;

public record EpisodeReplayMetadata(
    long effectiveSeed,
    long timeoutBudgetMillis,
    int expectedTotalSteps,
    GridPosition expectedFinalPosition,
    EpisodeEndReason expectedEndReason) {

  public EpisodeReplayMetadata {
    timeoutBudgetMillis = Math.max(0L, timeoutBudgetMillis);
    expectedFinalPosition =
        java.util.Objects.requireNonNull(expectedFinalPosition, "expectedFinalPosition must not be null");
    expectedEndReason = java.util.Objects.requireNonNull(expectedEndReason, "expectedEndReason must not be null");
  }
}
