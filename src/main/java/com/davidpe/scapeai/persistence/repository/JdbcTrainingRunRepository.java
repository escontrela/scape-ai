package com.davidpe.scapeai.persistence.repository;

import com.davidpe.scapeai.application.CellVisitFrequency;
import com.davidpe.scapeai.application.TrainingHealthIndexFormula;
import com.davidpe.scapeai.persistence.TrainingRunEntity;
import com.davidpe.scapeai.persistence.TrainingRunReplayDiagnosticEntity;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTrainingRunRepository implements TrainingRunRepository {

  private final JdbcTemplate jdbcTemplate;
  private final MazeCoverageRepository mazeCoverageRepository;
  private final String defaultRewardVersion;

  public JdbcTrainingRunRepository(
      JdbcTemplate jdbcTemplate,
      MazeCoverageRepository mazeCoverageRepository,
      @Value("${scape.ai.reward.version:v1}") String defaultRewardVersion) {
    this.jdbcTemplate = jdbcTemplate;
    this.mazeCoverageRepository = mazeCoverageRepository;
    this.defaultRewardVersion =
        defaultRewardVersion == null || defaultRewardVersion.isBlank()
            ? "v1"
            : defaultRewardVersion.trim();
  }

  @Override
  public TrainingRunEntity save(TrainingRunEntity run) {
    String terminalReason =
        normalizeTerminalReason(run.terminalReason(), run.success(), run.timeoutReached());
    String rewardVersion = normalizeRewardVersion(run.rewardVersion());
    ensureRewardVersion(rewardVersion, run.createdAtEpochMillis());
    double timeoutRatio = computeTimeoutRatio(run.mazeId(), run.timeoutReached());
    double trainingHealthIndex =
        run.trainingHealthIndex() > 0.0
            ? run.trainingHealthIndex()
            : TrainingHealthIndexFormula.calculate(
                run.success(), run.mazeCoverageRatio(), run.pathEntropy(), timeoutRatio);
    String formulaVersion =
        run.healthIndexFormulaVersion() == null || run.healthIndexFormulaVersion().isBlank()
            ? TrainingHealthIndexFormula.FORMULA_VERSION
            : run.healthIndexFormulaVersion();
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          PreparedStatement statement =
              connection.prepareStatement(
                  """
                  INSERT INTO training_runs(
                    maze_id, policy_id, policy_snapshot, reward_version, success, steps, elapsed_millis, total_reward, collisions, discovered_cells, final_distance_to_exit, net_progress, maze_coverage_ratio, q1_coverage, q2_coverage, q3_coverage, q4_coverage, left_side_coverage, right_side_coverage, path_entropy, episode_debug_snapshots, replay_debug_metadata, cell_visit_frequencies, terminal_reason, timeout_reached, training_health_index, health_index_formula_version, created_at_epoch_millis
                  )
                  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                  """,
                  Statement.RETURN_GENERATED_KEYS);
          statement.setLong(1, run.mazeId());
          statement.setString(2, run.policyId());
          statement.setString(3, run.policySnapshot());
          statement.setString(4, rewardVersion);
          statement.setBoolean(5, run.success());
          statement.setInt(6, run.steps());
          statement.setLong(7, run.elapsedMillis());
          statement.setDouble(8, run.totalReward());
          statement.setInt(9, run.collisions());
          statement.setInt(10, run.discoveredCells());
          statement.setInt(11, run.finalDistanceToExit());
          statement.setDouble(12, run.netProgress());
          statement.setDouble(13, run.mazeCoverageRatio());
          statement.setDouble(14, run.q1Coverage());
          statement.setDouble(15, run.q2Coverage());
          statement.setDouble(16, run.q3Coverage());
          statement.setDouble(17, run.q4Coverage());
          statement.setDouble(18, run.leftSideCoverage());
          statement.setDouble(19, run.rightSideCoverage());
          statement.setDouble(20, run.pathEntropy());
          statement.setString(21, run.episodeDebugSnapshots());
          statement.setString(22, run.replayDebugMetadata());
          statement.setString(23, run.cellVisitFrequencies());
          statement.setString(24, terminalReason);
          statement.setBoolean(25, run.timeoutReached());
          statement.setDouble(26, trainingHealthIndex);
          statement.setString(27, formulaVersion);
          statement.setLong(28, run.createdAtEpochMillis());
          return statement;
        },
        keyHolder);

    Number key = keyHolder.getKey();
    mazeCoverageRepository.upsertCoverage(
        run.mazeId(), run.policyId(), run.success(), run.createdAtEpochMillis());
    return new TrainingRunEntity(
        key.longValue(),
        run.mazeId(),
        run.policyId(),
        run.policySnapshot(),
        rewardVersion,
        run.success(),
        run.steps(),
        run.elapsedMillis(),
        run.totalReward(),
        run.collisions(),
        run.discoveredCells(),
        run.finalDistanceToExit(),
        run.netProgress(),
        run.mazeCoverageRatio(),
        run.q1Coverage(),
        run.q2Coverage(),
        run.q3Coverage(),
        run.q4Coverage(),
        run.leftSideCoverage(),
        run.rightSideCoverage(),
        run.pathEntropy(),
        run.episodeDebugSnapshots(),
        run.replayDebugMetadata(),
        run.cellVisitFrequencies(),
        terminalReason,
        run.timeoutReached(),
        trainingHealthIndex,
        formulaVersion,
        run.createdAtEpochMillis());
  }

  @Override
  public List<TrainingRunEntity> findByMazeId(long mazeId) {
    return findRecentByMazeId(mazeId, 100);
  }

  @Override
  public List<TrainingRunEntity> findRecentByMazeId(long mazeId, int limit) {
    return jdbcTemplate.query(
        """
        SELECT id, maze_id, policy_id, policy_snapshot, reward_version, success, steps, elapsed_millis, total_reward, collisions, discovered_cells, final_distance_to_exit, net_progress, maze_coverage_ratio, q1_coverage, q2_coverage, q3_coverage, q4_coverage, left_side_coverage, right_side_coverage, path_entropy, episode_debug_snapshots, replay_debug_metadata, cell_visit_frequencies, terminal_reason, timeout_reached, training_health_index, health_index_formula_version, created_at_epoch_millis
        FROM training_runs
        WHERE maze_id = ?
        ORDER BY created_at_epoch_millis DESC
        LIMIT ?
        """,
        (rs, rowNum) ->
            new TrainingRunEntity(
                rs.getLong("id"),
                rs.getLong("maze_id"),
                rs.getString("policy_id"),
                rs.getString("policy_snapshot"),
                normalizeRewardVersion(rs.getString("reward_version")),
                rs.getBoolean("success"),
                rs.getInt("steps"),
                rs.getLong("elapsed_millis"),
                rs.getDouble("total_reward"),
                rs.getInt("collisions"),
                rs.getInt("discovered_cells"),
                rs.getInt("final_distance_to_exit"),
                rs.getDouble("net_progress"),
                rs.getDouble("maze_coverage_ratio"),
                rs.getDouble("q1_coverage"),
                rs.getDouble("q2_coverage"),
                rs.getDouble("q3_coverage"),
                rs.getDouble("q4_coverage"),
                rs.getDouble("left_side_coverage"),
                rs.getDouble("right_side_coverage"),
                rs.getDouble("path_entropy"),
                rs.getString("episode_debug_snapshots"),
                rs.getString("replay_debug_metadata"),
                rs.getString("cell_visit_frequencies"),
                normalizeTerminalReason(
                    rs.getString("terminal_reason"),
                    rs.getBoolean("success"),
                    rs.getBoolean("timeout_reached")),
                rs.getBoolean("timeout_reached"),
                rs.getDouble("training_health_index"),
                rs.getString("health_index_formula_version"),
                rs.getLong("created_at_epoch_millis")),
        mazeId,
        limit);
  }

  @Override
  public List<CellVisitFrequency> findAccumulatedCellVisitsByMazeId(long mazeId, int limit) {
    List<String> encodedRuns =
        jdbcTemplate.query(
            """
            SELECT cell_visit_frequencies
            FROM training_runs
            WHERE maze_id = ?
            ORDER BY created_at_epoch_millis DESC
            LIMIT ?
            """,
            (rs, rowNum) -> rs.getString("cell_visit_frequencies"),
            mazeId,
            Math.max(1, limit));
    java.util.Map<com.davidpe.scapeai.simulation.GridPosition, Integer> totals = new java.util.HashMap<>();
    for (String encoded : encodedRuns) {
      for (CellVisitFrequency frequency : CellVisitFrequency.decode(encoded)) {
        totals.merge(frequency.position(), frequency.visits(), Integer::sum);
      }
    }
    return totals.entrySet().stream()
        .map(entry -> new CellVisitFrequency(entry.getKey(), entry.getValue()))
        .sorted(
            java.util.Comparator.comparingInt(
                    (CellVisitFrequency frequency) -> frequency.position().row())
                .thenComparingInt(frequency -> frequency.position().col()))
        .toList();
  }

  @Override
  public Optional<TrainingRunReplayDiagnosticEntity> findReplayDiagnosticByTrainingRunId(long trainingRunId) {
    List<TrainingRunReplayDiagnosticEntity> rows =
        jdbcTemplate.query(
            """
            SELECT id, created_at_epoch_millis, replay_debug_metadata
            FROM training_runs
            WHERE id = ?
            """,
            (rs, rowNum) ->
                new TrainingRunReplayDiagnosticEntity(
                    rs.getLong("id"),
                    rs.getLong("created_at_epoch_millis"),
                    rs.getString("replay_debug_metadata")),
            trainingRunId);
    return rows.stream().findFirst();
  }

  @Override
  public List<TrainingRunReplayDiagnosticEntity> findRecentReplayDiagnostics(int limit) {
    return jdbcTemplate.query(
        """
        SELECT id, created_at_epoch_millis, replay_debug_metadata
        FROM training_runs
        WHERE replay_debug_metadata IS NOT NULL
        ORDER BY created_at_epoch_millis DESC
        LIMIT ?
        """,
        (rs, rowNum) ->
            new TrainingRunReplayDiagnosticEntity(
                rs.getLong("id"),
                rs.getLong("created_at_epoch_millis"),
                rs.getString("replay_debug_metadata")),
        Math.max(1, limit));
  }

  private double computeTimeoutRatio(long mazeId, boolean currentTimeoutReached) {
    List<Boolean> timeoutFlags = new ArrayList<>();
    timeoutFlags.add(currentTimeoutReached);
    timeoutFlags.addAll(
        jdbcTemplate.query(
            """
            SELECT timeout_reached
            FROM training_runs
            WHERE maze_id = ?
            ORDER BY created_at_epoch_millis DESC
            LIMIT 9
            """,
            (rs, rowNum) -> rs.getBoolean("timeout_reached"),
            mazeId));
    return TrainingHealthIndexFormula.timeoutRatio(timeoutFlags);
  }

  private String normalizeTerminalReason(String terminalReason, boolean success, boolean timeoutReached) {
    if (terminalReason == null || terminalReason.isBlank()) {
      if (timeoutReached) {
        return "TIMEOUT";
      }
      return success ? "EXIT_REACHED" : "ABORTED";
    }
    return switch (terminalReason.trim().toUpperCase(java.util.Locale.ROOT)) {
      case "EXIT_REACHED", "TIMEOUT", "DEAD_END", "ABORTED", "ERROR" ->
          terminalReason.trim().toUpperCase(java.util.Locale.ROOT);
      default -> timeoutReached ? "TIMEOUT" : (success ? "EXIT_REACHED" : "ABORTED");
    };
  }

  private String normalizeRewardVersion(String rewardVersion) {
    if (rewardVersion == null || rewardVersion.isBlank()) {
      return defaultRewardVersion;
    }
    return rewardVersion.trim();
  }

  private void ensureRewardVersion(String rewardVersion, long fallbackActivatedAt) {
    long activatedAt = Math.max(1L, fallbackActivatedAt);
    jdbcTemplate.update(
        """
        INSERT INTO reward_config_versions(version_id, activated_at_epoch_millis)
        SELECT ?, ?
        WHERE NOT EXISTS (
          SELECT 1 FROM reward_config_versions WHERE version_id = ?
        )
        """,
        rewardVersion,
        activatedAt,
        rewardVersion);
  }
}
