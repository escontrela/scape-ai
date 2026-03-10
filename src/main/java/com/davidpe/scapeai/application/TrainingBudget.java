package com.davidpe.scapeai.application;

import java.time.Duration;

public record TrainingBudget(int maxEpisodes, Duration maxWallClock) {

  public TrainingBudget {
    maxEpisodes = Math.max(0, maxEpisodes);
    maxWallClock = maxWallClock == null ? Duration.ZERO : maxWallClock;
    if (maxWallClock.isNegative()) {
      maxWallClock = Duration.ZERO;
    }
  }

  public static TrainingBudget unlimited() {
    return new TrainingBudget(0, Duration.ZERO);
  }

  public boolean hasEpisodeLimit() {
    return maxEpisodes > 0;
  }

  public boolean hasWallClockLimit() {
    return !maxWallClock.isZero();
  }
}
