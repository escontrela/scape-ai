package com.davidpe.scapeai.persistence.repository;

import com.davidpe.scapeai.persistence.MazeCoverageSummaryEntity;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcMazeCoverageRepository implements MazeCoverageRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcMazeCoverageRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void upsertCoverage(long mazeId, String policyId, boolean solved, long updatedAtEpochMillis) {
    jdbcTemplate.update(
        """
        INSERT INTO maze_policy_coverage(maze_id, policy_id, solved, updated_at_epoch_millis)
        VALUES (?, ?, ?, ?)
        ON CONFLICT(maze_id, policy_id)
        DO UPDATE SET
          solved = CASE WHEN excluded.solved = 1 THEN 1 ELSE maze_policy_coverage.solved END,
          updated_at_epoch_millis = excluded.updated_at_epoch_millis
        """,
        mazeId,
        policyId == null ? "unknown" : policyId,
        solved ? 1 : 0,
        updatedAtEpochMillis);
  }

  @Override
  public List<MazeCoverageSummaryEntity> findPendingCoverageSummary() {
    return jdbcTemplate.query(
        """
        SELECT m.name AS maze_name, COUNT(*) AS pending_policies
        FROM maze_policy_coverage coverage
        JOIN mazes m ON m.id = coverage.maze_id
        WHERE coverage.solved = 0
        GROUP BY m.id, m.name
        ORDER BY pending_policies DESC, m.name ASC
        """,
        (rs, rowNum) ->
            new MazeCoverageSummaryEntity(rs.getString("maze_name"), rs.getLong("pending_policies")));
  }
}
