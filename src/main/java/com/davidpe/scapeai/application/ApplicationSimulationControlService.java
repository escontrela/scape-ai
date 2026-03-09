package com.davidpe.scapeai.application;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class ApplicationSimulationControlService implements SimulationControlService {

  private final ApplicationEventPublisher publisher;
  private final ActiveMovementPolicyService activeMovementPolicyService;

  public ApplicationSimulationControlService(
      ApplicationEventPublisher publisher, ActiveMovementPolicyService activeMovementPolicyService) {
    this.publisher = publisher;
    this.activeMovementPolicyService = activeMovementPolicyService;
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

  @Override
  public void selectMovementPolicy(String policyId) {
    activeMovementPolicyService.selectPolicy(policyId);
  }

  @Override
  public String activeMovementPolicy() {
    return activeMovementPolicyService.activePolicyId();
  }

  @Override
  public java.util.List<MovementPolicyOption> availableMovementPolicies() {
    return activeMovementPolicyService.availablePolicies();
  }

  private void publish(SimulationCommand command) {
    publisher.publishEvent(new SimulationCommandEvent(command));
  }
}
