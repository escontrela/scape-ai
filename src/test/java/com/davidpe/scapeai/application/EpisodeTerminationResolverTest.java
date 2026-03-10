package com.davidpe.scapeai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class EpisodeTerminationResolverTest {

  @Test
  void shouldResolveSingleTerminalReason() {
    assertEquals(
        EpisodeEndReason.EXIT_REACHED,
        EpisodeTerminationResolver.resolve(true, false, false, false));
    assertEquals(
        EpisodeEndReason.TIMEOUT,
        EpisodeTerminationResolver.resolve(false, true, false, false));
    assertEquals(
        EpisodeEndReason.DEAD_END,
        EpisodeTerminationResolver.resolve(false, false, true, false));
    assertEquals(
        EpisodeEndReason.ABORTED,
        EpisodeTerminationResolver.resolve(false, false, false, true));
  }

  @Test
  void shouldRejectIncompatibleTerminalCombinations() {
    assertThrows(
        IllegalStateException.class, () -> EpisodeTerminationResolver.resolve(true, true, false, false));
    assertThrows(
        IllegalStateException.class, () -> EpisodeTerminationResolver.resolve(false, false, false, false));
  }
}
