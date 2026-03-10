package com.davidpe.scapeai.application;

import java.util.List;

public final class TrainingSessionConfigValidationException extends RuntimeException {

  private final List<TrainingSessionConfigValidationError> errors;

  public TrainingSessionConfigValidationException(List<TrainingSessionConfigValidationError> errors) {
    super(resolveMessage(errors));
    this.errors = errors == null ? List.of() : List.copyOf(errors);
  }

  public List<TrainingSessionConfigValidationError> errors() {
    return errors;
  }

  private static String resolveMessage(List<TrainingSessionConfigValidationError> errors) {
    if (errors == null || errors.isEmpty()) {
      return "Training session config validation failed.";
    }
    TrainingSessionConfigValidationError first = errors.get(0);
    return first == null || first.message() == null
        ? "Training session config validation failed."
        : first.message();
  }
}
