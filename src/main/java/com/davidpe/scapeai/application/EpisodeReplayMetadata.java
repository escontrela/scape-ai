package com.davidpe.scapeai.application;

import com.davidpe.scapeai.simulation.GridPosition;

public record EpisodeReplayMetadata(
    int contractVersion,
    String mazeDescriptor,
    String policyDescriptor,
    long effectiveSeed,
    EpisodeEndReason terminationReason,
    double mazeCoverageRatio,
    int loopEvents,
    long timeoutBudgetMillis,
    int expectedTotalSteps,
    GridPosition expectedFinalPosition,
    EpisodeEndReason expectedEndReason) {

  public static final int CONTRACT_VERSION = 1;

  public EpisodeReplayMetadata {
    contractVersion = Math.max(CONTRACT_VERSION, contractVersion);
    mazeDescriptor = java.util.Objects.requireNonNullElse(mazeDescriptor, "unknown-maze");
    policyDescriptor = java.util.Objects.requireNonNullElse(policyDescriptor, "unknown-policy");
    terminationReason = java.util.Objects.requireNonNull(terminationReason, "terminationReason must not be null");
    mazeCoverageRatio = Math.max(0.0, mazeCoverageRatio);
    loopEvents = Math.max(0, loopEvents);
    timeoutBudgetMillis = Math.max(0L, timeoutBudgetMillis);
    expectedFinalPosition =
        java.util.Objects.requireNonNull(expectedFinalPosition, "expectedFinalPosition must not be null");
    expectedEndReason = java.util.Objects.requireNonNull(expectedEndReason, "expectedEndReason must not be null");
  }
}
