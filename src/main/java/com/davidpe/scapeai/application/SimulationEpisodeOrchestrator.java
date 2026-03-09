package com.davidpe.scapeai.application;

import com.davidpe.scapeai.simulation.MazeDefinition;
import com.davidpe.scapeai.simulation.GridPosition;
import com.davidpe.scapeai.simulation.SimulationState;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SimulationEpisodeOrchestrator {

  private final SimulationStepFlow simulationStepFlow;
  private final Duration defaultTimeout;
  private final int loopWindow;
  private final LongSupplier currentTimeMillis;

  @Autowired
  public SimulationEpisodeOrchestrator(
      SimulationStepFlow simulationStepFlow,
      @Value("${scape.simulation.episode-timeout:PT5M}") Duration defaultTimeout,
      @Value("${scape.simulation.loop-window:8}") int loopWindow) {
    this(simulationStepFlow, defaultTimeout, loopWindow, System::currentTimeMillis);
  }

  SimulationEpisodeOrchestrator(
      SimulationStepFlow simulationStepFlow,
      Duration defaultTimeout,
      int loopWindow,
      LongSupplier currentTimeMillis) {
    this.simulationStepFlow = simulationStepFlow;
    this.defaultTimeout = defaultTimeout;
    this.loopWindow = Math.max(2, loopWindow);
    this.currentTimeMillis = currentTimeMillis;
  }

  SimulationEpisodeOrchestrator(
      SimulationStepFlow simulationStepFlow, Duration defaultTimeout, LongSupplier currentTimeMillis) {
    this(simulationStepFlow, defaultTimeout, 8, currentTimeMillis);
  }

  public SimulationEpisodeResult runEpisode(MazeDefinition maze) {
    return runEpisode(maze, defaultTimeout);
  }

  public SimulationEpisodeResult runEpisode(MazeDefinition maze, Duration timeout) {
    long startedAt = currentTimeMillis.getAsLong();
    long deadline = startedAt + timeout.toMillis();
    int totalSteps = 0;
    int collisions = 0;
    int loopEvents = 0;
    double totalReward = 0.0;
    SimulationState currentState = SimulationState.initial(maze.start());
    Deque<GridPosition> recentPositions = new ArrayDeque<>();
    Map<GridPosition, Integer> positionCounts = new HashMap<>();
    rememberPosition(currentState.agentPosition(), recentPositions, positionCounts);

    while (!currentState.exitReached() && currentTimeMillis.getAsLong() < deadline) {
      boolean loopDetected = isLoopDetected(currentState.agentPosition(), positionCounts);
      if (loopDetected) {
        loopEvents++;
      }
      var outcome = simulationStepFlow.execute(maze, currentState, loopDetected);
      currentState = outcome.result().state();
      rememberPosition(currentState.agentPosition(), recentPositions, positionCounts);
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
        currentState.exitReached(), totalSteps, elapsed, endReason, totalReward, collisions, loopEvents);
  }

  private boolean isLoopDetected(
      GridPosition position, Map<GridPosition, Integer> positionCounts) {
    return positionCounts.getOrDefault(position, 0) > 1;
  }

  private void rememberPosition(
      GridPosition position, Deque<GridPosition> recentPositions, Map<GridPosition, Integer> positionCounts) {
    recentPositions.addLast(position);
    positionCounts.merge(position, 1, Integer::sum);
    while (recentPositions.size() > loopWindow) {
      GridPosition removed = recentPositions.removeFirst();
      int remaining = positionCounts.getOrDefault(removed, 1) - 1;
      if (remaining <= 0) {
        positionCounts.remove(removed);
      } else {
        positionCounts.put(removed, remaining);
      }
    }
  }
}
