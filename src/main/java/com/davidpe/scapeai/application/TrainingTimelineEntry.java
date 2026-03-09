package com.davidpe.scapeai.application;

public record TrainingTimelineEntry(TrainingTimelineStatus status, double reward, long durationMillis) {}
