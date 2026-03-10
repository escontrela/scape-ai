package com.davidpe.scapeai.application;

import com.davidpe.scapeai.simulation.MazeDefinition;
import org.springframework.stereotype.Service;

@Service
public class HeadlessBatchTrainingUseCase {

  private final IterativeEpisodeTrainingService iterativeTrainingService;
  private final TrainingSessionConfigValidator configValidator;

  public HeadlessBatchTrainingUseCase(
      IterativeEpisodeTrainingService iterativeTrainingService,
      TrainingSessionConfigValidator configValidator) {
    this.iterativeTrainingService = iterativeTrainingService;
    this.configValidator = configValidator;
  }

  public HeadlessBatchTrainingResult runBatch(TrainingSessionConfig sessionConfig, MazeDefinition maze, int episodes) {
    configValidator.ensureValid(sessionConfig);
    long startedAt = System.currentTimeMillis();
    IterativeTrainingSummary summary =
        iterativeTrainingService.train(maze, episodes, sessionConfig.timeout(), () -> false);
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
