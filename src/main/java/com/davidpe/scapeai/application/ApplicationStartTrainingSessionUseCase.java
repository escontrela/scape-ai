package com.davidpe.scapeai.application;

import com.davidpe.scapeai.ui.MazeCatalogService;
import org.springframework.stereotype.Service;

@Service
public class ApplicationStartTrainingSessionUseCase implements StartTrainingSessionUseCase {

  private final SimulationControlService simulationControlService;
  private final MazeCatalogService mazeCatalogService;

  public ApplicationStartTrainingSessionUseCase(
      SimulationControlService simulationControlService, MazeCatalogService mazeCatalogService) {
    this.simulationControlService = simulationControlService;
    this.mazeCatalogService = mazeCatalogService;
  }

  @Override
  public StartTrainingSessionResult start(StartTrainingSessionCommand command) {
    if (command == null) {
      return StartTrainingSessionResult.validationError("Select a maze before starting.");
    }

    TrainingTargetDifficulty targetDifficulty =
        command.targetDifficulty() == null ? TrainingTargetDifficulty.MEDIUM : command.targetDifficulty();
    var selectedMaze = mazeCatalogService.findCandidateByDifficulty(targetDifficulty).orElse(command.maze());
    if (selectedMaze == null) {
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

    if (mazeCatalogService.findCandidateByDifficulty(targetDifficulty).isEmpty()) {
      return StartTrainingSessionResult.validationError(
          "No mazes available for selected difficulty. Choose another level or add more mazes.");
    }

    simulationControlService.start();
    return StartTrainingSessionResult.ok(
        "TRAINING RUNNING — TARGET " + targetDifficulty.label().toUpperCase(), selectedMaze);
  }
}
