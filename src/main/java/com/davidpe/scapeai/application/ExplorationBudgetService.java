package com.davidpe.scapeai.application;

import com.davidpe.scapeai.ai.ExplorationBudgetAwarePolicy;
import com.davidpe.scapeai.ai.MovementPolicy;
import com.davidpe.scapeai.persistence.ExplorationBudgetEntity;
import com.davidpe.scapeai.persistence.repository.ExplorationBudgetRepository;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ExplorationBudgetService {

  private final ExplorationBudgetRepository repository;
  private final ActiveMovementPolicyService movementPolicyService;
  private final int defaultInitialBudget;
  private final int defaultConsumePerEpisode;
  private final AtomicReference<ExplorationBudgetState> activeState = new AtomicReference<>();

  public ExplorationBudgetService(
      ExplorationBudgetRepository repository,
      ActiveMovementPolicyService movementPolicyService,
      @Value("${scape.exploration-budget.initial:100}") int defaultInitialBudget,
      @Value("${scape.exploration-budget.consume-per-episode:5}") int defaultConsumePerEpisode) {
    this.repository = repository;
    this.movementPolicyService = movementPolicyService;
    this.defaultInitialBudget = Math.max(0, defaultInitialBudget);
    this.defaultConsumePerEpisode = Math.max(0, defaultConsumePerEpisode);
  }

  public Optional<ExplorationBudgetState> startSession(Long presetId, String policyId) {
    if (presetId == null || policyId == null || policyId.isBlank()) {
      activeState.set(null);
      applyBudgetToActivePolicy(0, 0);
      return Optional.empty();
    }

    ExplorationBudgetEntity entity =
        repository
            .findByPresetAndPolicy(presetId, policyId)
            .orElseGet(
                () ->
                    repository.upsert(
                        new ExplorationBudgetEntity(
                            null,
                            presetId,
                            policyId,
                            defaultInitialBudget,
                            defaultConsumePerEpisode,
                            defaultInitialBudget,
                            System.currentTimeMillis())));
    ExplorationBudgetState state =
        new ExplorationBudgetState(
            entity.presetId(),
            entity.policyId(),
            entity.initialBudget(),
            entity.consumePerEpisode(),
            entity.remainingBudget());
    activeState.set(state);
    applyBudgetToActivePolicy(state.remainingBudget(), state.consumePerEpisode());
    return Optional.of(state);
  }

  public Optional<ExplorationBudgetState> consumeEpisodeBudget() {
    ExplorationBudgetState current = activeState.get();
    if (current == null) {
      return Optional.empty();
    }
    int remaining = Math.max(0, current.remainingBudget() - current.consumePerEpisode());
    ExplorationBudgetEntity saved =
        repository.upsert(
            new ExplorationBudgetEntity(
                null,
                current.presetId(),
                current.policyId(),
                current.initialBudget(),
                current.consumePerEpisode(),
                remaining,
                System.currentTimeMillis()));
    ExplorationBudgetState updated =
        new ExplorationBudgetState(
            saved.presetId(),
            saved.policyId(),
            saved.initialBudget(),
            saved.consumePerEpisode(),
            saved.remainingBudget());
    activeState.set(updated);
    applyBudgetToActivePolicy(updated.remainingBudget(), updated.consumePerEpisode());
    return Optional.of(updated);
  }

  private void applyBudgetToActivePolicy(int remainingBudget, int consumePerEpisode) {
    MovementPolicy policy = movementPolicyService.activePolicy();
    if (policy instanceof ExplorationBudgetAwarePolicy budgetAwarePolicy) {
      budgetAwarePolicy.applyExplorationBudget(remainingBudget, consumePerEpisode);
    }
  }
}
