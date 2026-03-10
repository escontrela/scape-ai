package com.davidpe.scapeai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.scapeai.ai.ExplorationBudgetAwarePolicy;
import com.davidpe.scapeai.ai.MovementPolicy;
import com.davidpe.scapeai.ai.SpatialContext;
import com.davidpe.scapeai.persistence.ExplorationBudgetEntity;
import com.davidpe.scapeai.persistence.repository.ExplorationBudgetRepository;
import com.davidpe.scapeai.simulation.MoveDirection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ExplorationBudgetServiceTest {

  @Test
  void shouldLoadAndConsumeBudgetPerSession() {
    InMemoryExplorationBudgetRepository repository = new InMemoryExplorationBudgetRepository();
    BudgetAwarePolicy policy = new BudgetAwarePolicy();
    ActiveMovementPolicyService policyService =
        new ActiveMovementPolicyService(Map.of("heuristic-baseline", policy), "heuristic-baseline");
    ExplorationBudgetService service = new ExplorationBudgetService(repository, policyService, 20, 4);

    Optional<ExplorationBudgetState> started = service.startSession(1L, "heuristic-baseline");
    Optional<ExplorationBudgetState> consumed = service.consumeEpisodeBudget();

    assertTrue(started.isPresent());
    assertEquals(20, started.get().remainingBudget());
    assertTrue(consumed.isPresent());
    assertEquals(16, consumed.get().remainingBudget());
    assertEquals(16, policy.remainingBudget());
  }

  private static final class BudgetAwarePolicy implements MovementPolicy, ExplorationBudgetAwarePolicy {
    private int remainingBudget;

    @Override
    public MoveDirection chooseNextMove(SpatialContext context) {
      return MoveDirection.UP;
    }

    @Override
    public void applyExplorationBudget(int remainingBudget, int consumePerEpisode) {
      this.remainingBudget = remainingBudget;
    }

    int remainingBudget() {
      return remainingBudget;
    }
  }

  private static final class InMemoryExplorationBudgetRepository
      implements ExplorationBudgetRepository {

    private final AtomicReference<ExplorationBudgetEntity> stored = new AtomicReference<>();

    @Override
    public Optional<ExplorationBudgetEntity> findByPresetAndPolicy(long presetId, String policyId) {
      ExplorationBudgetEntity current = stored.get();
      if (current == null
          || current.presetId() != presetId
          || !current.policyId().equals(policyId)) {
        return Optional.empty();
      }
      return Optional.of(current);
    }

    @Override
    public ExplorationBudgetEntity upsert(ExplorationBudgetEntity budget) {
      ExplorationBudgetEntity saved =
          new ExplorationBudgetEntity(
              1L,
              budget.presetId(),
              budget.policyId(),
              budget.initialBudget(),
              budget.consumePerEpisode(),
              budget.remainingBudget(),
              budget.updatedAtEpochMillis());
      stored.set(saved);
      return saved;
    }
  }
}
