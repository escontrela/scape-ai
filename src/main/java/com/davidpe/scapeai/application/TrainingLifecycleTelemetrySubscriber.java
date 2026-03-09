package com.davidpe.scapeai.application;

import java.util.EnumSet;
import org.springframework.stereotype.Component;

@Component
public class TrainingLifecycleTelemetrySubscriber {

  public TrainingLifecycleTelemetrySubscriber(
      TrainingLifecycleSubscriberRouter router, LiveMetricsService liveMetricsService) {
    router.register(
        "telemetry-subscriber",
        EnumSet.allOf(TrainingLifecycleEventType.class),
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
}
