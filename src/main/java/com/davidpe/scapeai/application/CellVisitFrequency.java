package com.davidpe.scapeai.application;

import com.davidpe.scapeai.simulation.GridPosition;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public record CellVisitFrequency(GridPosition position, int visits) {

  public CellVisitFrequency {
    position = java.util.Objects.requireNonNull(position, "position must not be null");
    visits = Math.max(0, visits);
  }

  public static String encode(List<CellVisitFrequency> frequencies) {
    if (frequencies == null || frequencies.isEmpty()) {
      return "";
    }
    StringBuilder encoded = new StringBuilder();
    List<CellVisitFrequency> sorted =
        frequencies.stream()
            .filter(frequency -> frequency != null && frequency.visits() > 0)
            .sorted(
                Comparator.comparingInt((CellVisitFrequency frequency) -> frequency.position().row())
                    .thenComparingInt(frequency -> frequency.position().col()))
            .toList();
    for (CellVisitFrequency frequency : sorted) {
      if (!encoded.isEmpty()) {
        encoded.append(';');
      }
      encoded
          .append(frequency.position().row())
          .append(':')
          .append(frequency.position().col())
          .append(':')
          .append(frequency.visits());
    }
    return encoded.toString();
  }

  public static List<CellVisitFrequency> decode(String encoded) {
    if (encoded == null || encoded.isBlank()) {
      return List.of();
    }
    List<CellVisitFrequency> decoded = new ArrayList<>();
    String[] cells = encoded.split(";");
    for (String cell : cells) {
      if (cell == null || cell.isBlank()) {
        continue;
      }
      String[] parts = cell.split(":");
      if (parts.length != 3) {
        continue;
      }
      try {
        int row = Integer.parseInt(parts[0].trim());
        int col = Integer.parseInt(parts[1].trim());
        int visits = Integer.parseInt(parts[2].trim());
        if (visits <= 0) {
          continue;
        }
        decoded.add(new CellVisitFrequency(new GridPosition(row, col), visits));
      } catch (NumberFormatException ignored) {
        // Skip malformed fragments so historical rows do not break reads.
      }
    }
    return List.copyOf(decoded);
  }
}
