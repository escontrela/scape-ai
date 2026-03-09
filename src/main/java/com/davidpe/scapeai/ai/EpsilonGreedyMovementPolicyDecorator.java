package com.davidpe.scapeai.ai;

import com.davidpe.scapeai.simulation.MoveDirection;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.Optional;

public class EpsilonGreedyMovementPolicyDecorator implements MovementPolicy, InferenceTraceProvider {

  private final MovementPolicy delegate;
  private final double epsilon;
  private final Supplier<Random> randomSupplier;
  private volatile boolean lastDecisionExploration;

  public EpsilonGreedyMovementPolicyDecorator(
      MovementPolicy delegate, double epsilon, Supplier<Random> randomSupplier) {
    if (delegate == null) {
      throw new IllegalArgumentException("delegate policy is required");
    }
    if (epsilon < 0.0 || epsilon > 1.0) {
      throw new IllegalArgumentException("epsilon must be between 0 and 1");
    }
    this.delegate = delegate;
    this.epsilon = epsilon;
    this.randomSupplier = randomSupplier;
  }

  @Override
  public MoveDirection chooseNextMove(SpatialContext context) {
    if (context == null) {
      lastDecisionExploration = false;
      return delegate.chooseNextMove(context);
    }

    Random random = randomSupplier.get();
    if (random.nextDouble() < epsilon) {
      List<MoveDirection> validMoves = validMoves(context);
      if (!validMoves.isEmpty()) {
        lastDecisionExploration = true;
        return validMoves.get(random.nextInt(validMoves.size()));
      }
    }

    lastDecisionExploration = false;
    return delegate.chooseNextMove(context);
  }

  public boolean lastDecisionExploration() {
    return lastDecisionExploration;
  }

  @Override
  public Optional<com.davidpe.scapeai.application.PolicyInferenceTrace> latestInferenceTrace() {
    if (delegate instanceof InferenceTraceProvider provider) {
      return provider.latestInferenceTrace();
    }
    return Optional.empty();
  }

  private List<MoveDirection> validMoves(SpatialContext context) {
    var maze = context.maze();
    var current = context.simulationState().agentPosition();
    List<MoveDirection> moves = new ArrayList<>();
    for (MoveDirection direction : MoveDirection.values()) {
      var next = current.move(direction);
      if (maze.isInside(next) && !maze.isWall(next)) {
        moves.add(direction);
      }
    }
    return moves;
  }
}
