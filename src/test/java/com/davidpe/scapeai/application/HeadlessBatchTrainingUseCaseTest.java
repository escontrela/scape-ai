package com.davidpe.scapeai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.scapeai.persistence.ExperienceTransitionEntity;
import com.davidpe.scapeai.persistence.repository.ExperienceReplayRepository;
import com.davidpe.scapeai.persistence.repository.ExperienceReplaySamplingStrategy;
import com.davidpe.scapeai.simulation.GridPosition;
import com.davidpe.scapeai.simulation.MazeDefinition;
import java.time.Duration;
import java.util.List;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;

class HeadlessBatchTrainingUseCaseTest {

  @Test
  void shouldRunBatchAndExposeAggregatedMetrics() {
    IterativeEpisodeTrainingService trainingService =
        new StubIterativeTrainingService(
            new IterativeTrainingSummary(20, 20, false, 0.8, 2.4, 0.6));
    HeadlessBatchTrainingUseCase useCase =
        new HeadlessBatchTrainingUseCase(
            trainingService,
            new TrainingSessionConfigValidator(),
            new BalancedExperienceReplaySampler(new EmptyReplayRepository()));
    MazeDefinition maze =
        new MazeDefinition(2, 2, new boolean[2][2], new GridPosition(0, 0), new GridPosition(1, 1));
    TrainingSessionConfig sessionConfig =
        TrainingSessionConfig.v1(
            "maze-headless", "heuristic-baseline", Duration.ofSeconds(30), 7L, true, TrainingTargetDifficulty.MEDIUM);

    HeadlessBatchTrainingResult result = useCase.runBatch(sessionConfig, maze, 20);

    assertEquals(20, result.episodesRequested());
    assertEquals(20, result.episodesCompleted());
    assertEquals(false, result.cancelled());
    assertEquals(0.8, result.successRate());
    assertEquals(2.4, result.averageReward());
    assertEquals(0.6, result.averageCollisions());
    assertTrue(result.totalDurationMillis() >= 0);
  }

  @Test
  void shouldAllowSelectingReplaySamplingStrategyWithoutBreakingBatchContract() {
    IterativeEpisodeTrainingService trainingService =
        new StubIterativeTrainingService(
            new IterativeTrainingSummary(8, 8, false, 0.5, 1.1, 0.3));
    HeadlessBatchTrainingUseCase useCase =
        new HeadlessBatchTrainingUseCase(
            trainingService,
            new TrainingSessionConfigValidator(),
            new BalancedExperienceReplaySampler(new EmptyReplayRepository()));
    MazeDefinition maze =
        new MazeDefinition(2, 2, new boolean[2][2], new GridPosition(0, 0), new GridPosition(1, 1));
    TrainingSessionConfig sessionConfig =
        TrainingSessionConfig.v1(
            "maze-headless", "heuristic-baseline", Duration.ofSeconds(30), 7L, true, TrainingTargetDifficulty.MEDIUM);

    HeadlessBatchTrainingResult result =
        useCase.runBatch(sessionConfig, maze, 8, ExperienceReplaySamplingStrategy.REWARD_AWARE);

    assertEquals(8, result.episodesRequested());
    assertEquals(8, result.episodesCompleted());
    assertEquals(0.5, result.successRate());
  }

  private static final class StubIterativeTrainingService implements IterativeEpisodeTrainingService {

    private final IterativeTrainingSummary summary;

    private StubIterativeTrainingService(IterativeTrainingSummary summary) {
      this.summary = summary;
    }

    @Override
    public IterativeTrainingSummary train(
        MazeDefinition maze, int episodes, Duration timeout, BooleanSupplier cancellationRequested) {
      return summary;
    }
  }

  private static final class EmptyReplayRepository implements ExperienceReplayRepository {

    @Override
    public ExperienceTransitionEntity save(ExperienceTransitionEntity transition) {
      return transition;
    }

    @Override
    public List<ExperienceTransitionEntity> findRecent(int page, int pageSize) {
      return List.of();
    }
  }
}
