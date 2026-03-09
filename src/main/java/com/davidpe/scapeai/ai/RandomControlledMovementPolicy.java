package com.davidpe.scapeai.ai;

import com.davidpe.scapeai.simulation.MoveDirection;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomControlledMovementPolicy implements MovementPolicy {

  private static final List<MoveDirection> DIRECTION_PRIORITY =
      List.of(MoveDirection.UP, MoveDirection.RIGHT, MoveDirection.DOWN, MoveDirection.LEFT);
  private final Random random;

  public RandomControlledMovementPolicy(long seed) {
    this.random = new Random(seed);
  }

  @Override
  public synchronized MoveDirection chooseNextMove(SpatialContext context) {
    var maze = context.maze();
    var current = context.simulationState().agentPosition();
    List<MoveDirection> validMoves = new ArrayList<>();

    for (MoveDirection direction : DIRECTION_PRIORITY) {
      var target = current.move(direction);
      if (maze.isInside(target) && !maze.isWall(target)) {
        validMoves.add(direction);
      }
    }

    if (validMoves.isEmpty()) {
      return MoveDirection.UP;
    }
    return validMoves.get(random.nextInt(validMoves.size()));
  }
}
