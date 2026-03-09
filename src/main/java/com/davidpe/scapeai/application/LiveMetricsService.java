package com.davidpe.scapeai.application;

import java.util.function.Consumer;

public interface LiveMetricsService {

  void startEpisode();

  void pauseEpisode();

  void resetEpisode();

  void setSimulationSpeed(SimulationSpeed speed);

  SimulationSpeed simulationSpeed();

  void subscribe(Consumer<LiveEpisodeMetrics> listener);
}
