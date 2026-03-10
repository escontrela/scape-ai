package com.davidpe.scapeai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class EpsilonPhaseSchedulerTest {

  @Test
  void shouldReturnPhaseEpsilonByEpisodeProgress() {
    EpsilonPhaseScheduler scheduler = new EpsilonPhaseScheduler(0.40, 0.20, 0.05);

    assertEquals(0.40, scheduler.epsilonForEpisode(0, 9));
    assertEquals(0.20, scheduler.epsilonForEpisode(4, 9));
    assertEquals(0.05, scheduler.epsilonForEpisode(8, 9));
  }

  @Test
  void shouldRejectOutOfRangePhaseValues() {
    assertThrows(IllegalArgumentException.class, () -> new EpsilonPhaseScheduler(-0.1, 0.2, 0.1));
    assertThrows(IllegalArgumentException.class, () -> new EpsilonPhaseScheduler(0.1, 1.2, 0.1));
  }
}
