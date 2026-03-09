package com.davidpe.scapeai.persistence.repository;

import com.davidpe.scapeai.persistence.TrainingPresetEntity;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTrainingPresetRepository implements TrainingPresetRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcTrainingPresetRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public TrainingPresetEntity save(TrainingPresetEntity preset) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          PreparedStatement statement =
              connection.prepareStatement(
                  """
                  INSERT INTO training_presets(episodes, timeout_millis, policy, seed)
                  VALUES (?, ?, ?, ?)
                  """,
                  Statement.RETURN_GENERATED_KEYS);
          statement.setInt(1, preset.episodes());
          statement.setLong(2, preset.timeoutMillis());
          statement.setString(3, preset.policy());
          if (preset.seed() == null) {
            statement.setNull(4, java.sql.Types.BIGINT);
          } else {
            statement.setLong(4, preset.seed());
          }
          return statement;
        },
        keyHolder);
    Number key = keyHolder.getKey();
    return new TrainingPresetEntity(
        key.longValue(), preset.episodes(), preset.timeoutMillis(), preset.policy(), preset.seed());
  }

  @Override
  public List<TrainingPresetEntity> findAll() {
    return jdbcTemplate.query(
        """
        SELECT id, episodes, timeout_millis, policy, seed
        FROM training_presets
        ORDER BY id ASC
        """,
        (rs, rowNum) ->
            new TrainingPresetEntity(
                rs.getLong("id"),
                rs.getInt("episodes"),
                rs.getLong("timeout_millis"),
                rs.getString("policy"),
                rs.getObject("seed") == null ? null : rs.getLong("seed")));
  }

  @Override
  public Optional<TrainingPresetEntity> findById(long id) {
    return jdbcTemplate
        .query(
            """
            SELECT id, episodes, timeout_millis, policy, seed
            FROM training_presets
            WHERE id = ?
            """,
            (rs, rowNum) ->
                new TrainingPresetEntity(
                    rs.getLong("id"),
                    rs.getInt("episodes"),
                    rs.getLong("timeout_millis"),
                    rs.getString("policy"),
                    rs.getObject("seed") == null ? null : rs.getLong("seed")),
            id)
        .stream()
        .findFirst();
  }
}
