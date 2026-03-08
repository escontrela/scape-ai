package com.davidpe.scapeai.ai;

import com.davidpe.scapeai.simulation.SimulationState;
import com.davidpe.scapeai.simulation.SimulationStepResult;

public record RewardContext(SimulationState previousState, SimulationStepResult stepResult) {}
