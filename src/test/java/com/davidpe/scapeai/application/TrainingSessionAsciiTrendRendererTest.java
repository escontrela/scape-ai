package com.davidpe.scapeai.application;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.scapeai.persistence.TrainingRunEntity;
import com.davidpe.scapeai.persistence.TrainingRunReplayDiagnosticEntity;
import com.davidpe.scapeai.persistence.repository.TrainingRunRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TrainingSessionAsciiTrendRendererTest {

  @Test
  void shouldRenderShortSessionWithOutcomeAndRewardRows() {
    TrainingSessionAsciiTrendRenderer renderer =
        new TrainingSessionAsciiTrendRenderer(new SessionRunsRepository(4, -1.0, 2.0));

    String graph = renderer.renderForSession("session-short");

    assertTrue(graph.contains("OUTCOME"));
    assertTrue(graph.contains("REWARD"));
    assertTrue(graph.contains("COVERAGE"));
    assertTrue(graph.contains("episodes=4"));
  }

  @Test
  void shouldRenderLongSessionWithoutTruncationArtifacts() {
    TrainingSessionAsciiTrendRenderer renderer =
        new TrainingSessionAsciiTrendRenderer(new SessionRunsRepository(48, -5.0, 8.0));

    String graph = renderer.renderForSession("session-long");

    assertTrue(graph.contains("episodes=48"));
    assertTrue(graph.contains("OUTCOME  "));
    assertTrue(graph.lines().anyMatch(line -> line.startsWith("REWARD   ")));
  }

  @Test
  void shouldHandleExtremeRewardSpread() {
    TrainingSessionAsciiTrendRenderer renderer =
        new TrainingSessionAsciiTrendRenderer(new SessionRunsRepository(10, -1_000.0, 5_000.0));

    String graph = renderer.renderForSession("session-extreme");

    assertTrue(graph.contains("min=-1000.00"));
    assertTrue(graph.contains("max=5000.00"));
  }

  private static final class SessionRunsRepository implements TrainingRunRepository {
    private final List<TrainingRunEntity> runs = new ArrayList<>();

    private SessionRunsRepository(int episodes, double minReward, double maxReward) {
      int size = Math.max(1, episodes);
      for (int i = 0; i < size; i++) {
        double ratio = size == 1 ? 0.0 : (double) i / (double) (size - 1);
        double reward = minReward + ((maxReward - minReward) * ratio);
        String reason = i % 6 == 0 ? "TIMEOUT" : (i % 5 == 0 ? "ERROR" : (i % 2 == 0 ? "EXIT_REACHED" : "ABORTED"));
        runs.add(
            new TrainingRunEntity(
                (long) i + 1,
                "session-short",
                1L,
                "heuristic-baseline",
                "{}",
                "v1",
                "EXIT_REACHED".equals(reason),
                10 + i,
                1_000L + i,
                reward,
                i % 4,
                5 + i,
                0,
                1.0,
                Math.min(1.0, 0.1 + (ratio * 0.9)),
                0.25,
                0.25,
                0.25,
                0.25,
                0.5,
                0.5,
                1.0,
                "[]",
                "{}",
                "0,0R",
                "0:0:1",
                reason,
                "TIMEOUT".equals(reason),
                0.0,
                "v1.0.0",
                1_700_000_000_000L + i));
      }
    }

    @Override
    public TrainingRunEntity save(TrainingRunEntity run) {
      return run;
    }

    @Override
    public List<TrainingRunEntity> findByMazeId(long mazeId) {
      return List.of();
    }

    @Override
    public List<TrainingRunEntity> findRecentByMazeId(long mazeId, int limit) {
      return List.of();
    }

    @Override
    public List<TrainingRunEntity> findByTrainingSessionId(String trainingSessionId) {
      return runs;
    }

    @Override
    public List<TrainingRunEntity> findSuccessfulByTrainingSessionId(String trainingSessionId, int limit) {
      return runs.stream()
          .filter(run -> run.success() && "EXIT_REACHED".equals(run.terminalReason()))
          .limit(Math.max(1, limit))
          .toList();
    }

    @Override
    public Optional<TrainingRunEntity> findById(long trainingRunId) {
      return Optional.empty();
    }

    @Override
    public List<CellVisitFrequency> findAccumulatedCellVisitsByMazeId(long mazeId, int limit) {
      return List.of();
    }

    @Override
    public Optional<TrainingRunReplayDiagnosticEntity> findReplayDiagnosticByTrainingRunId(
        long trainingRunId) {
      return Optional.empty();
    }

    @Override
    public List<TrainingRunReplayDiagnosticEntity> findRecentReplayDiagnostics(int limit) {
      return List.of();
    }
  }
}
