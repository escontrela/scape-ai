package com.davidpe.scapeai.persistence.repository;

import com.davidpe.scapeai.persistence.TrainingSessionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTrainingSessionRepository implements TrainingSessionRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcTrainingSessionRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public TrainingSessionEntity save(TrainingSessionEntity session) {
    jdbcTemplate.update(
        """
        INSERT INTO training_sessions(
          id, maze_ref, policy_id, preset_id, effective_seed, started_at_epoch_millis, ended_at_epoch_millis
        )
        VALUES (?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(id) DO UPDATE SET
          maze_ref = excluded.maze_ref,
          policy_id = excluded.policy_id,
          preset_id = excluded.preset_id,
          effective_seed = excluded.effective_seed,
          started_at_epoch_millis = excluded.started_at_epoch_millis,
          ended_at_epoch_millis = CASE
            WHEN training_sessions.ended_at_epoch_millis IS NULL THEN excluded.ended_at_epoch_millis
            WHEN excluded.ended_at_epoch_millis IS NULL THEN training_sessions.ended_at_epoch_millis
            ELSE MAX(training_sessions.ended_at_epoch_millis, excluded.ended_at_epoch_millis)
          END
        """,
        session.id(),
        session.mazeRef(),
        session.policyId(),
        session.presetId(),
        session.effectiveSeed(),
        session.startedAtEpochMillis(),
        session.endedAtEpochMillis());
    return session;
  }

  @Override
  public Optional<TrainingSessionEntity> findById(String sessionId) {
    return jdbcTemplate
        .query(
            """
            SELECT id, maze_ref, policy_id, preset_id, effective_seed, started_at_epoch_millis, ended_at_epoch_millis
            FROM training_sessions
            WHERE id = ?
            """,
            (rs, rowNum) ->
                new TrainingSessionEntity(
                    rs.getString("id"),
                    rs.getString("maze_ref"),
                    rs.getString("policy_id"),
                    nullableLong(rs.getObject("preset_id")),
                    rs.getLong("effective_seed"),
                    rs.getLong("started_at_epoch_millis"),
                    nullableLong(rs.getObject("ended_at_epoch_millis"))),
            sessionId)
        .stream()
        .findFirst();
  }

  @Override
  public List<TrainingSessionEntity> findRecent(int limit) {
    return jdbcTemplate.query(
        """
        SELECT id, maze_ref, policy_id, preset_id, effective_seed, started_at_epoch_millis, ended_at_epoch_millis
        FROM training_sessions
        ORDER BY started_at_epoch_millis DESC
        LIMIT ?
        """,
        (rs, rowNum) ->
            new TrainingSessionEntity(
                rs.getString("id"),
                rs.getString("maze_ref"),
                rs.getString("policy_id"),
                nullableLong(rs.getObject("preset_id")),
                rs.getLong("effective_seed"),
                rs.getLong("started_at_epoch_millis"),
                nullableLong(rs.getObject("ended_at_epoch_millis"))),
        Math.max(1, limit));
  }

  @Override
  public void markEnded(String sessionId, long endedAtEpochMillis) {
    jdbcTemplate.update(
        """
        UPDATE training_sessions
        SET ended_at_epoch_millis = CASE
          WHEN ended_at_epoch_millis IS NULL THEN ?
          ELSE MAX(ended_at_epoch_millis, ?)
        END
        WHERE id = ?
        """,
        endedAtEpochMillis,
        endedAtEpochMillis,
        sessionId);
  }

  private static Long nullableLong(Object value) {
    return value instanceof Number number ? number.longValue() : null;
  }
}
