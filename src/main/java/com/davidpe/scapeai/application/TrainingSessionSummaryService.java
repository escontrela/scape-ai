package com.davidpe.scapeai.application;

import com.davidpe.scapeai.persistence.TrainingRunEntity;
import com.davidpe.scapeai.persistence.repository.TrainingRunRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class TrainingSessionSummaryService {

  private final TrainingRunRepository trainingRunRepository;

  public TrainingSessionSummaryService(TrainingRunRepository trainingRunRepository) {
    this.trainingRunRepository = trainingRunRepository;
  }

  public TrainingSessionSummary summarize(String trainingSessionId) {
    List<TrainingRunEntity> runs = trainingRunRepository.findByTrainingSessionId(trainingSessionId);
    int total = runs.size();
    if (total <= 0) {
      return new TrainingSessionSummary(
          trainingSessionId == null ? "" : trainingSessionId.trim(),
          0,
          0,
          0.0,
          0.0,
          0.0,
          0.0,
          0L,
          0,
          0,
          0,
          0);
    }

    int successes = 0;
    double rewardSum = 0.0;
    double collisionsSum = 0.0;
    double coverageSum = 0.0;
    long elapsedSum = 0L;
    int exitReached = 0;
    int timeout = 0;
    int aborted = 0;
    int error = 0;

    for (TrainingRunEntity run : runs) {
      if (run.success()) {
        successes++;
      }
      rewardSum += run.totalReward();
      collisionsSum += run.collisions();
      coverageSum += run.mazeCoverageRatio();
      elapsedSum += Math.max(0L, run.elapsedMillis());
      switch (normalizeTerminalReason(run)) {
        case "EXIT_REACHED" -> exitReached++;
        case "TIMEOUT" -> timeout++;
        case "ERROR" -> error++;
        default -> aborted++;
      }
    }

    double divisor = (double) total;
    return new TrainingSessionSummary(
        runs.get(0).trainingSessionId(),
        total,
        successes,
        successes / divisor,
        rewardSum / divisor,
        collisionsSum / divisor,
        coverageSum / divisor,
        elapsedSum,
        exitReached,
        timeout,
        aborted,
        error);
  }

  private String normalizeTerminalReason(TrainingRunEntity run) {
    String reason = run.terminalReason();
    if (reason == null || reason.isBlank()) {
      if (run.timeoutReached()) {
        return "TIMEOUT";
      }
      return run.success() ? "EXIT_REACHED" : "ABORTED";
    }
    return reason.trim().toUpperCase(Locale.ROOT);
  }
}
