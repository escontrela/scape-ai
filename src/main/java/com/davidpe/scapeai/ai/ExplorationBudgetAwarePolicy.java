package com.davidpe.scapeai.ai;

public interface ExplorationBudgetAwarePolicy {

  void applyExplorationBudget(int remainingBudget, int consumePerEpisode);
}
