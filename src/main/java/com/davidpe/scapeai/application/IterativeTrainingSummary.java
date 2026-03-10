package com.davidpe.scapeai.application;

public record IterativeTrainingSummary(
    int episodesRequested,
    int episodesCompleted,
    boolean cancelled,
    double successRate,
    double averageReward,
    double averageCollisions,
    int budgetEpisodesConsumed,
    int budgetEpisodesAvailable,
    long budgetWallClockConsumedMillis,
    long budgetWallClockAvailableMillis,
    String budgetExhaustedReason) {

  public IterativeTrainingSummary {
    budgetEpisodesConsumed = Math.max(0, budgetEpisodesConsumed);
    budgetEpisodesAvailable = Math.max(0, budgetEpisodesAvailable);
    budgetWallClockConsumedMillis = Math.max(0L, budgetWallClockConsumedMillis);
    budgetWallClockAvailableMillis = Math.max(0L, budgetWallClockAvailableMillis);
    budgetExhaustedReason =
        budgetExhaustedReason == null || budgetExhaustedReason.isBlank()
            ? "NONE"
            : budgetExhaustedReason;
  }

  public IterativeTrainingSummary(
      int episodesRequested,
      int episodesCompleted,
      boolean cancelled,
      double successRate,
      double averageReward,
      double averageCollisions) {
    this(
        episodesRequested,
        episodesCompleted,
        cancelled,
        successRate,
        averageReward,
        averageCollisions,
        0,
        0,
        0L,
        0L,
        "NONE");
  }
}
