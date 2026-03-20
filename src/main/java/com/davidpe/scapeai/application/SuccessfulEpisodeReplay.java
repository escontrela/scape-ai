package com.davidpe.scapeai.application;

import com.davidpe.scapeai.simulation.GridPosition;
import java.util.List;

public record SuccessfulEpisodeReplay(
    long trainingRunId,
    String trainingSessionId,
    String terminalReason,
    long createdAtEpochMillis,
    List<GridPosition> trajectory,
    String replayMetadata) {

  public SuccessfulEpisodeReplay {
    trainingSessionId = trainingSessionId == null ? "" : trainingSessionId;
    terminalReason = terminalReason == null ? "" : terminalReason;
    trajectory = trajectory == null ? List.of() : List.copyOf(trajectory);
    replayMetadata = replayMetadata == null ? "{}" : replayMetadata;
  }
}
