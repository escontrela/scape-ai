package com.davidpe.scapeai.persistence;

public record TrainingRunReplayDiagnosticEntity(
    long trainingRunId, long createdAtEpochMillis, String replayDebugMetadata) {}
