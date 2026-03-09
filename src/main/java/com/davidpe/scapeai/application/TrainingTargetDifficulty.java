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
}
