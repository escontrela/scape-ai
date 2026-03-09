package com.davidpe.scapeai.ui;

import com.davidpe.scapeai.application.TrainingTargetDifficulty;
import com.davidpe.scapeai.persistence.MazeEntity;
import com.davidpe.scapeai.persistence.repository.MazeRepository;
import com.davidpe.scapeai.simulation.GridPosition;
import com.davidpe.scapeai.simulation.MazeDefinition;
import com.davidpe.scapeai.simulation.MazeDifficultyScorer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

@DependsOn("sqliteSchemaMigrator")
@Component
public class MazeCatalogService {

  private final Map<String, MazeEntry> mazes;
  private final List<String> loadErrors;
  private final MazeRepository mazeRepository;
  private final MazeDifficultyScorer difficultyScorer;

  public MazeCatalogService(
      MazeJsonResourceLoader jsonResourceLoader,
      MazeRepository mazeRepository,
      MazeDifficultyScorer difficultyScorer,
      @Value("${scape.ui.maze-catalog-pattern:classpath:mazes/*.json}") String mazeCatalogPattern) {
    var loaded = jsonResourceLoader.load(mazeCatalogPattern);
    this.mazeRepository = mazeRepository;
    this.difficultyScorer = difficultyScorer;
    this.mazes = createCatalog(loaded.mazes(), mazeRepository, difficultyScorer);
    this.loadErrors = loaded.errors();
  }

  public Set<String> names() {
    return mazes.keySet();
  }

  public List<String> namesByDifficulty(boolean ascending) {
    return mazeRepository.findAllOrderByDifficulty(ascending).stream()
        .map(MazeEntity::name)
        .toList();
  }

  public Optional<MazeDefinition> findCandidateByDifficulty(TrainingTargetDifficulty target) {
    if (target == null) {
      return Optional.empty();
    }
    List<MazeEntity> ordered = mazeRepository.findAllOrderByDifficulty(true);
    if (ordered.isEmpty()) {
      return Optional.empty();
    }
    int segmentSize = (int) Math.ceil(ordered.size() / 3.0);
    int fromIndex;
    int toIndex;
    switch (target) {
      case LOW -> {
        fromIndex = 0;
        toIndex = Math.min(ordered.size(), segmentSize);
      }
      case MEDIUM -> {
        fromIndex = Math.min(ordered.size(), segmentSize);
        toIndex = Math.min(ordered.size(), segmentSize * 2);
      }
      case HIGH -> {
        fromIndex = Math.min(ordered.size(), segmentSize * 2);
        toIndex = ordered.size();
      }
      default -> {
        return Optional.empty();
      }
    }
    if (fromIndex >= toIndex) {
      return Optional.empty();
    }
    MazeEntity candidate = ordered.get(fromIndex);
    MazeEntry entry = mazes.get(candidate.name());
    return entry == null ? Optional.empty() : Optional.of(entry.maze());
  }

  public MazeDefinition byName(String name) {
    MazeEntry entry = mazes.get(name);
    return entry == null ? null : entry.maze();
  }

  public double difficultyScore(String name) {
    MazeEntry entry = mazes.get(name);
    return entry == null ? 0.0 : entry.difficultyScore();
  }

  public List<String> loadErrors() {
    return loadErrors;
  }

  private static Map<String, MazeEntry> createCatalog(
      Map<String, MazeDefinition> loadedMazes,
      MazeRepository mazeRepository,
      MazeDifficultyScorer difficultyScorer) {
    Map<String, MazeDefinition> catalog = new LinkedHashMap<>();
    catalog.putAll(loadedMazes);
    if (catalog.isEmpty()) {
      catalog.put("Neon Gate", buildNeonGateMaze());
      catalog.put("Circuit Hall", buildCircuitHallMaze());
    }
    Map<String, MazeEntry> entries = new LinkedHashMap<>();
    for (Map.Entry<String, MazeDefinition> mazeEntry : catalog.entrySet()) {
      String name = mazeEntry.getKey();
      MazeDefinition maze = mazeEntry.getValue();
      double score = difficultyScorer.score(maze);
      MazeEntity persisted =
          mazeRepository.upsertByName(
              new MazeEntity(null, name, maze.rows(), maze.cols(), encodeLayout(maze), score));
      entries.put(name, new MazeEntry(maze, persisted.difficultyScore()));
    }
    return Map.copyOf(entries);
  }

  private static String encodeLayout(MazeDefinition maze) {
    StringBuilder builder = new StringBuilder();
    for (int row = 0; row < maze.rows(); row++) {
      for (int col = 0; col < maze.cols(); col++) {
        builder.append(maze.isWall(new GridPosition(row, col)) ? '#' : '.');
      }
      if (row < maze.rows() - 1) {
        builder.append('\n');
      }
    }
    return builder.toString();
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

  private record MazeEntry(MazeDefinition maze, double difficultyScore) {}
}
