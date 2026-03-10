package com.davidpe.scapeai.application;

public record HeadlessBatchTrainingResult(
    int episodesRequested,
    int episodesCompleted,
    boolean cancelled,
    double successRate,
    double averageReward,
    double averageCollisions,
    long totalDurationMillis,
    int budgetEpisodesConsumed,
    int budgetEpisodesAvailable,
    long budgetWallClockConsumedMillis,
    long budgetWallClockAvailableMillis,
    String budgetExhaustedReason,
    double averageEpsilonApplied) {

  public HeadlessBatchTrainingResult(
      int episodesRequested,
      int episodesCompleted,
      boolean cancelled,
      double successRate,
      double averageReward,
      double averageCollisions,
      long totalDurationMillis) {
    this(
        episodesRequested,
        episodesCompleted,
        cancelled,
        successRate,
        averageReward,
        averageCollisions,
        totalDurationMillis,
        0,
        0,
        0L,
        0L,
        "NONE",
        0.0);
  }
}
