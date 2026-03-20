package com.davidpe.scapeai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.scapeai.persistence.TrainingRunEntity;
import com.davidpe.scapeai.persistence.TrainingRunReplayDiagnosticEntity;
import com.davidpe.scapeai.persistence.repository.TrainingRunRepository;
import com.davidpe.scapeai.simulation.GridPosition;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TrainingEpisodeDetailServiceTest {

  @Test
  void shouldLoadDecodedTrajectoryAndFinalPositionFromStoredEpisode() {
    String encodedTrajectory =
        EpisodeTrajectoryCodec.encode(
            List.of(
                new GridPosition(0, 0), new GridPosition(0, 1), new GridPosition(1, 1)));
    TrainingEpisodeDetailService service =
        new TrainingEpisodeDetailService(new OneRunRepository(encodedTrajectory));

    Optional<TrainingEpisodeDetail> detail = service.findByTrainingRunId(9L);

    assertTrue(detail.isPresent());
    assertEquals(3, detail.get().trajectory().size());
    assertEquals(new GridPosition(1, 1), detail.get().finalPosition());
  }

  private static final class OneRunRepository implements TrainingRunRepository {
    private final String trajectoryPath;

    private OneRunRepository(String trajectoryPath) {
      this.trajectoryPath = trajectoryPath;
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
      return List.of();
    }

    @Override
    public Optional<TrainingRunEntity> findById(long trainingRunId) {
      return Optional.of(
          new TrainingRunEntity(
              trainingRunId,
              "session-1",
              1L,
              "heuristic-baseline",
              "{}",
              "v1",
              true,
              4,
              1200L,
              1.5,
              0,
              4,
              0,
              1.0,
              0.8,
              0.2,
              0.2,
              0.2,
              0.2,
              0.4,
              0.6,
              1.0,
              "[]",
              "{\"expectedFinalRow\":1,\"expectedFinalCol\":1}",
              trajectoryPath,
              "0:0:2;0:1:1;1:1:1",
              "EXIT_REACHED",
              false,
              0.0,
              "v1.0.0",
              1_700_000_000_000L));
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
