package com.davidpe.scapeai.application;

public record TrainingLifecycleEvent(
    TrainingLifecycleEventType type, long occurredAtMillis, String detail) {

  public static TrainingLifecycleEvent now(TrainingLifecycleEventType type, String detail) {
    return new TrainingLifecycleEvent(type, System.currentTimeMillis(), detail);
  }
}
