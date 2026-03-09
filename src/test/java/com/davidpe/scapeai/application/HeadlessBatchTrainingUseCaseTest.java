package com.davidpe.scapeai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.scapeai.simulation.GridPosition;
import com.davidpe.scapeai.simulation.MazeDefinition;
import java.time.Duration;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;

class HeadlessBatchTrainingUseCaseTest {

  @Test
  void shouldRunBatchAndExposeAggregatedMetrics() {
    IterativeEpisodeTrainingService trainingService =
        new StubIterativeTrainingService(
            new IterativeTrainingSummary(20, 20, false, 0.8, 2.4, 0.6));
    HeadlessBatchTrainingUseCase useCase = new HeadlessBatchTrainingUseCase(trainingService);
    MazeDefinition maze =
        new MazeDefinition(2, 2, new boolean[2][2], new GridPosition(0, 0), new GridPosition(1, 1));

    HeadlessBatchTrainingResult result = useCase.runBatch(maze, 20, Duration.ofSeconds(30));

    assertEquals(20, result.episodesRequested());
    assertEquals(20, result.episodesCompleted());
    assertEquals(false, result.cancelled());
    assertEquals(0.8, result.successRate());
    assertEquals(2.4, result.averageReward());
    assertEquals(0.6, result.averageCollisions());
    assertTrue(result.totalDurationMillis() >= 0);
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
}
