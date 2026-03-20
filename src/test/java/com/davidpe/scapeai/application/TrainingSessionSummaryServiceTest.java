package com.davidpe.scapeai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.davidpe.scapeai.persistence.TrainingRunEntity;
import com.davidpe.scapeai.persistence.TrainingRunReplayDiagnosticEntity;
import com.davidpe.scapeai.persistence.repository.TrainingRunRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TrainingSessionSummaryServiceTest {

  @Test
  void shouldAggregateMetricsAndTerminalBreakdownBySession() {
    TrainingSessionSummaryService service = new TrainingSessionSummaryService(new InMemoryRunRepository());

    TrainingSessionSummary summary = service.summarize("session-1");

    assertEquals("session-1", summary.trainingSessionId());
    assertEquals(3, summary.episodesTotal());
    assertEquals(1, summary.successes());
    assertEquals(1.0 / 3.0, summary.successRate());
    assertEquals(1, summary.exitReachedCount());
    assertEquals(1, summary.timeoutCount());
    assertEquals(1, summary.abortedCount());
    assertEquals(0, summary.errorCount());
    assertEquals(4500L, summary.totalDurationMillis());
  }

  @Test
  void shouldReturnZeroSafeSummaryWhenSessionHasNoRuns() {
    TrainingSessionSummaryService service = new TrainingSessionSummaryService(new InMemoryRunRepository());

    TrainingSessionSummary summary = service.summarize("missing");

    assertEquals("missing", summary.trainingSessionId());
    assertEquals(0, summary.episodesTotal());
    assertEquals(0, summary.exitReachedCount());
    assertEquals(0, summary.timeoutCount());
    assertEquals(0, summary.abortedCount());
    assertEquals(0, summary.errorCount());
  }

  private static final class InMemoryRunRepository implements TrainingRunRepository {
    private final List<TrainingRunEntity> rows = new ArrayList<>();

    private InMemoryRunRepository() {
      rows.add(run(1L, "session-1", true, false, "EXIT_REACHED", 1000L, 2.0, 0, 0.60));
      rows.add(run(2L, "session-1", false, true, "TIMEOUT", 2000L, -1.0, 3, 0.35));
      rows.add(run(3L, "session-1", false, false, null, 1500L, -0.5, 2, 0.25));
      rows.add(run(4L, "session-2", false, false, "ERROR", 1100L, -2.0, 4, 0.10));
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
      return rows.stream().filter(run -> trainingSessionId.equals(run.trainingSessionId())).toList();
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

    private static TrainingRunEntity run(
        Long id,
        String sessionId,
        boolean success,
        boolean timeout,
        String terminalReason,
        long elapsed,
        double reward,
        int collisions,
        double coverage) {
      return new TrainingRunEntity(
          id,
          sessionId,
          1L,
          "heuristic-baseline",
          "{}",
          "reward-v1",
          success,
          10,
          elapsed,
          reward,
          collisions,
          7,
          0,
          0.5,
          coverage,
          0.25,
          0.25,
          0.25,
          0.25,
          0.50,
          0.50,
          1.0,
          null,
          null,
          null,
          terminalReason,
          timeout,
          0.0,
          "v1.0.0",
          1_700_000_000_000L);
    }
  }
}
