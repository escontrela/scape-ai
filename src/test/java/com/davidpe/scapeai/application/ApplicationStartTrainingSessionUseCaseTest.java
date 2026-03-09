package com.davidpe.scapeai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.scapeai.simulation.GridPosition;
import com.davidpe.scapeai.simulation.MazeDefinition;
import com.davidpe.scapeai.simulation.MoveDirection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ApplicationStartTrainingSessionUseCaseTest {

  @Test
  void shouldStartWhenMazePolicyAndPresetAreValid() {
    StubSimulationControlService controlService = new StubSimulationControlService();
    ApplicationStartTrainingSessionUseCase useCase =
        new ApplicationStartTrainingSessionUseCase(controlService);
    MazeDefinition maze =
        new MazeDefinition(2, 2, new boolean[2][2], new GridPosition(0, 0), new GridPosition(1, 1));

    StartTrainingSessionResult result =
        useCase.start(new StartTrainingSessionCommand(maze, 1L));

    assertTrue(result.started());
    assertEquals("TRAINING RUNNING", result.message());
    assertTrue(controlService.started);
    assertEquals(Long.valueOf(1L), controlService.activeTrainingPresetId());
  }

  @Test
  void shouldReturnValidationErrorWhenMazeIsMissing() {
    StubSimulationControlService controlService = new StubSimulationControlService();
    ApplicationStartTrainingSessionUseCase useCase =
        new ApplicationStartTrainingSessionUseCase(controlService);

    StartTrainingSessionResult result = useCase.start(new StartTrainingSessionCommand(null, 1L));

    assertFalse(result.started());
    assertEquals("Select a maze before starting.", result.message());
    assertFalse(controlService.started);
  }

  @Test
  void shouldReturnValidationErrorWhenPresetDoesNotExist() {
    StubSimulationControlService controlService = new StubSimulationControlService();
    ApplicationStartTrainingSessionUseCase useCase =
        new ApplicationStartTrainingSessionUseCase(controlService);
    MazeDefinition maze =
        new MazeDefinition(2, 2, new boolean[2][2], new GridPosition(0, 0), new GridPosition(1, 1));

    StartTrainingSessionResult result =
        useCase.start(new StartTrainingSessionCommand(maze, 999L));

    assertFalse(result.started());
    assertEquals("Selected preset does not exist.", result.message());
    assertFalse(controlService.started);
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
}
