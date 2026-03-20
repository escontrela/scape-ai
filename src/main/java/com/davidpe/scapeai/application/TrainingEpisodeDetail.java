package com.davidpe.scapeai.application;

import com.davidpe.scapeai.simulation.GridPosition;
import java.util.List;

public record TrainingEpisodeDetail(
    long trainingRunId,
    List<GridPosition> trajectory,
    GridPosition finalPosition,
    String replayMetadata) {}
