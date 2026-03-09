package com.davidpe.scapeai.application;

public record LiveEpisodeMetrics(
    int steps,
    int collisions,
    double accumulatedReward,
    long elapsedMillis,
    double leftSideCoverage,
    double rightSideCoverage) {}
