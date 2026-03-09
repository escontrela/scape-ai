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
    List<PolicyInferenceTrace> inferenceTraces) {

  public SimulationEpisodeResult {
    inferenceTraces = inferenceTraces == null ? List.of() : List.copyOf(inferenceTraces);
  }
}
