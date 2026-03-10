package com.davidpe.scapeai.application;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.scapeai.ai.DefaultRewardEvaluator;
import com.davidpe.scapeai.ai.SimpleMovementPolicy;
import com.davidpe.scapeai.simulation.GridPosition;
import com.davidpe.scapeai.simulation.MazeDefinition;
import com.davidpe.scapeai.simulation.SingleStepSimulationEngine;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.LongSupplier;
import org.junit.jupiter.api.Test;

class CoverageRegressionSuiteTest {

  private static final List<Long> FIXED_SEEDS = List.of(20260310L, 20260311L, 20260312L, 20260313L);
  private static final double MIN_RIGHT_SIDE_COVERAGE = 0.40;
  private static final double MIN_RIGHT_SIDE_COVERAGE_PER_EPISODE = 0.30;
  private static final int MAX_LOOP_EVENTS = 4;

  @Test
  void shouldMeetRightSideCoverageAndLoopThresholdsAcrossFixedSeeds() {
    MazeDefinition maze = benchmarkMaze();
    double rightCoverageSum = 0.0;
    int worstLoopEvents = 0;

    for (long seed : FIXED_SEEDS) {
      SimulationEpisodeResult result = runHeadlessEpisode(maze, seed);
      rightCoverageSum += result.rightSideCoverage();
      worstLoopEvents = Math.max(worstLoopEvents, result.loopEvents());
      assertTrue(
          result.rightSideCoverage() >= MIN_RIGHT_SIDE_COVERAGE_PER_EPISODE,
          "Right coverage below per-episode threshold for seed " + seed + ": " + result.rightSideCoverage());
    }

    double averageRightCoverage = rightCoverageSum / FIXED_SEEDS.size();
    assertTrue(
        averageRightCoverage >= MIN_RIGHT_SIDE_COVERAGE,
        "Average rightSideCoverage below threshold: " + averageRightCoverage);
    assertTrue(
        worstLoopEvents <= MAX_LOOP_EVENTS,
        "loopEvents exceeded threshold: " + worstLoopEvents);
  }

  private SimulationEpisodeResult runHeadlessEpisode(MazeDefinition maze, long seed) {
    SimulationStepFlow flow = flowWithSimplePolicy();
    LongSupplier deterministicTime = new FixedStepTime(0, 20);
    SimulationEpisodeOrchestrator orchestrator =
        new SimulationEpisodeOrchestrator(
            flow,
            ExperienceTransitionRecorder.noop(),
            Duration.ofMillis(240),
            6,
            deterministicTime,
            () -> seed,
            () -> new Random(seed),
            0.2);
    return orchestrator.runEpisode(maze, Duration.ofMillis(240));
  }

  private SimulationStepFlow flowWithSimplePolicy() {
    var policy = new SimpleMovementPolicy(6);
    ActiveMovementPolicyService policyService =
        new ActiveMovementPolicyService(
            Map.of("heuristic-baseline", policy, "random-controlled", policy), "heuristic-baseline");
    return new SimulationStepFlow(
        policyService, new DefaultRewardEvaluator(), new SingleStepSimulationEngine());
  }

  private MazeDefinition benchmarkMaze() {
    return new MazeDefinition(
        5,
        7,
        new boolean[][] {
          {true, true, true, true, true, true, true},
          {true, false, false, true, false, false, true},
          {true, false, false, false, false, false, true},
          {true, false, false, true, false, false, true},
          {true, true, true, true, true, true, true}
        },
        new GridPosition(2, 1),
        new GridPosition(2, 5));
  }

  private static final class FixedStepTime implements LongSupplier {
    private long current;
    private final long step;

    private FixedStepTime(long start, long step) {
      this.current = start;
      this.step = step;
    }

    @Override
    public long getAsLong() {
      long value = current;
      current += step;
      return value;
    }
  }
}
