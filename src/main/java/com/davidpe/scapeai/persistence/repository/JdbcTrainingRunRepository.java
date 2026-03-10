package com.davidpe.scapeai.persistence.repository;

import com.davidpe.scapeai.persistence.TrainingRunEntity;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
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
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          PreparedStatement statement =
              connection.prepareStatement(
                  """
                  INSERT INTO training_runs(
                    maze_id, policy_id, policy_snapshot, success, steps, elapsed_millis, total_reward, collisions, discovered_cells, final_distance_to_exit, net_progress, maze_coverage_ratio, q1_coverage, q2_coverage, q3_coverage, q4_coverage, left_side_coverage, right_side_coverage, path_entropy, created_at_epoch_millis
                  )
                  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
          statement.setLong(20, run.createdAtEpochMillis());
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
        SELECT id, maze_id, policy_id, policy_snapshot, success, steps, elapsed_millis, total_reward, collisions, discovered_cells, final_distance_to_exit, net_progress, maze_coverage_ratio, q1_coverage, q2_coverage, q3_coverage, q4_coverage, left_side_coverage, right_side_coverage, path_entropy, created_at_epoch_millis
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
                rs.getLong("created_at_epoch_millis")),
        mazeId,
        limit);
  }
}
