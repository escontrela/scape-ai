package com.davidpe.scapeai.application;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.davidpe.scapeai.simulation.GridPosition;
import com.davidpe.scapeai.simulation.MazeDefinition;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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

  @Test
  void shouldExecuteBatchTrainingAndPublishStartedFinishedPerBatch() throws Exception {
    List<TrainingLifecycleEvent> events = new ArrayList<>();
    TrainingLifecycleEventBus bus =
        new TrainingLifecycleEventBus() {
          @Override
          public void publish(TrainingLifecycleEvent event) {
            events.add(event);
          }

          @Override
          public Subscription subscribe(java.util.function.Consumer<TrainingLifecycleEvent> listener) {
            return () -> {};
          }
        };
    IterativeEpisodeTrainingService trainer =
        (maze, episodes, timeout, cancellationRequested) ->
            new IterativeTrainingSummary(episodes, episodes, false, 1.0, -1.0, 0.0);
    ApplicationTrainingExecutionService service = new ApplicationTrainingExecutionService(trainer, bus);
    MazeDefinition maze = new MazeDefinition(1, 1, new boolean[1][1], new GridPosition(0, 0), new GridPosition(0, 0));

    IterativeTrainingSummary summary =
        service.startBatchTraining(maze, 2, 3, Duration.ofMillis(100)).get(1, TimeUnit.SECONDS);

    assertEquals(6, summary.episodesRequested());
    assertEquals(6, summary.episodesCompleted());
    assertEquals(6, events.size());
    assertEquals(TrainingLifecycleEventType.STARTED, events.get(0).type());
    assertEquals(TrainingLifecycleEventType.FINISHED, events.get(1).type());
    assertTrue(events.get(0).detail().contains("1/3"));
    assertTrue(events.get(5).detail().contains("3/3"));
    service.shutdown();
  }

  @Test
  void shouldCancelBatchTrainingWithoutLeakingToFollowingBatches() throws Exception {
    CountDownLatch firstBatchEntered = new CountDownLatch(1);
    CountDownLatch cancelObserved = new CountDownLatch(1);
    IterativeEpisodeTrainingService trainer =
        (maze, episodes, timeout, cancellationRequested) -> {
          firstBatchEntered.countDown();
          while (!cancellationRequested.getAsBoolean()) {
            try {
              Thread.sleep(5);
            } catch (InterruptedException interrupted) {
              Thread.currentThread().interrupt();
              break;
            }
          }
          cancelObserved.countDown();
          return new IterativeTrainingSummary(episodes, 0, true, 0.0, 0.0, 0.0);
        };
    ApplicationTrainingExecutionService service = new ApplicationTrainingExecutionService(trainer);
    MazeDefinition maze = new MazeDefinition(1, 1, new boolean[1][1], new GridPosition(0, 0), new GridPosition(0, 0));

    service.startBatchTraining(maze, 4, 5, Duration.ofSeconds(1));
    assertTrue(firstBatchEntered.await(1, TimeUnit.SECONDS));
    service.cancelTraining();

    assertTrue(cancelObserved.await(1, TimeUnit.SECONDS));
    service.shutdown();
  }
}
