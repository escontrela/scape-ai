package com.davidpe.scapeai.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.scapeai.simulation.GridPosition;
import org.junit.jupiter.api.Test;

class MazeCatalogServiceTest {

  @Test
  void shouldProvideMazeDefinitionsWithStartAndExit() {
    MazeCatalogService catalog = new MazeCatalogService();

    assertFalse(catalog.names().isEmpty());

    var maze = catalog.byName("Neon Gate");
    assertNotNull(maze);
    assertTrue(maze.isInside(maze.start()));
    assertTrue(maze.isInside(maze.exit()));
    assertFalse(maze.isWall(new GridPosition(maze.start().row(), maze.start().col())));
  }
}
