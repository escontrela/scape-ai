package com.davidpe.scapeai.application;

import com.davidpe.scapeai.simulation.MazeDefinition;

public record StartTrainingSessionResult(
    boolean started, String message, MazeDefinition maze, Long effectiveSeed) {

  public static StartTrainingSessionResult ok(String message, MazeDefinition maze, long effectiveSeed) {
    return new StartTrainingSessionResult(true, message, maze, effectiveSeed);
  }

  public static StartTrainingSessionResult validationError(String message) {
    return new StartTrainingSessionResult(false, message, null, null);
  }
}
