package com.davidpe.scapeai.application;

public record SimulationEpisodeResult(
    boolean success,
    int totalSteps,
    long elapsedMillis,
    EpisodeEndReason endReason,
    double totalReward,
    int collisions,
    int loopEvents) {}
