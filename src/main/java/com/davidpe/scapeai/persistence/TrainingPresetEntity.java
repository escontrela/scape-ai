package com.davidpe.scapeai.persistence;

public record TrainingPresetEntity(
    Long id, int episodes, long timeoutMillis, String policy, Long seed) {}
