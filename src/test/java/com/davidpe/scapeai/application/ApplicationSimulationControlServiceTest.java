package com.davidpe.scapeai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.davidpe.scapeai.ai.MovementPolicy;
import com.davidpe.scapeai.simulation.MoveDirection;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

class ApplicationSimulationControlServiceTest {

  private CapturingPublisher publisher;
  private ActiveMovementPolicyService movementPolicyService;
  private TrainingPresetService trainingPresetService;
  private ApplicationSimulationControlService service;

  @BeforeEach
  void setUp() {
    publisher = new CapturingPublisher();
    MovementPolicy dummyPolicy = context -> MoveDirection.UP;
    movementPolicyService =
        new ActiveMovementPolicyService(
            Map.of("heuristic-baseline", dummyPolicy, "random-controlled", dummyPolicy),
            "heuristic-baseline");
    trainingPresetService = new InMemoryTrainingPresetService();
    service =
        new ApplicationSimulationControlService(
            publisher, movementPolicyService, trainingPresetService);
  }

  @Test
  void shouldPublishStartCommand() {
    service.start();
    assertEquals(SimulationCommand.START, publisher.lastCommand.command());
  }

  @Test
  void shouldPublishPauseCommand() {
    service.pause();
    assertEquals(SimulationCommand.PAUSE, publisher.lastCommand.command());
  }

  @Test
  void shouldPublishResetCommand() {
    service.reset();
    assertEquals(SimulationCommand.RESET, publisher.lastCommand.command());
  }

  @Test
  void shouldSwitchActiveMovementPolicy() {
    service.selectMovementPolicy("random-controlled");
    assertEquals("random-controlled", service.activeMovementPolicy());
  }

  @Test
  void shouldApplyPresetFromControlService() {
    service.applyTrainingPreset(1L);
    assertEquals(1L, service.activeTrainingPresetId());
  }

  private static final class CapturingPublisher implements ApplicationEventPublisher {

    private SimulationCommandEvent lastCommand;

    @Override
    public void publishEvent(Object event) {
      this.lastCommand = (SimulationCommandEvent) event;
    }

    @Override
    public void publishEvent(ApplicationEvent event) {
      this.lastCommand = (SimulationCommandEvent) event.getSource();
    }
  }

  private static final class InMemoryTrainingPresetService implements TrainingPresetService {

    private final TrainingPreset preset =
        new TrainingPreset(1L, 20, Duration.ofMinutes(2), "random-controlled", 7L);
    private TrainingPreset active;

    @Override
    public TrainingPreset save(TrainingPresetDraft preset) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<TrainingPreset> list() {
      return List.of(preset);
    }

    @Override
    public Optional<TrainingPreset> load(long id) {
      return id == 1L ? Optional.of(preset) : Optional.empty();
    }

    @Override
    public Optional<TrainingPreset> apply(long id) {
      Optional<TrainingPreset> loaded = load(id);
      loaded.ifPresent(value -> this.active = value);
      return loaded;
    }

    @Override
    public Optional<TrainingPreset> activePreset() {
      return Optional.ofNullable(active);
    }
  }
}
