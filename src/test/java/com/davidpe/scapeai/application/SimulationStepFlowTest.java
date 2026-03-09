package com.davidpe.scapeai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.davidpe.scapeai.ai.MovementPolicy;
import com.davidpe.scapeai.ai.RewardAssessment;
import com.davidpe.scapeai.ai.RewardContext;
import com.davidpe.scapeai.ai.RewardEvaluator;
import com.davidpe.scapeai.ai.RewardSignal;
import com.davidpe.scapeai.ai.SpatialContext;
import com.davidpe.scapeai.simulation.GridPosition;
import com.davidpe.scapeai.simulation.MazeDefinition;
import com.davidpe.scapeai.simulation.MoveDirection;
import com.davidpe.scapeai.simulation.SingleStepSimulationEngine;
import com.davidpe.scapeai.simulation.SimulationState;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SimulationStepFlowTest {

  @Test
  void shouldExecuteOneStepUsingPolicyAndRewardContracts() {
    MovementPolicy policy = context -> MoveDirection.RIGHT;
    RewardEvaluator evaluator = this::rewardByCollision;
    ActiveMovementPolicyService policyService =
        new ActiveMovementPolicyService(
            Map.of("heuristic-baseline", policy, "random-controlled", policy), "heuristic-baseline");

    SimulationStepFlow flow =
        new SimulationStepFlow(policyService, evaluator, new SingleStepSimulationEngine());

    MazeDefinition maze = new MazeDefinition(3, 3, new boolean[3][3], new GridPosition(2, 2));
    SimulationState start = SimulationState.initial(new GridPosition(0, 0));

    SimulationStepOutcome outcome = flow.execute(maze, start);

    assertEquals(MoveDirection.RIGHT, outcome.selectedDirection());
    assertEquals(new GridPosition(0, 1), outcome.result().state().agentPosition());
    assertEquals(RewardSignal.NEGATIVE, outcome.reward().signal());
  }

  @Test
  void shouldPassLoopSignalToRewardEvaluator() {
    MovementPolicy policy = context -> MoveDirection.RIGHT;
    RewardEvaluator evaluator =
        context ->
            context.loopDetected()
                ? RewardAssessment.of(RewardSignal.VERY_NEGATIVE)
                : RewardAssessment.of(RewardSignal.NEGATIVE);
    ActiveMovementPolicyService policyService =
        new ActiveMovementPolicyService(
            Map.of("heuristic-baseline", policy, "random-controlled", policy), "heuristic-baseline");

    SimulationStepFlow flow =
        new SimulationStepFlow(policyService, evaluator, new SingleStepSimulationEngine());

    MazeDefinition maze = new MazeDefinition(3, 3, new boolean[3][3], new GridPosition(2, 2));
    SimulationState start = SimulationState.initial(new GridPosition(0, 0));

    SimulationStepOutcome outcome = flow.execute(maze, start, true);

    assertEquals(RewardSignal.VERY_NEGATIVE, outcome.reward().signal());
  }

  private RewardAssessment rewardByCollision(RewardContext context) {
    return context.stepResult().collision()
        ? RewardAssessment.of(RewardSignal.VERY_NEGATIVE)
        : RewardAssessment.of(RewardSignal.NEGATIVE);
  }
}
