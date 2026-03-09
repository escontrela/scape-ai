package com.davidpe.scapeai.application;

import com.davidpe.scapeai.simulation.MazeDefinition;
import java.time.Duration;
import java.util.function.BooleanSupplier;
import org.springframework.stereotype.Service;

@Service
public class DefaultIterativeEpisodeTrainingService implements IterativeEpisodeTrainingService {

  private final SimulationEpisodeOrchestrator episodeOrchestrator;

  public DefaultIterativeEpisodeTrainingService(SimulationEpisodeOrchestrator episodeOrchestrator) {
    this.episodeOrchestrator = episodeOrchestrator;
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

    while (completed < episodes) {
      if (cancellationRequested.getAsBoolean()) {
        break;
      }
      SimulationEpisodeResult episode = episodeOrchestrator.runEpisode(maze, timeout);
      completed++;
      if (episode.success()) {
        successes++;
      }
      rewardSum += episode.totalReward();
      collisionsSum += episode.collisions();
    }

    boolean cancelled = completed < episodes;
    double divisor = completed == 0 ? 1.0 : completed;
    return new IterativeTrainingSummary(
        episodes,
        completed,
        cancelled,
        successes / divisor,
        rewardSum / divisor,
        collisionsSum / divisor);
  }
}
