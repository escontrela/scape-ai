package com.davidpe.scapeai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.scapeai.ai.MovementPolicy;
import com.davidpe.scapeai.simulation.MoveDirection;
import com.davidpe.scapeai.persistence.repository.TrainingPresetRepository;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ApplicationTrainingPresetServiceTest {

  @Test
  void shouldSaveListLoadAndApplyPresets() {
    TrainingPresetRepository repository = new InMemoryTrainingPresetRepository();
    MovementPolicy dummy = context -> MoveDirection.UP;
    ActiveMovementPolicyService movementPolicyService =
        new ActiveMovementPolicyService(
            Map.of("heuristic-baseline", dummy, "random-controlled", dummy), "heuristic-baseline");
    ApplicationTrainingPresetService service =
        new ApplicationTrainingPresetService(repository, movementPolicyService);

    TrainingPreset saved =
        service.save(
            new TrainingPresetDraft(12, Duration.ofSeconds(30), "random-controlled", 9L));
    var listed = service.list();
    var loaded = service.load(saved.id());
    var applied = service.apply(saved.id());

    assertEquals(12, saved.episodes());
    assertTrue(listed.stream().anyMatch(preset -> preset.id().equals(saved.id())));
    assertTrue(loaded.isPresent());
    assertTrue(applied.isPresent());
    assertEquals("random-controlled", movementPolicyService.activePolicyId());
  }

  private static final class InMemoryTrainingPresetRepository
      implements TrainingPresetRepository {

    private long sequence = 0;
    private final java.util.Map<Long, com.davidpe.scapeai.persistence.TrainingPresetEntity> storage =
        new java.util.LinkedHashMap<>();

    @Override
    public com.davidpe.scapeai.persistence.TrainingPresetEntity save(
        com.davidpe.scapeai.persistence.TrainingPresetEntity preset) {
      long id = ++sequence;
      com.davidpe.scapeai.persistence.TrainingPresetEntity stored =
          new com.davidpe.scapeai.persistence.TrainingPresetEntity(
              id, preset.episodes(), preset.timeoutMillis(), preset.policy(), preset.seed());
      storage.put(id, stored);
      return stored;
    }

    @Override
    public java.util.List<com.davidpe.scapeai.persistence.TrainingPresetEntity> findAll() {
      return new java.util.ArrayList<>(storage.values());
    }

    @Override
    public java.util.Optional<com.davidpe.scapeai.persistence.TrainingPresetEntity> findById(long id) {
      return java.util.Optional.ofNullable(storage.get(id));
    }
  }
}
