package com.davidpe.scapeai.ai.infrastructure.djl;

import com.davidpe.scapeai.ai.MovementPolicy;
import com.davidpe.scapeai.ai.SpatialContext;
import com.davidpe.scapeai.simulation.MoveDirection;

public class DjlMovementPolicyAdapter implements MovementPolicy {

  private final DjlDirectionPredictor predictor;
  private final MovementPolicy fallbackPolicy;

  public DjlMovementPolicyAdapter(DjlDirectionPredictor predictor, MovementPolicy fallbackPolicy) {
    this.predictor = predictor;
    this.fallbackPolicy = fallbackPolicy;
  }

  @Override
  public MoveDirection chooseNextMove(SpatialContext context) {
    try {
      MoveDirection predicted = predictor.predictNext(context);
      if (predicted == null) {
        return fallbackPolicy.chooseNextMove(context);
      }

      var target = context.simulationState().agentPosition().move(predicted);
      if (!context.maze().isInside(target) || context.maze().isWall(target)) {
        return fallbackPolicy.chooseNextMove(context);
      }
      return predicted;
    } catch (RuntimeException predictionFailure) {
      return fallbackPolicy.chooseNextMove(context);
    }
  }
}
