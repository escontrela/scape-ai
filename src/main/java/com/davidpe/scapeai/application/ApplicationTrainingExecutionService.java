package com.davidpe.scapeai.application;

import com.davidpe.scapeai.simulation.MazeDefinition;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;

@Service
public class ApplicationTrainingExecutionService implements TrainingExecutionService {

  private final IterativeEpisodeTrainingService iterativeTrainingService;
  private final ExecutorService executor =
      Executors.newSingleThreadExecutor(
          runnable -> {
            Thread thread = new Thread(runnable, "training-execution");
            thread.setDaemon(true);
            return thread;
          });
  private final AtomicReference<TrainingRunHandle> currentRun = new AtomicReference<>();

  public ApplicationTrainingExecutionService(IterativeEpisodeTrainingService iterativeTrainingService) {
    this.iterativeTrainingService = iterativeTrainingService;
  }

  @Override
  public synchronized CompletableFuture<IterativeTrainingSummary> startTraining(
      MazeDefinition maze, int episodes, Duration timeout) {
    cancelTraining();
    AtomicBoolean cancelled = new AtomicBoolean(false);
    CompletableFuture<IterativeTrainingSummary> future =
        CompletableFuture.supplyAsync(
            () -> iterativeTrainingService.train(maze, episodes, timeout, cancelled::get), executor);

    TrainingRunHandle handle = new TrainingRunHandle(cancelled, future);
    currentRun.set(handle);
    future.whenComplete((ignored, error) -> currentRun.compareAndSet(handle, null));
    return future;
  }

  @Override
  public synchronized void cancelTraining() {
    TrainingRunHandle handle = currentRun.getAndSet(null);
    if (handle == null) {
      return;
    }
    handle.cancelled().set(true);
    handle.future().cancel(true);
  }

  @PreDestroy
  public synchronized void shutdown() {
    cancelTraining();
    executor.shutdownNow();
  }

  private record TrainingRunHandle(
      AtomicBoolean cancelled, CompletableFuture<IterativeTrainingSummary> future) {}
}
