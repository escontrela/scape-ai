package com.davidpe.scapeai.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.scapeai.persistence.repository.JdbcMazeRepository;
import com.davidpe.scapeai.persistence.repository.JdbcMazeCoverageRepository;
import com.davidpe.scapeai.persistence.repository.JdbcExperienceReplayRepository;
import com.davidpe.scapeai.persistence.repository.JdbcTrainingPresetRepository;
import com.davidpe.scapeai.persistence.repository.JdbcTrainingRunRepository;
import com.davidpe.scapeai.persistence.repository.ExperienceReplaySamplingStrategy;
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
            name TEXT NOT NULL UNIQUE,
            rows_count INTEGER NOT NULL,
            cols_count INTEGER NOT NULL,
            layout TEXT NOT NULL,
            difficulty_score REAL NOT NULL DEFAULT 0
          )
          """);
      jdbcTemplate.execute(
          """
          CREATE TABLE training_runs (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            training_session_id TEXT,
            maze_id INTEGER NOT NULL,
            policy_id TEXT,
            policy_snapshot TEXT,
            reward_version TEXT NOT NULL DEFAULT 'v1',
            success INTEGER NOT NULL,
            steps INTEGER NOT NULL,
            elapsed_millis INTEGER NOT NULL,
            total_reward REAL NOT NULL,
            collisions INTEGER NOT NULL,
            discovered_cells INTEGER NOT NULL,
            final_distance_to_exit INTEGER NOT NULL,
            net_progress REAL NOT NULL DEFAULT 0,
            maze_coverage_ratio REAL NOT NULL DEFAULT 0,
            q1_coverage REAL NOT NULL DEFAULT 0,
            q2_coverage REAL NOT NULL DEFAULT 0,
            q3_coverage REAL NOT NULL DEFAULT 0,
            q4_coverage REAL NOT NULL DEFAULT 0,
            left_side_coverage REAL NOT NULL DEFAULT 0,
            right_side_coverage REAL NOT NULL DEFAULT 0,
            path_entropy REAL NOT NULL DEFAULT 0,
            episode_debug_snapshots TEXT,
            replay_debug_metadata TEXT,
            cell_visit_frequencies TEXT,
            terminal_reason TEXT NOT NULL DEFAULT 'ABORTED',
            timeout_reached INTEGER NOT NULL DEFAULT 0,
            training_health_index REAL NOT NULL DEFAULT 0,
            health_index_formula_version TEXT NOT NULL DEFAULT 'v1.0.0',
            created_at_epoch_millis INTEGER NOT NULL
          )
          """);
      jdbcTemplate.execute(
          """
          CREATE TABLE reward_config_versions (
            version_id TEXT PRIMARY KEY,
            activated_at_epoch_millis INTEGER NOT NULL
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
      jdbcTemplate.execute(
          """
          CREATE TABLE experience_transitions (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            state_summary TEXT NOT NULL,
            action TEXT NOT NULL,
            reward REAL NOT NULL,
            next_state_summary TEXT NOT NULL,
            created_at_epoch_millis INTEGER NOT NULL
          )
          """);
      jdbcTemplate.execute(
          """
          CREATE TABLE maze_policy_coverage (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            maze_id INTEGER NOT NULL,
            policy_id TEXT NOT NULL,
            solved INTEGER NOT NULL DEFAULT 0,
            updated_at_epoch_millis INTEGER NOT NULL,
            UNIQUE(maze_id, policy_id)
          )
          """);

      JdbcMazeRepository mazeRepository = new JdbcMazeRepository(jdbcTemplate);
      JdbcMazeCoverageRepository coverageRepository = new JdbcMazeCoverageRepository(jdbcTemplate);
      JdbcTrainingRunRepository runRepository =
          new JdbcTrainingRunRepository(jdbcTemplate, coverageRepository, "reward-v1");
      JdbcTrainingPresetRepository presetRepository = new JdbcTrainingPresetRepository(jdbcTemplate);
      JdbcExperienceReplayRepository replayRepository = new JdbcExperienceReplayRepository(jdbcTemplate);

      MazeEntity maze =
          mazeRepository.save(new MazeEntity(null, "Training Maze", 10, 10, "########", 42.0));
      MazeEntity easier =
          mazeRepository.upsertByName(new MazeEntity(null, "Easy Maze", 6, 6, "......", 18.5));
      MazeEntity harder =
          mazeRepository.upsertByName(new MazeEntity(null, "Hard Maze", 20, 20, "######", 91.0));
      assertTrue(maze.id() > 0);

      runRepository.save(
          new TrainingRunEntity(
              null,
              "session-alpha",
              maze.id(),
              "heuristic-baseline",
              "{\"policy\":\"heuristic-baseline\",\"seed\":null}",
              "reward-v1",
              false,
              24,
              1_500,
              -3.5,
              8,
              12,
              4,
              -1.0,
              0.35,
              0.40,
              0.25,
              0.10,
              0.15,
              0.50,
              0.40,
              0.88,
              "[{\"milestone\":\"FINAL\"}]",
              "{\"seed\":20260309}",
              "0:0:3;0:1:2;1:1:1",
              "TIMEOUT",
              true,
              58.4,
              "v1.0.0",
              1000));
      runRepository.save(
          new TrainingRunEntity(
              null,
              "session-alpha",
              maze.id(),
              "random-controlled",
              "{\"policy\":\"random-controlled\",\"seed\":20260309}",
              "reward-v2",
              true,
              18,
              1_000,
              4.0,
              1,
              19,
              0,
              3.0,
              0.62,
              0.55,
              0.68,
              0.35,
              0.72,
              0.45,
              0.70,
              1.32,
              "[{\"milestone\":\"FINAL\"}]",
              "{\"seed\":20260310}",
              "0:0:4;0:1:1;2:2:5",
              "EXIT_REACHED",
              false,
              83.1,
              "v1.0.0",
              2000));

      var history = runRepository.findByMazeId(maze.id());
      var latestOnly = runRepository.findRecentByMazeId(maze.id(), 1);
      var accumulatedHeatmap = runRepository.findAccumulatedCellVisitsByMazeId(maze.id(), 2);
      var byRunId = runRepository.findReplayDiagnosticByTrainingRunId(history.get(0).id());
      var latestDiagnostics = runRepository.findRecentReplayDiagnostics(2);
      var sortedAsc = mazeRepository.findAllOrderByDifficulty(true);
      var sortedDesc = mazeRepository.findAllOrderByDifficulty(false);
      replayRepository.save(new ExperienceTransitionEntity(null, "s0", "RIGHT", 0.2, "s1", 1000));
      replayRepository.save(new ExperienceTransitionEntity(null, "s1", "RIGHT", 0.3, "s2", 2000));
      replayRepository.save(new ExperienceTransitionEntity(null, "s2", "UP", -0.1, "s3", 3000));
      var replayPage0 = replayRepository.findRecent(0, 2);
      var replayPage1 = replayRepository.findRecent(1, 2);
      var replayRewardAware = replayRepository.findRecent(0, 2, ExperienceReplaySamplingStrategy.REWARD_AWARE);
      var pendingCoverage = coverageRepository.findPendingCoverageSummary();
      var preset =
          presetRepository.save(
              new TrainingPresetEntity(null, 30, 20_000L, "heuristic-baseline", 20260309L));
      var presets = presetRepository.findAll();
      var loadedPreset = presetRepository.findById(preset.id());

      assertEquals(2, history.size());
      assertEquals(1, latestOnly.size());
      assertEquals(4, accumulatedHeatmap.size());
      assertEquals(7, accumulatedHeatmap.get(0).visits());
      assertTrue(history.get(0).createdAtEpochMillis() >= history.get(1).createdAtEpochMillis());
      assertEquals(18, history.get(0).steps());
      assertEquals(true, history.get(0).success());
      assertEquals("random-controlled", history.get(0).policyId());
      assertEquals("{\"policy\":\"random-controlled\",\"seed\":20260309}", history.get(0).policySnapshot());
      assertEquals("reward-v2", history.get(0).rewardVersion());
      assertEquals(19, history.get(0).discoveredCells());
      assertEquals(0, history.get(0).finalDistanceToExit());
      assertEquals(3.0, history.get(0).netProgress());
      assertEquals(0.62, history.get(0).mazeCoverageRatio());
      assertEquals(0.70, history.get(0).rightSideCoverage());
      assertEquals(1.32, history.get(0).pathEntropy());
      assertEquals("[{\"milestone\":\"FINAL\"}]", history.get(0).episodeDebugSnapshots());
      assertEquals("{\"seed\":20260310}", history.get(0).replayDebugMetadata());
      assertEquals("0:0:4;0:1:1;2:2:5", history.get(0).cellVisitFrequencies());
      assertTrue(byRunId.isPresent());
      assertEquals(history.get(0).id(), byRunId.get().trainingRunId());
      assertEquals("{\"seed\":20260310}", byRunId.get().replayDebugMetadata());
      assertEquals(2, latestDiagnostics.size());
      assertEquals(history.get(0).id(), latestDiagnostics.get(0).trainingRunId());
      assertEquals("EXIT_REACHED", history.get(0).terminalReason());
      assertEquals(false, history.get(0).timeoutReached());
      assertEquals(83.1, history.get(0).trainingHealthIndex());
      assertEquals("v1.0.0", history.get(0).healthIndexFormulaVersion());
      assertEquals(3, sortedAsc.size());
      assertEquals(easier.name(), sortedAsc.get(0).name());
      assertEquals(harder.name(), sortedDesc.get(0).name());
      assertEquals(2, replayPage0.size());
      assertEquals("s2", replayPage0.get(0).stateSummary());
      assertEquals(1, replayPage1.size());
      assertEquals("s0", replayPage1.get(0).stateSummary());
      assertEquals(2, replayRewardAware.size());
      assertEquals("s1", replayRewardAware.get(0).stateSummary());
      assertEquals(1, pendingCoverage.size());
      assertEquals("Training Maze", pendingCoverage.get(0).mazeName());
      assertEquals(1L, pendingCoverage.get(0).pendingPolicies());
      assertEquals(1, presets.size());
      assertTrue(loadedPreset.isPresent());
      assertEquals(30, loadedPreset.get().episodes());
      assertEquals("heuristic-baseline", loadedPreset.get().policy());
    }

    Files.deleteIfExists(dbFile);
  }
}
