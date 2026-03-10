package com.davidpe.scapeai.application;

import java.util.EnumSet;
import org.springframework.stereotype.Component;

@Component
public class TrainingLifecycleTelemetrySubscriber {

  public TrainingLifecycleTelemetrySubscriber(
      TrainingLifecycleSubscriberRouter router,
      LiveMetricsService liveMetricsService,
      TrainingPresetService trainingPresetService) {
    router.register(
        "telemetry-subscriber",
        EnumSet.allOf(TrainingLifecycleEventType.class),
        event -> {
          java.time.Duration activeTimeout =
              trainingPresetService.activePreset().map(TrainingPreset::timeout).orElse(java.time.Duration.ZERO);
          switch (event.type()) {
            case STARTED -> liveMetricsService.startEpisode(activeTimeout);
            case PAUSED -> liveMetricsService.pauseEpisode();
            case RESUMED -> liveMetricsService.resumeEpisode();
            case FINISHED -> {
              liveMetricsService.completeEpisode();
              liveMetricsService.resetEpisode();
            }
            case TIMED_OUT -> liveMetricsService.completeEpisodeAtTimeout(activeTimeout);
          }
        });
  }
}
