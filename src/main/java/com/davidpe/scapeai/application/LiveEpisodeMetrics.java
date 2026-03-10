package com.davidpe.scapeai.application;

public record LiveEpisodeMetrics(
    int steps,
    int collisions,
    double accumulatedReward,
    long elapsedMillis,
    long remainingMillis,
    String terminationReason,
    double mazeCoverageRatio,
    double leftSideCoverage,
    double rightSideCoverage) {}
