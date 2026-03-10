package com.davidpe.scapeai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AdaptiveDifficultyServiceTest {

  @Test
  void shouldKeepRequestedDifficultyWhenAdaptiveIsDisabled() {
    AdaptiveDifficultyService service = new AdaptiveDifficultyService(false, 6, 0.70, 0.35);

    AdaptiveDifficultyDecision decision = service.resolve(TrainingTargetDifficulty.MEDIUM);

    assertEquals(TrainingTargetDifficulty.MEDIUM, decision.resolved());
    assertEquals(false, decision.adaptiveApplied());
  }

  @Test
  void shouldPromoteAndDemoteAccordingToSlidingWindow() {
    AdaptiveDifficultyService service = new AdaptiveDifficultyService(true, 4, 0.70, 0.35);
    service.recordOutcome(true);
    service.recordOutcome(true);
    service.recordOutcome(true);

    AdaptiveDifficultyDecision promoted = service.resolve(TrainingTargetDifficulty.MEDIUM);
    assertEquals(TrainingTargetDifficulty.HIGH, promoted.resolved());

    service.recordOutcome(false);
    service.recordOutcome(false);
    service.recordOutcome(false);
    service.recordOutcome(false);

    AdaptiveDifficultyDecision demoted = service.resolve(TrainingTargetDifficulty.MEDIUM);
    assertEquals(TrainingTargetDifficulty.LOW, demoted.resolved());
  }
}
