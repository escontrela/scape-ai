package com.davidpe.scapeai.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.davidpe.scapeai.simulation.GridPosition;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MazeCatalogServiceTest {

  @Test
  void shouldProvideMazeDefinitionsWithStartAndExit() {
    MazeCatalogService catalog =
        new MazeCatalogService(
            new MazeJsonResourceLoader(new ObjectMapper()), "classpath:mazes/*.json");

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
            "file:" + tempDir.toAbsolutePath() + "/*.json");

    assertNotNull(catalog.byName("Temp Maze"));
    assertFalse(catalog.loadErrors().isEmpty());
    assertTrue(catalog.loadErrors().get(0).contains("Invalid maze file"));
  }
}
