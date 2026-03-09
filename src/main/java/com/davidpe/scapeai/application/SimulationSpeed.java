package com.davidpe.scapeai.application;

public enum SimulationSpeed {
  SLOW("Slow", 320L, 260L),
  NORMAL("Normal", 200L, 120L),
  FAST("Fast", 90L, 55L);

  private final String label;
  private final long metricsTickMillis;
  private final long trajectoryTickMillis;

  SimulationSpeed(String label, long metricsTickMillis, long trajectoryTickMillis) {
    this.label = label;
    this.metricsTickMillis = metricsTickMillis;
    this.trajectoryTickMillis = trajectoryTickMillis;
  }

  public String label() {
    return label;
  }

  public long metricsTickMillis() {
    return metricsTickMillis;
  }

  public long trajectoryTickMillis() {
    return trajectoryTickMillis;
  }
}
