package com.davidpe.scapeai.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.scapeai.simulation.GridPosition;
import com.davidpe.scapeai.simulation.MazeDefinition;
import com.davidpe.scapeai.simulation.MoveDirection;
import com.davidpe.scapeai.simulation.SingleStepSimulationEngine;
import com.davidpe.scapeai.simulation.SimulationState;
import org.junit.jupiter.api.Test;

class DefaultRewardEvaluatorTest {

  private final DefaultRewardEvaluator evaluator = new DefaultRewardEvaluator();
  private final SingleStepSimulationEngine engine = new SingleStepSimulationEngine();

  @Test
  void shouldIncreaseRewardForNewCellInUnderExploredSide() {
    MazeDefinition maze =
        new MazeDefinition(3, 6, new boolean[3][6], new GridPosition(1, 1), new GridPosition(1, 5));
    SimulationState previous =
        new SimulationState(
            new GridPosition(1, 2),
            java.util.Set.of(new GridPosition(1, 1), new GridPosition(1, 2)),
            0,
            false);
    var step = engine.step(previous, maze, MoveDirection.RIGHT);

    RewardAssessment shaped =
        evaluator.evaluate(new RewardContext(previous, step, false, true, true, 0));
    RewardAssessment baseline = evaluator.evaluate(new RewardContext(previous, step, false));

    assertTrue(shaped.value() > baseline.value());
  }

  @Test
  void shouldApplyIncrementalPenaltyForRepeatedLoopTransitions() {
    MazeDefinition maze =
        new MazeDefinition(2, 2, new boolean[2][2], new GridPosition(0, 0), new GridPosition(1, 1));
    SimulationState previous = SimulationState.initial(new GridPosition(0, 0));
    var step = engine.step(previous, maze, MoveDirection.RIGHT);

    RewardAssessment firstLoopPenalty =
        evaluator.evaluate(new RewardContext(previous, step, true, false, false, 1));
    RewardAssessment repeatedLoopPenalty =
        evaluator.evaluate(new RewardContext(previous, step, true, false, false, 3));

    assertTrue(repeatedLoopPenalty.value() < firstLoopPenalty.value());
  }

  @Test
  void shouldKeepPositiveSignalForExitSuccess() {
    MazeDefinition maze =
        new MazeDefinition(1, 2, new boolean[1][2], new GridPosition(0, 0), new GridPosition(0, 1));
    SimulationState previous = SimulationState.initial(new GridPosition(0, 0));
    var step = engine.step(previous, maze, MoveDirection.RIGHT);

    RewardAssessment reward = evaluator.evaluate(new RewardContext(previous, step, false, true, true, 0));

    assertEquals(RewardSignal.POSITIVE, reward.signal());
    assertTrue(reward.value() > 0.0);
  }
}
