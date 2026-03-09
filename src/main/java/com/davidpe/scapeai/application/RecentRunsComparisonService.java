package com.davidpe.scapeai.application;

import com.davidpe.scapeai.persistence.MazeEntity;
import com.davidpe.scapeai.persistence.repository.MazeRepository;
import com.davidpe.scapeai.persistence.repository.TrainingRunRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class RecentRunsComparisonService {

  private static final int MAX_RECENT_RUNS = 10;
  private final MazeRepository mazeRepository;
  private final TrainingRunRepository trainingRunRepository;

  public RecentRunsComparisonService(
      MazeRepository mazeRepository, TrainingRunRepository trainingRunRepository) {
    this.mazeRepository = mazeRepository;
    this.trainingRunRepository = trainingRunRepository;
  }

  public List<RecentRunComparisonRow> recentRuns(String mazeName, RecentRunsSortOption sortOption) {
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

    Comparator<RecentRunComparisonRow> comparator =
        switch (sortOption) {
          case BY_REWARD -> Comparator.comparingDouble(RecentRunComparisonRow::reward).reversed();
          case BY_NET_PROGRESS ->
              Comparator.comparingDouble(RecentRunComparisonRow::netProgress).reversed();
          case BY_DATE -> Comparator.comparingLong(RecentRunComparisonRow::createdAtEpochMillis).reversed();
        };

    return trainingRunRepository.findRecentByMazeId(maze.get().id(), MAX_RECENT_RUNS).stream()
        .map(
            entity ->
                new RecentRunComparisonRow(
                    entity.success(),
                    entity.totalReward(),
                    entity.collisions(),
                    entity.netProgress(),
                    entity.leftSideCoverage(),
                    entity.rightSideCoverage(),
                    entity.elapsedMillis(),
                    entity.createdAtEpochMillis()))
        .sorted(comparator)
        .toList();
  }
}
