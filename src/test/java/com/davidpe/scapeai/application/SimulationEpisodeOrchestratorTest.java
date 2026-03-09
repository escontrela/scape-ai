package com.davidpe.scapeai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.scapeai.ai.MovementPolicy;
import com.davidpe.scapeai.ai.RewardAssessment;
import com.davidpe.scapeai.ai.RewardEvaluator;
import com.davidpe.scapeai.ai.RewardSignal;
import com.davidpe.scapeai.ai.SimpleMovementPolicy;
import com.davidpe.scapeai.ai.SpatialContext;
import com.davidpe.scapeai.simulation.GridPosition;
import com.davidpe.scapeai.simulation.MazeDefinition;
import com.davidpe.scapeai.simulation.MoveDirection;
import com.davidpe.scapeai.simulation.SingleStepSimulationEngine;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.LongSupplier;
import org.junit.jupiter.api.Test;

class SimulationEpisodeOrchestratorTest {

  @Test
  void shouldFinishEpisodeByExitReached() {
    SimulationStepFlow flow = flowWithPolicy(context -> MoveDirection.RIGHT);
    SimulationEpisodeOrchestrator orchestrator =
        new SimulationEpisodeOrchestrator(flow, Duration.ofMinutes(5), new FixedStepTime(0, 25));
    MazeDefinition maze = new MazeDefinition(1, 2, new boolean[1][2], new GridPosition(0, 0), new GridPosition(0, 1));

    SimulationEpisodeResult result = orchestrator.runEpisode(maze);

    assertTrue(result.success());
    assertEquals(1, result.totalSteps());
    assertEquals(EpisodeEndReason.EXIT_REACHED, result.endReason());
    assertEquals(-1.0, result.totalReward());
    assertEquals(0, result.collisions());
    assertEquals(0, result.loopEvents());
    assertTrue(result.netProgress() > 0.0);
    assertEquals(0, result.explorationDecisions());
    assertTrue(result.exploitationDecisions() > 0);
    assertEquals(0, result.inferenceTraces().size());
    assertTrue(result.elapsedMillis() >= 0);
  }

  @Test
  void shouldFinishEpisodeByTimeout() {
    SimulationStepFlow flow = flowWithPolicy(context -> MoveDirection.LEFT);
    SimulationEpisodeOrchestrator orchestrator =
        new SimulationEpisodeOrchestrator(flow, Duration.ofMillis(60), new FixedStepTime(0, 30));
    MazeDefinition maze =
        new MazeDefinition(3, 3, new boolean[3][3], new GridPosition(1, 1), new GridPosition(0, 2));

    SimulationEpisodeResult result = orchestrator.runEpisode(maze);

    assertFalse(result.success());
    assertEquals(EpisodeEndReason.TIMEOUT, result.endReason());
    assertTrue(result.totalSteps() > 0);
    assertTrue(result.totalReward() <= 0.0);
    assertTrue(result.collisions() >= 0);
    assertEquals(0, result.loopEvents());
    assertTrue(result.elapsedMillis() >= 60);
  }

  @Test
  void shouldCountLoopEventsWhenAgentRepeatsWindowPositions() {
    SimulationStepFlow flow =
        flowWithPolicy(
            context ->
                context.simulationState().agentPosition().col() == 1
                    ? MoveDirection.LEFT
                    : MoveDirection.RIGHT);
    SimulationEpisodeOrchestrator orchestrator =
        new SimulationEpisodeOrchestrator(flow, Duration.ofMillis(140), 4, new FixedStepTime(0, 20));
    MazeDefinition maze =
        new MazeDefinition(1, 4, new boolean[1][4], new GridPosition(0, 1), new GridPosition(0, 3));

    SimulationEpisodeResult result = orchestrator.runEpisode(maze);

    assertFalse(result.success());
    assertEquals(EpisodeEndReason.TIMEOUT, result.endReason());
    assertTrue(result.loopEvents() > 0);
  }

  @Test
  void shouldResumeEpisodeFromCheckpointWithoutLosingCounters() {
    SimulationStepFlow flow =
        flowWithPolicy(
            context ->
                context.simulationState().agentPosition().col() == 1
                    ? MoveDirection.LEFT
                    : MoveDirection.RIGHT);
    MazeDefinition maze =
        new MazeDefinition(1, 4, new boolean[1][4], new GridPosition(0, 1), new GridPosition(0, 3));

    SimulationEpisodeOrchestrator fullRunOrchestrator =
        new SimulationEpisodeOrchestrator(flow, Duration.ofMillis(140), 4, new FixedStepTime(0, 20));
    SimulationEpisodeResult fullRun = fullRunOrchestrator.runEpisode(maze, Duration.ofMillis(140));

    SimulationEpisodeOrchestrator pauseResumeOrchestrator =
        new SimulationEpisodeOrchestrator(flow, Duration.ofMillis(140), 4, new FixedStepTime(0, 20));
    EpisodeCheckpoint checkpoint =
        pauseResumeOrchestrator.runEpisodeUntilCheckpoint(maze, Duration.ofMillis(140), 2);
    SimulationEpisodeResult resumed = pauseResumeOrchestrator.resumeEpisode(maze, checkpoint);

    assertFalse(checkpoint.currentState().exitReached());
    assertEquals(2, checkpoint.totalSteps());
    assertTrue(checkpoint.remainingMillis() > 0);
    assertTrue(checkpoint.trajectory().size() >= 3);
    assertEquals(fullRun.endReason(), resumed.endReason());
    assertTrue(resumed.totalSteps() > checkpoint.totalSteps());
    assertTrue(resumed.collisions() >= checkpoint.collisions());
    assertTrue(resumed.loopEvents() >= checkpoint.loopEvents());
    assertTrue(resumed.elapsedMillis() >= checkpoint.elapsedMillis());
  }

