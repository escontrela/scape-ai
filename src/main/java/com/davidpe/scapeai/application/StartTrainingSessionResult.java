package com.davidpe.scapeai.application;

import com.davidpe.scapeai.simulation.MazeDefinition;
import java.util.List;

public record StartTrainingSessionResult(
    boolean started,
    String message,
    MazeDefinition maze,
    Long effectiveSeed,
    List<TrainingSessionConfigValidationError> validationErrors) {

  public static StartTrainingSessionResult ok(String message, MazeDefinition maze, long effectiveSeed) {
    return new StartTrainingSessionResult(true, message, maze, effectiveSeed, List.of());
  }

  public static StartTrainingSessionResult validationError(String message) {
    return validationError(message, List.of());
  }

  public static StartTrainingSessionResult validationError(
      String message, List<TrainingSessionConfigValidationError> validationErrors) {
    return new StartTrainingSessionResult(
        false, message, null, null, validationErrors == null ? List.of() : List.copyOf(validationErrors));
  }
}
