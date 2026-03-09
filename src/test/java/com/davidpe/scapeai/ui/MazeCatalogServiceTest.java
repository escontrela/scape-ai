package com.davidpe.scapeai.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.davidpe.scapeai.persistence.MazeEntity;
import com.davidpe.scapeai.persistence.repository.MazeRepository;
import com.davidpe.scapeai.simulation.GridPosition;
import com.davidpe.scapeai.simulation.MazeDifficultyScorer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MazeCatalogServiceTest {

  @Test
  void shouldProvideMazeDefinitionsWithStartAndExit() {
    MazeCatalogService catalog =
        new MazeCatalogService(
            new MazeJsonResourceLoader(new ObjectMapper()),
            new InMemoryMazeRepository(),
            new MazeDifficultyScorer(),
            "classpath:mazes/*.json");

    assertFalse(catalog.names().isEmpty());

    var maze = catalog.byName("Neon Gate");
    assertNotNull(maze);
    assertTrue(maze.isInside(maze.start()));
    assertTrue(maze.isInside(maze.exit()));
    assertFalse(maze.isWall(new GridPosition(maze.start().row(), maze.start().col())));
  }

  @Test
  void shouldReportInvalidJsonFilesWithoutStoppingCatalogLoad() throws Exception {
    Path tempDir = Files.createTempDirectory("maze-catalog-test");
    Path valid = tempDir.resolve("valid.json");
    Path invalid = tempDir.resolve("invalid.json");
    Files.writeString(
        valid,
        """
        {
          "name":"Temp Maze",
          "rows":2,
          "cols":2,
          "start":{"row":0,"col":0},
          "exit":{"row":1,"col":1},
          "walls":[[false,false],[false,false]]
        }
        """);
    Files.writeString(
        invalid,
        """
        {
          "name":"Broken Maze",
          "rows":2,
          "cols":2,
          "start":{"row":0,"col":0},
          "exit":{"row":3,"col":1},
          "walls":[[false,false],[false,false]]
        }
        """);

    MazeCatalogService catalog =
        new MazeCatalogService(
            new MazeJsonResourceLoader(new ObjectMapper()),
            new InMemoryMazeRepository(),
            new MazeDifficultyScorer(),
            "file:" + tempDir.toAbsolutePath() + "/*.json");

    assertNotNull(catalog.byName("Temp Maze"));
    assertFalse(catalog.loadErrors().isEmpty());
    assertTrue(catalog.loadErrors().get(0).contains("Invalid maze file"));
  }

  private static final class InMemoryMazeRepository implements MazeRepository {

    private final List<MazeEntity> stored = new ArrayList<>();
    private long sequence = 1L;

    @Override
    public MazeEntity save(MazeEntity maze) {
      MazeEntity persisted =
          new MazeEntity(sequence++, maze.name(), maze.rows(), maze.cols(), maze.layout(), maze.difficultyScore());
      stored.add(persisted);
      return persisted;
    }

    @Override
    public MazeEntity upsertByName(MazeEntity maze) {
      for (int i = 0; i < stored.size(); i++) {
        MazeEntity existing = stored.get(i);
        if (existing.name().equals(maze.name())) {
          MazeEntity updated =
              new MazeEntity(
                  existing.id(), maze.name(), maze.rows(), maze.cols(), maze.layout(), maze.difficultyScore());
          stored.set(i, updated);
          return updated;
        }
      }
      return save(maze);
    }

    @Override
    public Optional<MazeEntity> findById(long id) {
      return stored.stream().filter(maze -> maze.id() == id).findFirst();
    }

    @Override
    public List<MazeEntity> findAllOrderByDifficulty(boolean ascending) {
      Comparator<MazeEntity> comparator = Comparator.comparingDouble(MazeEntity::difficultyScore);
      if (!ascending) {
        comparator = comparator.reversed();
      }
      return stored.stream().sorted(comparator).toList();
    }
  }
}
