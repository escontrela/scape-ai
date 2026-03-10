package com.davidpe.scapeai.application;

public record AdaptiveDifficultyDecision(
    TrainingTargetDifficulty requested,
    TrainingTargetDifficulty resolved,
    double successRate,
    int sampleSize,
    boolean adaptiveApplied,
    String reason) {}
