package com.davidpe.scapeai.application;

import com.davidpe.scapeai.simulation.MazeDefinition;
import com.davidpe.scapeai.simulation.GridPosition;
import com.davidpe.scapeai.simulation.MoveDirection;
import com.davidpe.scapeai.simulation.SimulationState;
import java.time.Duration;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
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
    EpisodeExecutionState state =
        EpisodeExecutionState.initial(
            SimulationState.initial(maze.start()), startedAt, deadline, loopWindow);
    runLoop(maze, state, -1);
    return state.toResult(currentTimeMillis.getAsLong());
  }

  public EpisodeCheckpoint runEpisodeUntilCheckpoint(
      MazeDefinition maze, Duration timeout, int maxSteps) {
    long startedAt = currentTimeMillis.getAsLong();
    long deadline = startedAt + timeout.toMillis();
    EpisodeExecutionState state =
        EpisodeExecutionState.initial(
            SimulationState.initial(maze.start()), startedAt, deadline, loopWindow);
    runLoop(maze, state, Math.max(0, maxSteps));
    return state.toCheckpoint(currentTimeMillis.getAsLong());
  }

  public SimulationEpisodeResult resumeEpisode(MazeDefinition maze, EpisodeCheckpoint checkpoint) {
    long resumedAt = currentTimeMillis.getAsLong();
    long deadline = resumedAt + checkpoint.remainingMillis();
    EpisodeExecutionState state =
        EpisodeExecutionState.fromCheckpoint(checkpoint, resumedAt, deadline, loopWindow);
    runLoop(maze, state, -1);
    return state.toResult(currentTimeMillis.getAsLong());
  }

  private void runLoop(MazeDefinition maze, EpisodeExecutionState state, int maxSteps) {
    int executed = 0;
    while (!state.currentState.exitReached()
        && currentTimeMillis.getAsLong() < state.deadline
        && (maxSteps < 0 || executed < maxSteps)) {
      boolean loopDetected = isLoopDetected(state.currentState.agentPosition(), state.positionCounts);
      if (loopDetected) {
        state.loopEvents++;
      }
      int previousDistanceToExit = manhattanDistance(state.currentState.agentPosition(), maze.exit());
      var outcome =
          simulationStepFlow.execute(
              maze, state.currentState, state.previousDirection, state.noProgressStreak, loopDetected);
      state.currentState = outcome.result().state();
      state.previousDirection = outcome.selectedDirection();
      int currentDistanceToExit = manhattanDistance(state.currentState.agentPosition(), maze.exit());
      state.noProgressStreak =
          currentDistanceToExit < previousDistanceToExit ? 0 : state.noProgressStreak + 1;
      rememberPosition(state.currentState.agentPosition(), state.recentPositions, state.positionCounts);
      state.trajectory.add(state.currentState.agentPosition());
      state.totalSteps++;
      state.totalReward += outcome.reward().value();
      if (outcome.result().collision()) {
        state.collisions++;
      }
      outcome.inferenceTrace().ifPresent(state.inferenceTraces::add);
      executed++;
    }
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

  private int manhattanDistance(GridPosition from, GridPosition to) {
    return Math.abs(from.row() - to.row()) + Math.abs(from.col() - to.col());
  }

  private static final class EpisodeExecutionState {

    private final long startedAt;
    private final long deadline;
    private final Deque<GridPosition> recentPositions;
    private final Map<GridPosition, Integer> positionCounts;
    private final List<GridPosition> trajectory;
    private final List<PolicyInferenceTrace> inferenceTraces;
    private SimulationState currentState;
    private MoveDirection previousDirection;
    private int noProgressStreak;
    private int totalSteps;
    private int collisions;
    private int loopEvents;
    private double totalReward;
    private long elapsedBeforeSegment;

    private EpisodeExecutionState(
        long startedAt,
        long deadline,
        SimulationState currentState,
        MoveDirection previousDirection,
        int noProgressStreak,
        int totalSteps,
        int collisions,
        int loopEvents,
        double totalReward,
        long elapsedBeforeSegment,
        Deque<GridPosition> recentPositions,
        Map<GridPosition, Integer> positionCounts,
        List<GridPosition> trajectory,
        List<PolicyInferenceTrace> inferenceTraces) {
      this.startedAt = startedAt;
      this.deadline = deadline;
      this.currentState = currentState;
      this.previousDirection = previousDirection;
      this.noProgressStreak = noProgressStreak;
      this.totalSteps = totalSteps;
      this.collisions = collisions;
      this.loopEvents = loopEvents;
      this.totalReward = totalReward;
      this.elapsedBeforeSegment = elapsedBeforeSegment;
      this.recentPositions = recentPositions;
      this.positionCounts = positionCounts;
      this.trajectory = trajectory;
      this.inferenceTraces = inferenceTraces;
    }

    static EpisodeExecutionState initial(
        SimulationState initialState, long startedAt, long deadline, int loopWindow) {
      Deque<GridPosition> recent = new ArrayDeque<>();
      Map<GridPosition, Integer> counts = new HashMap<>();
      List<GridPosition> trajectory = new ArrayList<>();
      GridPosition position = initialState.agentPosition();
      remember(position, recent, counts, loopWindow);
      trajectory.add(position);
      return new EpisodeExecutionState(
          startedAt,
          deadline,
          initialState,
          null,
          0,
          0,
          0,
          0,
          0.0,
          0L,
          recent,
          counts,
          trajectory,
          new ArrayList<>());
    }

    static EpisodeExecutionState fromCheckpoint(
        EpisodeCheckpoint checkpoint, long resumedAt, long deadline, int loopWindow) {
      Deque<GridPosition> recent = new ArrayDeque<>();
      Map<GridPosition, Integer> counts = new HashMap<>();
      List<GridPosition> sourceRecent = checkpoint.recentPositions();
      if (sourceRecent == null || sourceRecent.isEmpty()) {
        remember(checkpoint.currentState().agentPosition(), recent, counts, loopWindow);
      } else {
        for (GridPosition position : sourceRecent) {
          remember(position, recent, counts, loopWindow);
        }
      }
      List<GridPosition> trajectory = new ArrayList<>(checkpoint.trajectory());
      if (trajectory.isEmpty()) {
        trajectory.add(checkpoint.currentState().agentPosition());
      }
      return new EpisodeExecutionState(
          resumedAt,
          deadline,
          checkpoint.currentState(),
          checkpoint.previousDirection(),
          checkpoint.noProgressStreak(),
          checkpoint.totalSteps(),
          checkpoint.collisions(),
          checkpoint.loopEvents(),
          checkpoint.totalReward(),
          checkpoint.elapsedMillis(),
          recent,
          counts,
          trajectory,
          new ArrayList<>());
    }

    EpisodeCheckpoint toCheckpoint(long currentTime) {
      long elapsed = elapsedMillis(currentTime);
      long remaining = Math.max(0L, deadline - currentTime);
      return new EpisodeCheckpoint(
          currentState,
          previousDirection,
          noProgressStreak,
          totalSteps,
          collisions,
          loopEvents,
          totalReward,
          elapsed,
          remaining,
          List.copyOf(recentPositions),
          List.copyOf(trajectory));
    }

    SimulationEpisodeResult toResult(long currentTime) {
      long elapsed = elapsedMillis(currentTime);
      EpisodeEndReason endReason =
          currentState.exitReached() ? EpisodeEndReason.EXIT_REACHED : EpisodeEndReason.TIMEOUT;
      return new SimulationEpisodeResult(
          currentState.exitReached(),
          totalSteps,
          elapsed,
          endReason,
          totalReward,
          collisions,
          loopEvents,
          List.copyOf(inferenceTraces));
    }

    private long elapsedMillis(long currentTime) {
      return Math.max(0L, elapsedBeforeSegment + currentTime - startedAt);
    }

    private static void remember(
        GridPosition position,
        Deque<GridPosition> recentPositions,
        Map<GridPosition, Integer> positionCounts,
        int loopWindow) {
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
}
