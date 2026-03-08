package com.davidpe.scapeai.application;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class ApplicationSimulationControlService implements SimulationControlService {

  private final ApplicationEventPublisher publisher;

  public ApplicationSimulationControlService(ApplicationEventPublisher publisher) {
    this.publisher = publisher;
  }

  @Override
  public void start() {
    publish(SimulationCommand.START);
  }

  @Override
  public void pause() {
    publish(SimulationCommand.PAUSE);
  }

  @Override
  public void reset() {
    publish(SimulationCommand.RESET);
  }

  private void publish(SimulationCommand command) {
    publisher.publishEvent(new SimulationCommandEvent(command));
  }
}
