package com.davidpe.scapeai.application;

import java.time.Duration;
import java.util.function.Consumer;

public interface LiveMetricsService {

  void startEpisode();

  default void startEpisode(Duration timeout) {
    startEpisode();
  }

  void pauseEpisode();

  void resumeEpisode();

  void resetEpisode();

  void completeEpisode();

  default void completeEpisodeAtTimeout(Duration timeout) {
    completeEpisode();
  }

  void setSimulationSpeed(SimulationSpeed speed);

  SimulationSpeed simulationSpeed();

  void subscribe(Consumer<LiveEpisodeMetrics> listener);

  void subscribeTimeline(Consumer<java.util.List<TrainingTimelineEntry>> listener);
}
