package com.davidpe.scapeai.persistence.repository;

import com.davidpe.scapeai.persistence.ExplorationBudgetEntity;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcExplorationBudgetRepository implements ExplorationBudgetRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcExplorationBudgetRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public Optional<ExplorationBudgetEntity> findByPresetAndPolicy(long presetId, String policyId) {
    return jdbcTemplate
        .query(
            """
            SELECT id, preset_id, policy_id, initial_budget, consume_per_episode, remaining_budget, updated_at_epoch_millis
            FROM exploration_budgets
            WHERE preset_id = ? AND policy_id = ?
            LIMIT 1
            """,
            (rs, rowNum) ->
                new ExplorationBudgetEntity(
                    rs.getLong("id"),
                    rs.getLong("preset_id"),
                    rs.getString("policy_id"),
                    rs.getInt("initial_budget"),
                    rs.getInt("consume_per_episode"),
                    rs.getInt("remaining_budget"),
                    rs.getLong("updated_at_epoch_millis")),
            presetId,
            policyId)
        .stream()
        .findFirst();
  }

  @Override
  public ExplorationBudgetEntity upsert(ExplorationBudgetEntity budget) {
    Optional<ExplorationBudgetEntity> existing =
        findByPresetAndPolicy(budget.presetId(), budget.policyId());
    if (existing.isPresent()) {
      jdbcTemplate.update(
          """
          UPDATE exploration_budgets
          SET initial_budget = ?, consume_per_episode = ?, remaining_budget = ?, updated_at_epoch_millis = ?
          WHERE id = ?
          """,
          budget.initialBudget(),
          budget.consumePerEpisode(),
          budget.remainingBudget(),
          budget.updatedAtEpochMillis(),
          existing.get().id());
      return new ExplorationBudgetEntity(
          existing.get().id(),
          budget.presetId(),
          budget.policyId(),
          budget.initialBudget(),
          budget.consumePerEpisode(),
          budget.remainingBudget(),
          budget.updatedAtEpochMillis());
    }

    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          PreparedStatement statement =
              connection.prepareStatement(
                  """
                  INSERT INTO exploration_budgets(
                    preset_id, policy_id, initial_budget, consume_per_episode, remaining_budget, updated_at_epoch_millis
                  )
                  VALUES (?, ?, ?, ?, ?, ?)
                  """,
                  Statement.RETURN_GENERATED_KEYS);
          statement.setLong(1, budget.presetId());
          statement.setString(2, budget.policyId());
          statement.setInt(3, budget.initialBudget());
          statement.setInt(4, budget.consumePerEpisode());
          statement.setInt(5, budget.remainingBudget());
          statement.setLong(6, budget.updatedAtEpochMillis());
          return statement;
        },
        keyHolder);
    Number key = keyHolder.getKey();
    return new ExplorationBudgetEntity(
        key == null ? null : key.longValue(),
        budget.presetId(),
        budget.policyId(),
        budget.initialBudget(),
        budget.consumePerEpisode(),
        budget.remainingBudget(),
        budget.updatedAtEpochMillis());
  }
}
