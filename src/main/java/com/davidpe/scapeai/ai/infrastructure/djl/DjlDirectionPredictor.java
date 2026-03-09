package com.davidpe.scapeai.ai.infrastructure.djl;

import com.davidpe.scapeai.ai.SpatialContext;
import com.davidpe.scapeai.simulation.MoveDirection;

@FunctionalInterface
public interface DjlDirectionPredictor {

  MoveDirection predictNext(SpatialContext context);
}
