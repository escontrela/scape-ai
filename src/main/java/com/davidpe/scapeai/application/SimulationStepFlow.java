package com.davidpe.scapeai.application;

import com.davidpe.scapeai.ai.MovementPolicy;
import com.davidpe.scapeai.ai.RewardAssessment;
import com.davidpe.scapeai.ai.RewardContext;
import com.davidpe.scapeai.ai.RewardEvaluator;
import com.davidpe.scapeai.ai.SpatialContext;
import com.davidpe.scapeai.simulation.MazeDefinition;
import com.davidpe.scapeai.simulation.SingleStepSimulationEngine;
import com.davidpe.scapeai.simulation.SimulationState;
import com.davidpe.scapeai.simulation.SimulationStepResult;
import org.springframework.stereotype.Component;

@Component
public class SimulationStepFlow {

  private final MovementPolicy movementPolicy;
  private final RewardEvaluator rewardEvaluator;
  private final SingleStepSimulationEngine simulationEngine;

  public SimulationStepFlow(
      MovementPolicy movementPolicy,
      RewardEvaluator rewardEvaluator,
      SingleStepSimulationEngine simulationEngine) {
    this.movementPolicy = movementPolicy;
    this.rewardEvaluator = rewardEvaluator;
    this.simulationEngine = simulationEngine;
  }

  public SimulationStepOutcome execute(MazeDefinition maze, SimulationState currentState) {
    var direction = movementPolicy.chooseNextMove(new SpatialContext(maze, currentState));
    SimulationStepResult result = simulationEngine.step(currentState, maze, direction);
    RewardAssessment reward = rewardEvaluator.evaluate(new RewardContext(currentState, result));
    return new SimulationStepOutcome(direction, result, reward);
  }
}
