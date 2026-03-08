package com.davidpe.scapeai.ai;

public enum RewardSignal {
  POSITIVE(1.0),
  NEGATIVE(-1.0),
  VERY_NEGATIVE(-5.0);

  private final double score;

  RewardSignal(double score) {
    this.score = score;
  }

  public double score() {
    return score;
  }
}
