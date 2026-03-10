package com.davidpe.scapeai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TrainingHealthIndexFormulaTest {

  @Test
  void shouldRewardHealthySignalsAndPenalizeTimeoutRatio() {
    double healthy = TrainingHealthIndexFormula.calculate(true, 0.8, 1.6, 0.0);
    double degraded = TrainingHealthIndexFormula.calculate(false, 0.4, 0.6, 0.7);

    assertTrue(healthy > degraded);
    assertTrue(healthy <= 100.0);
    assertTrue(degraded >= 0.0);
  }

  @Test
  void shouldComputeTimeoutRatioFromRecentWindow() {
    double ratio = TrainingHealthIndexFormula.timeoutRatio(List.of(true, false, true, false, false));

    assertEquals(0.4, ratio, 0.0001);
  }
}
