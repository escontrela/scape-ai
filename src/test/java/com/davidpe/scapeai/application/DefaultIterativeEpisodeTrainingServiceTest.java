package com.davidpe.scapeai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.scapeai.ai.MovementPolicy;
import com.davidpe.scapeai.ai.DefaultRewardEvaluator;
import com.davidpe.scapeai.ai.RewardAssessment;
import com.davidpe.scapeai.ai.RewardEvaluator;
import com.davidpe.scapeai.ai.RewardSignal;
import com.davidpe.scapeai.simulation.GridPosition;
import com.davidpe.scapeai.simulation.MazeDefinition;
import com.davidpe.scapeai.simulation.MoveDirection;
import com.davidpe.scapeai.simulation.SingleStepSimulationEngine;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class DefaultIterativeEpisodeTrainingServiceTest {

  @Test
  void shouldAggregateEpisodeMetricsAcrossRequestedIterations() {
    SimulationEpisodeOrchestrator orchestrator = orchestratorWithPolicy(context -> MoveDirection.RIGHT);
    DefaultIterativeEpisodeTrainingService service =
        new DefaultIterativeEpisodeTrainingService(orchestrator);
    MazeDefinition maze = new MazeDefinition(1, 2, new boolean[1][2], new GridPosition(0, 0), new GridPosition(0, 1));

    IterativeTrainingSummary summary = service.train(maze, 3, Duration.ofMinutes(1), () -> false);

    assertEquals(3, summary.episodesRequested());
    assertEquals(3, summary.episodesCompleted());
    assertEquals(1.0, summary.successRate());
    assertEquals(1.0, summary.averageReward());
    assertEquals(0.0, summary.averageCollisions());
    assertTrue(summary.averageEpsilonApplied() >= 0.0);
    assertEquals(false, summary.cancelled());
  }

  @Test
  void shouldStopEarlyWhenCancellationIsRequested() {
    SimulationEpisodeOrchestrator orchestrator = orchestratorWithPolicy(context -> MoveDirection.RIGHT);
    DefaultIterativeEpisodeTrainingService service =
        new DefaultIterativeEpisodeTrainingService(orchestrator);
    MazeDefinition maze = new MazeDefinition(1, 2, new boolean[1][2], new GridPosition(0, 0), new GridPosition(0, 1));
    AtomicInteger checks = new AtomicInteger(0);

    IterativeTrainingSummary summary =
        service.train(maze, 5, Duration.ofMinutes(1), () -> checks.incrementAndGet() > 1);

    assertEquals(5, summary.episodesRequested());
    assertEquals(1, summary.episodesCompleted());
    assertTrue(summary.cancelled());
  }

  @Test
  void shouldPublishTimeoutAndFinishedEvents() {
    SimulationEpisodeOrchestrator orchestrator = orchestratorWithPolicy(context -> MoveDirection.RIGHT);
    CapturingEventBus eventBus = new CapturingEventBus();
    DefaultIterativeEpisodeTrainingService service =
        new DefaultIterativeEpisodeTrainingService(orchestrator, eventBus);
    MazeDefinition maze = new MazeDefinition(1, 4, new boolean[1][4], new GridPosition(0, 0), new GridPosition(0, 3));

    service.train(maze, 1, Duration.ZERO, () -> false);

    assertEquals(
        List.of(TrainingLifecycleEventType.TIMED_OUT, TrainingLifecycleEventType.FINISHED),
        eventBus.types());
  }

  @Test
  void shouldImproveCoverageWeightedRewardWithoutReducingSuccessRate() {
    MovementPolicy policy = context -> MoveDirection.RIGHT;
    MazeDefinition maze = new MazeDefinition(1, 3, new boolean[1][3], new GridPosition(0, 0), new GridPosition(0, 2));
    DefaultIterativeEpisodeTrainingService legacyService =
        new DefaultIterativeEpisodeTrainingService(
            orchestratorWithPolicyAndEvaluator(policy, context -> RewardAssessment.of(RewardSignal.NEGATIVE)));
    DefaultIterativeEpisodeTrainingService shapedService =
        new DefaultIterativeEpisodeTrainingService(
            orchestratorWithPolicyAndEvaluator(policy, new DefaultRewardEvaluator()));

    IterativeTrainingSummary legacy = legacyService.train(maze, 5, Duration.ofMinutes(1), () -> false);
    IterativeTrainingSummary shaped = shapedService.train(maze, 5, Duration.ofMinutes(1), () -> false);

    assertEquals(legacy.successRate(), shaped.successRate());
    assertTrue(shaped.averageReward() >= legacy.averageReward());
  }

  private SimulationEpisodeOrchestrator orchestratorWithPolicy(MovementPolicy policy) {
    return orchestratorWithPolicyAndEvaluator(
        policy, context -> RewardAssessment.of(RewardSignal.POSITIVE));
  }

  private SimulationEpisodeOrchestrator orchestratorWithPolicyAndEvaluator(
      MovementPolicy policy, RewardEvaluator rewardEvaluator) {
    ActiveMovementPolicyService policyService =
        new ActiveMovementPolicyService(
            Map.of("heuristic-baseline", policy, "random-controlled", policy), "heuristic-baseline");
    SimulationStepFlow flow =
        new SimulationStepFlow(policyService, rewardEvaluator, new SingleStepSimulationEngine());
    return new SimulationEpisodeOrchestrator(flow, Duration.ofMinutes(5), System::currentTimeMillis);
  }

  private static final class CapturingEventBus implements TrainingLifecycleEventBus {

    private final List<TrainingLifecycleEventType> types = new ArrayList<>();

    @Override
    public void publish(TrainingLifecycleEvent event) {
      types.add(event.type());
    }

    @Override
    public Subscription subscribe(java.util.function.Consumer<TrainingLifecycleEvent> listener) {
      return () -> {};
    }

    private List<TrainingLifecycleEventType> types() {
      return List.copyOf(types);
    }
  }
}
