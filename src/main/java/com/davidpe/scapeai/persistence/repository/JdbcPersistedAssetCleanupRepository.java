package com.davidpe.scapeai.persistence.repository;

import com.davidpe.scapeai.application.PersistedAssetType;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPersistedAssetCleanupRepository implements PersistedAssetCleanupRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcPersistedAssetCleanupRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public Optional<String> findMazeNameById(long mazeId) {
    try {
      return jdbcTemplate
          .queryForList("SELECT name FROM mazes WHERE id = ? LIMIT 1", String.class, mazeId)
          .stream()
          .findFirst();
    } catch (DataAccessException ignored) {
      return Optional.empty();
    }
  }

  @Override
  public long countTrainingRunsByMazeId(long mazeId) {
    return safeCount("SELECT COUNT(1) FROM training_runs WHERE maze_id = ?", mazeId);
  }

  @Override
  public long countMazeCoverageByMazeId(long mazeId) {
    return safeCount("SELECT COUNT(1) FROM maze_policy_coverage WHERE maze_id = ?", mazeId);
  }

  @Override
  public long countTrainingSessionsByMazeRef(String mazeRef) {
    return safeCount("SELECT COUNT(1) FROM training_sessions WHERE maze_ref = ?", mazeRef);
  }

  @Override
  public long countTrainingRunsBySessionId(String sessionId) {
    return safeCount(
        "SELECT COUNT(1) FROM training_runs WHERE training_session_id = ?", sessionId);
  }

  @Override
  public long countTrainingSessionsByPresetId(long presetId) {
    return safeCount("SELECT COUNT(1) FROM training_sessions WHERE preset_id = ?", presetId);
  }

  @Override
  public long countExplorationBudgetsByPresetId(long presetId) {
    return safeCount("SELECT COUNT(1) FROM exploration_budgets WHERE preset_id = ?", presetId);
  }

  @Override
  public boolean deleteByTypeAndId(PersistedAssetType type, String assetId) {
    if (type == null || assetId == null || assetId.isBlank()) {
      return false;
    }
    try {
      int updated =
          switch (type) {
            case MAZE ->
                jdbcTemplate.update("DELETE FROM mazes WHERE id = ?", Long.parseLong(assetId));
            case TRAINING_RUN ->
                jdbcTemplate.update(
                    "DELETE FROM training_runs WHERE id = ?", Long.parseLong(assetId));
            case TRAINING_PRESET ->
                jdbcTemplate.update(
                    "DELETE FROM training_presets WHERE id = ?", Long.parseLong(assetId));
            case TRAINING_SESSION ->
                jdbcTemplate.update("DELETE FROM training_sessions WHERE id = ?", assetId.trim());
            case EXPERIENCE_TRANSITION ->
                jdbcTemplate.update(
                    "DELETE FROM experience_transitions WHERE id = ?", Long.parseLong(assetId));
            case MAZE_POLICY_COVERAGE ->
                jdbcTemplate.update(
                    "DELETE FROM maze_policy_coverage WHERE id = ?", Long.parseLong(assetId));
            case EXPLORATION_BUDGET ->
                jdbcTemplate.update(
                    "DELETE FROM exploration_budgets WHERE id = ?", Long.parseLong(assetId));
          };
      return updated > 0;
    } catch (DataAccessException | NumberFormatException ignored) {
      return false;
    }
  }

  private long safeCount(String sql, Object arg) {
    try {
      Long value = jdbcTemplate.queryForObject(sql, Long.class, arg);
      return value == null ? 0L : value;
    } catch (DataAccessException ignored) {
      return 0L;
    }
  }
}
