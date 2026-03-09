package com.davidpe.scapeai.application;

public record RecentRunComparisonRow(
    boolean success,
    double reward,
    int collisions,
    long elapsedMillis,
    long createdAtEpochMillis) {}
