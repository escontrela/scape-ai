package com.davidpe.scapeai.application;

import com.davidpe.scapeai.ui.MazeCatalogService;
import org.springframework.stereotype.Service;

@Service
public class ApplicationStartTrainingSessionUseCase implements StartTrainingSessionUseCase {

  private final SimulationControlService simulationControlService;
  private final MazeCatalogService mazeCatalogService;
  private final SessionRandomSource sessionRandomSource;
  private final ExplorationBudgetService explorationBudgetService;
  private final AdaptiveDifficultyService adaptiveDifficultyService;
  private final TrainingSessionConfigValidator configValidator;
  private final TrainingSessionContextHolder trainingSessionContextHolder;

  public ApplicationStartTrainingSessionUseCase(
      SimulationControlService simulationControlService,
      MazeCatalogService mazeCatalogService,
      SessionRandomSource sessionRandomSource,
      ExplorationBudgetService explorationBudgetService,
      AdaptiveDifficultyService adaptiveDifficultyService,
      TrainingSessionConfigValidator configValidator,
      TrainingSessionContextHolder trainingSessionContextHolder) {
    this.simulationControlService = simulationControlService;
    this.mazeCatalogService = mazeCatalogService;
    this.sessionRandomSource = sessionRandomSource;
    this.explorationBudgetService = explorationBudgetService;
    this.adaptiveDifficultyService = adaptiveDifficultyService;
    this.configValidator = configValidator;
    this.trainingSessionContextHolder = trainingSessionContextHolder;
  }

  @Override
  public StartTrainingSessionResult start(StartTrainingSessionCommand command) {
    if (command == null) {
      return StartTrainingSessionResult.validationError("Select a maze before starting.");
    }
    TrainingSessionConfig sessionConfig = command.sessionConfig();
    try {
      configValidator.ensureValid(sessionConfig);
    } catch (TrainingSessionConfigValidationException validationException) {
      return StartTrainingSessionResult.validationError(
          validationException.getMessage(), validationException.errors());
    }

    TrainingTargetDifficulty requestedDifficulty =
        sessionConfig.difficultyTarget() == null
            ? TrainingTargetDifficulty.MEDIUM
            : sessionConfig.difficultyTarget();
    AdaptiveDifficultyDecision difficultyDecision =
        adaptiveDifficultyService.resolve(requestedDifficulty);
    TrainingTargetDifficulty targetDifficulty = difficultyDecision.resolved();
    var selectedMaze =
        mazeCatalogService.findCandidateByDifficulty(targetDifficulty).orElse(command.maze());
    if (selectedMaze == null) {
      return StartTrainingSessionResult.validationError("Select a maze before starting.");
    }

    if (simulationControlService.availableMovementPolicies().isEmpty()) {
      return StartTrainingSessionResult.validationError("No movement policy available.");
    }

    String activePolicy = sessionConfig.policyId();
    boolean policyExists =
        simulationControlService.availableMovementPolicies().stream()
            .anyMatch(option -> option.id().equals(activePolicy));
    if (!policyExists) {
      return StartTrainingSessionResult.validationError("Active policy is not valid.");
    }
    simulationControlService.selectMovementPolicy(activePolicy);

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
      return StartTrainingSessionResult.validationError(
          "Select a training preset before starting.");
    }

    if (mazeCatalogService.findCandidateByDifficulty(targetDifficulty).isEmpty()) {
      return StartTrainingSessionResult.validationError(
          "No mazes available for selected difficulty. Choose another level or add more mazes.");
    }

    long effectiveSeed = sessionRandomSource.resolveAndApplySeed(sessionConfig.seed());
    trainingSessionContextHolder.activate(
        sessionConfig.mazeId(),
        sessionConfig.policyId(),
        simulationControlService.activeTrainingPresetId(),
        effectiveSeed,
        System.currentTimeMillis());
    explorationBudgetService.startSession(
        simulationControlService.activeTrainingPresetId(),
        simulationControlService.activeMovementPolicy());
    simulationControlService.start();
    return StartTrainingSessionResult.ok(
        "TRAINING RUNNING — TARGET "
            + targetDifficulty.label().toUpperCase()
            + " — ADAPT "
            + requestedDifficulty.label().toUpperCase()
            + "->"
            + targetDifficulty.label().toUpperCase()
            + " (SR "
            + String.format(java.util.Locale.ROOT, "%.2f", difficultyDecision.successRate())
            + " n="
            + difficultyDecision.sampleSize()
            + ")"
            + " — SEED "
            + effectiveSeed,
        selectedMaze,
        effectiveSeed);
  }
}
