package com.davidpe.scapeai.persistence.repository;

import com.davidpe.scapeai.persistence.ExperienceTransitionEntity;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcExperienceReplayRepository implements ExperienceReplayRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcExperienceReplayRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public ExperienceTransitionEntity save(ExperienceTransitionEntity transition) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          PreparedStatement statement =
              connection.prepareStatement(
                  """
                  INSERT INTO experience_transitions(
                    state_summary, action, reward, next_state_summary, created_at_epoch_millis
                  )
                  VALUES (?, ?, ?, ?, ?)
                  """,
                  Statement.RETURN_GENERATED_KEYS);
          statement.setString(1, transition.stateSummary());
          statement.setString(2, transition.action());
          statement.setDouble(3, transition.reward());
          statement.setString(4, transition.nextStateSummary());
          statement.setLong(5, transition.createdAtEpochMillis());
          return statement;
        },
        keyHolder);
    Number id = keyHolder.getKey();
    return new ExperienceTransitionEntity(
        id.longValue(),
        transition.stateSummary(),
        transition.action(),
        transition.reward(),
        transition.nextStateSummary(),
        transition.createdAtEpochMillis());
  }

  @Override
  public List<ExperienceTransitionEntity> findRecent(int page, int pageSize) {
    int safePage = Math.max(0, page);
    int safePageSize = Math.max(1, pageSize);
    int offset = safePage * safePageSize;
    return jdbcTemplate.query(
        """
        SELECT id, state_summary, action, reward, next_state_summary, created_at_epoch_millis
        FROM experience_transitions
        ORDER BY created_at_epoch_millis DESC, id DESC
        LIMIT ? OFFSET ?
        """,
        (rs, rowNum) ->
            new ExperienceTransitionEntity(
                rs.getLong("id"),
                rs.getString("state_summary"),
                rs.getString("action"),
                rs.getDouble("reward"),
                rs.getString("next_state_summary"),
                rs.getLong("created_at_epoch_millis")),
        safePageSize,
        offset);
  }
}
