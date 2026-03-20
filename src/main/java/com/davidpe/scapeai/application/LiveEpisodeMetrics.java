package com.davidpe.scapeai.application;

import com.davidpe.scapeai.simulation.GridPosition;
import java.util.List;

public record LiveEpisodeMetrics(
    int steps,
    int collisions,
    double accumulatedReward,
    long elapsedMillis,
    long remainingMillis,
    String terminationReason,
    double mazeCoverageRatio,
    double leftSideCoverage,
    double rightSideCoverage,
    GridPosition currentPosition,
    List<GridPosition> trajectory) {}
