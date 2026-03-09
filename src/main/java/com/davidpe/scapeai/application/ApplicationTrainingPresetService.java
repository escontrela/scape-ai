package com.davidpe.scapeai.application;

import com.davidpe.scapeai.persistence.TrainingPresetEntity;
import com.davidpe.scapeai.persistence.repository.TrainingPresetRepository;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;

@Service
public class ApplicationTrainingPresetService implements TrainingPresetService {

  private final TrainingPresetRepository repository;
  private final ActiveMovementPolicyService movementPolicyService;
  private final AtomicReference<TrainingPreset> activePreset = new AtomicReference<>();

  public ApplicationTrainingPresetService(
      TrainingPresetRepository repository, ActiveMovementPolicyService movementPolicyService) {
    this.repository = repository;
    this.movementPolicyService = movementPolicyService;
  }

  @PostConstruct
  void ensureDefaultPresets() {
    if (!repository.findAll().isEmpty()) {
      return;
    }
    repository.save(new TrainingPresetEntity(null, 25, Duration.ofMinutes(5).toMillis(), "heuristic-baseline", null));
    repository.save(new TrainingPresetEntity(null, 40, Duration.ofMinutes(3).toMillis(), "random-controlled", 20260309L));
  }

  @Override
  public TrainingPreset save(TrainingPresetDraft preset) {
    if (preset.episodes() <= 0) {
      throw new IllegalArgumentException("episodes must be greater than zero");
    }
    if (preset.timeout() == null || preset.timeout().isZero() || preset.timeout().isNegative()) {
      throw new IllegalArgumentException("timeout must be positive");
    }
    TrainingPresetEntity saved =
        repository.save(
            new TrainingPresetEntity(
                null,
                preset.episodes(),
                preset.timeout().toMillis(),
                preset.policy(),
                preset.seed()));
    return toPreset(saved);
  }

  @Override
  public List<TrainingPreset> list() {
    return repository.findAll().stream().map(this::toPreset).toList();
  }

  @Override
  public Optional<TrainingPreset> load(long id) {
    return repository.findById(id).map(this::toPreset);
  }

  @Override
  public Optional<TrainingPreset> apply(long id) {
    Optional<TrainingPreset> preset = load(id);
    preset.ifPresent(
        value -> {
          movementPolicyService.selectPolicy(value.policy());
          activePreset.set(value);
        });
    return preset;
  }

  @Override
  public Optional<TrainingPreset> activePreset() {
    return Optional.ofNullable(activePreset.get());
  }

  private TrainingPreset toPreset(TrainingPresetEntity entity) {
    return new TrainingPreset(
        entity.id(),
        entity.episodes(),
        Duration.ofMillis(entity.timeoutMillis()),
        entity.policy(),
        entity.seed());
  }
}
