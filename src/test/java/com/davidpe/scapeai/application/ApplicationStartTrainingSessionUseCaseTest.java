package com.davidpe.scapeai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.scapeai.simulation.GridPosition;
import com.davidpe.scapeai.simulation.MazeDefinition;
import com.davidpe.scapeai.simulation.MoveDirection;
import com.davidpe.scapeai.persistence.ExplorationBudgetEntity;
import com.davidpe.scapeai.persistence.repository.ExplorationBudgetRepository;
import com.davidpe.scapeai.ui.MazeCatalogService;
import com.davidpe.scapeai.ui.MazeJsonResourceLoader;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ApplicationStartTrainingSessionUseCaseTest {

  @Test
  void shouldStartWhenMazePolicyAndPresetAreValid() {
    StubSimulationControlService controlService = new StubSimulationControlService();
    StubMazeCatalogService mazeCatalogService = new StubMazeCatalogService();
    SessionRandomSource randomSource = new SessionRandomSource(20260309L);
    ApplicationStartTrainingSessionUseCase useCase =
        new ApplicationStartTrainingSessionUseCase(
            controlService, mazeCatalogService, randomSource, noOpExplorationBudgetService());
    MazeDefinition maze =
        new MazeDefinition(2, 2, new boolean[2][2], new GridPosition(0, 0), new GridPosition(1, 1));

    StartTrainingSessionResult result =
        useCase.start(new StartTrainingSessionCommand(maze, 1L, TrainingTargetDifficulty.LOW, 77L));

    assertTrue(result.started());
    assertEquals("TRAINING RUNNING — TARGET BAJA — SEED 77", result.message());
    assertNotNull(result.maze());
    assertEquals(Long.valueOf(77L), result.effectiveSeed());
    assertTrue(controlService.started);
    assertEquals(Long.valueOf(1L), controlService.activeTrainingPresetId());
  }

  @Test
  void shouldReturnValidationErrorWhenMazeIsMissing() {
    StubSimulationControlService controlService = new StubSimulationControlService();
    StubMazeCatalogService mazeCatalogService = new StubMazeCatalogService();
    mazeCatalogService.lowCandidate = Optional.empty();
    SessionRandomSource randomSource = new SessionRandomSource(20260309L);
    ApplicationStartTrainingSessionUseCase useCase =
        new ApplicationStartTrainingSessionUseCase(
            controlService, mazeCatalogService, randomSource, noOpExplorationBudgetService());

    StartTrainingSessionResult result =
        useCase.start(new StartTrainingSessionCommand(null, 1L, TrainingTargetDifficulty.LOW, null));

    assertFalse(result.started());
    assertEquals("Select a maze before starting.", result.message());
    assertFalse(controlService.started);
  }

  @Test
  void shouldReturnValidationErrorWhenPresetDoesNotExist() {
    StubSimulationControlService controlService = new StubSimulationControlService();
    StubMazeCatalogService mazeCatalogService = new StubMazeCatalogService();
    SessionRandomSource randomSource = new SessionRandomSource(20260309L);
    ApplicationStartTrainingSessionUseCase useCase =
        new ApplicationStartTrainingSessionUseCase(
            controlService, mazeCatalogService, randomSource, noOpExplorationBudgetService());
    MazeDefinition maze =
        new MazeDefinition(2, 2, new boolean[2][2], new GridPosition(0, 0), new GridPosition(1, 1));

    StartTrainingSessionResult result =
        useCase.start(
            new StartTrainingSessionCommand(maze, 999L, TrainingTargetDifficulty.MEDIUM, null));

    assertFalse(result.started());
    assertEquals("Selected preset does not exist.", result.message());
    assertFalse(controlService.started);
  }

  @Test
  void shouldReturnValidationErrorWhenDifficultyHasNoCandidate() {
    StubSimulationControlService controlService = new StubSimulationControlService();
    StubMazeCatalogService mazeCatalogService = new StubMazeCatalogService();
    mazeCatalogService.mediumCandidate = Optional.empty();
    SessionRandomSource randomSource = new SessionRandomSource(20260309L);
    ApplicationStartTrainingSessionUseCase useCase =
        new ApplicationStartTrainingSessionUseCase(
            controlService, mazeCatalogService, randomSource, noOpExplorationBudgetService());
    MazeDefinition maze =
        new MazeDefinition(2, 2, new boolean[2][2], new GridPosition(0, 0), new GridPosition(1, 1));

    StartTrainingSessionResult result =
        useCase.start(new StartTrainingSessionCommand(maze, 1L, TrainingTargetDifficulty.MEDIUM, null));

    assertFalse(result.started());
    assertEquals(
        "No mazes available for selected difficulty. Choose another level or add more mazes.",
        result.message());
    assertFalse(controlService.started);
  }

  @Test
  void shouldGenerateEffectiveSeedWhenNotProvided() {
    StubSimulationControlService controlService = new StubSimulationControlService();
    StubMazeCatalogService mazeCatalogService = new StubMazeCatalogService();
    SessionRandomSource randomSource = new SessionRandomSource(20260309L);
    ApplicationStartTrainingSessionUseCase useCase =
        new ApplicationStartTrainingSessionUseCase(
            controlService, mazeCatalogService, randomSource, noOpExplorationBudgetService());
    MazeDefinition maze =
        new MazeDefinition(2, 2, new boolean[2][2], new GridPosition(0, 0), new GridPosition(1, 1));

    StartTrainingSessionResult result =
        useCase.start(new StartTrainingSessionCommand(maze, 1L, TrainingTargetDifficulty.HIGH, null));

    assertTrue(result.started());
    assertNotNull(result.effectiveSeed());
    assertTrue(result.message().contains("SEED " + result.effectiveSeed()));
  }

  private static final class StubSimulationControlService implements SimulationControlService {

    private final ActiveMovementPolicyService policyService =
        new ActiveMovementPolicyService(
            Map.of(
                "heuristic-baseline", context -> MoveDirection.RIGHT,
                "random-controlled", context -> MoveDirection.UP),
            "heuristic-baseline");
    private final List<TrainingPresetOption> presets =
        List.of(
            new TrainingPresetOption(1L, "Preset #1 - 25 ep - heuristic-baseline"),
            new TrainingPresetOption(2L, "Preset #2 - 40 ep - random-controlled"));
    private Long activePresetId = 1L;
    private boolean started = false;

    @Override
    public void start() {
      started = true;
    }

    @Override
    public void pause() {}

    @Override
    public void reset() {}

    @Override
    public void selectMovementPolicy(String policyId) {
      policyService.selectPolicy(policyId);
    }

    @Override
    public String activeMovementPolicy() {
      return policyService.activePolicyId();
    }

    @Override
    public List<MovementPolicyOption> availableMovementPolicies() {
      return policyService.availablePolicies();
    }

    @Override
    public List<TrainingPresetOption> availableTrainingPresets() {
      return presets;
    }

    @Override
    public void applyTrainingPreset(long presetId) {
      boolean exists = presets.stream().anyMatch(preset -> preset.id() == presetId);
      if (!exists) {
        throw new IllegalArgumentException("Preset not found: " + presetId);
      }
      activePresetId = presetId;
    }

    @Override
    public Long activeTrainingPresetId() {
      return activePresetId;
    }
  }

  private static final class StubMazeCatalogService extends MazeCatalogService {

    private Optional<MazeDefinition> lowCandidate;
    private Optional<MazeDefinition> mediumCandidate;
    private Optional<MazeDefinition> highCandidate;

    private StubMazeCatalogService() {
      super(
          new MazeJsonResourceLoader(new ObjectMapper()),
          new InMemoryMazeRepository(),
          new com.davidpe.scapeai.simulation.MazeDifficultyScorer(),
          "classpath:mazes/*.json");
      MazeDefinition maze =
          new MazeDefinition(2, 2, new boolean[2][2], new GridPosition(0, 0), new GridPosition(1, 1));
      this.lowCandidate = Optional.of(maze);
      this.mediumCandidate = Optional.of(maze);
      this.highCandidate = Optional.of(maze);
    }

    @Override
    public Optional<MazeDefinition> findCandidateByDifficulty(TrainingTargetDifficulty target) {
      return switch (target) {
        case LOW -> lowCandidate;
        case MEDIUM -> mediumCandidate;
        case HIGH -> highCandidate;
      };
    }
  }

  private static final class InMemoryMazeRepository
      implements com.davidpe.scapeai.persistence.repository.MazeRepository {

    @Override
    public com.davidpe.scapeai.persistence.MazeEntity save(
        com.davidpe.scapeai.persistence.MazeEntity maze) {
      return new com.davidpe.scapeai.persistence.MazeEntity(
          1L, maze.name(), maze.rows(), maze.cols(), maze.layout(), maze.difficultyScore());
    }

    @Override
    public com.davidpe.scapeai.persistence.MazeEntity upsertByName(
        com.davidpe.scapeai.persistence.MazeEntity maze) {
      return save(maze);
    }

    @Override
    public Optional<com.davidpe.scapeai.persistence.MazeEntity> findById(long id) {
      return Optional.empty();
    }

    @Override
    public List<com.davidpe.scapeai.persistence.MazeEntity> findAllOrderByDifficulty(boolean ascending) {
      return List.of();
    }
  }

  private static ExplorationBudgetService noOpExplorationBudgetService() {
    ActiveMovementPolicyService policyService =
        new ActiveMovementPolicyService(
            Map.of(
                "heuristic-baseline", context -> MoveDirection.RIGHT,
                "random-controlled", context -> MoveDirection.UP),
            "heuristic-baseline");
    return new ExplorationBudgetService(new InMemoryExplorationBudgetRepository(), policyService, 100, 5);
  }

  private static final class InMemoryExplorationBudgetRepository
      implements ExplorationBudgetRepository {

    @Override
    public Optional<ExplorationBudgetEntity> findByPresetAndPolicy(long presetId, String policyId) {
      return Optional.empty();
    }

    @Override
    public ExplorationBudgetEntity upsert(ExplorationBudgetEntity budget) {
      return new ExplorationBudgetEntity(
          1L,
          budget.presetId(),
          budget.policyId(),
          budget.initialBudget(),
          budget.consumePerEpisode(),
          budget.remainingBudget(),
          budget.updatedAtEpochMillis());
    }
  }
}
