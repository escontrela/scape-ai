package com.davidpe.scapeai.application;

import java.util.function.Consumer;

public interface LiveMetricsService {

  void startEpisode();

  void pauseEpisode();

  void resetEpisode();

  void subscribe(Consumer<LiveEpisodeMetrics> listener);
}
