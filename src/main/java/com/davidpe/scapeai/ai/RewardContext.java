package com.davidpe.scapeai.ai;

import com.davidpe.scapeai.simulation.SimulationState;
import com.davidpe.scapeai.simulation.SimulationStepResult;

public record RewardContext(
    SimulationState previousState,
    SimulationStepResult stepResult,
    boolean loopDetected,
    boolean discoveredNewCell,
    boolean movedToUnderExploredSide,
    int transitionRepeatCount) {

  public RewardContext(
      SimulationState previousState, SimulationStepResult stepResult, boolean loopDetected) {
    this(previousState, stepResult, loopDetected, false, false, 0);
  }
}
