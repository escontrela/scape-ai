package com.davidpe.scapeai.application;

import com.davidpe.scapeai.simulation.MazeDefinition;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ApplicationTrainingExecutionService implements TrainingExecutionService {

  private final IterativeEpisodeTrainingService iterativeTrainingService;
  private final TrainingLifecycleEventBus trainingLifecycleEventBus;
  private final SmokeRunProbe smokeRunProbe;
  private final SessionRandomSource sessionRandomSource;
  private final boolean smokeRunEnabled;
  private final long smokeRunSeed;
  private final Duration smokeRunTimeout;
  private final double smokeRunMinCoverage;
  private final ExecutorService executor =
      Executors.newSingleThreadExecutor(
          runnable -> {
            Thread thread = new Thread(runnable, "training-execution");
            thread.setDaemon(true);
            return thread;
          });
  private final AtomicReference<TrainingRunHandle> currentRun = new AtomicReference<>();

  @Autowired
  public ApplicationTrainingExecutionService(
      IterativeEpisodeTrainingService iterativeTrainingService,
      TrainingLifecycleEventBus trainingLifecycleEventBus,
      SimulationEpisodeOrchestrator simulationEpisodeOrchestrator,
      SessionRandomSource sessionRandomSource,
      @Value("${scape.training.smoke-run.enabled:true}") boolean smokeRunEnabled,
      @Value("${scape.training.smoke-run.seed:20260309}") long smokeRunSeed,
      @Value("${scape.training.smoke-run.timeout:PT20S}") Duration smokeRunTimeout,
      @Value("${scape.training.smoke-run.min-coverage:0.15}") double smokeRunMinCoverage) {
    this(
        iterativeTrainingService,
        trainingLifecycleEventBus,
        simulationEpisodeOrchestrator::runEpisode,
        sessionRandomSource,
        smokeRunEnabled,
        smokeRunSeed,
        smokeRunTimeout,
        smokeRunMinCoverage);
  }

  ApplicationTrainingExecutionService(
      IterativeEpisodeTrainingService iterativeTrainingService,
      TrainingLifecycleEventBus trainingLifecycleEventBus,
      SmokeRunProbe smokeRunProbe,
      SessionRandomSource sessionRandomSource,
      boolean smokeRunEnabled,
      long smokeRunSeed,
      Duration smokeRunTimeout,
      double smokeRunMinCoverage) {
    this.iterativeTrainingService = iterativeTrainingService;
    this.trainingLifecycleEventBus = trainingLifecycleEventBus;
    this.smokeRunProbe = smokeRunProbe;
    this.sessionRandomSource = sessionRandomSource;
    this.smokeRunEnabled = smokeRunEnabled;
    this.smokeRunSeed = smokeRunSeed;
    this.smokeRunTimeout = smokeRunTimeout == null ? Duration.ofSeconds(20) : smokeRunTimeout;
    this.smokeRunMinCoverage = Math.max(0.0, Math.min(1.0, smokeRunMinCoverage));
  }

  ApplicationTrainingExecutionService(IterativeEpisodeTrainingService iterativeTrainingService) {
    this(
        iterativeTrainingService,
        TrainingLifecycleEventBus.noop(),
        (SmokeRunProbe) null,
        null,
        false,
        20260309L,
        Duration.ofSeconds(20),
        0.15);
  }

  ApplicationTrainingExecutionService(
      IterativeEpisodeTrainingService iterativeTrainingService,
      TrainingLifecycleEventBus trainingLifecycleEventBus) {
    this(
        iterativeTrainingService,
        trainingLifecycleEventBus,
        (SmokeRunProbe) null,
        null,
        false,
        20260309L,
        Duration.ofSeconds(20),
        0.15);
  }

  @Override
  public synchronized CompletableFuture<IterativeTrainingSummary> startTraining(
      MazeDefinition maze, int episodes, Duration timeout) {
    cancelTraining();
    AtomicBoolean cancelled = new AtomicBoolean(false);
    CompletableFuture<IterativeTrainingSummary> future =
        CompletableFuture.supplyAsync(
            () -> iterativeTrainingService.train(maze, episodes, timeout, cancelled::get),
            executor);

    TrainingRunHandle handle = new TrainingRunHandle(cancelled, future);
    currentRun.set(handle);
    future.whenComplete((ignored, error) -> currentRun.compareAndSet(handle, null));
    return future;
  }

  @Override
  public synchronized CompletableFuture<IterativeTrainingSummary> startBatchTraining(
      MazeDefinition maze, int episodesPerBatch, int batches, Duration timeout) {
    return startBatchTraining(maze, episodesPerBatch, batches, timeout, TrainingBudget.unlimited());
  }

  @Override
  public synchronized CompletableFuture<IterativeTrainingSummary> startBatchTraining(
      MazeDefinition maze,
      int episodesPerBatch,
      int batches,
      Duration timeout,
      TrainingBudget budget) {
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
            () -> runBatches(maze, episodesPerBatch, batches, timeout, budget, cancelled),
            executor);

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
      TrainingBudget budget,
      AtomicBoolean cancelled) {
    TrainingBudget effectiveBudget = budget == null ? TrainingBudget.unlimited() : budget;
    int requestedEpisodes = episodesPerBatch * batches;
    int budgetEpisodesAvailable =
        effectiveBudget.hasEpisodeLimit()
            ? Math.min(requestedEpisodes, effectiveBudget.maxEpisodes())
            : requestedEpisodes;
    long budgetWallClockAvailableMillis =
        effectiveBudget.hasWallClockLimit() ? effectiveBudget.maxWallClock().toMillis() : 0L;
    long budgetStartedAt = System.currentTimeMillis();
    SmokeRunOutcome smokeRunOutcome = runSmokeRun(maze);
    if (!smokeRunOutcome.passed()) {
      trainingLifecycleEventBus.publish(
          TrainingLifecycleEvent.now(
              TrainingLifecycleEventType.FINISHED,
              "SMOKE-RUN BLOCKED: " + smokeRunOutcome.reason()));
      return new IterativeTrainingSummary(
          requestedEpisodes,
          0,
          true,
          0.0,
          0.0,
          0.0,
          0,
          budgetEpisodesAvailable,
          0L,
          budgetWallClockAvailableMillis,
          "SMOKE_RUN_BLOCKED",
          0.0);
    }
    int batchesCompleted = 0;
    int episodesRequested = requestedEpisodes;
    int episodesCompleted = 0;
    double successfulEpisodes = 0.0;
    double rewardWeightedSum = 0.0;
    double collisionsWeightedSum = 0.0;
    double epsilonWeightedSum = 0.0;
    String budgetExhaustedReason = "NONE";
    boolean interrupted = false;

    for (int batchIndex = 1; batchIndex <= batches; batchIndex++) {
      if (cancelled.get()) {
        break;
      }
      if (effectiveBudget.hasEpisodeLimit() && episodesCompleted >= effectiveBudget.maxEpisodes()) {
        budgetExhaustedReason = "EPISODE_LIMIT";
        break;
      }
      long wallClockConsumed = Math.max(0L, System.currentTimeMillis() - budgetStartedAt);
      if (effectiveBudget.hasWallClockLimit()
          && wallClockConsumed >= effectiveBudget.maxWallClock().toMillis()) {
        budgetExhaustedReason = "WALL_CLOCK_LIMIT";
        break;
      }
      int episodesForThisBatch =
          effectiveBudget.hasEpisodeLimit()
              ? Math.min(episodesPerBatch, effectiveBudget.maxEpisodes() - episodesCompleted)
              : episodesPerBatch;
      if (episodesForThisBatch <= 0) {
        budgetExhaustedReason = "EPISODE_LIMIT";
        break;
      }
      trainingLifecycleEventBus.publish(
          TrainingLifecycleEvent.now(
              TrainingLifecycleEventType.STARTED,
              "BATCH " + batchIndex + "/" + batches + " STARTED"));
      int remainingEpisodesBudget = Math.max(0, budgetEpisodesAvailable - episodesCompleted);
      IterativeTrainingSummary batchSummary =
          iterativeTrainingService.train(
              maze,
              episodesForThisBatch,
              timeout,
              () ->
                  cancelled.get()
                      || (effectiveBudget.hasWallClockLimit()
                          && (System.currentTimeMillis() - budgetStartedAt)
                              >= effectiveBudget.maxWallClock().toMillis())
                      || remainingEpisodesBudget <= 0);
      batchesCompleted++;
      episodesCompleted += batchSummary.episodesCompleted();
      successfulEpisodes += batchSummary.successRate() * batchSummary.episodesCompleted();
      rewardWeightedSum += batchSummary.averageReward() * batchSummary.episodesCompleted();
      collisionsWeightedSum += batchSummary.averageCollisions() * batchSummary.episodesCompleted();
      epsilonWeightedSum += batchSummary.averageEpsilonApplied() * batchSummary.episodesCompleted();
      trainingLifecycleEventBus.publish(
          TrainingLifecycleEvent.now(
              TrainingLifecycleEventType.FINISHED,
              "BATCH " + batchIndex + "/" + batches + " FINISHED"));
      if (batchSummary.cancelled()) {
        if (effectiveBudget.hasWallClockLimit()
            && (System.currentTimeMillis() - budgetStartedAt)
                >= effectiveBudget.maxWallClock().toMillis()) {
          budgetExhaustedReason = "WALL_CLOCK_LIMIT";
        }
        interrupted = true;
        break;
      }
    }

    if ("NONE".equals(budgetExhaustedReason)
        && effectiveBudget.hasEpisodeLimit()
        && episodesCompleted >= effectiveBudget.maxEpisodes()) {
      budgetExhaustedReason = "EPISODE_LIMIT";
    }
    boolean runCancelled =
        cancelled.get()
            || interrupted
            || batchesCompleted < batches
            || !"NONE".equals(budgetExhaustedReason);
    if (!"NONE".equals(budgetExhaustedReason)) {
      trainingLifecycleEventBus.publish(
          TrainingLifecycleEvent.now(
              TrainingLifecycleEventType.FINISHED, "BUDGET_EXHAUSTED: " + budgetExhaustedReason));
    }
    double divisor = episodesCompleted <= 0 ? 1.0 : episodesCompleted;
    long wallClockConsumed = Math.max(0L, System.currentTimeMillis() - budgetStartedAt);
    return new IterativeTrainingSummary(
        episodesRequested,
        episodesCompleted,
        runCancelled,
        successfulEpisodes / divisor,
        rewardWeightedSum / divisor,
        collisionsWeightedSum / divisor,
        episodesCompleted,
        budgetEpisodesAvailable,
        wallClockConsumed,
        budgetWallClockAvailableMillis,
        budgetExhaustedReason,
        epsilonWeightedSum / divisor);
  }

  private SmokeRunOutcome runSmokeRun(MazeDefinition maze) {
    if (!smokeRunEnabled || smokeRunProbe == null || sessionRandomSource == null) {
      return SmokeRunOutcome.success();
    }
    long previousSeed = sessionRandomSource.effectiveSeed();
    try {
      trainingLifecycleEventBus.publish(
          TrainingLifecycleEvent.now(
              TrainingLifecycleEventType.STARTED,
              "SMOKE-RUN START seed=" + smokeRunSeed + " timeout=" + smokeRunTimeout));
      sessionRandomSource.reset(smokeRunSeed);
      SimulationEpisodeResult smokeResult = smokeRunProbe.run(maze, smokeRunTimeout);
      if (smokeResult.endReason() == EpisodeEndReason.TIMEOUT) {
        return SmokeRunOutcome.failed("timeout reached before long training");
      }
      if (smokeResult.mazeCoverageRatio() < smokeRunMinCoverage) {
        return SmokeRunOutcome.failed(
            "coverage "
                + String.format(java.util.Locale.ROOT, "%.2f", smokeResult.mazeCoverageRatio())
                + " below "
                + String.format(java.util.Locale.ROOT, "%.2f", smokeRunMinCoverage));
      }
      trainingLifecycleEventBus.publish(
          TrainingLifecycleEvent.now(
              TrainingLifecycleEventType.STARTED,
              "SMOKE-RUN PASS coverage="
                  + String.format(java.util.Locale.ROOT, "%.2f", smokeResult.mazeCoverageRatio())));
      return SmokeRunOutcome.success();
    } finally {
      sessionRandomSource.reset(previousSeed);
    }
  }

  interface SmokeRunProbe {
    SimulationEpisodeResult run(MazeDefinition maze, Duration timeout);
  }

  private record SmokeRunOutcome(boolean passed, String reason) {

    static SmokeRunOutcome success() {
      return new SmokeRunOutcome(true, "");
    }

    static SmokeRunOutcome failed(String reason) {
      return new SmokeRunOutcome(false, reason == null ? "unknown reason" : reason);
    }
  }
}
