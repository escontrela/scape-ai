package com.davidpe.scapeai.persistence.repository;

import com.davidpe.scapeai.persistence.ExplorationBudgetEntity;
import java.util.Optional;

public interface ExplorationBudgetRepository {

  Optional<ExplorationBudgetEntity> findByPresetAndPolicy(long presetId, String policyId);

  ExplorationBudgetEntity upsert(ExplorationBudgetEntity budget);
}
