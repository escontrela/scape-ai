package com.davidpe.scapeai.persistence.repository;

import com.davidpe.scapeai.application.PersistedAssetType;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPersistedAssetPreviewRepository implements PersistedAssetPreviewRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcPersistedAssetPreviewRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public Optional<Map<String, Object>> findByTypeAndId(PersistedAssetType type, String assetId) {
    if (type == null || assetId == null || assetId.isBlank()) {
      return Optional.empty();
    }
    try {
      return switch (type) {
        case MAZE -> queryOptionalMap(
            """
            SELECT id, name, rows_count, cols_count, difficulty_score, layout
            FROM mazes
            WHERE id = ?
            LIMIT 1
            """,
            parseLong(assetId));
        case TRAINING_RUN -> queryOptionalMap(
            """
            SELECT id, training_session_id, maze_id, policy_id, success, steps, elapsed_millis, total_reward,
                   collisions, discovered_cells, final_distance_to_exit, terminal_reason, timeout_reached,
                   replay_debug_metadata, trajectory_path, created_at_epoch_millis
            FROM training_runs
            WHERE id = ?
            LIMIT 1
            """,
            parseLong(assetId));
        case TRAINING_PRESET -> queryOptionalMap(
            """
            SELECT id, episodes, timeout_millis, policy, seed
            FROM training_presets
            WHERE id = ?
            LIMIT 1
            """,
            parseLong(assetId));
        case TRAINING_SESSION -> queryOptionalMap(
            """
            SELECT id, maze_ref, policy_id, preset_id, effective_seed, started_at_epoch_millis, ended_at_epoch_millis
            FROM training_sessions
            WHERE id = ?
            LIMIT 1
            """,
            assetId.trim());
        case EXPERIENCE_TRANSITION -> queryOptionalMap(
            """
            SELECT id, state_summary, action, reward, next_state_summary, created_at_epoch_millis
            FROM experience_transitions
            WHERE id = ?
            LIMIT 1
            """,
            parseLong(assetId));
        case MAZE_POLICY_COVERAGE -> queryOptionalMap(
            """
            SELECT id, maze_id, policy_id, solved, updated_at_epoch_millis
            FROM maze_policy_coverage
            WHERE id = ?
            LIMIT 1
            """,
            parseLong(assetId));
        case EXPLORATION_BUDGET -> queryOptionalMap(
            """
            SELECT id, preset_id, policy_id, initial_budget, consume_per_episode, remaining_budget, updated_at_epoch_millis
            FROM exploration_budgets
            WHERE id = ?
            LIMIT 1
            """,
            parseLong(assetId));
      };
    } catch (DataAccessException | NumberFormatException ignored) {
      return Optional.empty();
    }
  }

  private Optional<Map<String, Object>> queryOptionalMap(String sql, Object arg) {
    return jdbcTemplate.queryForList(sql, arg).stream().findFirst();
  }

  private static long parseLong(String value) {
    return Long.parseLong(value.trim());
  }
}
