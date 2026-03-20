package com.davidpe.scapeai.persistence.repository;

import com.davidpe.scapeai.application.PersistedAssetCatalogItem;
import com.davidpe.scapeai.application.PersistedAssetType;
import java.util.ArrayList;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPersistedAssetCatalogRepository implements PersistedAssetCatalogRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcPersistedAssetCatalogRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<PersistedAssetCatalogItem> findAll() {
    List<PersistedAssetCatalogItem> items = new ArrayList<>();
    items.addAll(readMazes());
    items.addAll(readTrainingRuns());
    items.addAll(readPresets());
    items.addAll(readSessions());
    items.addAll(readTransitions());
    items.addAll(readCoverage());
    items.addAll(readBudgets());
    return items;
  }

  private List<PersistedAssetCatalogItem> readMazes() {
    return safeQuery(
        """
        SELECT id, name, rows_count, cols_count, difficulty_score, layout
        FROM mazes
        ORDER BY id ASC
        """,
        (rs, rowNum) ->
            new PersistedAssetCatalogItem(
                PersistedAssetType.MAZE,
                Long.toString(rs.getLong("id")),
                null,
                estimateLength(rs.getString("name")) + estimateLength(rs.getString("layout")),
                "name="
                    + rs.getString("name")
                    + ",dims="
                    + rs.getInt("rows_count")
                    + "x"
                    + rs.getInt("cols_count")
                    + ",difficulty="
                    + String.format(java.util.Locale.US, "%.2f", rs.getDouble("difficulty_score"))));
  }

  private List<PersistedAssetCatalogItem> readTrainingRuns() {
    return safeQuery(
        """
        SELECT id, maze_id, policy_id, success, steps, total_reward, terminal_reason, created_at_epoch_millis,
               policy_snapshot, replay_debug_metadata, trajectory_path, cell_visit_frequencies
        FROM training_runs
        ORDER BY created_at_epoch_millis DESC
        """,
        (rs, rowNum) ->
            new PersistedAssetCatalogItem(
                PersistedAssetType.TRAINING_RUN,
                Long.toString(rs.getLong("id")),
                rs.getLong("created_at_epoch_millis"),
                estimateLength(rs.getString("policy_snapshot"))
                    + estimateLength(rs.getString("replay_debug_metadata"))
                    + estimateLength(rs.getString("trajectory_path"))
                    + estimateLength(rs.getString("cell_visit_frequencies")),
                "maze="
                    + rs.getLong("maze_id")
                    + ",policy="
                    + sanitize(rs.getString("policy_id"), "unknown")
                    + ",success="
                    + rs.getBoolean("success")
                    + ",steps="
                    + rs.getInt("steps")
                    + ",reward="
                    + String.format(java.util.Locale.US, "%.2f", rs.getDouble("total_reward"))
                    + ",terminal="
                    + sanitize(rs.getString("terminal_reason"), "UNKNOWN")));
  }

  private List<PersistedAssetCatalogItem> readPresets() {
    return safeQuery(
        """
        SELECT id, episodes, timeout_millis, policy, seed
        FROM training_presets
        ORDER BY id ASC
        """,
        (rs, rowNum) ->
            new PersistedAssetCatalogItem(
                PersistedAssetType.TRAINING_PRESET,
                Long.toString(rs.getLong("id")),
                null,
                estimateLength(rs.getString("policy")) + 32,
                "episodes="
                    + rs.getInt("episodes")
                    + ",timeoutMs="
                    + rs.getLong("timeout_millis")
                    + ",policy="
                    + sanitize(rs.getString("policy"), "unknown")
                    + ",seed="
                    + (rs.getObject("seed") == null ? "auto" : rs.getLong("seed"))));
  }

  private List<PersistedAssetCatalogItem> readSessions() {
    return safeQuery(
        """
        SELECT id, maze_ref, policy_id, preset_id, effective_seed, started_at_epoch_millis, ended_at_epoch_millis
        FROM training_sessions
        ORDER BY started_at_epoch_millis DESC
        """,
        (rs, rowNum) ->
            new PersistedAssetCatalogItem(
                PersistedAssetType.TRAINING_SESSION,
                sanitize(rs.getString("id"), "unknown"),
                rs.getLong("started_at_epoch_millis"),
                estimateLength(rs.getString("id"))
                    + estimateLength(rs.getString("maze_ref"))
                    + estimateLength(rs.getString("policy_id"))
                    + 64,
                "maze="
                    + sanitize(rs.getString("maze_ref"), "unknown")
                    + ",policy="
                    + sanitize(rs.getString("policy_id"), "unknown")
                    + ",preset="
                    + (rs.getObject("preset_id") == null ? "-" : rs.getLong("preset_id"))
                    + ",seed="
                    + rs.getLong("effective_seed")
                    + ",ended="
                    + (rs.getObject("ended_at_epoch_millis") == null)));
  }

  private List<PersistedAssetCatalogItem> readTransitions() {
    return safeQuery(
        """
        SELECT id, action, reward, state_summary, next_state_summary, created_at_epoch_millis
        FROM experience_transitions
        ORDER BY created_at_epoch_millis DESC
        """,
        (rs, rowNum) ->
            new PersistedAssetCatalogItem(
                PersistedAssetType.EXPERIENCE_TRANSITION,
                Long.toString(rs.getLong("id")),
                rs.getLong("created_at_epoch_millis"),
                estimateLength(rs.getString("state_summary"))
                    + estimateLength(rs.getString("next_state_summary"))
                    + estimateLength(rs.getString("action")),
                "action="
                    + sanitize(rs.getString("action"), "UNKNOWN")
                    + ",reward="
                    + String.format(java.util.Locale.US, "%.2f", rs.getDouble("reward"))));
  }

  private List<PersistedAssetCatalogItem> readCoverage() {
    return safeQuery(
        """
        SELECT id, maze_id, policy_id, solved, updated_at_epoch_millis
        FROM maze_policy_coverage
        ORDER BY updated_at_epoch_millis DESC
        """,
        (rs, rowNum) ->
            new PersistedAssetCatalogItem(
                PersistedAssetType.MAZE_POLICY_COVERAGE,
                Long.toString(rs.getLong("id")),
                rs.getLong("updated_at_epoch_millis"),
                estimateLength(rs.getString("policy_id")) + 24,
                "maze="
                    + rs.getLong("maze_id")
                    + ",policy="
                    + sanitize(rs.getString("policy_id"), "unknown")
                    + ",solved="
                    + rs.getBoolean("solved")));
  }

  private List<PersistedAssetCatalogItem> readBudgets() {
    return safeQuery(
        """
        SELECT id, preset_id, policy_id, initial_budget, consume_per_episode, remaining_budget, updated_at_epoch_millis
        FROM exploration_budgets
        ORDER BY updated_at_epoch_millis DESC
        """,
        (rs, rowNum) ->
            new PersistedAssetCatalogItem(
                PersistedAssetType.EXPLORATION_BUDGET,
                Long.toString(rs.getLong("id")),
                rs.getLong("updated_at_epoch_millis"),
                estimateLength(rs.getString("policy_id")) + 40,
                "preset="
                    + rs.getLong("preset_id")
                    + ",policy="
                    + sanitize(rs.getString("policy_id"), "unknown")
                    + ",initial="
                    + rs.getInt("initial_budget")
                    + ",remaining="
                    + rs.getInt("remaining_budget")));
  }

  private <T> List<T> safeQuery(
      String sql, org.springframework.jdbc.core.RowMapper<T> rowMapper) {
    try {
      return jdbcTemplate.query(sql, rowMapper);
    } catch (DataAccessException ignored) {
      return List.of();
    }
  }

  private static long estimateLength(String value) {
    return value == null ? 0 : value.length();
  }

  private static String sanitize(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }
}
