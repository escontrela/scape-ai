package com.davidpe.scapeai.application;

public enum TrainingTargetDifficulty {
  LOW("Baja"),
  MEDIUM("Media"),
  HIGH("Alta");

  private final String label;

  TrainingTargetDifficulty(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  public TrainingTargetDifficulty harder() {
    return switch (this) {
      case LOW -> MEDIUM;
      case MEDIUM -> HIGH;
      case HIGH -> HIGH;
    };
  }

  public TrainingTargetDifficulty easier() {
    return switch (this) {
      case LOW -> LOW;
      case MEDIUM -> LOW;
      case HIGH -> MEDIUM;
    };
  }
}
