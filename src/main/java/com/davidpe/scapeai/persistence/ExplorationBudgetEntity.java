package com.davidpe.scapeai.persistence;

public record ExplorationBudgetEntity(
    Long id,
    long presetId,
    String policyId,
    int initialBudget,
    int consumePerEpisode,
    int remainingBudget,
    long updatedAtEpochMillis) {}
