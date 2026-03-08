package com.davidpe.scapeai.persistence.repository;

import com.davidpe.scapeai.persistence.MazeEntity;
import java.sql.PreparedStatement;
import java.sql.Statement;
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
                  INSERT INTO mazes(name, rows_count, cols_count, layout)
                  VALUES (?, ?, ?, ?)
                  """,
                  Statement.RETURN_GENERATED_KEYS);
          statement.setString(1, maze.name());
          statement.setInt(2, maze.rows());
          statement.setInt(3, maze.cols());
          statement.setString(4, maze.layout());
          return statement;
        },
        keyHolder);

    Number key = keyHolder.getKey();
    return new MazeEntity(key.longValue(), maze.name(), maze.rows(), maze.cols(), maze.layout());
  }

  @Override
  public Optional<MazeEntity> findById(long id) {
    return jdbcTemplate
        .query(
            """
            SELECT id, name, rows_count, cols_count, layout
            FROM mazes
            WHERE id = ?
            """,
            (rs, rowNum) ->
                new MazeEntity(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getInt("rows_count"),
                    rs.getInt("cols_count"),
                    rs.getString("layout")),
            id)
        .stream()
        .findFirst();
  }
}
