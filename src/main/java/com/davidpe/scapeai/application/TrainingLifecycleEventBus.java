package com.davidpe.scapeai.application;

import java.util.function.Consumer;

public interface TrainingLifecycleEventBus {

  void publish(TrainingLifecycleEvent event);

  Subscription subscribe(Consumer<TrainingLifecycleEvent> listener);

  static TrainingLifecycleEventBus noop() {
    return new TrainingLifecycleEventBus() {
      @Override
      public void publish(TrainingLifecycleEvent event) {}

      @Override
      public Subscription subscribe(Consumer<TrainingLifecycleEvent> listener) {
        return () -> {};
      }
    };
  }

  @FunctionalInterface
  interface Subscription {
    void unsubscribe();
  }
}
