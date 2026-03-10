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

  /** Keep at most the last 500 lifecycle events in memory. */
  private static final int MAX_RETAINED_EVENTS = 500;

  private final List<TrainingLifecycleEvent> receivedEvents = new ArrayList<>();

  public TrainingLifecyclePersistenceSubscriber(TrainingLifecycleSubscriberRouter router) {
    router.register(
        "persistence-subscriber",
        EnumSet.allOf(TrainingLifecycleEventType.class),
        event -> {
          synchronized (receivedEvents) {
            receivedEvents.add(event);
            while (receivedEvents.size() > MAX_RETAINED_EVENTS) {
              receivedEvents.remove(0);
            }
          }
        });
  }

  public List<TrainingLifecycleEvent> receivedEvents() {
    synchronized (receivedEvents) {
      return List.copyOf(receivedEvents);
    }
  }
}
