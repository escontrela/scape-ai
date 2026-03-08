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

  public JdbcTrainingRunRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
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
                    maze_id, success, steps, elapsed_millis, total_reward, collisions, discovered_cells, final_distance_to_exit, created_at_epoch_millis
                  )
                  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                  """,
                  Statement.RETURN_GENERATED_KEYS);
          statement.setLong(1, run.mazeId());
          statement.setBoolean(2, run.success());
          statement.setInt(3, run.steps());
          statement.setLong(4, run.elapsedMillis());
          statement.setDouble(5, run.totalReward());
          statement.setInt(6, run.collisions());
          statement.setInt(7, run.discoveredCells());
          statement.setInt(8, run.finalDistanceToExit());
          statement.setLong(9, run.createdAtEpochMillis());
          return statement;
        },
        keyHolder);

    Number key = keyHolder.getKey();
    return new TrainingRunEntity(
        key.longValue(),
        run.mazeId(),
        run.success(),
        run.steps(),
        run.elapsedMillis(),
        run.totalReward(),
        run.collisions(),
        run.discoveredCells(),
        run.finalDistanceToExit(),
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
        SELECT id, maze_id, success, steps, elapsed_millis, total_reward, collisions, discovered_cells, final_distance_to_exit, created_at_epoch_millis
        FROM training_runs
        WHERE maze_id = ?
        ORDER BY created_at_epoch_millis DESC
        LIMIT ?
        """,
        (rs, rowNum) ->
            new TrainingRunEntity(
                rs.getLong("id"),
                rs.getLong("maze_id"),
                rs.getBoolean("success"),
                rs.getInt("steps"),
                rs.getLong("elapsed_millis"),
                rs.getDouble("total_reward"),
                rs.getInt("collisions"),
                rs.getInt("discovered_cells"),
                rs.getInt("final_distance_to_exit"),
                rs.getLong("created_at_epoch_millis")),
        mazeId,
        limit);
  }
}
