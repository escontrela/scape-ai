package com.davidpe.scapeai.ai;

import com.davidpe.scapeai.simulation.MoveDirection;

public interface MovementPolicy {

  MoveDirection chooseNextMove(SpatialContext context);
}
