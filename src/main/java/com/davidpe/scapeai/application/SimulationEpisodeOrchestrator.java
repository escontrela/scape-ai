package com.davidpe.scapeai.application;

import com.davidpe.scapeai.ai.EpsilonGreedyMovementPolicyDecorator;
import com.davidpe.scapeai.ai.MovementPolicy;
import com.davidpe.scapeai.simulation.GridPosition;
import com.davidpe.scapeai.simulation.MazeDefinition;
import com.davidpe.scapeai.simulation.MoveDirection;
import com.davidpe.scapeai.simulation.SimulationState;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SimulationEpisodeOrchestrator {

  /** Keep at most the last N positions in the trajectory to bound memory. */
  private static final int MAX_TRAJECTORY_SIZE = 10_000;

  /** Keep at most the last N inference traces per episode. */
  private static final int MAX_INFERENCE_TRACES = 5_000;

  private final SimulationStepFlow simulationStepFlow;
  private final Duration defaultTimeout;
  private final int loopWindow;
  private final LongSupplier monotonicTimeMillis;
  private final java.util.function.LongSupplier entropySupplier;
  private final int deadEndNoProgressLimit;
  private final double epsilon;
  private final ExperienceTransitionRecorder experienceTransitionRecorder;

  @Autowired
  public SimulationEpisodeOrchestrator(
      SimulationStepFlow simulationStepFlow,
      ExperienceTransitionRecorder experienceTransitionRecorder,
      SessionRandomSource sessionRandomSource,
      @Value("${scape.simulation.episode-timeout:PT5M}") Duration defaultTimeout,
      @Value("${scape.simulation.loop-window:8}") int loopWindow,
      @Value("${scape.simulation.dead-end-no-progress-limit:24}") int deadEndNoProgressLimit,
      @Value("${scape.ai.epsilon:0.0}") double epsilon) {
    this(
        simulationStepFlow,
        experienceTransitionRecorder,
        defaultTimeout,
        loopWindow,
        () -> System.nanoTime() / 1_000_000L,
        () -> sessionRandomSource.random().nextLong(),
        sessionRandomSource::random,
        deadEndNoProgressLimit,
        epsilon);
  }

  SimulationEpisodeOrchestrator(
      SimulationStepFlow simulationStepFlow,
      ExperienceTransitionRecorder experienceTransitionRecorder,
      Duration defaultTimeout,
      int loopWindow,
      LongSupplier monotonicTimeMillis) {
    this(
        simulationStepFlow,
        experienceTransitionRecorder,
        defaultTimeout,
        loopWindow,
        monotonicTimeMillis,
        () -> 0L,
        () -> new Random(0L),
        24,
        0.0);
  }

  SimulationEpisodeOrchestrator(
      SimulationStepFlow simulationStepFlow,
      ExperienceTransitionRecorder experienceTransitionRecorder,
      Duration defaultTimeout,
      int loopWindow,
      LongSupplier monotonicTimeMillis,
      java.util.function.LongSupplier entropySupplier,
      java.util.function.Supplier<Random> randomSupplier,
      double epsilon) {
    this(
        simulationStepFlow,
        experienceTransitionRecorder,
        defaultTimeout,
        loopWindow,
        monotonicTimeMillis,
        entropySupplier,
        randomSupplier,
        24,
        epsilon);
  }

  SimulationEpisodeOrchestrator(
      SimulationStepFlow simulationStepFlow,
      ExperienceTransitionRecorder experienceTransitionRecorder,
      Duration defaultTimeout,
      int loopWindow,
      LongSupplier monotonicTimeMillis,
      java.util.function.LongSupplier entropySupplier,
      java.util.function.Supplier<Random> randomSupplier,
      int deadEndNoProgressLimit,
      double epsilon) {
    this.simulationStepFlow = simulationStepFlow;
    this.defaultTimeout = defaultTimeout;
    this.loopWindow = Math.max(2, loopWindow);
    this.monotonicTimeMillis = monotonicTimeMillis;
    this.experienceTransitionRecorder = experienceTransitionRecorder;
    this.entropySupplier = entropySupplier;
    this.deadEndNoProgressLimit = Math.max(0, deadEndNoProgressLimit);
    if (epsilon < 0.0 || epsilon > 1.0) {
      throw new IllegalArgumentException("scape.ai.epsilon must be in range [0,1]");
    }
    this.epsilon = epsilon;
  }

  SimulationEpisodeOrchestrator(
      SimulationStepFlow simulationStepFlow,
      Duration defaultTimeout,
      LongSupplier monotonicTimeMillis) {
    this(
        simulationStepFlow,
        ExperienceTransitionRecorder.noop(),
        defaultTimeout,
        8,
        monotonicTimeMillis);
  }

  SimulationEpisodeOrchestrator(
      SimulationStepFlow simulationStepFlow,
      Duration defaultTimeout,
      int loopWindow,
      LongSupplier monotonicTimeMillis) {
    this(
        simulationStepFlow,
        ExperienceTransitionRecorder.noop(),
        defaultTimeout,
        loopWindow,
        monotonicTimeMillis);
  }

  public SimulationEpisodeResult runEpisode(MazeDefinition maze) {
    return runEpisode(maze, defaultTimeout);
  }

  public SimulationEpisodeResult runEpisode(MazeDefinition maze, Duration timeout) {
    return runEpisode(maze, timeout, null);
  }

  public SimulationEpisodeResult runEpisode(
      MazeDefinition maze, Duration timeout, Double epsilonOverride) {
    long effectiveSeed = entropySupplier.getAsLong();
    long startedAt = monotonicTimeMillis.getAsLong();
    long deadline = startedAt + timeout.toMillis();
    String policyDescriptor = simulationStepFlow.activePolicy().getClass().getSimpleName();
    EpsilonGreedyMovementPolicyDecorator policy =
        buildEpisodePolicy(effectiveSeed, resolveEpsilon(epsilonOverride));
    EpisodeExecutionState state =
        EpisodeExecutionState.initial(
            SimulationState.initial(maze.start()),
            maze.exit(),
            startedAt,
            deadline,
            loopWindow,
            deadEndNoProgressLimit,
            effectiveSeed,
            policyDescriptor);
    runLoop(maze, state, policy, -1);
    return state.toResult(monotonicTimeMillis.getAsLong(), maze, timeout.toMillis());
  }

  public EpisodeCheckpoint runEpisodeUntilCheckpoint(
      MazeDefinition maze, Duration timeout, int maxSteps) {
    long effectiveSeed = entropySupplier.getAsLong();
    long startedAt = monotonicTimeMillis.getAsLong();
    long deadline = startedAt + timeout.toMillis();
    String policyDescriptor = simulationStepFlow.activePolicy().getClass().getSimpleName();
    EpsilonGreedyMovementPolicyDecorator policy = buildEpisodePolicy(effectiveSeed, epsilon);
    EpisodeExecutionState state =
        EpisodeExecutionState.initial(
            SimulationState.initial(maze.start()),
            maze.exit(),
            startedAt,
            deadline,
            loopWindow,
            deadEndNoProgressLimit,
            effectiveSeed,
            policyDescriptor);
    runLoop(maze, state, policy, Math.max(0, maxSteps));
    return state.toCheckpoint(monotonicTimeMillis.getAsLong());
  }

  public SimulationEpisodeResult resumeEpisode(MazeDefinition maze, EpisodeCheckpoint checkpoint) {
    long effectiveSeed = entropySupplier.getAsLong();
    long resumedAt = monotonicTimeMillis.getAsLong();
    long deadline = resumedAt + checkpoint.remainingMillis();
    String policyDescriptor = simulationStepFlow.activePolicy().getClass().getSimpleName();
    EpsilonGreedyMovementPolicyDecorator policy = buildEpisodePolicy(effectiveSeed, epsilon);
    EpisodeExecutionState state =
        EpisodeExecutionState.fromCheckpoint(
            checkpoint,
            maze.exit(),
            resumedAt,
            deadline,
            loopWindow,
            deadEndNoProgressLimit,
            effectiveSeed,
            policyDescriptor);
    runLoop(maze, state, policy, -1);
    return state.toResult(
        monotonicTimeMillis.getAsLong(),
        maze,
        Math.max(0L, checkpoint.elapsedMillis() + checkpoint.remainingMillis()));
  }

  private EpsilonGreedyMovementPolicyDecorator buildEpisodePolicy(
      long effectiveSeed, double epsilonValue) {
    MovementPolicy basePolicy = simulationStepFlow.activePolicy();
    return new EpsilonGreedyMovementPolicyDecorator(
        basePolicy, epsilonValue, () -> new Random(effectiveSeed));
  }

  private double resolveEpsilon(Double epsilonOverride) {
    if (epsilonOverride == null) {
      return epsilon;
    }
    return Math.max(0.0, Math.min(1.0, epsilonOverride));
  }

  private void runLoop(
      MazeDefinition maze,
      EpisodeExecutionState state,
      EpsilonGreedyMovementPolicyDecorator policy,
      int maxSteps) {
    int executed = 0;
    while (!state.currentState.exitReached() && (maxSteps < 0 || executed < maxSteps)) {
      long tickNow = monotonicTimeMillis.getAsLong();
      if (tickNow >= state.deadline) {
        break;
      }
      state.captureTimedMilestones(tickNow);
      boolean loopDetected =
          isLoopDetected(state.currentState.agentPosition(), state.positionCounts);
      if (loopDetected) {
        state.loopEvents++;
      }
      int previousDistanceToExit =
          manhattanDistance(state.currentState.agentPosition(), maze.exit());
      SimulationState previousState = state.currentState;
      var outcome =
          simulationStepFlow.execute(
              maze,
              state.currentState,
              List.copyOf(state.recentPositions),
              state.transitionCounts,
              state.previousDirection,
              state.noProgressStreak,
              loopDetected,
              policy);
      state.currentState = outcome.result().state();
      state.previousDirection = outcome.selectedDirection();
      state.movementCounts.merge(outcome.selectedDirection(), 1, Integer::sum);
      if (policy.lastDecisionExploration()) {
        state.explorationDecisions++;
      } else {
        state.exploitationDecisions++;
      }
      int currentDistanceToExit =
          manhattanDistance(state.currentState.agentPosition(), maze.exit());
      boolean discoveredNewCell =
          state.currentState.visitedCells().size() > previousState.visitedCells().size();
      if (currentDistanceToExit < previousDistanceToExit) {
        state.improvementDistance += previousDistanceToExit - currentDistanceToExit;
      }
      state.noProgressStreak =
          (currentDistanceToExit < previousDistanceToExit || discoveredNewCell)
              ? 0
              : state.noProgressStreak + 1;
      state.deadEndStreak =
          (currentDistanceToExit < previousDistanceToExit || discoveredNewCell)
              ? 0
              : state.deadEndStreak + 1;
      rememberPosition(
          state.currentState.agentPosition(), state.recentPositions, state.positionCounts);
      state.trajectory.add(state.currentState.agentPosition());
      if (state.trajectory.size() > MAX_TRAJECTORY_SIZE) {
        state.trajectory.subList(0, state.trajectory.size() - MAX_TRAJECTORY_SIZE).clear();
      }
      state.totalSteps++;
      state.totalReward += outcome.reward().value();
      if (outcome.result().collision()) {
        state.collisions++;
      }
      experienceTransitionRecorder.recordTransition(
          previousState, outcome.selectedDirection(), outcome.reward().value(), state.currentState);
      outcome
          .inferenceTrace()
          .ifPresent(
              trace -> {
                state.inferenceTraces.add(trace);
                if (state.inferenceTraces.size() > MAX_INFERENCE_TRACES) {
                  state
                      .inferenceTraces
                      .subList(0, state.inferenceTraces.size() - MAX_INFERENCE_TRACES)
                      .clear();
                }
              });
      if (deadEndNoProgressLimit > 0 && state.deadEndStreak >= deadEndNoProgressLimit) {
        state.deadEndReached = true;
        break;
      }
      executed++;
    }
  }

  private boolean isLoopDetected(GridPosition position, Map<GridPosition, Integer> positionCounts) {
    return positionCounts.getOrDefault(position, 0) > 1;
  }

  private void rememberPosition(
      GridPosition position,
      Deque<GridPosition> recentPositions,
      Map<GridPosition, Integer> positionCounts) {
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

    private static final long PRE_TIMEOUT_WINDOW_MILLIS = 1_000L;
    private final long startedAtMonotonic;
    private final long deadline;
    private final long effectiveSeed;
    private final GridPosition mazeExit;
    private final String policyDescriptor;
    private final int deadEndNoProgressLimit;
    private final int initialDistanceToExit;
    private final Deque<GridPosition> recentPositions;
    private final Map<GridPosition, Integer> positionCounts;
    private final List<GridPosition> trajectory;
    private final List<PolicyInferenceTrace> inferenceTraces;
    private final List<EpisodeDebugSnapshot> debugSnapshots;
    private final Map<String, Integer> transitionCounts;
    private final Map<MoveDirection, Integer> movementCounts;
    private SimulationState currentState;
    private MoveDirection previousDirection;
    private int noProgressStreak;
    private int totalSteps;
    private int collisions;
    private int loopEvents;
    private int explorationDecisions;
    private int exploitationDecisions;
    private int deadEndStreak;
    private double improvementDistance;
    private double totalReward;
    private long elapsedBeforeSegment;
    private boolean midpointCaptured;
    private boolean preTimeoutCaptured;
    private boolean finalCaptured;
    private boolean deadEndReached;

    private EpisodeExecutionState(
        long startedAtMonotonic,
        long deadline,
        long effectiveSeed,
        GridPosition mazeExit,
        String policyDescriptor,
        int deadEndNoProgressLimit,
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
        List<PolicyInferenceTrace> inferenceTraces,
        List<EpisodeDebugSnapshot> debugSnapshots,
        Map<String, Integer> transitionCounts,
        Map<MoveDirection, Integer> movementCounts) {
      this.startedAtMonotonic = startedAtMonotonic;
      this.deadline = deadline;
      this.effectiveSeed = effectiveSeed;
      this.mazeExit = mazeExit;
      this.policyDescriptor = policyDescriptor;
      this.deadEndNoProgressLimit = Math.max(0, deadEndNoProgressLimit);
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
      this.debugSnapshots = debugSnapshots;
      this.transitionCounts = transitionCounts;
      this.movementCounts = movementCounts;
    }

    static EpisodeExecutionState initial(
        SimulationState initialState,
        GridPosition mazeExit,
        long startedAt,
        long deadline,
        int loopWindow,
        int deadEndNoProgressLimit,
        long effectiveSeed,
        String policyDescriptor) {
      Deque<GridPosition> recent = new ArrayDeque<>();
      Map<GridPosition, Integer> counts = new HashMap<>();
      List<GridPosition> trajectory = new ArrayList<>();
      GridPosition position = initialState.agentPosition();
      remember(position, recent, counts, loopWindow);
      trajectory.add(position);
      int initialDistance = distanceToExit(initialState.agentPosition(), mazeExit);
      EpisodeExecutionState state =
          new EpisodeExecutionState(
              startedAt,
              deadline,
              effectiveSeed,
              mazeExit,
              policyDescriptor,
              deadEndNoProgressLimit,
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
              new ArrayList<>(),
              new ArrayList<>(),
              new HashMap<>(),
              new EnumMap<>(MoveDirection.class));
      state.debugSnapshots.add(state.snapshot("START", startedAt));
      return state;
    }

    static EpisodeExecutionState fromCheckpoint(
        EpisodeCheckpoint checkpoint,
        GridPosition mazeExit,
        long resumedAt,
        long deadline,
        int loopWindow,
        int deadEndNoProgressLimit,
        long effectiveSeed,
        String policyDescriptor) {
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
      EpisodeExecutionState state =
          new EpisodeExecutionState(
              resumedAt,
              deadline,
              effectiveSeed,
              mazeExit,
              policyDescriptor,
              deadEndNoProgressLimit,
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
              new ArrayList<>(),
              new ArrayList<>(),
              new HashMap<>(),
              new EnumMap<>(MoveDirection.class));
      state.debugSnapshots.add(state.snapshot("START", resumedAt));
      return state;
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

    void captureTimedMilestones(long currentTime) {
      long elapsed = elapsedMillis(currentTime);
      long timeoutBudget = timeoutBudgetMillis();
      if (!midpointCaptured && timeoutBudget > 0 && elapsed >= timeoutBudget / 2L) {
        debugSnapshots.add(snapshot("MIDPOINT", currentTime));
        midpointCaptured = true;
      }
      long remaining = Math.max(0L, deadline - currentTime);
      if (!preTimeoutCaptured && remaining <= Math.min(PRE_TIMEOUT_WINDOW_MILLIS, timeoutBudget)) {
        debugSnapshots.add(snapshot("PRE_TIMEOUT", currentTime));
        preTimeoutCaptured = true;
      }
    }

    SimulationEpisodeResult toResult(
        long currentTimeMonotonic, MazeDefinition maze, long timeoutBudgetOverride) {
      long elapsed = elapsedMillis(currentTimeMonotonic);
      boolean timeoutReached = !currentState.exitReached() && currentTimeMonotonic >= deadline;
      boolean reachedDeadEnd =
          deadEndReached || (deadEndNoProgressLimit > 0 && deadEndStreak >= deadEndNoProgressLimit);
      if (reachedDeadEnd) {
        timeoutReached = false;
      }
      EpisodeEndReason terminationReason =
          EpisodeTerminationResolver.resolve(
              currentState.exitReached(), timeoutReached, reachedDeadEnd, false);
      long timeoutBudget =
          timeoutBudgetOverride > 0 ? timeoutBudgetOverride : timeoutBudgetMillis();
      if (terminationReason == EpisodeEndReason.TIMEOUT) {
        elapsed = Math.min(elapsed, timeoutBudget);
      }
      long terminatedAt =
          terminationReason == EpisodeEndReason.TIMEOUT ? deadline : currentTimeMonotonic;
      int finalDistanceToExit = distanceToExit(currentState.agentPosition(), mazeExit);
      double netProgress = (initialDistanceToExit - finalDistanceToExit) + improvementDistance;
      MazeQuadrantCoverage coverage = MazeQuadrantCoverage.from(maze, currentState.visitedCells());
      double pathEntropy = calculatePathEntropy();
      if (!finalCaptured) {
        debugSnapshots.add(snapshot("FINAL", currentTimeMonotonic));
        finalCaptured = true;
      }
      EpisodeReplayMetadata replayMetadata =
          new EpisodeReplayMetadata(
              EpisodeReplayMetadata.CONTRACT_VERSION,
              "rows="
                  + maze.rows()
                  + ",cols="
                  + maze.cols()
                  + ",exit="
                  + maze.exit().row()
                  + ":"
                  + maze.exit().col(),
              policyDescriptor,
              effectiveSeed,
              terminationReason,
              coverage.mazeCoverageRatio(),
              loopEvents,
              timeoutBudget,
              totalSteps,
              currentState.agentPosition(),
              terminationReason);
      return new SimulationEpisodeResult(
          currentState.exitReached(),
          totalSteps,
          elapsed,
          terminationReason,
          terminatedAt,
          totalReward,
          collisions,
          loopEvents,
          currentState.visitedCells().size(),
          netProgress,
          coverage.mazeCoverageRatio(),
          coverage.q1Coverage(),
          coverage.q2Coverage(),
          coverage.q3Coverage(),
          coverage.q4Coverage(),
          coverage.leftSideCoverage(),
          coverage.rightSideCoverage(),
          pathEntropy,
          effectiveSeed,
          timeoutBudget,
          explorationDecisions,
          exploitationDecisions,
          buildCellVisitFrequencies(),
          List.copyOf(debugSnapshots),
          replayMetadata,
          List.copyOf(inferenceTraces));
    }

    private List<CellVisitFrequency> buildCellVisitFrequencies() {
      Map<GridPosition, Integer> visitCounts = new HashMap<>();
      for (GridPosition position : trajectory) {
        visitCounts.merge(position, 1, Integer::sum);
      }
      return visitCounts.entrySet().stream()
          .map(entry -> new CellVisitFrequency(entry.getKey(), entry.getValue()))
          .sorted(
              java.util.Comparator.comparingInt(
                      (CellVisitFrequency frequency) -> frequency.position().row())
                  .thenComparingInt(frequency -> frequency.position().col()))
          .collect(Collectors.toList());
    }

    private long elapsedMillis(long currentTime) {
      return Math.max(0L, elapsedBeforeSegment + currentTime - startedAtMonotonic);
    }

    private long timeoutBudgetMillis() {
      return Math.max(0L, elapsedBeforeSegment + (deadline - startedAtMonotonic));
    }

    private EpisodeDebugSnapshot snapshot(String milestone, long currentTime) {
      long elapsed = elapsedMillis(currentTime);
      long remaining = Math.max(0L, deadline - currentTime);
      return new EpisodeDebugSnapshot(
          milestone,
          currentState.agentPosition(),
          totalSteps,
          totalReward,
          collisions,
          loopEvents,
          currentState.visitedCells().size(),
          elapsed,
          remaining,
          effectiveSeed);
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

    private double calculatePathEntropy() {
      double moveEntropy =
          entropy(movementCounts.values().stream().mapToInt(Integer::intValue).toArray());
      int[] visitFrequencies =
          positionCounts.values().stream().mapToInt(Integer::intValue).toArray();
      double visitEntropy = entropy(visitFrequencies);
      return (moveEntropy + visitEntropy) / 2.0;
    }

    private double entropy(int[] frequencies) {
      int total = 0;
      for (int frequency : frequencies) {
        total += Math.max(0, frequency);
      }
      if (total <= 0) {
        return 0.0;
      }
      double entropy = 0.0;
      for (int frequency : frequencies) {
        if (frequency <= 0) {
          continue;
        }
        double probability = (double) frequency / (double) total;
        entropy -= probability * (Math.log(probability) / Math.log(2.0));
      }
      return entropy;
    }
  }
}
