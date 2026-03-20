package com.davidpe.scapeai.application;

import com.davidpe.scapeai.persistence.repository.TrainingRunRepository;
import com.davidpe.scapeai.simulation.GridPosition;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class TrainingEpisodeDetailService {

  private final TrainingRunRepository trainingRunRepository;

  public TrainingEpisodeDetailService(TrainingRunRepository trainingRunRepository) {
    this.trainingRunRepository = trainingRunRepository;
  }

  public Optional<TrainingEpisodeDetail> findByTrainingRunId(long trainingRunId) {
    return trainingRunRepository
        .findById(trainingRunId)
        .map(
            run -> {
              List<GridPosition> trajectory = EpisodeTrajectoryCodec.decode(run.trajectoryPath());
              GridPosition finalPosition =
                  trajectory.isEmpty() ? parseFinalPosition(run.replayDebugMetadata()) : trajectory.get(trajectory.size() - 1);
              return new TrainingEpisodeDetail(
                  run.id(), trajectory, finalPosition, run.replayDebugMetadata());
            });
  }

  private GridPosition parseFinalPosition(String replayMetadata) {
    if (replayMetadata == null || replayMetadata.isBlank()) {
      return new GridPosition(0, 0);
    }
    String row = extractNumericField(replayMetadata, "\"expectedFinalRow\":");
    String col = extractNumericField(replayMetadata, "\"expectedFinalCol\":");
    try {
      return new GridPosition(Integer.parseInt(row), Integer.parseInt(col));
    } catch (NumberFormatException ignored) {
      return new GridPosition(0, 0);
    }
  }

  private String extractNumericField(String json, String marker) {
    int start = json.indexOf(marker);
    if (start < 0) {
      return "0";
    }
    int from = start + marker.length();
    int end = from;
    while (end < json.length()) {
      char c = json.charAt(end);
      if (!Character.isDigit(c) && c != '-') {
        break;
      }
      end++;
    }
    if (end <= from) {
      return "0";
    }
    return json.substring(from, end);
  }
}
