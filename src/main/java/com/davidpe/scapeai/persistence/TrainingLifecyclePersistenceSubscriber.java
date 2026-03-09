package com.davidpe.scapeai.persistence;

import com.davidpe.scapeai.application.TrainingLifecycleEvent;
import com.davidpe.scapeai.application.TrainingLifecycleEventType;
import com.davidpe.scapeai.application.TrainingLifecycleSubscriberRouter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TrainingLifecyclePersistenceSubscriber {

  private final List<TrainingLifecycleEvent> receivedEvents = new ArrayList<>();

  public TrainingLifecyclePersistenceSubscriber(TrainingLifecycleSubscriberRouter router) {
    router.register(
        "persistence-subscriber",
        EnumSet.allOf(TrainingLifecycleEventType.class),
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

}
