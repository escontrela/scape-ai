package com.davidpe.scapeai.application;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class ApplicationSimulationControlService implements SimulationControlService {

  private final ApplicationEventPublisher publisher;
  private final ActiveMovementPolicyService activeMovementPolicyService;
  private final TrainingPresetService trainingPresetService;

  public ApplicationSimulationControlService(
      ApplicationEventPublisher publisher,
      ActiveMovementPolicyService activeMovementPolicyService,
      TrainingPresetService trainingPresetService) {
    this.publisher = publisher;
    this.activeMovementPolicyService = activeMovementPolicyService;
    this.trainingPresetService = trainingPresetService;
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

  @Override
  public java.util.List<TrainingPresetOption> availableTrainingPresets() {
    return trainingPresetService.list().stream()
        .map(
            preset ->
                new TrainingPresetOption(
                    preset.id(),
                    "Preset #"
                        + preset.id()
                        + " - "
                        + preset.episodes()
                        + " ep - "
                        + preset.policy()))
        .toList();
  }

  @Override
  public void applyTrainingPreset(long presetId) {
    trainingPresetService
        .apply(presetId)
        .orElseThrow(() -> new IllegalArgumentException("Preset not found: " + presetId));
  }

  @Override
  public Long activeTrainingPresetId() {
    return trainingPresetService.activePreset().map(TrainingPreset::id).orElse(null);
  }

  private void publish(SimulationCommand command) {
    publisher.publishEvent(new SimulationCommandEvent(command));
  }
}
