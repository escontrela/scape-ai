package com.davidpe.scapeai.application;

import java.time.Duration;

public record TrainingSessionConfig(
    int version,
    String mazeId,
    String policyId,
    Duration timeout,
    Long seed,
    boolean smokeRunEnabled,
    TrainingTargetDifficulty difficultyTarget) {

  public static final int VERSION_1 = 1;

  public static TrainingSessionConfig v1(
      String mazeId,
      String policyId,
      Duration timeout,
      Long seed,
      boolean smokeRunEnabled,
      TrainingTargetDifficulty difficultyTarget) {
    return new TrainingSessionConfig(
        VERSION_1, mazeId, policyId, timeout, seed, smokeRunEnabled, difficultyTarget);
  }
}
