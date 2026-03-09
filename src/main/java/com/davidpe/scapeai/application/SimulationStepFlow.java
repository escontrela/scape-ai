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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        new HashMap<>(),
        previousDirection,
        noProgressStreak,
        loopDetected,
        movementPolicyService.activePolicy());
  }

  public SimulationStepOutcome execute(
      MazeDefinition maze,
      SimulationState currentState,
      List<com.davidpe.scapeai.simulation.GridPosition> recentPositions,
      Map<String, Integer> transitionCounts,
      MoveDirection previousDirection,
      int noProgressStreak,
      boolean loopDetected) {
    return execute(
        maze,
        currentState,
        recentPositions,
        transitionCounts,
        previousDirection,
        noProgressStreak,
        loopDetected,
        movementPolicyService.activePolicy());
  }

  SimulationStepOutcome execute(
      MazeDefinition maze,
      SimulationState currentState,
      List<com.davidpe.scapeai.simulation.GridPosition> recentPositions,
      Map<String, Integer> transitionCounts,
      MoveDirection previousDirection,
      int noProgressStreak,
      boolean loopDetected,
      MovementPolicy movementPolicy) {
    var direction =
        movementPolicy.chooseNextMove(
            new SpatialContext(
                maze, currentState, recentPositions, previousDirection, noProgressStreak));
    String transitionKey = transitionKey(currentState, direction);
    int repeatCount = transitionCounts.getOrDefault(transitionKey, 0);
    SimulationStepResult result = simulationEngine.step(currentState, maze, direction);
    boolean discoveredNewCell =
        result.state().visitedCells().size() > currentState.visitedCells().size();
    boolean movedToUnderExploredSide = movedToUnderExploredSide(maze, currentState, result.state());
    RewardAssessment reward =
        rewardEvaluator.evaluate(
            new RewardContext(
                currentState,
                result,
                loopDetected,
                discoveredNewCell,
                movedToUnderExploredSide,
                repeatCount));
    transitionCounts.put(transitionKey, repeatCount + 1);
    Optional<PolicyInferenceTrace> trace = Optional.empty();
    if (movementPolicy instanceof InferenceTraceProvider provider) {
      trace = provider.latestInferenceTrace();
    }
    return new SimulationStepOutcome(direction, result, reward, trace);
  }

  MovementPolicy activePolicy() {
    return movementPolicyService.activePolicy();
  }

  private boolean movedToUnderExploredSide(
      MazeDefinition maze, SimulationState previousState, SimulationState nextState) {
    int midCol = maze.cols() / 2;
    int left = 0;
    int right = 0;
    for (var visited : previousState.visitedCells()) {
      if (visited.col() < midCol) {
        left++;
      } else {
        right++;
      }
    }
    boolean movedLeft = nextState.agentPosition().col() < midCol;
    if (left == right) {
      return false;
    }
    return movedLeft ? left < right : right < left;
  }

  private String transitionKey(SimulationState state, MoveDirection direction) {
    return state.agentPosition().row()
        + ":"
        + state.agentPosition().col()
        + "->"
        + direction.name();
  }
}
