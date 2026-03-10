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
    return runBatch(
        sessionConfig,
        maze,
        episodes,
        ExperienceReplaySamplingStrategy.UNIFORM,
        TrainingBudget.unlimited());
  }

  public HeadlessBatchTrainingResult runBatch(
      TrainingSessionConfig sessionConfig,
      MazeDefinition maze,
      int episodes,
      ExperienceReplaySamplingStrategy samplingStrategy) {
    return runBatch(sessionConfig, maze, episodes, samplingStrategy, TrainingBudget.unlimited());
  }

  public HeadlessBatchTrainingResult runBatch(
      TrainingSessionConfig sessionConfig,
      MazeDefinition maze,
      int episodes,
      ExperienceReplaySamplingStrategy samplingStrategy,
      TrainingBudget budget) {
    configValidator.ensureValid(sessionConfig);
    TrainingBudget effectiveBudget = budget == null ? TrainingBudget.unlimited() : budget;
    int episodesRequested = Math.max(1, episodes);
    int episodesTarget =
        effectiveBudget.hasEpisodeLimit()
            ? Math.min(episodesRequested, effectiveBudget.maxEpisodes())
            : episodesRequested;
    replaySampler.sampleRecent(Math.max(8, episodesTarget), samplingStrategy);
    long startedAt = System.currentTimeMillis();
    java.util.concurrent.atomic.AtomicInteger consumedEpisodes = new java.util.concurrent.atomic.AtomicInteger();
    IterativeTrainingSummary summary =
        iterativeTrainingService.train(
            maze,
            episodesTarget,
            sessionConfig.timeout(),
            () -> {
              if (effectiveBudget.hasEpisodeLimit()
                  && consumedEpisodes.get() >= effectiveBudget.maxEpisodes()) {
                return true;
              }
              if (!effectiveBudget.hasWallClockLimit()) {
                return false;
              }
              long elapsed = System.currentTimeMillis() - startedAt;
              return elapsed >= effectiveBudget.maxWallClock().toMillis();
            });
    long totalDurationMillis = Math.max(0L, System.currentTimeMillis() - startedAt);
    consumedEpisodes.set(summary.episodesCompleted());
    String exhaustedReason = "NONE";
    if (effectiveBudget.hasEpisodeLimit() && summary.episodesCompleted() >= effectiveBudget.maxEpisodes()) {
      exhaustedReason = "EPISODE_LIMIT";
    } else if (effectiveBudget.hasWallClockLimit()
        && totalDurationMillis >= effectiveBudget.maxWallClock().toMillis()) {
      exhaustedReason = "WALL_CLOCK_LIMIT";
    }
    return new HeadlessBatchTrainingResult(
        episodesRequested,
        summary.episodesCompleted(),
        summary.cancelled() || !"NONE".equals(exhaustedReason),
        summary.successRate(),
        summary.averageReward(),
        summary.averageCollisions(),
        totalDurationMillis,
        summary.episodesCompleted(),
        effectiveBudget.hasEpisodeLimit() ? effectiveBudget.maxEpisodes() : episodesRequested,
        totalDurationMillis,
        effectiveBudget.hasWallClockLimit() ? effectiveBudget.maxWallClock().toMillis() : 0L,
        exhaustedReason);
  }
}
