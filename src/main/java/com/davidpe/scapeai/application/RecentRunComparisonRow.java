package com.davidpe.scapeai.application;

public record RecentRunComparisonRow(
    boolean success,
    double reward,
    int collisions,
    double netProgress,
    double leftSideCoverage,
    double rightSideCoverage,
    long elapsedMillis,
    long createdAtEpochMillis) {}
