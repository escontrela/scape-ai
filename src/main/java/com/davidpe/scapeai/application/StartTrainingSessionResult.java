package com.davidpe.scapeai.application;

public record StartTrainingSessionResult(boolean started, String message) {

  public static StartTrainingSessionResult ok(String message) {
    return new StartTrainingSessionResult(true, message);
  }

  public static StartTrainingSessionResult validationError(String message) {
    return new StartTrainingSessionResult(false, message);
  }
}
