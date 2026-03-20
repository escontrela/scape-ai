package com.davidpe.scapeai.persistence;

public record TrainingSessionEntity(
    String id,
    String mazeRef,
    String policyId,
    Long presetId,
    long effectiveSeed,
    long startedAtEpochMillis,
    Long endedAtEpochMillis) {}
