package com.davidpe.scapeai.persistence;

public record ExperienceTransitionEntity(
    Long id,
    String stateSummary,
    String action,
    double reward,
    String nextStateSummary,
    long createdAtEpochMillis) {}
