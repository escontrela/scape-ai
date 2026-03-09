package com.davidpe.scapeai.persistence;

import com.davidpe.scapeai.application.TrainingLifecycleEvent;
import com.davidpe.scapeai.application.TrainingLifecycleEventBus;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TrainingLifecyclePersistenceSubscriber {

  private final List<TrainingLifecycleEvent> receivedEvents = new ArrayList<>();
  private final TrainingLifecycleEventBus.Subscription subscription;

  public TrainingLifecyclePersistenceSubscriber(TrainingLifecycleEventBus trainingLifecycleEventBus) {
    this.subscription =
        trainingLifecycleEventBus.subscribe(
            event -> {
              synchronized (receivedEvents) {
                receivedEvents.add(event);
              }
            });
  }

  public List<TrainingLifecycleEvent> receivedEvents() {
    synchronized (receivedEvents) {
      return List.copyOf(receivedEvents);
    }
  }

  @PreDestroy
  void shutdown() {
    subscription.unsubscribe();
  }
}
