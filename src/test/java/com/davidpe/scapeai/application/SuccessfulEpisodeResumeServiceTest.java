package com.davidpe.scapeai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.scapeai.persistence.TrainingRunEntity;
import com.davidpe.scapeai.persistence.repository.TrainingRunRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SuccessfulEpisodeResumeServiceTest {

  @Test
  void shouldReturnOnlySuccessfulEpisodesOrderedByRecency() {
    TrainingRunEntity newestSuccess = run(12L, "session-a", true, "EXIT_REACHED", 5000L, "0,0R");
    TrainingRunEntity timeout = run(13L, "session-a", false, "TIMEOUT", 6000L, "0,0R");
    TrainingRunEntity olderSuccess = run(10L, "session-a", true, "EXIT_REACHED", 3000L, "0,0R");

    SuccessfulEpisodeResumeService service =
        new SuccessfulEpisodeResumeService(
            new StubTrainingRunRepository(List.of(newestSuccess, olderSuccess), List.of(timeout)));

    List<SuccessfulEpisodeReplay> replays = service.listByTrainingSession("session-a", 10);

    assertEquals(2, replays.size());
    assertEquals(12L, replays.get(0).trainingRunId());
    assertEquals("EXIT_REACHED", replays.get(0).terminalReason());
    assertTrue(replays.get(0).trajectory().size() >= 1);
    assertEquals(10L, replays.get(1).trainingRunId());
  }

  private static TrainingRunEntity run(
      long id,
      String session,
      boolean success,
      String terminal,
      long createdAt,
      String trajectoryPath) {
    return new TrainingRunEntity(
        id,
        session,
        1L,
        "heuristic-baseline",
        "{}",
        "v1",
        success,
        10,
        1000,
        success ? 3.0 : -1.0,
        1,
        8,
        success ? 0 : 4,
        success ? 2.0 : -1.0,
        0.4,
        0.3,
        0.3,
        0.2,
        0.2,
        0.4,
        0.6,
        0.7,
        "[]",
        "{\"seed\":1}",
        trajectoryPath,
        "",
        terminal,
        "TIMEOUT".equals(terminal),
        60.0,
        "v1.0.0",
        createdAt);
  }

  private static final class StubTrainingRunRepository implements TrainingRunRepository {

    private final List<TrainingRunEntity> successes;
    private final List<TrainingRunEntity> others;

    private StubTrainingRunRepository(List<TrainingRunEntity> successes, List<TrainingRunEntity> others) {
      this.successes = successes;
      this.others = others;
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
      return others;
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
    public Optional<com.davidpe.scapeai.persistence.TrainingRunReplayDiagnosticEntity>
        findReplayDiagnosticByTrainingRunId(long trainingRunId) {
      return Optional.empty();
    }

    @Override
    public List<com.davidpe.scapeai.persistence.TrainingRunReplayDiagnosticEntity>
        findRecentReplayDiagnostics(int limit) {
      return List.of();
    }

    @Override
    public List<TrainingRunEntity> findSuccessfulByTrainingSessionId(String trainingSessionId, int limit) {
      return successes;
    }
  }
}
