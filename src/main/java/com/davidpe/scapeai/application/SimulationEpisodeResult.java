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
    int uniqueCellsVisited,
    double netProgress,
    double mazeCoverageRatio,
    double q1Coverage,
    double q2Coverage,
    double q3Coverage,
    double q4Coverage,
    double leftSideCoverage,
    double rightSideCoverage,
    double pathEntropy,
    long effectiveSeed,
    long timeoutBudgetMillis,
    int explorationDecisions,
    int exploitationDecisions,
    java.util.List<EpisodeDebugSnapshot> debugSnapshots,
    EpisodeReplayMetadata replayMetadata,
    List<PolicyInferenceTrace> inferenceTraces) {

  public SimulationEpisodeResult {
    terminationReason = java.util.Objects.requireNonNull(terminationReason, "terminationReason must not be null");
    terminatedAtEpochMillis = Math.max(0L, terminatedAtEpochMillis);
    timeoutBudgetMillis = Math.max(0L, timeoutBudgetMillis);
    debugSnapshots = debugSnapshots == null ? List.of() : List.copyOf(debugSnapshots);
    replayMetadata = java.util.Objects.requireNonNull(replayMetadata, "replayMetadata must not be null");
    inferenceTraces = inferenceTraces == null ? List.of() : List.copyOf(inferenceTraces);
  }

  public EpisodeEndReason endReason() {
    return terminationReason;
  }

  public String debugSnapshotsJson() {
    if (debugSnapshots.isEmpty()) {
      return "[]";
    }
    StringBuilder json = new StringBuilder("[");
    for (int i = 0; i < debugSnapshots.size(); i++) {
      EpisodeDebugSnapshot snapshot = debugSnapshots.get(i);
      if (i > 0) {
        json.append(',');
      }
      json.append('{')
          .append("\"milestone\":\"")
          .append(snapshot.milestone())
          .append("\",\"row\":")
          .append(snapshot.position().row())
          .append(",\"col\":")
          .append(snapshot.position().col())
          .append(",\"steps\":")
          .append(snapshot.steps())
          .append(",\"reward\":")
          .append(String.format(java.util.Locale.ROOT, "%.3f", snapshot.totalReward()))
          .append(",\"collisions\":")
          .append(snapshot.collisions())
          .append(",\"loops\":")
          .append(snapshot.loopEvents())
          .append(",\"uniqueCells\":")
          .append(snapshot.uniqueCellsVisited())
          .append(",\"elapsed\":")
          .append(snapshot.elapsedMillis())
          .append(",\"remaining\":")
          .append(snapshot.remainingMillis())
          .append(",\"seed\":")
          .append(snapshot.effectiveSeed())
          .append('}');
    }
    return json.append(']').toString();
  }

  public String replayMetadataJson() {
    return new StringBuilder()
        .append('{')
        .append("\"version\":")
        .append(replayMetadata.contractVersion())
        .append(",\"maze\":\"")
        .append(replayMetadata.mazeDescriptor())
        .append("\",\"policy\":\"")
        .append(replayMetadata.policyDescriptor())
        .append("\",\"terminationReason\":\"")
        .append(replayMetadata.terminationReason().name())
        .append("\",\"mazeCoverageRatio\":")
        .append(String.format(java.util.Locale.ROOT, "%.6f", replayMetadata.mazeCoverageRatio()))
        .append(",\"loopEvents\":")
        .append(replayMetadata.loopEvents())
        .append(",\"seed\":")
        .append(replayMetadata.effectiveSeed())
        .append(",\"timeoutBudgetMillis\":")
        .append(replayMetadata.timeoutBudgetMillis())
        .append(",\"expectedTotalSteps\":")
        .append(replayMetadata.expectedTotalSteps())
        .append(",\"expectedFinalRow\":")
        .append(replayMetadata.expectedFinalPosition().row())
        .append(",\"expectedFinalCol\":")
        .append(replayMetadata.expectedFinalPosition().col())
        .append(",\"expectedEndReason\":\"")
        .append(replayMetadata.expectedEndReason().name())
        .append("\"}")
        .toString();
  }
}
