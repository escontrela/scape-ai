package com.davidpe.scapeai.application;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.scapeai.simulation.GridPosition;
import com.davidpe.scapeai.simulation.MazeDefinition;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ApplicationTrainingExecutionServiceTest {

  @Test
  void shouldAllowCancellationFromApplicationLayer() throws Exception {
    CountDownLatch cancelledObserved = new CountDownLatch(1);
    IterativeEpisodeTrainingService trainer =
        (maze, episodes, timeout, cancellationRequested) -> {
          while (!cancellationRequested.getAsBoolean()) {
            try {
              Thread.sleep(5);
            } catch (InterruptedException interrupted) {
              Thread.currentThread().interrupt();
              break;
            }
          }
          cancelledObserved.countDown();
          return new IterativeTrainingSummary(episodes, 0, true, 0.0, 0.0, 0.0);
        };
    ApplicationTrainingExecutionService service = new ApplicationTrainingExecutionService(trainer);
    MazeDefinition maze = new MazeDefinition(1, 1, new boolean[1][1], new GridPosition(0, 0), new GridPosition(0, 0));

    service.startTraining(maze, 2, Duration.ofSeconds(1));
    service.cancelTraining();

    assertTrue(cancelledObserved.await(1, TimeUnit.SECONDS));
    service.shutdown();
  }
}
