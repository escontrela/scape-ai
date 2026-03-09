package com.davidpe.scapeai.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.scapeai.persistence.repository.JdbcMazeRepository;
import com.davidpe.scapeai.persistence.repository.JdbcTrainingPresetRepository;
import com.davidpe.scapeai.persistence.repository.JdbcTrainingRunRepository;
import com.davidpe.scapeai.persistence.TrainingPresetEntity;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

class JdbcPersistenceRepositoriesTest {

  @Test
  void shouldPersistMazeAndTrainingRunsAndListHistoryByMaze() throws Exception {
    Path dbFile = Files.createTempFile("scape-ai-test", ".db");

    try (var connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile)) {
      JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
      jdbcTemplate.execute(
          """
          CREATE TABLE mazes (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            rows_count INTEGER NOT NULL,
            cols_count INTEGER NOT NULL,
            layout TEXT NOT NULL
          )
          """);
      jdbcTemplate.execute(
          """
          CREATE TABLE training_runs (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            maze_id INTEGER NOT NULL,
            policy_id TEXT,
            policy_snapshot TEXT,
            success INTEGER NOT NULL,
            steps INTEGER NOT NULL,
            elapsed_millis INTEGER NOT NULL,
            total_reward REAL NOT NULL,
            collisions INTEGER NOT NULL,
            discovered_cells INTEGER NOT NULL,
            final_distance_to_exit INTEGER NOT NULL,
            created_at_epoch_millis INTEGER NOT NULL
          )
          """);
      jdbcTemplate.execute(
          """
          CREATE TABLE training_presets (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            episodes INTEGER NOT NULL,
            timeout_millis INTEGER NOT NULL,
            policy TEXT NOT NULL,
            seed INTEGER
          )
          """);

      JdbcMazeRepository mazeRepository = new JdbcMazeRepository(jdbcTemplate);
      JdbcTrainingRunRepository runRepository = new JdbcTrainingRunRepository(jdbcTemplate);
      JdbcTrainingPresetRepository presetRepository = new JdbcTrainingPresetRepository(jdbcTemplate);

      MazeEntity maze = mazeRepository.save(new MazeEntity(null, "Training Maze", 10, 10, "########"));
      assertTrue(maze.id() > 0);

      runRepository.save(
          new TrainingRunEntity(
              null,
              maze.id(),
              "heuristic-baseline",
              "{\"policy\":\"heuristic-baseline\",\"seed\":null}",
              false,
              24,
              1_500,
              -3.5,
              8,
              12,
              4,
              1000));
      runRepository.save(
          new TrainingRunEntity(
              null,
              maze.id(),
              "random-controlled",
              "{\"policy\":\"random-controlled\",\"seed\":20260309}",
              true,
              18,
              1_000,
              4.0,
              1,
              19,
              0,
              2000));

      var history = runRepository.findByMazeId(maze.id());
      var latestOnly = runRepository.findRecentByMazeId(maze.id(), 1);
      var preset =
          presetRepository.save(
              new TrainingPresetEntity(null, 30, 20_000L, "heuristic-baseline", 20260309L));
      var presets = presetRepository.findAll();
      var loadedPreset = presetRepository.findById(preset.id());

      assertEquals(2, history.size());
      assertEquals(1, latestOnly.size());
      assertTrue(history.get(0).createdAtEpochMillis() >= history.get(1).createdAtEpochMillis());
      assertEquals(18, history.get(0).steps());
      assertEquals(true, history.get(0).success());
      assertEquals("random-controlled", history.get(0).policyId());
      assertEquals("{\"policy\":\"random-controlled\",\"seed\":20260309}", history.get(0).policySnapshot());
      assertEquals(19, history.get(0).discoveredCells());
      assertEquals(0, history.get(0).finalDistanceToExit());
      assertEquals(1, presets.size());
      assertTrue(loadedPreset.isPresent());
      assertEquals(30, loadedPreset.get().episodes());
      assertEquals("heuristic-baseline", loadedPreset.get().policy());
    }

    Files.deleteIfExists(dbFile);
  }
}
