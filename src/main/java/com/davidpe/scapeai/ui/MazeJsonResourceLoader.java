package com.davidpe.scapeai.ui;

import com.davidpe.scapeai.simulation.GridPosition;
import com.davidpe.scapeai.simulation.MazeDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
public class MazeJsonResourceLoader {

  private final ObjectMapper objectMapper;
  private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

  public MazeJsonResourceLoader(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public MazeCatalogLoadResult load(String pattern) {
    Map<String, MazeDefinition> mazes = new LinkedHashMap<>();
    java.util.ArrayList<String> errors = new java.util.ArrayList<>();
    try {
      Resource[] resources = resolver.getResources(pattern);
      for (Resource resource : resources) {
        parseResource(resource, mazes, errors);
      }
    } catch (IOException ioException) {
      errors.add("Maze catalog pattern error (" + pattern + "): " + ioException.getMessage());
    }
    return new MazeCatalogLoadResult(Map.copyOf(mazes), List.copyOf(errors));
  }

  private void parseResource(Resource resource, Map<String, MazeDefinition> mazes, List<String> errors) {
    String source = resource.getFilename() == null ? resource.getDescription() : resource.getFilename();
    try (InputStream inputStream = resource.getInputStream()) {
      MazeJsonSpec spec = objectMapper.readValue(inputStream, MazeJsonSpec.class);
      validateSpec(spec, source);
      MazeDefinition maze =
          new MazeDefinition(
              spec.rows(),
              spec.cols(),
              spec.walls(),
              new GridPosition(spec.start().row(), spec.start().col()),
              new GridPosition(spec.exit().row(), spec.exit().col()));
      mazes.put(spec.name(), maze);
    } catch (Exception exception) {
      errors.add("Invalid maze file " + source + ": " + exception.getMessage());
    }
  }

  private void validateSpec(MazeJsonSpec spec, String source) {
    if (spec == null) {
      throw new IllegalArgumentException("empty JSON payload");
    }
    if (spec.name() == null || spec.name().isBlank()) {
      throw new IllegalArgumentException("missing maze name");
    }
    if (spec.rows() <= 0 || spec.cols() <= 0) {
      throw new IllegalArgumentException("rows and cols must be positive");
    }
    if (spec.walls() == null || spec.walls().length != spec.rows()) {
      throw new IllegalArgumentException("walls row count must match rows");
    }
    for (int row = 0; row < spec.rows(); row++) {
      if (spec.walls()[row] == null || spec.walls()[row].length != spec.cols()) {
        throw new IllegalArgumentException("walls column count mismatch at row " + row);
      }
    }
    requireInside(spec.start(), spec.rows(), spec.cols(), "start");
    requireInside(spec.exit(), spec.rows(), spec.cols(), "exit");
    if (spec.walls()[spec.start().row()][spec.start().col()]) {
      throw new IllegalArgumentException("start cannot be a wall");
    }
    if (spec.walls()[spec.exit().row()][spec.exit().col()]) {
      throw new IllegalArgumentException("exit cannot be a wall");
    }
  }

  private void requireInside(PositionSpec position, int rows, int cols, String label) {
    if (position == null) {
      throw new IllegalArgumentException(label + " is required");
    }
    if (position.row() < 0 || position.row() >= rows || position.col() < 0 || position.col() >= cols) {
      throw new IllegalArgumentException(label + " must be inside maze dimensions");
    }
  }

  public record MazeCatalogLoadResult(Map<String, MazeDefinition> mazes, List<String> errors) {}

  private record MazeJsonSpec(
      String name, int rows, int cols, PositionSpec start, PositionSpec exit, boolean[][] walls) {}

  private record PositionSpec(int row, int col) {}
}
