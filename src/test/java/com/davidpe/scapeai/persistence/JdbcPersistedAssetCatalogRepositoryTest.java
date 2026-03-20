package com.davidpe.scapeai.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.scapeai.application.PersistedAssetType;
import com.davidpe.scapeai.persistence.repository.JdbcPersistedAssetCatalogRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

class JdbcPersistedAssetCatalogRepositoryTest {

  @Test
  void shouldListPersistedAssetsAcrossOperationalTables() throws Exception {
    Path dbFile = Files.createTempFile("scape-ai-assets", ".db");
    try (var connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile)) {
      JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
      jdbcTemplate.execute(
          "CREATE TABLE mazes (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, rows_count INTEGER, cols_count INTEGER, layout TEXT, difficulty_score REAL)");
      jdbcTemplate.execute(
          "CREATE TABLE training_runs (id INTEGER PRIMARY KEY AUTOINCREMENT, maze_id INTEGER, policy_id TEXT, success INTEGER, steps INTEGER, total_reward REAL, terminal_reason TEXT, created_at_epoch_millis INTEGER, policy_snapshot TEXT, replay_debug_metadata TEXT, trajectory_path TEXT, cell_visit_frequencies TEXT)");
      jdbcTemplate.execute(
          "CREATE TABLE training_presets (id INTEGER PRIMARY KEY AUTOINCREMENT, episodes INTEGER, timeout_millis INTEGER, policy TEXT, seed INTEGER)");
      jdbcTemplate.execute(
          "CREATE TABLE training_sessions (id TEXT PRIMARY KEY, maze_ref TEXT, policy_id TEXT, preset_id INTEGER, effective_seed INTEGER, started_at_epoch_millis INTEGER, ended_at_epoch_millis INTEGER)");
      jdbcTemplate.execute(
          "CREATE TABLE experience_transitions (id INTEGER PRIMARY KEY AUTOINCREMENT, state_summary TEXT, action TEXT, reward REAL, next_state_summary TEXT, created_at_epoch_millis INTEGER)");
      jdbcTemplate.execute(
          "CREATE TABLE maze_policy_coverage (id INTEGER PRIMARY KEY AUTOINCREMENT, maze_id INTEGER, policy_id TEXT, solved INTEGER, updated_at_epoch_millis INTEGER)");
      jdbcTemplate.execute(
          "CREATE TABLE exploration_budgets (id INTEGER PRIMARY KEY AUTOINCREMENT, preset_id INTEGER, policy_id TEXT, initial_budget INTEGER, consume_per_episode INTEGER, remaining_budget INTEGER, updated_at_epoch_millis INTEGER)");

      jdbcTemplate.execute(
          "INSERT INTO mazes(name, rows_count, cols_count, layout, difficulty_score) VALUES ('maze-a', 10, 12, '########', 0.8)");
      jdbcTemplate.execute(
          "INSERT INTO training_runs(maze_id, policy_id, success, steps, total_reward, terminal_reason, created_at_epoch_millis, policy_snapshot, replay_debug_metadata, trajectory_path, cell_visit_frequencies) VALUES (1, 'heuristic-baseline', 1, 42, 12.3, 'EXIT_REACHED', 2000, '{}', '{}', '0,0->0,1', '0:0:1')");
      jdbcTemplate.execute(
          "INSERT INTO training_presets(episodes, timeout_millis, policy, seed) VALUES (50, 300000, 'heuristic-baseline', NULL)");
      jdbcTemplate.execute(
          "INSERT INTO training_sessions(id, maze_ref, policy_id, preset_id, effective_seed, started_at_epoch_millis, ended_at_epoch_millis) VALUES ('session-1', 'maze-a', 'heuristic-baseline', 1, 123, 1000, NULL)");
      jdbcTemplate.execute(
          "INSERT INTO experience_transitions(state_summary, action, reward, next_state_summary, created_at_epoch_millis) VALUES ('s0', 'UP', 1.0, 's1', 1500)");
      jdbcTemplate.execute(
          "INSERT INTO maze_policy_coverage(maze_id, policy_id, solved, updated_at_epoch_millis) VALUES (1, 'heuristic-baseline', 1, 1400)");
      jdbcTemplate.execute(
          "INSERT INTO exploration_budgets(preset_id, policy_id, initial_budget, consume_per_episode, remaining_budget, updated_at_epoch_millis) VALUES (1, 'heuristic-baseline', 120, 3, 87, 1300)");

      JdbcPersistedAssetCatalogRepository repository = new JdbcPersistedAssetCatalogRepository(jdbcTemplate);
      var items = repository.findAll();

      assertEquals(7, items.size());
      assertTrue(items.stream().anyMatch(item -> item.type() == PersistedAssetType.MAZE));
      assertTrue(items.stream().anyMatch(item -> item.type() == PersistedAssetType.TRAINING_RUN));
      assertTrue(items.stream().anyMatch(item -> item.type() == PersistedAssetType.TRAINING_PRESET));
      assertTrue(items.stream().anyMatch(item -> item.type() == PersistedAssetType.TRAINING_SESSION));
      assertTrue(items.stream().anyMatch(item -> item.type() == PersistedAssetType.EXPERIENCE_TRANSITION));
      assertTrue(items.stream().anyMatch(item -> item.type() == PersistedAssetType.MAZE_POLICY_COVERAGE));
      assertTrue(items.stream().anyMatch(item -> item.type() == PersistedAssetType.EXPLORATION_BUDGET));
    } finally {
      Files.deleteIfExists(dbFile);
    }
  }

  @Test
  void shouldIgnoreMissingOrPartiallyMigratedTables() throws Exception {
    Path dbFile = Files.createTempFile("scape-ai-assets-partial", ".db");
    try (var connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile)) {
      JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
      jdbcTemplate.execute(
          "CREATE TABLE mazes (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, rows_count INTEGER, cols_count INTEGER, layout TEXT, difficulty_score REAL)");
      jdbcTemplate.execute(
          "INSERT INTO mazes(name, rows_count, cols_count, layout, difficulty_score) VALUES ('maze-partial', 6, 6, '..##..', 0.4)");
      jdbcTemplate.execute(
          "CREATE TABLE training_runs (id INTEGER PRIMARY KEY AUTOINCREMENT, maze_id INTEGER)");

      JdbcPersistedAssetCatalogRepository repository = new JdbcPersistedAssetCatalogRepository(jdbcTemplate);
      var items = repository.findAll();

      assertEquals(1, items.size());
      assertEquals(PersistedAssetType.MAZE, items.get(0).type());
    } finally {
      Files.deleteIfExists(dbFile);
    }
  }
}
