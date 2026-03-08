package com.davidpe.scapeai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InMemoryLiveMetricsServiceTest {

  @Test
  void shouldUpdateMetricsWhileEpisodeIsRunningAndResetForNewEpisode() throws Exception {
    InMemoryLiveMetricsService service = new InMemoryLiveMetricsService();
    try {
      service.startEpisode();
      Thread.sleep(450);

      LiveEpisodeMetrics firstSnapshot = captureLatest(service);
      assertTrue(firstSnapshot.steps() > 0);
      assertTrue(firstSnapshot.elapsedMillis() > 0);

      service.startEpisode();
      LiveEpisodeMetrics secondSnapshot = captureLatest(service);
      assertEquals(0, secondSnapshot.steps());
      assertEquals(0, secondSnapshot.collisions());
      assertEquals(0.0, secondSnapshot.accumulatedReward(), 0.0001);
    } finally {
      service.shutdown();
    }
  }

  private LiveEpisodeMetrics captureLatest(InMemoryLiveMetricsService service) {
    final LiveEpisodeMetrics[] holder = new LiveEpisodeMetrics[1];
    service.subscribe(metrics -> holder[0] = metrics);
    return holder[0];
  }
}
