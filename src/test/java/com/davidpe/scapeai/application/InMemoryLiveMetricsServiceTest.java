package com.davidpe.scapeai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryLiveMetricsServiceTest {

  @Test
  void shouldUpdateMetricsWhileEpisodeIsRunningAndResetForNewEpisode() throws Exception {
    InMemoryLiveMetricsService service = new InMemoryLiveMetricsService();
    try {
      service.setSimulationSpeed(SimulationSpeed.SLOW);
      service.startEpisode(Duration.ofSeconds(2));
      Thread.sleep(420);

      LiveEpisodeMetrics firstSnapshot = captureLatest(service);
      assertTrue(firstSnapshot.steps() > 0);
      assertTrue(firstSnapshot.elapsedMillis() > 0);
      assertTrue(firstSnapshot.remainingMillis() < 2_000);
      assertTrue(firstSnapshot.leftSideCoverage() >= 0.0);
      assertTrue(firstSnapshot.rightSideCoverage() >= 0.0);

      service.setSimulationSpeed(SimulationSpeed.FAST);
      Thread.sleep(320);
      LiveEpisodeMetrics speedChangedSnapshot = captureLatest(service);
      assertTrue(speedChangedSnapshot.steps() > firstSnapshot.steps());

      service.startEpisode(Duration.ofSeconds(2));
      LiveEpisodeMetrics secondSnapshot = captureLatest(service);
      assertEquals(0, secondSnapshot.steps());
      assertEquals(0, secondSnapshot.collisions());
      assertEquals(0.0, secondSnapshot.accumulatedReward(), 0.0001);
      assertEquals(2_000, secondSnapshot.remainingMillis());
      assertEquals(0.0, secondSnapshot.leftSideCoverage(), 0.0001);
      assertEquals(0.0, secondSnapshot.rightSideCoverage(), 0.0001);
    } finally {
      service.shutdown();
    }
  }

  @Test
  void shouldPublishTimelineEntryWhenEpisodeCompletes() throws Exception {
    InMemoryLiveMetricsService service = new InMemoryLiveMetricsService();
    try {
      service.setSimulationSpeed(SimulationSpeed.FAST);
      service.startEpisode(Duration.ofSeconds(1));
      Thread.sleep(220);
      service.completeEpisodeAtTimeout(Duration.ofSeconds(1));

      List<TrainingTimelineEntry>[] holder = new List[] {List.of()};
      service.subscribeTimeline(entries -> holder[0] = entries);

      assertEquals(1, holder[0].size());
      assertEquals(1_000, holder[0].get(0).durationMillis());
      assertEquals(TrainingTimelineStatus.TIMEOUT, holder[0].get(0).status());
    } finally {
      service.shutdown();
    }
  }

  @Test
  void shouldResumeEpisodeWithoutResettingMetrics() throws Exception {
    InMemoryLiveMetricsService service = new InMemoryLiveMetricsService();
    try {
      service.setSimulationSpeed(SimulationSpeed.FAST);
      service.startEpisode(Duration.ofSeconds(3));
      Thread.sleep(140);
      service.pauseEpisode();
      LiveEpisodeMetrics pausedSnapshot = captureLatest(service);

      Thread.sleep(100);
      service.resumeEpisode();
      Thread.sleep(140);
      LiveEpisodeMetrics resumedSnapshot = captureLatest(service);

      assertTrue(resumedSnapshot.steps() >= pausedSnapshot.steps());
      assertTrue(resumedSnapshot.elapsedMillis() > pausedSnapshot.elapsedMillis());
      assertTrue(resumedSnapshot.remainingMillis() <= pausedSnapshot.remainingMillis());
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
