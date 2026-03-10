package com.davidpe.scapeai.application;

public record ExplorationBudgetState(
    long presetId, String policyId, int initialBudget, int consumePerEpisode, int remainingBudget) {

  public ExplorationBudgetState {
    policyId = policyId == null ? "" : policyId;
    initialBudget = Math.max(0, initialBudget);
    consumePerEpisode = Math.max(0, consumePerEpisode);
    remainingBudget = Math.max(0, remainingBudget);
  }
}
