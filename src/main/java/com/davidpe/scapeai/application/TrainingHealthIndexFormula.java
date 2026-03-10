package com.davidpe.scapeai.application;

import java.util.List;

public final class TrainingHealthIndexFormula {

  public static final String FORMULA_VERSION = "v1.0.0";
  private static final double SUCCESS_WEIGHT = 0.35;
  private static final double COVERAGE_WEIGHT = 0.30;
  private static final double ENTROPY_WEIGHT = 0.20;
  private static final double TIMEOUT_WEIGHT = 0.15;
  private static final double MAX_EXPECTED_ENTROPY = 2.0;

  private TrainingHealthIndexFormula() {}

  public static double calculate(
      boolean success, double mazeCoverageRatio, double pathEntropy, double timeoutRatio) {
    double successSignal = success ? 1.0 : 0.0;
    double coverageSignal = clamp01(mazeCoverageRatio);
    double entropySignal = clamp01(pathEntropy / MAX_EXPECTED_ENTROPY);
    double timeoutSignal = 1.0 - clamp01(timeoutRatio);
    double weighted =
        (SUCCESS_WEIGHT * successSignal)
            + (COVERAGE_WEIGHT * coverageSignal)
            + (ENTROPY_WEIGHT * entropySignal)
            + (TIMEOUT_WEIGHT * timeoutSignal);
    return Math.max(0.0, Math.min(100.0, weighted * 100.0));
  }

  public static double timeoutRatio(List<Boolean> timeoutFlags) {
    if (timeoutFlags == null || timeoutFlags.isEmpty()) {
      return 0.0;
    }
    long timeouts = timeoutFlags.stream().filter(Boolean.TRUE::equals).count();
    return (double) timeouts / (double) timeoutFlags.size();
  }

  private static double clamp01(double value) {
    if (Double.isNaN(value)) {
      return 0.0;
    }
    return Math.max(0.0, Math.min(1.0, value));
  }
}
