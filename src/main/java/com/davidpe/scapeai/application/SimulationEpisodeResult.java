package com.davidpe.scapeai.application;

import java.util.List;

public record SimulationEpisodeResult(
    boolean success,
    int totalSteps,
    long elapsedMillis,
    EpisodeEndReason terminationReason,
    long terminatedAtEpochMillis,
    double totalReward,
    int collisions,
    int loopEvents,
    double netProgress,
    double mazeCoverageRatio,
    double q1Coverage,
    double q2Coverage,
    double q3Coverage,
    double q4Coverage,
    double leftSideCoverage,
    double rightSideCoverage,
    double pathEntropy,
    int explorationDecisions,
    int exploitationDecisions,
    List<PolicyInferenceTrace> inferenceTraces) {

  public SimulationEpisodeResult {
    terminationReason = java.util.Objects.requireNonNull(terminationReason, "terminationReason must not be null");
    terminatedAtEpochMillis = Math.max(0L, terminatedAtEpochMillis);
    inferenceTraces = inferenceTraces == null ? List.of() : List.copyOf(inferenceTraces);
  }

  public EpisodeEndReason endReason() {
    return terminationReason;
  }
}
