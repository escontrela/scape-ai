package com.davidpe.scapeai.persistence;

public record TrainingRunEntity(
    Long id,
    long mazeId,
    boolean success,
    int steps,
    long elapsedMillis,
    double totalReward,
    long createdAtEpochMillis) {}
