package com.davidpe.scapeai.application;

public record TrainingTimelineEntry(
    TrainingTimelineStatus status, String terminalReason, double reward, long durationMillis) {}
