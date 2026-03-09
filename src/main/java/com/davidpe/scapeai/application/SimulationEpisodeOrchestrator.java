package com.davidpe.scapeai.application;

import com.davidpe.scapeai.ai.EpsilonGreedyMovementPolicyDecorator;
import com.davidpe.scapeai.ai.MovementPolicy;
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
import java.util.Random;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SimulationEpisodeOrchestrator {

  private final SimulationStepFlow simulationStepFlow;
  private final Duration defaultTimeout;
  private final int loopWindow;
  private final LongSupplier currentTimeMillis;
  private final java.util.function.LongSupplier entropySupplier;
  private final Supplier<Random> randomSupplier;
  private final double epsilon;
  private final ExperienceTransitionRecorder experienceTransitionRecorder;

  @Autowired
  public SimulationEpisodeOrchestrator(
      SimulationStepFlow simulationStepFlow,
      ExperienceTransitionRecorder experienceTransitionRecorder,
      SessionRandomSource sessionRandomSource,
      @Value("${scape.simulation.episode-timeout:PT5M}") Duration defaultTimeout,
      @Value("${scape.simulation.loop-window:8}") int loopWindow,
      @Value("${scape.ai.epsilon:0.0}") double epsilon) {
    this(
        simulationStepFlow,
        experienceTransitionRecorder,
        defaultTimeout,
        loopWindow,
        System::currentTimeMillis,
        () -> sessionRandomSource.random().nextLong(),
        sessionRandomSource::random,
        epsilon);
  }

  SimulationEpisodeOrchestrator(
      SimulationStepFlow simulationStepFlow,
      ExperienceTransitionRecorder experienceTransitionRecorder,
      Duration defaultTimeout,
      int loopWindow,
      LongSupplier currentTimeMillis) {
    this(
        simulationStepFlow,
        experienceTransitionRecorder,
        defaultTimeout,
        loopWindow,
        currentTimeMillis,
        () -> 0L,
        () -> new Random(0L),
        0.0);
  }

  SimulationEpisodeOrchestrator(
      SimulationStepFlow simulationStepFlow,
      ExperienceTransitionRecorder experienceTransitionRecorder,
      Duration defaultTimeout,
      int loopWindow,
      LongSupplier currentTimeMillis,
      java.util.function.LongSupplier entropySupplier,
      Supplier<Random> randomSupplier,
      double epsilon) {
    this.simulationStepFlow = simulationStepFlow;
    this.defaultTimeout = defaultTimeout;
    this.loopWindow = Math.max(2, loopWindow);
    this.currentTimeMillis = currentTimeMillis;
    this.experienceTransitionRecorder = experienceTransitionRecorder;
    this.entropySupplier = entropySupplier;
    if (epsilon < 0.0 || epsilon > 1.0) {
      throw new IllegalArgumentException("scape.ai.epsilon must be in range [0,1]");
    }
    this.randomSupplier = randomSupplier;
    this.epsilon = epsilon;
  }

  SimulationEpisodeOrchestrator(
      SimulationStepFlow simulationStepFlow, Duration defaultTimeout, LongSupplier currentTimeMillis) {
    this(
        simulationStepFlow,
        ExperienceTransitionRecorder.noop(),
        defaultTimeout,
        8,
        currentTimeMillis);
  }

  SimulationEpisodeOrchestrator(
      SimulationStepFlow simulationStepFlow,
      Duration defaultTimeout,
      int loopWindow,
      LongSupplier currentTimeMillis) {
    this(
        simulationStepFlow,
        ExperienceTransitionRecorder.noop(),
        defaultTimeout,
        loopWindow,
        currentTimeMillis);
  }

  public SimulationEpisodeResult runEpisode(MazeDefinition maze) {
    return runEpisode(maze, defaultTimeout);
  }

  public SimulationEpisodeResult runEpisode(MazeDefinition maze, Duration timeout) {
    entropySupplier.getAsLong();
    long startedAt = currentTimeMillis.getAsLong();
    long deadline = startedAt + timeout.toMillis();
    EpsilonGreedyMovementPolicyDecorator policy = buildEpisodePolicy();
    EpisodeExecutionState state =
        EpisodeExecutionState.initial(
            SimulationState.initial(maze.start()), maze.exit(), startedAt, deadline, loopWindow);
    runLoop(maze, state, policy, -1);
    return state.toResult(currentTimeMillis.getAsLong(), maze);
  }

  public EpisodeCheckpoint runEpisodeUntilCheckpoint(
      MazeDefinition maze, Duration timeout, int maxSteps) {
    entropySupplier.getAsLong();
    long startedAt = currentTimeMillis.getAsLong();
    long deadline = startedAt + timeout.toMillis();
    EpsilonGreedyMovementPolicyDecorator policy = buildEpisodePolicy();
    EpisodeExecutionState state =
        EpisodeExecutionState.initial(
            SimulationState.initial(maze.start()), maze.exit(), startedAt, deadline, loopWindow);
    runLoop(maze, state, policy, Math.max(0, maxSteps));
    return state.toCheckpoint(currentTimeMillis.getAsLong());
  }

  public SimulationEpisodeResult resumeEpisode(MazeDefinition maze, EpisodeCheckpoint checkpoint) {
    entropySupplier.getAsLong();
    long resumedAt = currentTimeMillis.getAsLong();
    long deadline = resumedAt + checkpoint.remainingMillis();
    EpsilonGreedyMovementPolicyDecorator policy = buildEpisodePolicy();
    EpisodeExecutionState state =
        EpisodeExecutionState.fromCheckpoint(checkpoint, maze.exit(), resumedAt, deadline, loopWindow);
    runLoop(maze, state, policy, -1);
    return state.toResult(currentTimeMillis.getAsLong(), maze);
  }

  private EpsilonGreedyMovementPolicyDecorator buildEpisodePolicy() {
    MovementPolicy basePolicy = simulationStepFlow.activePolicy();
    return new EpsilonGreedyMovementPolicyDecorator(basePolicy, epsilon, randomSupplier);
  }

  private void runLoop(
      MazeDefinition maze,
      EpisodeExecutionState state,
      EpsilonGreedyMovementPolicyDecorator policy,
      int maxSteps) {
    int executed = 0;
    while (!state.currentState.exitReached()
        && currentTimeMillis.getAsLong() < state.deadline
        && (maxSteps < 0 || executed < maxSteps)) {
      boolean loopDetected = isLoopDetected(state.currentState.agentPosition(), state.positionCounts);
      if (loopDetected) {
        state.loopEvents++;
      }
      int previousDistanceToExit = manhattanDistance(state.currentState.agentPosition(), maze.exit());
      SimulationState previousState = state.currentState;
      var outcome =
          simulationStepFlow.execute(
              maze,
              state.currentState,
              state.previousDirection,
              state.noProgressStreak,
              loopDetected,
              policy);
      state.currentState = outcome.result().state();
      state.previousDirection = outcome.selectedDirection();
      if (policy.lastDecisionExploration()) {
        state.explorationDecisions++;
      } else {
        state.exploitationDecisions++;
      }
      int currentDistanceToExit = manhattanDistance(state.currentState.agentPosition(), maze.exit());
      if (currentDistanceToExit < previousDistanceToExit) {
        state.improvementDistance += previousDistanceToExit - currentDistanceToExit;
      }
      state.noProgressStreak =
          currentDistanceToExit < previousDistanceToExit ? 0 : state.noProgressStreak + 1;
      rememberPosition(state.currentState.agentPosition(), state.recentPositions, state.positionCounts);
      state.trajectory.add(state.currentState.agentPosition());
      state.totalSteps++;
      state.totalReward += outcome.reward().value();
      if (outcome.result().collision()) {
        state.collisions++;
      }
      experienceTransitionRecorder.recordTransition(
          previousState, outcome.selectedDirection(), outcome.reward().value(), state.currentState);
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
    private final GridPosition mazeExit;
    private final int initialDistanceToExit;
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
    private int explorationDecisions;
    private int exploitationDecisions;
    private double improvementDistance;
    private double totalReward;
    private long elapsedBeforeSegment;

    private EpisodeExecutionState(
        long startedAt,
        long deadline,
        GridPosition mazeExit,
        int initialDistanceToExit,
        SimulationState currentState,
        MoveDirection previousDirection,
        int noProgressStreak,
        int totalSteps,
        int collisions,
        int loopEvents,
        double improvementDistance,
        double totalReward,
        long elapsedBeforeSegment,
        Deque<GridPosition> recentPositions,
        Map<GridPosition, Integer> positionCounts,
        List<GridPosition> trajectory,
        List<PolicyInferenceTrace> inferenceTraces) {
      this.startedAt = startedAt;
      this.deadline = deadline;
      this.mazeExit = mazeExit;
      this.initialDistanceToExit = initialDistanceToExit;
      this.currentState = currentState;
      this.previousDirection = previousDirection;
      this.noProgressStreak = noProgressStreak;
      this.totalSteps = totalSteps;
      this.collisions = collisions;
      this.loopEvents = loopEvents;
      this.improvementDistance = improvementDistance;
      this.totalReward = totalReward;
      this.elapsedBeforeSegment = elapsedBeforeSegment;
      this.recentPositions = recentPositions;
      this.positionCounts = positionCounts;
      this.trajectory = trajectory;
      this.inferenceTraces = inferenceTraces;
    }

    static EpisodeExecutionState initial(
        SimulationState initialState, GridPosition mazeExit, long startedAt, long deadline, int loopWindow) {
      Deque<GridPosition> recent = new ArrayDeque<>();
      Map<GridPosition, Integer> counts = new HashMap<>();
      List<GridPosition> trajectory = new ArrayList<>();
      GridPosition position = initialState.agentPosition();
      remember(position, recent, counts, loopWindow);
      trajectory.add(position);
      int initialDistance = distanceToExit(initialState.agentPosition(), mazeExit);
      return new EpisodeExecutionState(
          startedAt,
          deadline,
          mazeExit,
          initialDistance,
          initialState,
          null,
          0,
          0,
          0,
          0,
          0.0,
          0.0,
          0L,
          recent,
          counts,
          trajectory,
          new ArrayList<>());
    }

    static EpisodeExecutionState fromCheckpoint(
        EpisodeCheckpoint checkpoint, GridPosition mazeExit, long resumedAt, long deadline, int loopWindow) {
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
      GridPosition initialPosition =
          trajectory.isEmpty() ? checkpoint.currentState().agentPosition() : trajectory.get(0);
      int initialDistance = distanceToExit(initialPosition, mazeExit);
      return new EpisodeExecutionState(
          resumedAt,
          deadline,
          mazeExit,
          initialDistance,
          checkpoint.currentState(),
          checkpoint.previousDirection(),
          checkpoint.noProgressStreak(),
          checkpoint.totalSteps(),
          checkpoint.collisions(),
          checkpoint.loopEvents(),
          0.0,
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

    SimulationEpisodeResult toResult(long currentTime, MazeDefinition maze) {
      long elapsed = elapsedMillis(currentTime);
      EpisodeEndReason endReason =
          currentState.exitReached() ? EpisodeEndReason.EXIT_REACHED : EpisodeEndReason.TIMEOUT;
      int finalDistanceToExit = distanceToExit(currentState.agentPosition(), mazeExit);
      double netProgress = (initialDistanceToExit - finalDistanceToExit) + improvementDistance;
      MazeQuadrantCoverage coverage = MazeQuadrantCoverage.from(maze, currentState.visitedCells());
      return new SimulationEpisodeResult(
          currentState.exitReached(),
          totalSteps,
          elapsed,
          endReason,
          totalReward,
          collisions,
          loopEvents,
          netProgress,
          coverage.q1Coverage(),
          coverage.q2Coverage(),
          coverage.q3Coverage(),
          coverage.q4Coverage(),
          coverage.leftSideCoverage(),
          coverage.rightSideCoverage(),
          explorationDecisions,
          exploitationDecisions,
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

    private static int distanceToExit(GridPosition position, GridPosition mazeExit) {
      return Math.abs(position.row() - mazeExit.row()) + Math.abs(position.col() - mazeExit.col());
    }
  }
}
