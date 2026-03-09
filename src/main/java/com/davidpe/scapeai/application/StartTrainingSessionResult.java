package com.davidpe.scapeai.application;

import com.davidpe.scapeai.simulation.MazeDefinition;

public record StartTrainingSessionResult(boolean started, String message, MazeDefinition maze) {

  public static StartTrainingSessionResult ok(String message, MazeDefinition maze) {
    return new StartTrainingSessionResult(true, message, maze);
  }

  public static StartTrainingSessionResult validationError(String message) {
    return new StartTrainingSessionResult(false, message, null);
  }
}
