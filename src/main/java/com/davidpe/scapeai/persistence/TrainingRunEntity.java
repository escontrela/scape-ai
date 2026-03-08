package com.davidpe.scapeai.persistence;

public record TrainingRunEntity(
    Long id,
    long mazeId,
    boolean success,
    int steps,
    long elapsedMillis,
    double totalReward,
    int collisions,
    int discoveredCells,
    int finalDistanceToExit,
    long createdAtEpochMillis) {}
