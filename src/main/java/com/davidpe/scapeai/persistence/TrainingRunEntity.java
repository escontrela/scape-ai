package com.davidpe.scapeai.persistence;

public record TrainingRunEntity(
    Long id,
    long mazeId,
    String policyId,
    String policySnapshot,
    boolean success,
    int steps,
    long elapsedMillis,
    double totalReward,
    int collisions,
    int discoveredCells,
    int finalDistanceToExit,
    double netProgress,
    double q1Coverage,
    double q2Coverage,
    double q3Coverage,
    double q4Coverage,
    double leftSideCoverage,
    double rightSideCoverage,
    double pathEntropy,
    long createdAtEpochMillis) {}
