package com.davidpe.scapeai.application;

public record RecentRunComparisonRow(
    boolean success,
    double reward,
    int collisions,
    double netProgress,
    double leftSideCoverage,
    double rightSideCoverage,
    double pathEntropy,
    boolean lowEntropyAlert,
    long elapsedMillis,
    long createdAtEpochMillis) {}
