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
    if (!columns.contains("net_progress")) {
      jdbcTemplate.execute(
          "ALTER TABLE training_runs ADD COLUMN net_progress REAL NOT NULL DEFAULT 0");
    }
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
}
