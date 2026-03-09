package com.davidpe.scapeai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class TrainingLifecycleSubscriberRouterTest {

  @Test
  void shouldDispatchOnlyRegisteredEventTypes() {
    InMemoryTrainingLifecycleEventBus bus = new InMemoryTrainingLifecycleEventBus();
    TrainingLifecycleSubscriberRouter router = new TrainingLifecycleSubscriberRouter(bus);
    List<TrainingLifecycleEventType> received = new ArrayList<>();
    router.register("ui", EnumSet.of(TrainingLifecycleEventType.STARTED), event -> received.add(event.type()));

    bus.publish(TrainingLifecycleEvent.now(TrainingLifecycleEventType.STARTED, "start"));
    bus.publish(TrainingLifecycleEvent.now(TrainingLifecycleEventType.PAUSED, "pause"));

    assertEquals(List.of(TrainingLifecycleEventType.STARTED), received);
    router.shutdown();
  }

  @Test
  void shouldRejectDuplicateSubscriberRegistrationPerEventType() {
    InMemoryTrainingLifecycleEventBus bus = new InMemoryTrainingLifecycleEventBus();
    TrainingLifecycleSubscriberRouter router = new TrainingLifecycleSubscriberRouter(bus);
    router.register("ui", EnumSet.of(TrainingLifecycleEventType.STARTED), ignored -> {});

    assertThrows(
        IllegalStateException.class,
        () -> router.register("ui", EnumSet.of(TrainingLifecycleEventType.STARTED), ignored -> {}));
    router.shutdown();
  }
}
