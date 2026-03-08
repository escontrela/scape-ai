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
                  INSERT INTO training_runs(maze_id, success, steps, elapsed_millis, total_reward, created_at_epoch_millis)
                  VALUES (?, ?, ?, ?, ?, ?)
                  """,
                  Statement.RETURN_GENERATED_KEYS);
          statement.setLong(1, run.mazeId());
          statement.setBoolean(2, run.success());
          statement.setInt(3, run.steps());
          statement.setLong(4, run.elapsedMillis());
          statement.setDouble(5, run.totalReward());
          statement.setLong(6, run.createdAtEpochMillis());
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
        run.createdAtEpochMillis());
  }

  @Override
  public List<TrainingRunEntity> findByMazeId(long mazeId) {
    return jdbcTemplate.query(
        """
        SELECT id, maze_id, success, steps, elapsed_millis, total_reward, created_at_epoch_millis
        FROM training_runs
        WHERE maze_id = ?
        ORDER BY created_at_epoch_millis DESC
        """,
        (rs, rowNum) ->
            new TrainingRunEntity(
                rs.getLong("id"),
                rs.getLong("maze_id"),
                rs.getBoolean("success"),
                rs.getInt("steps"),
                rs.getLong("elapsed_millis"),
                rs.getDouble("total_reward"),
                rs.getLong("created_at_epoch_millis")),
        mazeId);
  }
}
