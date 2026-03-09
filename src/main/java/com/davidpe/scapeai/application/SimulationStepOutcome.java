package com.davidpe.scapeai.application;

import com.davidpe.scapeai.ai.RewardAssessment;
import com.davidpe.scapeai.simulation.MoveDirection;
import com.davidpe.scapeai.simulation.SimulationStepResult;
import java.util.Optional;

public record SimulationStepOutcome(
    MoveDirection selectedDirection,
    SimulationStepResult result,
    RewardAssessment reward,
    Optional<PolicyInferenceTrace> inferenceTrace) {}
