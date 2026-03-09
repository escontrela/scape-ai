package com.davidpe.scapeai.application;

import java.util.List;

public record SimulationEpisodeResult(
    boolean success,
    int totalSteps,
    long elapsedMillis,
    EpisodeEndReason endReason,
    double totalReward,
    int collisions,
    int loopEvents,
    double netProgress,
    double q1Coverage,
    double q2Coverage,
    double q3Coverage,
    double q4Coverage,
    double leftSideCoverage,
    double rightSideCoverage,
    int explorationDecisions,
    int exploitationDecisions,
    List<PolicyInferenceTrace> inferenceTraces) {

  public SimulationEpisodeResult {
    inferenceTraces = inferenceTraces == null ? List.of() : List.copyOf(inferenceTraces);
  }
}
