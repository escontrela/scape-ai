package com.davidpe.scapeai.application;

import com.davidpe.scapeai.simulation.MoveDirection;
import com.davidpe.scapeai.simulation.SimulationState;

public interface ExperienceTransitionRecorder {

  void recordTransition(
      SimulationState previousState,
      MoveDirection action,
      double reward,
      SimulationState nextState);

  static ExperienceTransitionRecorder noop() {
    return (previousState, action, reward, nextState) -> {};
  }
}
