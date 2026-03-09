package com.davidpe.scapeai.application;

public record HeadlessBatchTrainingResult(
    int episodesRequested,
    int episodesCompleted,
    boolean cancelled,
    double successRate,
    double averageReward,
    double averageCollisions,
    long totalDurationMillis) {}
