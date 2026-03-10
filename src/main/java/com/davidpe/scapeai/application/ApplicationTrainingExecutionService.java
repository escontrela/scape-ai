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
  private final TrainingLifecycleEventBus trainingLifecycleEventBus;
  private final ExecutorService executor =
      Executors.newSingleThreadExecutor(
          runnable -> {
            Thread thread = new Thread(runnable, "training-execution");
            thread.setDaemon(true);
            return thread;
          });
  private final AtomicReference<TrainingRunHandle> currentRun = new AtomicReference<>();

  public ApplicationTrainingExecutionService(
      IterativeEpisodeTrainingService iterativeTrainingService,
      TrainingLifecycleEventBus trainingLifecycleEventBus) {
    this.iterativeTrainingService = iterativeTrainingService;
    this.trainingLifecycleEventBus = trainingLifecycleEventBus;
  }

  ApplicationTrainingExecutionService(IterativeEpisodeTrainingService iterativeTrainingService) {
    this(iterativeTrainingService, TrainingLifecycleEventBus.noop());
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
  public synchronized CompletableFuture<IterativeTrainingSummary> startBatchTraining(
      MazeDefinition maze, int episodesPerBatch, int batches, Duration timeout) {
    if (episodesPerBatch <= 0) {
      throw new IllegalArgumentException("episodesPerBatch must be greater than zero");
    }
    if (batches <= 0) {
      throw new IllegalArgumentException("batches must be greater than zero");
    }
    cancelTraining();
    AtomicBoolean cancelled = new AtomicBoolean(false);
    CompletableFuture<IterativeTrainingSummary> future =
        CompletableFuture.supplyAsync(
            () -> runBatches(maze, episodesPerBatch, batches, timeout, cancelled), executor);

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

  private IterativeTrainingSummary runBatches(
      MazeDefinition maze,
      int episodesPerBatch,
      int batches,
      Duration timeout,
      AtomicBoolean cancelled) {
    int batchesCompleted = 0;
    int episodesRequested = episodesPerBatch * batches;
    int episodesCompleted = 0;
    double successfulEpisodes = 0.0;
    double rewardWeightedSum = 0.0;
    double collisionsWeightedSum = 0.0;
    boolean interrupted = false;

    for (int batchIndex = 1; batchIndex <= batches; batchIndex++) {
      if (cancelled.get()) {
        break;
      }
      trainingLifecycleEventBus.publish(
          TrainingLifecycleEvent.now(
              TrainingLifecycleEventType.STARTED, "BATCH " + batchIndex + "/" + batches + " STARTED"));
      IterativeTrainingSummary batchSummary =
          iterativeTrainingService.train(maze, episodesPerBatch, timeout, cancelled::get);
      batchesCompleted++;
      episodesCompleted += batchSummary.episodesCompleted();
      successfulEpisodes += batchSummary.successRate() * batchSummary.episodesCompleted();
      rewardWeightedSum += batchSummary.averageReward() * batchSummary.episodesCompleted();
      collisionsWeightedSum += batchSummary.averageCollisions() * batchSummary.episodesCompleted();
      trainingLifecycleEventBus.publish(
          TrainingLifecycleEvent.now(
              TrainingLifecycleEventType.FINISHED,
              "BATCH " + batchIndex + "/" + batches + " FINISHED"));
      if (batchSummary.cancelled()) {
        interrupted = true;
        break;
      }
    }

    boolean runCancelled = cancelled.get() || interrupted || batchesCompleted < batches;
    double divisor = episodesCompleted <= 0 ? 1.0 : episodesCompleted;
    return new IterativeTrainingSummary(
        episodesRequested,
        episodesCompleted,
        runCancelled,
        successfulEpisodes / divisor,
        rewardWeightedSum / divisor,
        collisionsWeightedSum / divisor);
  }
}
