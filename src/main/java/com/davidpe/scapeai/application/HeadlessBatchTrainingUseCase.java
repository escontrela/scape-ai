package com.davidpe.scapeai.application;

import com.davidpe.scapeai.simulation.MazeDefinition;
import java.time.Duration;
import org.springframework.stereotype.Service;

@Service
public class HeadlessBatchTrainingUseCase {

  private final IterativeEpisodeTrainingService iterativeTrainingService;

  public HeadlessBatchTrainingUseCase(IterativeEpisodeTrainingService iterativeTrainingService) {
    this.iterativeTrainingService = iterativeTrainingService;
  }

  public HeadlessBatchTrainingResult runBatch(
      MazeDefinition maze, int episodes, Duration timeout) {
    long startedAt = System.currentTimeMillis();
    IterativeTrainingSummary summary =
        iterativeTrainingService.train(maze, episodes, timeout, () -> false);
    long totalDurationMillis = Math.max(0L, System.currentTimeMillis() - startedAt);
    return new HeadlessBatchTrainingResult(
        summary.episodesRequested(),
        summary.episodesCompleted(),
        summary.cancelled(),
        summary.successRate(),
        summary.averageReward(),
        summary.averageCollisions(),
        totalDurationMillis);
  }
}
