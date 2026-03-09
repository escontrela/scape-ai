package com.davidpe.scapeai.application;

import com.davidpe.scapeai.simulation.MazeDefinition;
import com.davidpe.scapeai.simulation.SimulationState;
import java.time.Duration;
import java.util.function.LongSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SimulationEpisodeOrchestrator {

  private final SimulationStepFlow simulationStepFlow;
  private final Duration defaultTimeout;
  private final LongSupplier currentTimeMillis;

  @Autowired
  public SimulationEpisodeOrchestrator(
      SimulationStepFlow simulationStepFlow,
      @Value("${scape.simulation.episode-timeout:PT5M}") Duration defaultTimeout) {
    this(simulationStepFlow, defaultTimeout, System::currentTimeMillis);
  }

  SimulationEpisodeOrchestrator(
      SimulationStepFlow simulationStepFlow,
      Duration defaultTimeout,
      LongSupplier currentTimeMillis) {
    this.simulationStepFlow = simulationStepFlow;
    this.defaultTimeout = defaultTimeout;
    this.currentTimeMillis = currentTimeMillis;
  }

  public SimulationEpisodeResult runEpisode(MazeDefinition maze) {
    return runEpisode(maze, defaultTimeout);
  }

  public SimulationEpisodeResult runEpisode(MazeDefinition maze, Duration timeout) {
    long startedAt = currentTimeMillis.getAsLong();
    long deadline = startedAt + timeout.toMillis();
    int totalSteps = 0;
    int collisions = 0;
    double totalReward = 0.0;
    SimulationState currentState = SimulationState.initial(maze.start());

    while (!currentState.exitReached() && currentTimeMillis.getAsLong() < deadline) {
      var outcome = simulationStepFlow.execute(maze, currentState);
      currentState = outcome.result().state();
      totalSteps++;
      totalReward += outcome.reward().value();
      if (outcome.result().collision()) {
        collisions++;
      }
    }

    long elapsed = Math.max(0, currentTimeMillis.getAsLong() - startedAt);
    EpisodeEndReason endReason =
        currentState.exitReached() ? EpisodeEndReason.EXIT_REACHED : EpisodeEndReason.TIMEOUT;

    return new SimulationEpisodeResult(
        currentState.exitReached(), totalSteps, elapsed, endReason, totalReward, collisions);
  }
}
