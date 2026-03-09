package com.davidpe.scapeai.application;

public record IterativeTrainingSummary(
    int episodesRequested,
    int episodesCompleted,
    boolean cancelled,
    double successRate,
    double averageReward,
    double averageCollisions) {}
