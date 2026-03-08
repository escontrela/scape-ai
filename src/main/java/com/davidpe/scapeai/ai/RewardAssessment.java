package com.davidpe.scapeai.ai;

public record RewardAssessment(RewardSignal signal, double value) {

  public static RewardAssessment of(RewardSignal signal) {
    return new RewardAssessment(signal, signal.score());
  }
}
