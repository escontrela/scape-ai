package com.davidpe.scapeai.application;

import com.davidpe.scapeai.simulation.MazeDefinition;
import java.time.Duration;
import java.util.function.BooleanSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DefaultIterativeEpisodeTrainingService implements IterativeEpisodeTrainingService {

  private final SimulationEpisodeOrchestrator episodeOrchestrator;
  private final TrainingLifecycleEventBus trainingLifecycleEventBus;
  private final EpsilonPhaseScheduler epsilonPhaseScheduler;

  @Autowired
  public DefaultIterativeEpisodeTrainingService(SimulationEpisodeOrchestrator episodeOrchestrator) {
    this(episodeOrchestrator, TrainingLifecycleEventBus.noop(), new EpsilonPhaseScheduler(0.35, 0.20, 0.05));
  }

  public DefaultIterativeEpisodeTrainingService(
      SimulationEpisodeOrchestrator episodeOrchestrator,
      TrainingLifecycleEventBus trainingLifecycleEventBus) {
    this(episodeOrchestrator, trainingLifecycleEventBus, new EpsilonPhaseScheduler(0.35, 0.20, 0.05));
  }

  public DefaultIterativeEpisodeTrainingService(
      SimulationEpisodeOrchestrator episodeOrchestrator,
      TrainingLifecycleEventBus trainingLifecycleEventBus,
      EpsilonPhaseScheduler epsilonPhaseScheduler) {
    this.episodeOrchestrator = episodeOrchestrator;
    this.trainingLifecycleEventBus = trainingLifecycleEventBus;
    this.epsilonPhaseScheduler = epsilonPhaseScheduler;
  }

  @Override
  public IterativeTrainingSummary train(
      MazeDefinition maze, int episodes, Duration timeout, BooleanSupplier cancellationRequested) {
    if (episodes <= 0) {
      throw new IllegalArgumentException("episodes must be greater than zero");
    }

    int completed = 0;
    int successes = 0;
    double rewardSum = 0.0;
    double collisionsSum = 0.0;
    double epsilonAppliedSum = 0.0;

    while (completed < episodes) {
      if (cancellationRequested.getAsBoolean()) {
        break;
      }
      double epsilonForEpisode = epsilonPhaseScheduler.epsilonForEpisode(completed, episodes);
      SimulationEpisodeResult episode = episodeOrchestrator.runEpisode(maze, timeout, epsilonForEpisode);
      if (episode.endReason() == EpisodeEndReason.TIMEOUT) {
        trainingLifecycleEventBus.publish(
            TrainingLifecycleEvent.now(TrainingLifecycleEventType.TIMED_OUT, "Episode timeout"));
      }
      completed++;
      epsilonAppliedSum += epsilonForEpisode;
      if (episode.success()) {
        successes++;
      }
      rewardSum += episode.totalReward();
      collisionsSum += episode.collisions();
    }

    boolean cancelled = completed < episodes;
    if (!cancelled) {
      trainingLifecycleEventBus.publish(
          TrainingLifecycleEvent.now(
              TrainingLifecycleEventType.FINISHED, "Completed " + completed + " episodes"));
    }
    double divisor = completed == 0 ? 1.0 : completed;
    return new IterativeTrainingSummary(
        episodes,
        completed,
        cancelled,
        successes / divisor,
        rewardSum / divisor,
        collisionsSum / divisor,
        0,
        0,
        0L,
        0L,
        "NONE",
        epsilonAppliedSum / divisor);
  }
}
