package com.davidpe.scapeai.simulation;

public record SimulationStepResult(SimulationState state, boolean moved, boolean collision) {}
