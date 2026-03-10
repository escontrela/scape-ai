package com.davidpe.scapeai.application;

import com.davidpe.scapeai.simulation.MazeDefinition;
import com.davidpe.scapeai.persistence.repository.ExperienceReplaySamplingStrategy;
import org.springframework.stereotype.Service;

@Service
public class HeadlessBatchTrainingUseCase {

  private final IterativeEpisodeTrainingService iterativeTrainingService;
  private final TrainingSessionConfigValidator configValidator;
  private final BalancedExperienceReplaySampler replaySampler;

  public HeadlessBatchTrainingUseCase(
      IterativeEpisodeTrainingService iterativeTrainingService,
      TrainingSessionConfigValidator configValidator,
      BalancedExperienceReplaySampler replaySampler) {
    this.iterativeTrainingService = iterativeTrainingService;
    this.configValidator = configValidator;
    this.replaySampler = replaySampler;
  }

  public HeadlessBatchTrainingResult runBatch(TrainingSessionConfig sessionConfig, MazeDefinition maze, int episodes) {
    return runBatch(sessionConfig, maze, episodes, ExperienceReplaySamplingStrategy.UNIFORM);
  }

  public HeadlessBatchTrainingResult runBatch(
      TrainingSessionConfig sessionConfig,
      MazeDefinition maze,
      int episodes,
      ExperienceReplaySamplingStrategy samplingStrategy) {
    configValidator.ensureValid(sessionConfig);
    replaySampler.sampleRecent(Math.max(8, episodes), samplingStrategy);
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
