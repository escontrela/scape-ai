package com.davidpe.scapeai.application;

import com.davidpe.scapeai.persistence.MazeEntity;
import com.davidpe.scapeai.persistence.repository.MazeRepository;
import com.davidpe.scapeai.persistence.repository.TrainingRunRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RecentRunsComparisonService {

  private static final int MAX_RECENT_RUNS = 10;
  private static final int REGRESSION_WINDOW = 3;
  private static final double REGRESSION_DELTA = 2.0;
  private final MazeRepository mazeRepository;
  private final TrainingRunRepository trainingRunRepository;
  private final double pathEntropyAlertThreshold;

  public RecentRunsComparisonService(
      MazeRepository mazeRepository,
      TrainingRunRepository trainingRunRepository,
      @Value("${scape.metrics.path-entropy-alert-threshold:1.10}")
          double pathEntropyAlertThreshold) {
    this.mazeRepository = mazeRepository;
    this.trainingRunRepository = trainingRunRepository;
    this.pathEntropyAlertThreshold = pathEntropyAlertThreshold;
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
          case BY_HEALTH_INDEX ->
              Comparator.comparingDouble(RecentRunComparisonRow::trainingHealthIndex).reversed();
          case BY_DATE -> Comparator.comparingLong(RecentRunComparisonRow::createdAtEpochMillis).reversed();
        };

    List<com.davidpe.scapeai.persistence.TrainingRunEntity> entities =
        trainingRunRepository.findRecentByMazeId(maze.get().id(), MAX_RECENT_RUNS).stream()
            .sorted(
                Comparator.comparingLong(com.davidpe.scapeai.persistence.TrainingRunEntity::createdAtEpochMillis)
                    .reversed())
            .toList();
    java.util.ArrayList<RecentRunComparisonRow> rows = new java.util.ArrayList<>(entities.size());
    for (int i = 0; i < entities.size(); i++) {
      var entity = entities.get(i);
      boolean healthRegression = isHealthRegression(entities, i, entity.trainingHealthIndex());
      rows.add(
          new RecentRunComparisonRow(
              entity.success(),
              entity.totalReward(),
              entity.collisions(),
              entity.netProgress(),
              entity.leftSideCoverage(),
              entity.rightSideCoverage(),
              entity.pathEntropy(),
              entity.pathEntropy() < pathEntropyAlertThreshold,
              entity.trainingHealthIndex(),
              healthRegression,
              entity.healthIndexFormulaVersion(),
              entity.elapsedMillis(),
              entity.createdAtEpochMillis()));
    }
    return rows.stream().sorted(comparator).toList();
  }

  private boolean isHealthRegression(
      List<com.davidpe.scapeai.persistence.TrainingRunEntity> rows, int currentIndex, double currentValue) {
    int start = currentIndex + 1;
    int end = Math.min(rows.size(), start + REGRESSION_WINDOW);
    if (start >= end) {
      return false;
    }
    double baseline = 0.0;
    int samples = 0;
    for (int i = start; i < end; i++) {
      baseline += rows.get(i).trainingHealthIndex();
      samples++;
    }
    if (samples <= 0) {
      return false;
    }
    baseline /= samples;
    return currentValue + REGRESSION_DELTA < baseline;
  }
}
