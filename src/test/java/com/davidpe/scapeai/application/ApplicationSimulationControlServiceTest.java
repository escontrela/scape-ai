package com.davidpe.scapeai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.davidpe.scapeai.ai.MovementPolicy;
import com.davidpe.scapeai.simulation.MoveDirection;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

class ApplicationSimulationControlServiceTest {

  private CapturingPublisher publisher;
  private ActiveMovementPolicyService movementPolicyService;
  private ApplicationSimulationControlService service;

  @BeforeEach
  void setUp() {
    publisher = new CapturingPublisher();
    MovementPolicy dummyPolicy = context -> MoveDirection.UP;
    movementPolicyService =
        new ActiveMovementPolicyService(
            Map.of("heuristic-baseline", dummyPolicy, "random-controlled", dummyPolicy),
            "heuristic-baseline");
    service = new ApplicationSimulationControlService(publisher, movementPolicyService);
  }

  @Test
  void shouldPublishStartCommand() {
    service.start();
    assertEquals(SimulationCommand.START, publisher.lastCommand.command());
  }

  @Test
  void shouldPublishPauseCommand() {
    service.pause();
    assertEquals(SimulationCommand.PAUSE, publisher.lastCommand.command());
  }

  @Test
  void shouldPublishResetCommand() {
    service.reset();
    assertEquals(SimulationCommand.RESET, publisher.lastCommand.command());
  }

  @Test
  void shouldSwitchActiveMovementPolicy() {
    service.selectMovementPolicy("random-controlled");
    assertEquals("random-controlled", service.activeMovementPolicy());
  }

  private static final class CapturingPublisher implements ApplicationEventPublisher {

    private SimulationCommandEvent lastCommand;

    @Override
    public void publishEvent(Object event) {
      this.lastCommand = (SimulationCommandEvent) event;
    }

    @Override
    public void publishEvent(ApplicationEvent event) {
      this.lastCommand = (SimulationCommandEvent) event.getSource();
    }
  }
}
