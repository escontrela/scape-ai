package com.davidpe.scapeai.application;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AdaptiveDifficultyService {

  private final Deque<Boolean> recentOutcomes = new ArrayDeque<>();
  private final boolean enabled;
  private final int windowSize;
  private final double promoteThreshold;
  private final double demoteThreshold;

  public AdaptiveDifficultyService(
      TrainingLifecycleSubscriberRouter router,
      @Value("${scape.adaptive-difficulty.enabled:true}") boolean enabled,
      @Value("${scape.adaptive-difficulty.window-size:8}") int windowSize,
      @Value("${scape.adaptive-difficulty.promote-threshold:0.70}") double promoteThreshold,
      @Value("${scape.adaptive-difficulty.demote-threshold:0.35}") double demoteThreshold) {
    this.enabled = enabled;
    this.windowSize = Math.max(1, windowSize);
    this.promoteThreshold = clamp(promoteThreshold);
    this.demoteThreshold = clamp(demoteThreshold);
    router.register(
        "adaptive-difficulty",
        EnumSet.of(TrainingLifecycleEventType.FINISHED, TrainingLifecycleEventType.TIMED_OUT),
        event -> recordOutcome(event.type() == TrainingLifecycleEventType.FINISHED));
  }

  AdaptiveDifficultyService(
      boolean enabled, int windowSize, double promoteThreshold, double demoteThreshold) {
    this.enabled = enabled;
    this.windowSize = Math.max(1, windowSize);
    this.promoteThreshold = clamp(promoteThreshold);
    this.demoteThreshold = clamp(demoteThreshold);
  }

  public synchronized AdaptiveDifficultyDecision resolve(TrainingTargetDifficulty requested) {
    TrainingTargetDifficulty safeRequested =
        requested == null ? TrainingTargetDifficulty.MEDIUM : requested;
    if (!enabled || recentOutcomes.isEmpty()) {
      return new AdaptiveDifficultyDecision(
          safeRequested, safeRequested, successRate(), recentOutcomes.size(), false, "adaptive disabled or no history");
    }
    double rate = successRate();
    TrainingTargetDifficulty resolved = safeRequested;
    String reason = "within thresholds";
    if (rate >= promoteThreshold) {
      resolved = safeRequested.harder();
      reason = "success-rate promoted";
    } else if (rate <= demoteThreshold) {
      resolved = safeRequested.easier();
      reason = "success-rate demoted";
    }
    return new AdaptiveDifficultyDecision(
        safeRequested, resolved, rate, recentOutcomes.size(), true, reason);
  }

  synchronized void recordOutcome(boolean success) {
    recentOutcomes.addLast(success);
    while (recentOutcomes.size() > windowSize) {
      recentOutcomes.removeFirst();
    }
  }

  private double successRate() {
    if (recentOutcomes.isEmpty()) {
      return 0.0;
    }
    int success = 0;
    for (boolean outcome : recentOutcomes) {
      if (outcome) {
        success++;
      }
    }
    return (double) success / (double) recentOutcomes.size();
  }

  private double clamp(double value) {
    return Math.max(0.0, Math.min(1.0, value));
  }
}
