package com.davidpe.scapeai.ui;

import com.davidpe.scapeai.simulation.GridPosition;
import com.davidpe.scapeai.simulation.MazeDefinition;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class MazeCatalogService {

  private final Map<String, MazeDefinition> mazes;

  public MazeCatalogService() {
    this.mazes = createCatalog();
  }

  public Set<String> names() {
    return mazes.keySet();
  }

  public MazeDefinition byName(String name) {
    return mazes.get(name);
  }

  private static Map<String, MazeDefinition> createCatalog() {
    Map<String, MazeDefinition> catalog = new LinkedHashMap<>();
    catalog.put("Neon Gate", buildNeonGateMaze());
    catalog.put("Circuit Hall", buildCircuitHallMaze());
    return Map.copyOf(catalog);
  }

  private static MazeDefinition buildNeonGateMaze() {
    boolean[][] walls = new boolean[10][14];
    fillRect(walls, 2, 1, 2, 10);
    fillRect(walls, 5, 3, 5, 13);
    fillRect(walls, 1, 9, 7, 9);
    fillRect(walls, 7, 4, 8, 4);
    fillRect(walls, 3, 12, 6, 12);
    clearCell(walls, 2, 6);
    clearCell(walls, 5, 7);
    clearCell(walls, 7, 9);
    return new MazeDefinition(10, 14, walls, new GridPosition(0, 0), new GridPosition(9, 13));
  }

  private static MazeDefinition buildCircuitHallMaze() {
    boolean[][] walls = new boolean[12][16];
    fillRect(walls, 1, 2, 10, 2);
    fillRect(walls, 1, 6, 10, 6);
    fillRect(walls, 1, 10, 10, 10);
    fillRect(walls, 1, 14, 10, 14);
    clearCell(walls, 2, 2);
    clearCell(walls, 4, 6);
    clearCell(walls, 6, 10);
    clearCell(walls, 8, 14);
    clearCell(walls, 10, 6);
    return new MazeDefinition(12, 16, walls, new GridPosition(0, 1), new GridPosition(11, 15));
  }

  private static void fillRect(
      boolean[][] walls, int startRow, int startCol, int endRowInclusive, int endColInclusive) {
    for (int row = startRow; row <= endRowInclusive; row++) {
      for (int col = startCol; col <= endColInclusive; col++) {
        walls[row][col] = true;
      }
    }
  }

  private static void clearCell(boolean[][] walls, int row, int col) {
    walls[row][col] = false;
  }
}
