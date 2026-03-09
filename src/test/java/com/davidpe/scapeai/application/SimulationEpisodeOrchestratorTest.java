package com.davidpe.scapeai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.scapeai.ai.MovementPolicy;
import com.davidpe.scapeai.ai.RewardAssessment;
import com.davidpe.scapeai.ai.RewardEvaluator;
import com.davidpe.scapeai.ai.RewardSignal;
import com.davidpe.scapeai.simulation.GridPosition;
import com.davidpe.scapeai.simulation.MazeDefinition;
import com.davidpe.scapeai.simulation.MoveDirection;
import com.davidpe.scapeai.simulation.SingleStepSimulationEngine;
import java.time.Duration;
import java.util.Map;
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
}
