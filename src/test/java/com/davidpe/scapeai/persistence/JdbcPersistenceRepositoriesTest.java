package com.davidpe.scapeai.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.scapeai.persistence.repository.JdbcMazeRepository;
import com.davidpe.scapeai.persistence.repository.JdbcTrainingRunRepository;
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
            success INTEGER NOT NULL,
            steps INTEGER NOT NULL,
            elapsed_millis INTEGER NOT NULL,
            total_reward REAL NOT NULL,
            created_at_epoch_millis INTEGER NOT NULL
          )
          """);

      JdbcMazeRepository mazeRepository = new JdbcMazeRepository(jdbcTemplate);
      JdbcTrainingRunRepository runRepository = new JdbcTrainingRunRepository(jdbcTemplate);

      MazeEntity maze = mazeRepository.save(new MazeEntity(null, "Training Maze", 10, 10, "########"));
      assertTrue(maze.id() > 0);

      runRepository.save(new TrainingRunEntity(null, maze.id(), false, 24, 1_500, -3.5, 1000));
      runRepository.save(new TrainingRunEntity(null, maze.id(), true, 18, 1_000, 4.0, 2000));

      var history = runRepository.findByMazeId(maze.id());

      assertEquals(2, history.size());
      assertTrue(history.get(0).createdAtEpochMillis() >= history.get(1).createdAtEpochMillis());
      assertEquals(18, history.get(0).steps());
      assertEquals(true, history.get(0).success());
    }

    Files.deleteIfExists(dbFile);
  }
}
