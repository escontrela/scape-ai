package com.davidpe.scapeai.application;

public record RecentRunComparisonRow(
    boolean success,
    double reward,
    int collisions,
    double netProgress,
    long elapsedMillis,
    long createdAtEpochMillis) {}