  @Test
  void shouldCollectInferenceTracesWhenPolicyProvidesThem() {
    TraceablePolicy policy = new TraceablePolicy();
    RewardEvaluator rewardEvaluator = context -> RewardAssessment.of(RewardSignal.NEGATIVE);
    ActiveMovementPolicyService policyService =
        new ActiveMovementPolicyService(
            Map.of("heuristic-baseline", policy, "random-controlled", policy), "heuristic-baseline");
    SimulationStepFlow flow =
        new SimulationStepFlow(policyService, rewardEvaluator, new SingleStepSimulationEngine());
    SimulationEpisodeOrchestrator orchestrator =
        new SimulationEpisodeOrchestrator(flow, Duration.ofMillis(80), new FixedStepTime(0, 20));
    MazeDefinition maze =
        new MazeDefinition(1, 3, new boolean[1][3], new GridPosition(0, 0), new GridPosition(0, 2));

    SimulationEpisodeResult result = orchestrator.runEpisode(maze, Duration.ofMillis(80));

    assertTrue(result.totalSteps() > 0);
    assertEquals(result.totalSteps(), result.inferenceTraces().size());
    assertEquals("traceable-test", result.inferenceTraces().get(0).policyId());
  }

  @Test
  void shouldRecordExperienceTransitionsDuringEpisode() {
    RecordingExperienceRecorder recorder = new RecordingExperienceRecorder();
    SimulationStepFlow flow = flowWithPolicy(context -> MoveDirection.RIGHT);
    SimulationEpisodeOrchestrator orchestrator =
        new SimulationEpisodeOrchestrator(
            flow, recorder, Duration.ofMillis(100), 6, new FixedStepTime(0, 20));
    MazeDefinition maze =
        new MazeDefinition(1, 4, new boolean[1][4], new GridPosition(0, 0), new GridPosition(0, 3));

    SimulationEpisodeResult result = orchestrator.runEpisode(maze, Duration.ofMillis(100));

    assertTrue(result.totalSteps() > 0);
    assertEquals(result.totalSteps(), recorder.transitions().size());
  }

  @Test
  void shouldCountExplorationDecisionsWhenEpsilonEnabled() {
    SimulationStepFlow flow = flowWithPolicy(context -> MoveDirection.RIGHT);
    SimulationEpisodeOrchestrator orchestrator =
        new SimulationEpisodeOrchestrator(
            flow,
            ExperienceTransitionRecorder.noop(),
            Duration.ofMillis(100),
            6,
            new FixedStepTime(0, 20),
            () -> 1L,
            () -> new Random(42L),
            1.0);
    MazeDefinition maze =
        new MazeDefinition(1, 4, new boolean[1][4], new GridPosition(0, 0), new GridPosition(0, 3));

    SimulationEpisodeResult result = orchestrator.runEpisode(maze, Duration.ofMillis(100));

    assertTrue(result.totalSteps() > 0);
    assertTrue(result.explorationDecisions() > 0);
    assertEquals(0, result.exploitationDecisions());
  }

  @Test
  void shouldPublishQuadrantAndSideCoveragePerEpisode() {
    SimulationStepFlow flow = flowWithPolicy(context -> MoveDirection.RIGHT);
    SimulationEpisodeOrchestrator orchestrator =
        new SimulationEpisodeOrchestrator(flow, Duration.ofMillis(120), new FixedStepTime(0, 20));
    MazeDefinition maze =
        new MazeDefinition(2, 4, new boolean[2][4], new GridPosition(0, 0), new GridPosition(0, 3));

    SimulationEpisodeResult result = orchestrator.runEpisode(maze, Duration.ofMillis(120));

    assertTrue(result.q1Coverage() >= 0.0 && result.q1Coverage() <= 1.0);
    assertTrue(result.q2Coverage() >= 0.0 && result.q2Coverage() <= 1.0);
    assertTrue(result.q3Coverage() >= 0.0 && result.q3Coverage() <= 1.0);
    assertTrue(result.q4Coverage() >= 0.0 && result.q4Coverage() <= 1.0);
    assertTrue(result.leftSideCoverage() >= 0.0 && result.leftSideCoverage() <= 1.0);
    assertTrue(result.rightSideCoverage() >= 0.0 && result.rightSideCoverage() <= 1.0);
    assertTrue(result.rightSideCoverage() >= result.leftSideCoverage());
  }

