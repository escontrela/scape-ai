package com.davidpe.scapeai.application;

public record TrainingSessionSummary(
    String trainingSessionId,
    int episodesTotal,
    int successes,
    double successRate,
    double averageReward,
    double averageCollisions,
    double averageCoverage,
    long totalDurationMillis,
    int exitReachedCount,
    int timeoutCount,
    int abortedCount,
    int errorCount) {}
