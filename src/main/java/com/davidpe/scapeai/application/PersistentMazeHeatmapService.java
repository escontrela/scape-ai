package com.davidpe.scapeai.application;

import com.davidpe.scapeai.persistence.MazeEntity;
import com.davidpe.scapeai.persistence.repository.MazeRepository;
import com.davidpe.scapeai.persistence.repository.TrainingRunRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class PersistentMazeHeatmapService {

  private final MazeRepository mazeRepository;
  private final TrainingRunRepository trainingRunRepository;

  public PersistentMazeHeatmapService(
      MazeRepository mazeRepository, TrainingRunRepository trainingRunRepository) {
    this.mazeRepository = mazeRepository;
    this.trainingRunRepository = trainingRunRepository;
  }

  public List<CellVisitFrequency> loadAccumulatedHeatmap(String mazeName, int recentRunsLimit) {
    if (mazeName == null || mazeName.isBlank()) {
      return List.of();
    }
    Optional<MazeEntity> maze =
        mazeRepository.findAllOrderByDifficulty(true).stream()
            .filter(candidate -> mazeName.equals(candidate.name()))
            .findFirst();
    if (maze.isEmpty()) {
      return List.of();
    }
    return trainingRunRepository.findAccumulatedCellVisitsByMazeId(maze.get().id(), recentRunsLimit);
  }
}
