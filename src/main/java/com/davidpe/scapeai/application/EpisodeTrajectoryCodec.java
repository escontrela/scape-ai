package com.davidpe.scapeai.application;

import com.davidpe.scapeai.simulation.GridPosition;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class EpisodeTrajectoryCodec {

  private EpisodeTrajectoryCodec() {}

  public static String encode(List<GridPosition> trajectory) {
    if (trajectory == null || trajectory.isEmpty()) {
      return "";
    }
    StringBuilder encoded = new StringBuilder();
    GridPosition start = trajectory.get(0);
    appendAbsolute(encoded, start);
    GridPosition previous = start;
    for (int i = 1; i < trajectory.size(); i++) {
      GridPosition current = trajectory.get(i);
      int dRow = current.row() - previous.row();
      int dCol = current.col() - previous.col();
      if (dRow == -1 && dCol == 0) {
        encoded.append('U');
      } else if (dRow == 1 && dCol == 0) {
        encoded.append('D');
      } else if (dRow == 0 && dCol == -1) {
        encoded.append('L');
      } else if (dRow == 0 && dCol == 1) {
        encoded.append('R');
      } else if (dRow == 0 && dCol == 0) {
        encoded.append('N');
      } else {
        encoded.append('|');
        appendAbsolute(encoded, current);
      }
      previous = current;
    }
    return encoded.toString();
  }

  public static List<GridPosition> decode(String encoded) {
    if (encoded == null || encoded.isBlank()) {
      return List.of();
    }
    int separator = encoded.indexOf(',');
    if (separator <= 0) {
      return List.of();
    }
    int cursor = separator + 1;
    while (cursor < encoded.length()) {
      char marker = encoded.charAt(cursor);
      if (marker == 'U' || marker == 'D' || marker == 'L' || marker == 'R' || marker == 'N' || marker == '|') {
        break;
      }
      cursor++;
    }
    GridPosition start = parseAbsolute(encoded.substring(0, cursor));
    List<GridPosition> trajectory = new ArrayList<>();
    trajectory.add(start);
    GridPosition previous = start;
    int index = cursor;
    while (index < encoded.length()) {
      char token = encoded.charAt(index);
      switch (token) {
        case 'U' -> previous = new GridPosition(previous.row() - 1, previous.col());
        case 'D' -> previous = new GridPosition(previous.row() + 1, previous.col());
        case 'L' -> previous = new GridPosition(previous.row(), previous.col() - 1);
        case 'R' -> previous = new GridPosition(previous.row(), previous.col() + 1);
        case 'N' -> previous = new GridPosition(previous.row(), previous.col());
        case '|' -> {
          int end = index + 1;
          while (end < encoded.length()) {
            char next = encoded.charAt(end);
            if (next == 'U' || next == 'D' || next == 'L' || next == 'R' || next == 'N' || next == '|') {
              break;
            }
            end++;
          }
          previous = parseAbsolute(encoded.substring(index + 1, end));
          index = end - 1;
        }
        default -> {
          index++;
          continue;
        }
      }
      trajectory.add(previous);
      index++;
    }
    return List.copyOf(trajectory);
  }

  private static void appendAbsolute(StringBuilder output, GridPosition position) {
    output.append(Integer.toString(position.row(), 36).toUpperCase(Locale.ROOT));
    output.append(',');
    output.append(Integer.toString(position.col(), 36).toUpperCase(Locale.ROOT));
  }

  private static GridPosition parseAbsolute(String token) {
    String[] parts = token.split(",", 2);
    if (parts.length != 2) {
      return new GridPosition(0, 0);
    }
    try {
      int row = Integer.parseInt(parts[0].trim(), 36);
      int col = Integer.parseInt(parts[1].trim(), 36);
      return new GridPosition(row, col);
    } catch (NumberFormatException ignored) {
      return new GridPosition(0, 0);
    }
  }
}
