package com.davidpe.scapeai.persistence.repository;

import com.davidpe.scapeai.persistence.MazeEntity;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcMazeRepository implements MazeRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcMazeRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public MazeEntity save(MazeEntity maze) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          PreparedStatement statement =
              connection.prepareStatement(
                  """
                  INSERT INTO mazes(name, rows_count, cols_count, layout, difficulty_score)
                  VALUES (?, ?, ?, ?, ?)
                  """,
                  Statement.RETURN_GENERATED_KEYS);
          statement.setString(1, maze.name());
          statement.setInt(2, maze.rows());
          statement.setInt(3, maze.cols());
          statement.setString(4, maze.layout());
          statement.setDouble(5, maze.difficultyScore());
          return statement;
        },
        keyHolder);

    Number key = keyHolder.getKey();
    return new MazeEntity(
        key.longValue(),
        maze.name(),
        maze.rows(),
        maze.cols(),
        maze.layout(),
        maze.difficultyScore());
  }

  @Override
  public MazeEntity upsertByName(MazeEntity maze) {
    jdbcTemplate.update(
        """
        INSERT INTO mazes(name, rows_count, cols_count, layout, difficulty_score)
        VALUES (?, ?, ?, ?, ?)
        ON CONFLICT(name) DO UPDATE SET
          rows_count = excluded.rows_count,
          cols_count = excluded.cols_count,
          layout = excluded.layout,
          difficulty_score = excluded.difficulty_score
        """,
        maze.name(),
        maze.rows(),
        maze.cols(),
        maze.layout(),
        maze.difficultyScore());
    return jdbcTemplate
        .query(
            """
            SELECT id, name, rows_count, cols_count, layout, difficulty_score
            FROM mazes
            WHERE name = ?
            """,
            (rs, rowNum) ->
                new MazeEntity(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getInt("rows_count"),
                    rs.getInt("cols_count"),
                    rs.getString("layout"),
                    rs.getDouble("difficulty_score")),
            maze.name())
        .stream()
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Could not upsert maze: " + maze.name()));
  }

  @Override
  public Optional<MazeEntity> findById(long id) {
    return jdbcTemplate
        .query(
            """
            SELECT id, name, rows_count, cols_count, layout, difficulty_score
            FROM mazes
            WHERE id = ?
            """,
            (rs, rowNum) ->
                new MazeEntity(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getInt("rows_count"),
                    rs.getInt("cols_count"),
                    rs.getString("layout"),
                    rs.getDouble("difficulty_score")),
            id)
        .stream()
        .findFirst();
  }

  @Override
  public List<MazeEntity> findAllOrderByDifficulty(boolean ascending) {
    String direction = ascending ? "ASC" : "DESC";
    String query =
        "SELECT id, name, rows_count, cols_count, layout, difficulty_score "
            + "FROM mazes "
            + "ORDER BY difficulty_score "
            + direction
            + ", name ASC";
    return jdbcTemplate.query(
        query,
        (rs, rowNum) ->
            new MazeEntity(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getInt("rows_count"),
                rs.getInt("cols_count"),
                rs.getString("layout"),
                rs.getDouble("difficulty_score")));
  }
}
