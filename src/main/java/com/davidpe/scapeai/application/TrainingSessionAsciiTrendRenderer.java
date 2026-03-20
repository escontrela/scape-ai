package com.davidpe.scapeai.application;

import com.davidpe.scapeai.persistence.TrainingRunEntity;
import com.davidpe.scapeai.persistence.repository.TrainingRunRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class TrainingSessionAsciiTrendRenderer {

  private static final String REWARD_SCALE = " .:-=+*#%@";
  private final TrainingRunRepository trainingRunRepository;

  public TrainingSessionAsciiTrendRenderer(TrainingRunRepository trainingRunRepository) {
    this.trainingRunRepository = trainingRunRepository;
  }

  public String renderForSession(String trainingSessionId) {
    return render(trainingRunRepository.findByTrainingSessionId(trainingSessionId), trainingSessionId);
  }

  public String render(List<TrainingRunEntity> runs, String trainingSessionId) {
    List<TrainingRunEntity> safeRuns = runs == null ? List.of() : runs;
    if (safeRuns.isEmpty()) {
      return "SESSION " + safeId(trainingSessionId) + "\n(no episodes)";
    }
    int total = safeRuns.size();
    double minReward = safeRuns.stream().mapToDouble(TrainingRunEntity::totalReward).min().orElse(0.0);
    double maxReward = safeRuns.stream().mapToDouble(TrainingRunEntity::totalReward).max().orElse(0.0);
    double spread = Math.max(1e-9, maxReward - minReward);
    StringBuilder outcome = new StringBuilder(total);
    StringBuilder reward = new StringBuilder(total);
    StringBuilder coverage = new StringBuilder(total);
    int exitCount = 0;
    int timeoutCount = 0;
    for (TrainingRunEntity run : safeRuns) {
      char outcomeChar = outcomeChar(run);
      outcome.append(outcomeChar);
      if (outcomeChar == 'E') {
        exitCount++;
      } else if (outcomeChar == 'T') {
        timeoutCount++;
      }
      int rewardIndex =
          (int)
              Math.round(
                  ((run.totalReward() - minReward) / spread) * (REWARD_SCALE.length() - 1));
      reward.append(REWARD_SCALE.charAt(Math.max(0, Math.min(REWARD_SCALE.length() - 1, rewardIndex))));
      int coverageIndex = (int) Math.round(Math.max(0.0, Math.min(1.0, run.mazeCoverageRatio())) * 9.0);
      coverage.append((char) ('0' + Math.max(0, Math.min(9, coverageIndex))));
    }
    return new StringBuilder()
        .append("SESSION ")
        .append(safeId(trainingSessionId))
        .append(" episodes=")
        .append(total)
        .append(" success=")
        .append(exitCount)
        .append(" timeout=")
        .append(timeoutCount)
        .append('\n')
        .append("OUTCOME  ")
        .append(outcome)
        .append('\n')
        .append("REWARD   ")
        .append(reward)
        .append("  min=")
        .append(String.format(Locale.ROOT, "%.2f", minReward))
        .append(" max=")
        .append(String.format(Locale.ROOT, "%.2f", maxReward))
        .append('\n')
        .append("COVERAGE ")
        .append(coverage)
        .append("  scale=0..9")
        .toString();
  }

  private String safeId(String trainingSessionId) {
    if (trainingSessionId == null || trainingSessionId.isBlank()) {
      return "unknown";
    }
    return trainingSessionId.trim();
  }

  private char outcomeChar(TrainingRunEntity run) {
    String reason = run.terminalReason();
    if (reason == null || reason.isBlank()) {
      return run.success() ? 'E' : (run.timeoutReached() ? 'T' : 'A');
    }
    return switch (reason.trim().toUpperCase(Locale.ROOT)) {
      case "EXIT_REACHED" -> 'E';
      case "TIMEOUT" -> 'T';
      case "ERROR" -> 'X';
      default -> 'A';
    };
  }
}
