package com.davidpe.scapeai.application;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

@Component
public class TrainingLifecycleTelemetrySubscriber {

  private final TrainingLifecycleEventBus.Subscription subscription;

  public TrainingLifecycleTelemetrySubscriber(
      TrainingLifecycleEventBus trainingLifecycleEventBus, LiveMetricsService liveMetricsService) {
    this.subscription =
        trainingLifecycleEventBus.subscribe(
            event -> {
              switch (event.type()) {
                case STARTED -> liveMetricsService.startEpisode();
                case PAUSED -> liveMetricsService.pauseEpisode();
                case RESUMED -> liveMetricsService.resumeEpisode();
                case FINISHED -> {
                  liveMetricsService.completeEpisode();
                  liveMetricsService.resetEpisode();
                }
                case TIMED_OUT -> liveMetricsService.completeEpisode();
              }
            });
  }

  @PreDestroy
  void shutdown() {
    subscription.unsubscribe();
  }
}
