package com.davidpe.scapeai.persistence;

import java.util.HashSet;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component("sqliteSchemaMigrator")
public class SqliteSchemaMigrator {

  private final JdbcTemplate jdbcTemplate;

  public SqliteSchemaMigrator(DataSource dataSource) {
    this.jdbcTemplate = new JdbcTemplate(dataSource);
    migrateMazes();
    migrateTrainingRuns();
    migrateExplorationBudgets();
  }

  private void migrateMazes() {
    if (!tableExists("mazes")) {
      return;
    }
    Set<String> columns = tableColumns("mazes");
    if (!columns.contains("difficulty_score")) {
      jdbcTemplate.execute("ALTER TABLE mazes ADD COLUMN difficulty_score REAL NOT NULL DEFAULT 0");
    }
  }

  private void migrateTrainingRuns() {
    if (!tableExists("training_runs")) {
      return;
    }
    Set<String> columns = tableColumns("training_runs");
    if (!columns.contains("policy_id")) {
      jdbcTemplate.execute("ALTER TABLE training_runs ADD COLUMN policy_id TEXT");
    }
    if (!columns.contains("policy_snapshot")) {
      jdbcTemplate.execute("ALTER TABLE training_runs ADD COLUMN policy_snapshot TEXT");
    }
    if (!columns.contains("reward_version")) {
      jdbcTemplate.execute("ALTER TABLE training_runs ADD COLUMN reward_version TEXT NOT NULL DEFAULT 'v1'");
    }
    if (!columns.contains("net_progress")) {
      jdbcTemplate.execute(
          "ALTER TABLE training_runs ADD COLUMN net_progress REAL NOT NULL DEFAULT 0");
    }
    if (!columns.contains("maze_coverage_ratio")) {
      jdbcTemplate.execute(
          "ALTER TABLE training_runs ADD COLUMN maze_coverage_ratio REAL NOT NULL DEFAULT 0");
    }
    if (!columns.contains("q1_coverage")) {
      jdbcTemplate.execute(
          "ALTER TABLE training_runs ADD COLUMN q1_coverage REAL NOT NULL DEFAULT 0");
    }
    if (!columns.contains("q2_coverage")) {
      jdbcTemplate.execute(
          "ALTER TABLE training_runs ADD COLUMN q2_coverage REAL NOT NULL DEFAULT 0");
    }
    if (!columns.contains("q3_coverage")) {
      jdbcTemplate.execute(
          "ALTER TABLE training_runs ADD COLUMN q3_coverage REAL NOT NULL DEFAULT 0");
    }
    if (!columns.contains("q4_coverage")) {
      jdbcTemplate.execute(
          "ALTER TABLE training_runs ADD COLUMN q4_coverage REAL NOT NULL DEFAULT 0");
    }
    if (!columns.contains("left_side_coverage")) {
      jdbcTemplate.execute(
          "ALTER TABLE training_runs ADD COLUMN left_side_coverage REAL NOT NULL DEFAULT 0");
    }
    if (!columns.contains("right_side_coverage")) {
      jdbcTemplate.execute(
          "ALTER TABLE training_runs ADD COLUMN right_side_coverage REAL NOT NULL DEFAULT 0");
    }
    if (!columns.contains("path_entropy")) {
      jdbcTemplate.execute(
          "ALTER TABLE training_runs ADD COLUMN path_entropy REAL NOT NULL DEFAULT 0");
    }
    if (!columns.contains("episode_debug_snapshots")) {
      jdbcTemplate.execute("ALTER TABLE training_runs ADD COLUMN episode_debug_snapshots TEXT");
    }
    if (!columns.contains("replay_debug_metadata")) {
      jdbcTemplate.execute("ALTER TABLE training_runs ADD COLUMN replay_debug_metadata TEXT");
    }
    if (!columns.contains("cell_visit_frequencies")) {
      jdbcTemplate.execute("ALTER TABLE training_runs ADD COLUMN cell_visit_frequencies TEXT");
    }
    if (!columns.contains("timeout_reached")) {
      jdbcTemplate.execute(
          "ALTER TABLE training_runs ADD COLUMN timeout_reached INTEGER NOT NULL DEFAULT 0");
    }
    if (!columns.contains("terminal_reason")) {
      jdbcTemplate.execute("ALTER TABLE training_runs ADD COLUMN terminal_reason TEXT");
      jdbcTemplate.execute(
          """
          UPDATE training_runs
          SET terminal_reason = CASE
            WHEN timeout_reached = 1 THEN 'TIMEOUT'
            WHEN success = 1 THEN 'EXIT_REACHED'
            ELSE 'ABORTED'
          END
          WHERE terminal_reason IS NULL OR TRIM(terminal_reason) = ''
          """);
    }
    if (!columns.contains("training_health_index")) {
      jdbcTemplate.execute(
          "ALTER TABLE training_runs ADD COLUMN training_health_index REAL NOT NULL DEFAULT 0");
    }
    if (!columns.contains("health_index_formula_version")) {
      jdbcTemplate.execute(
          "ALTER TABLE training_runs ADD COLUMN health_index_formula_version TEXT NOT NULL DEFAULT 'v1.0.0'");
    }
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS reward_config_versions (
          version_id TEXT PRIMARY KEY,
          activated_at_epoch_millis INTEGER NOT NULL
        )
        """);
    jdbcTemplate.update(
        """
        INSERT INTO reward_config_versions(version_id, activated_at_epoch_millis)
        SELECT 'v1', CAST(strftime('%s','now') AS INTEGER) * 1000
        WHERE NOT EXISTS (
          SELECT 1 FROM reward_config_versions WHERE version_id = 'v1'
        )
        """);
  }

  private boolean tableExists(String tableName) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT count(1) FROM sqlite_master WHERE type='table' AND name=?",
            Integer.class,
            tableName);
    return count != null && count > 0;
  }

  private Set<String> tableColumns(String tableName) {
    Set<String> columns = new HashSet<>();
    jdbcTemplate
        .query("PRAGMA table_info(" + tableName + ")", (rs, rowNum) -> rs.getString("name"))
        .forEach(columns::add);
    return columns;
  }

  private void migrateExplorationBudgets() {
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS exploration_budgets (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          preset_id INTEGER NOT NULL,
          policy_id TEXT NOT NULL,
          initial_budget INTEGER NOT NULL,
          consume_per_episode INTEGER NOT NULL,
          remaining_budget INTEGER NOT NULL,
          updated_at_epoch_millis INTEGER NOT NULL,
          UNIQUE(preset_id, policy_id)
        )
        """);
  }
}
