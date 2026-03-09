package com.davidpe.scapeai.application;

import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class TrainingLifecycleSubscriberRouter {

  private final Map<TrainingLifecycleEventType, Map<String, Consumer<TrainingLifecycleEvent>>> handlersByType =
      new EnumMap<>(TrainingLifecycleEventType.class);
  private final TrainingLifecycleEventBus.Subscription busSubscription;

  public TrainingLifecycleSubscriberRouter(TrainingLifecycleEventBus trainingLifecycleEventBus) {
    for (TrainingLifecycleEventType type : TrainingLifecycleEventType.values()) {
      handlersByType.put(type, new java.util.LinkedHashMap<>());
    }
    this.busSubscription = trainingLifecycleEventBus.subscribe(this::dispatch);
  }

  public synchronized void register(
      String subscriberId,
      Set<TrainingLifecycleEventType> eventTypes,
      Consumer<TrainingLifecycleEvent> handler) {
    if (subscriberId == null || subscriberId.isBlank()) {
      throw new IllegalArgumentException("subscriberId must not be blank");
    }
    if (eventTypes == null || eventTypes.isEmpty()) {
      throw new IllegalArgumentException("eventTypes must not be empty");
    }
    if (handler == null) {
      throw new IllegalArgumentException("handler must not be null");
    }
    EnumSet<TrainingLifecycleEventType> selectedTypes = EnumSet.copyOf(eventTypes);
    for (TrainingLifecycleEventType type : selectedTypes) {
      Map<String, Consumer<TrainingLifecycleEvent>> handlers = handlersByType.get(type);
      if (handlers.containsKey(subscriberId)) {
        throw new IllegalStateException(
            "Duplicate training lifecycle subscriber '" + subscriberId + "' for event " + type);
      }
      handlers.put(subscriberId, handler);
    }
  }

  private void dispatch(TrainingLifecycleEvent event) {
    List<Consumer<TrainingLifecycleEvent>> handlers;
    synchronized (this) {
      handlers = new ArrayList<>(handlersByType.get(event.type()).values());
    }
    for (Consumer<TrainingLifecycleEvent> handler : handlers) {
      handler.accept(event);
    }
  }

  @PreDestroy
  void shutdown() {
    busSubscription.unsubscribe();
  }
}
