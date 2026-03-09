package com.davidpe.scapeai.application;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;

@Service
public class InMemoryTrainingLifecycleEventBus implements TrainingLifecycleEventBus {

  private final List<Consumer<TrainingLifecycleEvent>> listeners = new CopyOnWriteArrayList<>();

  @Override
  public void publish(TrainingLifecycleEvent event) {
    for (Consumer<TrainingLifecycleEvent> listener : listeners) {
      listener.accept(event);
    }
  }

  @Override
  public Subscription subscribe(Consumer<TrainingLifecycleEvent> listener) {
    listeners.add(listener);
    return () -> listeners.remove(listener);
  }
}
