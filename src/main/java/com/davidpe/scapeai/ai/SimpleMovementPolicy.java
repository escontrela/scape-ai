package com.davidpe.scapeai.ai;

import com.davidpe.scapeai.simulation.MoveDirection;
import org.springframework.stereotype.Component;

@Component
public class SimpleMovementPolicy implements MovementPolicy {

  @Override
  public MoveDirection chooseNextMove(SpatialContext context) {
    return MoveDirection.RIGHT;
  }
}
