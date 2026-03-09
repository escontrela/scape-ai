package com.davidpe.scapeai.application;

import com.davidpe.scapeai.simulation.MazeDefinition;

public record StartTrainingSessionCommand(
    MazeDefinition maze, Long presetId, TrainingTargetDifficulty targetDifficulty) {}
