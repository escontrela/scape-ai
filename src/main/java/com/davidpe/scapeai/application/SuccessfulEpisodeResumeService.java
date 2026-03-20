package com.davidpe.scapeai.application;

import com.davidpe.scapeai.persistence.repository.TrainingRunRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SuccessfulEpisodeResumeService {

  private final TrainingRunRepository trainingRunRepository;

  public SuccessfulEpisodeResumeService(TrainingRunRepository trainingRunRepository) {
    this.trainingRunRepository = trainingRunRepository;
  }

  public List<SuccessfulEpisodeReplay> listByTrainingSession(String trainingSessionId, int limit) {
    if (trainingSessionId == null || trainingSessionId.isBlank()) {
      return List.of();
    }
    return trainingRunRepository.findSuccessfulByTrainingSessionId(trainingSessionId, Math.max(1, limit)).stream()
        .map(
            run ->
                new SuccessfulEpisodeReplay(
                    run.id(),
                    run.trainingSessionId(),
                    run.terminalReason(),
                    run.createdAtEpochMillis(),
                    run.elapsedMillis(),
                    run.totalReward(),
                    EpisodeTrajectoryCodec.decode(run.trajectoryPath()),
                    run.replayDebugMetadata()))
        .toList();
  }
}