  @Test
  void shouldImproveRightCoverageAndReduceLoopsAgainstLegacyBaseline() {
    MazeDefinition maze =
        new MazeDefinition(
            5,
            7,
            new boolean[][] {
              {true, true, true, true, true, true, true},
              {true, false, false, true, false, false, true},
              {true, false, false, false, false, false, true},
              {true, false, false, true, false, false, true},
              {true, true, true, true, true, true, true}
            },
            new GridPosition(2, 1),
            new GridPosition(2, 5));

    SimulationEpisodeResult baseline =
        new SimulationEpisodeOrchestrator(
                flowWithPolicy(new LegacyBaselineMovementPolicy()),
                Duration.ofMillis(220),
                6,
                new FixedStepTime(0, 20))
            .runEpisode(maze, Duration.ofMillis(220));
    SimulationEpisodeResult improved =
        new SimulationEpisodeOrchestrator(
                flowWithPolicy(new SimpleMovementPolicy(6)),
                Duration.ofMillis(220),
                6,
                new FixedStepTime(0, 20))
            .runEpisode(maze, Duration.ofMillis(220));

    assertTrue(improved.rightSideCoverage() >= baseline.rightSideCoverage());
    assertTrue(improved.loopEvents() <= baseline.loopEvents());
  }

  private SimulationStepFlow flowWithPolicy(MovementPolicy policy) {
    RewardEvaluator rewardEvaluator = context -> RewardAssessment.of(RewardSignal.NEGATIVE);
    ActiveMovementPolicyService policyService =
        new ActiveMovementPolicyService(
            Map.of("heuristic-baseline", policy, "random-controlled", policy), "heuristic-baseline");
    return new SimulationStepFlow(policyService, rewardEvaluator, new SingleStepSimulationEngine());
  }

  private static final class FixedStepTime implements LongSupplier {

    private long current;
    private final long step;

    private FixedStepTime(long start, long step) {
      this.current = start;
      this.step = step;
    }

    @Override
    public long getAsLong() {
      long value = current;
      current += step;
      return value;
    }
  }

  private static final class TraceablePolicy
      implements MovementPolicy, com.davidpe.scapeai.ai.InferenceTraceProvider {

    private PolicyInferenceTrace lastTrace;

    @Override
    public MoveDirection chooseNextMove(com.davidpe.scapeai.ai.SpatialContext context) {
      lastTrace = new PolicyInferenceTrace("traceable-test", 0.75, 1L, false, null);
      return MoveDirection.RIGHT;
    }

    @Override
    public Optional<PolicyInferenceTrace> latestInferenceTrace() {
      return Optional.ofNullable(lastTrace);
    }
  }

  private static final class RecordingExperienceRecorder implements ExperienceTransitionRecorder {

    private final List<String> transitions = new ArrayList<>();

    @Override
    public void recordTransition(
        com.davidpe.scapeai.simulation.SimulationState previousState,
        MoveDirection action,
        double reward,
        com.davidpe.scapeai.simulation.SimulationState nextState) {
      transitions.add(previousState.agentPosition() + "->" + nextState.agentPosition() + ":" + action);
    }

    public List<String> transitions() {
      return transitions;
    }
  }

  private static final class LegacyBaselineMovementPolicy implements MovementPolicy {

    private static final List<MoveDirection> ORDER =
        List.of(MoveDirection.UP, MoveDirection.RIGHT, MoveDirection.DOWN, MoveDirection.LEFT);

    @Override
    public MoveDirection chooseNextMove(SpatialContext context) {
      GridPosition current = context.simulationState().agentPosition();
      Set<GridPosition> recent = context.simulationState().visitedCells();
      List<MoveDirection> valid =
          ORDER.stream().filter(direction -> context.canMove(direction)).toList();
      if (valid.isEmpty()) {
        return MoveDirection.UP;
      }
      List<MoveDirection> preferred =
          valid.stream().filter(direction -> !recent.contains(current.move(direction))).toList();
      List<MoveDirection> pool = preferred.isEmpty() ? valid : preferred;
      MoveDirection best = pool.get(0);
      int bestDistance = distance(current.move(best), context.maze().exit());
      for (MoveDirection direction : pool) {
        int candidate = distance(current.move(direction), context.maze().exit());
        if (candidate < bestDistance) {
          bestDistance = candidate;
          best = direction;
        }
      }
      return best;
    }

    private int distance(GridPosition from, GridPosition to) {
      return Math.abs(from.row() - to.row()) + Math.abs(from.col() - to.col());
    }
  }
}
