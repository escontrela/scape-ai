package com.davidpe.scapeai.application;

import com.davidpe.scapeai.persistence.MazeEntity;
import com.davidpe.scapeai.persistence.TrainingRunEntity;
import com.davidpe.scapeai.persistence.repository.MazeRepository;
import com.davidpe.scapeai.persistence.repository.TrainingRunRepository;
import com.davidpe.scapeai.simulation.MazeDefinition;
import java.time.Duration;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DefaultIterativeEpisodeTrainingService implements IterativeEpisodeTrainingService {

  private final SimulationEpisodeOrchestrator episodeOrchestrator;
  private final TrainingLifecycleEventBus trainingLifecycleEventBus;
  private final EpsilonPhaseScheduler epsilonPhaseScheduler;
  private final TrainingRunRepository trainingRunRepository;
  private final MazeRepository mazeRepository;
  private final TrainingSessionContextHolder sessionContextHolder;

  @Autowired
  public DefaultIterativeEpisodeTrainingService(
      SimulationEpisodeOrchestrator episodeOrchestrator,
      TrainingRunRepository trainingRunRepository,
      MazeRepository mazeRepository,
      TrainingSessionContextHolder sessionContextHolder) {
    this(
        episodeOrchestrator,
        TrainingLifecycleEventBus.noop(),
        new EpsilonPhaseScheduler(0.35, 0.20, 0.05),
        trainingRunRepository,
        mazeRepository,
        sessionContextHolder);
  }

  public DefaultIterativeEpisodeTrainingService(
      SimulationEpisodeOrchestrator episodeOrchestrator,
      TrainingLifecycleEventBus trainingLifecycleEventBus) {
    this(
        episodeOrchestrator,
        trainingLifecycleEventBus,
        new EpsilonPhaseScheduler(0.35, 0.20, 0.05),
        null,
        null,
        null);
  }

  DefaultIterativeEpisodeTrainingService(SimulationEpisodeOrchestrator episodeOrchestrator) {
    this(
        episodeOrchestrator,
        TrainingLifecycleEventBus.noop(),
        new EpsilonPhaseScheduler(0.35, 0.20, 0.05),
        null,
        null,
        null);
  }

  public DefaultIterativeEpisodeTrainingService(
      SimulationEpisodeOrchestrator episodeOrchestrator,
      TrainingLifecycleEventBus trainingLifecycleEventBus,
      EpsilonPhaseScheduler epsilonPhaseScheduler) {
    this(episodeOrchestrator, trainingLifecycleEventBus, epsilonPhaseScheduler, null, null, null);
  }

  DefaultIterativeEpisodeTrainingService(
      SimulationEpisodeOrchestrator episodeOrchestrator,
      TrainingLifecycleEventBus trainingLifecycleEventBus,
      EpsilonPhaseScheduler epsilonPhaseScheduler,
      TrainingRunRepository trainingRunRepository,
      MazeRepository mazeRepository,
      TrainingSessionContextHolder sessionContextHolder) {
    this.episodeOrchestrator = episodeOrchestrator;
    this.trainingLifecycleEventBus = trainingLifecycleEventBus;
    this.epsilonPhaseScheduler = epsilonPhaseScheduler;
    this.trainingRunRepository = trainingRunRepository;
    this.mazeRepository = mazeRepository;
    this.sessionContextHolder = sessionContextHolder;
  }

  @Override
  public IterativeTrainingSummary train(
      MazeDefinition maze, int episodes, Duration timeout, BooleanSupplier cancellationRequested) {
    if (episodes <= 0) {
      throw new IllegalArgumentException("episodes must be greater than zero");
    }

    Long resolvedMazeId = resolveMazeId();

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
      SimulationEpisodeResult episode =
          episodeOrchestrator.runEpisode(maze, timeout, epsilonForEpisode);
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
      persistEpisodeResult(resolvedMazeId, episode);
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

  private Long resolveMazeId() {
    if (mazeRepository == null || sessionContextHolder == null) {
      return null;
    }
    String mazeName = sessionContextHolder.mazeName();
    if (mazeName == null || mazeName.isBlank()) {
      return null;
    }
    Optional<MazeEntity> mazeEntity =
        mazeRepository.findAllOrderByDifficulty(true).stream()
            .filter(m -> mazeName.equals(m.name()))
            .findFirst();
    return mazeEntity.map(MazeEntity::id).orElse(null);
  }

  private void persistEpisodeResult(Long mazeId, SimulationEpisodeResult episode) {
    if (trainingRunRepository == null || mazeId == null) {
      return;
    }
    String policyId = sessionContextHolder != null ? sessionContextHolder.policyId() : "unknown";
    TrainingRunEntity entity =
        new TrainingRunEntity(
            null,
            mazeId,
            policyId,
            policyId,
            "v1",
            episode.success(),
            episode.totalSteps(),
            episode.elapsedMillis(),
            episode.totalReward(),
            episode.collisions(),
            episode.uniqueCellsVisited(),
            0,
            episode.netProgress(),
            episode.mazeCoverageRatio(),
            episode.q1Coverage(),
            episode.q2Coverage(),
            episode.q3Coverage(),
            episode.q4Coverage(),
            episode.leftSideCoverage(),
            episode.rightSideCoverage(),
            episode.pathEntropy(),
            episode.debugSnapshotsJson(),
            episode.replayMetadataJson(),
            CellVisitFrequency.encode(episode.cellVisitFrequencies()),
            episode.terminationReason().name(),
            episode.terminationReason() == EpisodeEndReason.TIMEOUT,
            0.0,
            null,
            episode.terminatedAtEpochMillis());
    trainingRunRepository.save(entity);
  }
}
