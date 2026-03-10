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
    double trainingHealthIndex,
    boolean healthRegression,
    String healthIndexFormulaVersion,
    long elapsedMillis,
    long createdAtEpochMillis) {}
