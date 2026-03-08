package com.davidpe.scapeai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

class ApplicationSimulationControlServiceTest {

  private CapturingPublisher publisher;
  private ApplicationSimulationControlService service;

  @BeforeEach
  void setUp() {
    publisher = new CapturingPublisher();
    service = new ApplicationSimulationControlService(publisher);
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
