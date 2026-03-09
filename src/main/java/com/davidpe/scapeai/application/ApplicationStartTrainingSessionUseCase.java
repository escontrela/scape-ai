package com.davidpe.scapeai.application;

import org.springframework.stereotype.Service;

@Service
public class ApplicationStartTrainingSessionUseCase implements StartTrainingSessionUseCase {

  private final SimulationControlService simulationControlService;

  public ApplicationStartTrainingSessionUseCase(SimulationControlService simulationControlService) {
    this.simulationControlService = simulationControlService;
  }

  @Override
  public StartTrainingSessionResult start(StartTrainingSessionCommand command) {
    if (command == null || command.maze() == null) {
      return StartTrainingSessionResult.validationError("Select a maze before starting.");
    }

    if (simulationControlService.availableMovementPolicies().isEmpty()) {
      return StartTrainingSessionResult.validationError("No movement policy available.");
    }

    String activePolicy = simulationControlService.activeMovementPolicy();
    boolean policyExists =
        simulationControlService.availableMovementPolicies().stream()
            .anyMatch(option -> option.id().equals(activePolicy));
    if (!policyExists) {
      return StartTrainingSessionResult.validationError("Active policy is not valid.");
    }

    Long presetId = command.presetId();
    if (presetId != null) {
      boolean presetExists =
          simulationControlService.availableTrainingPresets().stream()
              .anyMatch(option -> option.id() == presetId.longValue());
      if (!presetExists) {
        return StartTrainingSessionResult.validationError("Selected preset does not exist.");
      }
      simulationControlService.applyTrainingPreset(presetId);
    }

    if (simulationControlService.activeTrainingPresetId() == null) {
      return StartTrainingSessionResult.validationError("Select a training preset before starting.");
    }

    simulationControlService.start();
    return StartTrainingSessionResult.ok("TRAINING RUNNING");
  }
}
