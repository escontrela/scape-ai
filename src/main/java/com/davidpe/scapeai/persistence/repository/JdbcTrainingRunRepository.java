package com.davidpe.scapeai.persistence.repository;

import com.davidpe.scapeai.application.TrainingHealthIndexFormula;
import com.davidpe.scapeai.persistence.TrainingRunEntity;
import com.davidpe.scapeai.persistence.TrainingRunReplayDiagnosticEntity;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTrainingRunRepository implements TrainingRunRepository {

  private final JdbcTemplate jdbcTemplate;
  private final MazeCoverageRepository mazeCoverageRepository;

  public JdbcTrainingRunRepository(
      JdbcTemplate jdbcTemplate, MazeCoverageRepository mazeCoverageRepository) {
    this.jdbcTemplate = jdbcTemplate;
    this.mazeCoverageRepository = mazeCoverageRepository;
  }

  @Override
  public TrainingRunEntity save(TrainingRunEntity run) {
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
                    maze_id, policy_id, policy_snapshot, success, steps, elapsed_millis, total_reward, collisions, discovered_cells, final_distance_to_exit, net_progress, maze_coverage_ratio, q1_coverage, q2_coverage, q3_coverage, q4_coverage, left_side_coverage, right_side_coverage, path_entropy, episode_debug_snapshots, replay_debug_metadata, timeout_reached, training_health_index, health_index_formula_version, created_at_epoch_millis
                  )
                  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                  """,
                  Statement.RETURN_GENERATED_KEYS);
          statement.setLong(1, run.mazeId());
          statement.setString(2, run.policyId());
          statement.setString(3, run.policySnapshot());
          statement.setBoolean(4, run.success());
          statement.setInt(5, run.steps());
          statement.setLong(6, run.elapsedMillis());
          statement.setDouble(7, run.totalReward());
          statement.setInt(8, run.collisions());
          statement.setInt(9, run.discoveredCells());
          statement.setInt(10, run.finalDistanceToExit());
          statement.setDouble(11, run.netProgress());
          statement.setDouble(12, run.mazeCoverageRatio());
          statement.setDouble(13, run.q1Coverage());
          statement.setDouble(14, run.q2Coverage());
          statement.setDouble(15, run.q3Coverage());
          statement.setDouble(16, run.q4Coverage());
          statement.setDouble(17, run.leftSideCoverage());
          statement.setDouble(18, run.rightSideCoverage());
          statement.setDouble(19, run.pathEntropy());
          statement.setString(20, run.episodeDebugSnapshots());
          statement.setString(21, run.replayDebugMetadata());
          statement.setBoolean(22, run.timeoutReached());
          statement.setDouble(23, trainingHealthIndex);
          statement.setString(24, formulaVersion);
          statement.setLong(25, run.createdAtEpochMillis());
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
        SELECT id, maze_id, policy_id, policy_snapshot, success, steps, elapsed_millis, total_reward, collisions, discovered_cells, final_distance_to_exit, net_progress, maze_coverage_ratio, q1_coverage, q2_coverage, q3_coverage, q4_coverage, left_side_coverage, right_side_coverage, path_entropy, episode_debug_snapshots, replay_debug_metadata, timeout_reached, training_health_index, health_index_formula_version, created_at_epoch_millis
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
                rs.getBoolean("timeout_reached"),
                rs.getDouble("training_health_index"),
                rs.getString("health_index_formula_version"),
                rs.getLong("created_at_epoch_millis")),
        mazeId,
        limit);
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
}
