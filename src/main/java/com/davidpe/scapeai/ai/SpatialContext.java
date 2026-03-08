package com.davidpe.scapeai.ai;

import com.davidpe.scapeai.simulation.MazeDefinition;
import com.davidpe.scapeai.simulation.SimulationState;

public record SpatialContext(MazeDefinition maze, SimulationState simulationState) {}
