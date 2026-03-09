package com.davidpe.scapeai.application;

import com.davidpe.scapeai.ai.InferenceTraceProvider;
import com.davidpe.scapeai.ai.MovementPolicy;
import com.davidpe.scapeai.ai.RewardAssessment;
import com.davidpe.scapeai.ai.RewardContext;
import com.davidpe.scapeai.ai.RewardEvaluator;
import com.davidpe.scapeai.ai.SpatialContext;
import com.davidpe.scapeai.simulation.MazeDefinition;
import com.davidpe.scapeai.simulation.MoveDirection;
import com.davidpe.scapeai.simulation.SingleStepSimulationEngine;
import com.davidpe.scapeai.simulation.SimulationState;
import com.davidpe.scapeai.simulation.SimulationStepResult;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class SimulationStepFlow {

  private final ActiveMovementPolicyService movementPolicyService;
  private final RewardEvaluator rewardEvaluator;
  private final SingleStepSimulationEngine simulationEngine;

  public SimulationStepFlow(
      ActiveMovementPolicyService movementPolicyService,
      RewardEvaluator rewardEvaluator,
      SingleStepSimulationEngine simulationEngine) {
    this.movementPolicyService = movementPolicyService;
    this.rewardEvaluator = rewardEvaluator;
    this.simulationEngine = simulationEngine;
  }

  public SimulationStepOutcome execute(MazeDefinition maze, SimulationState currentState) {
    return execute(maze, currentState, false);
  }

  public SimulationStepOutcome execute(
      MazeDefinition maze, SimulationState currentState, boolean loopDetected) {
    return execute(maze, currentState, null, 0, loopDetected);
  }

  public SimulationStepOutcome execute(
      MazeDefinition maze,
      SimulationState currentState,
      MoveDirection previousDirection,
      int noProgressStreak,
      boolean loopDetected) {
    return execute(
        maze,
        currentState,
        List.of(),
        previousDirection,
        noProgressStreak,
        loopDetected,
        movementPolicyService.activePolicy());
  }

  public SimulationStepOutcome execute(
      MazeDefinition maze,
      SimulationState currentState,
      List<com.davidpe.scapeai.simulation.GridPosition> recentPositions,
      MoveDirection previousDirection,
      int noProgressStreak,
      boolean loopDetected) {
    return execute(
        maze,
        currentState,
        recentPositions,
        previousDirection,
        noProgressStreak,
        loopDetected,
        movementPolicyService.activePolicy());
  }

  SimulationStepOutcome execute(
      MazeDefinition maze,
      SimulationState currentState,
      List<com.davidpe.scapeai.simulation.GridPosition> recentPositions,
      MoveDirection previousDirection,
      int noProgressStreak,
      boolean loopDetected,
      MovementPolicy movementPolicy) {
    var direction =
        movementPolicy.chooseNextMove(
            new SpatialContext(
                maze, currentState, recentPositions, previousDirection, noProgressStreak));
    SimulationStepResult result = simulationEngine.step(currentState, maze, direction);
    RewardAssessment reward = rewardEvaluator.evaluate(new RewardContext(currentState, result, loopDetected));
    Optional<PolicyInferenceTrace> trace = Optional.empty();
    if (movementPolicy instanceof InferenceTraceProvider provider) {
      trace = provider.latestInferenceTrace();
    }
    return new SimulationStepOutcome(direction, result, reward, trace);
  }

  MovementPolicy activePolicy() {
    return movementPolicyService.activePolicy();
  }
}
