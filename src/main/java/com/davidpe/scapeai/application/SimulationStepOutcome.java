package com.davidpe.scapeai.application;

import com.davidpe.scapeai.ai.RewardAssessment;
import com.davidpe.scapeai.simulation.MoveDirection;
import com.davidpe.scapeai.simulation.SimulationStepResult;

public record SimulationStepOutcome(
    MoveDirection selectedDirection, SimulationStepResult result, RewardAssessment reward) {}
