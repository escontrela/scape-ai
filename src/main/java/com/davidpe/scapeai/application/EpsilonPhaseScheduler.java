package com.davidpe.scapeai.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EpsilonPhaseScheduler {

  private final double startEpsilon;
  private final double middleEpsilon;
  private final double endEpsilon;

  public EpsilonPhaseScheduler(
      @Value("${scape.ai.epsilon.start:0.35}") double startEpsilon,
      @Value("${scape.ai.epsilon.middle:0.20}") double middleEpsilon,
      @Value("${scape.ai.epsilon.end:0.05}") double endEpsilon) {
    this.startEpsilon = validate(startEpsilon, "start");
    this.middleEpsilon = validate(middleEpsilon, "middle");
    this.endEpsilon = validate(endEpsilon, "end");
  }

  public double epsilonForEpisode(int episodeIndexZeroBased, int totalEpisodes) {
    int safeTotal = Math.max(1, totalEpisodes);
    int safeIndex = Math.max(0, episodeIndexZeroBased);
    if (safeTotal <= 1) {
      return startEpsilon;
    }
    double progress = (double) safeIndex / (double) Math.max(1, safeTotal - 1);
    if (progress < (1.0 / 3.0)) {
      return startEpsilon;
    }
    if (progress < (2.0 / 3.0)) {
      return middleEpsilon;
    }
    return endEpsilon;
  }

  private double validate(double epsilon, String phase) {
    if (epsilon < 0.0 || epsilon > 1.0) {
      throw new IllegalArgumentException("scape.ai.epsilon." + phase + " must be in range [0,1]");
    }
    return epsilon;
  }
}
